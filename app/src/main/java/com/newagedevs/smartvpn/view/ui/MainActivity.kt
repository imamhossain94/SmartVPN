package com.newagedevs.smartvpn.view.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.net.VpnService
import android.os.Bundle
import android.os.RemoteException
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.app.ShareCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance
import androidx.multidex.MultiDex
import androidx.recyclerview.widget.RecyclerView
import com.hjq.bar.OnTitleBarListener
import com.hjq.bar.TitleBar
import com.newagedevs.smartvpn.R
import com.newagedevs.smartvpn.databinding.ActivityMainBinding
import com.newagedevs.smartvpn.extensions.isNetworkConnected
import com.newagedevs.smartvpn.interfaces.ChangeServer
import com.newagedevs.smartvpn.model.CustomItem
import com.newagedevs.smartvpn.utils.ItemUtils
import com.newagedevs.smartvpn.model.VpnServer
import com.newagedevs.smartvpn.preferences.SharedPrefRepository
import com.newagedevs.smartvpn.utils.Constants
import com.newagedevs.smartvpn.utils.Constants.Companion.ServerStatus
import com.newagedevs.smartvpn.utils.Constants.Companion.VPN_REQUEST_ID
import com.newagedevs.smartvpn.view.CustomListBalloonFactory
import com.newagedevs.smartvpn.view.adapter.CustomAdapter
import com.newagedevs.smartvpn.view.adapter.FavoriteServerAdapter
import com.newagedevs.smartvpn.view.adapter.ServerAdapter
import com.newagedevs.smartvpn.view.dialog.BaseDialog
import com.newagedevs.smartvpn.view.dialog.MessageDialog
import com.skydoves.balloon.Balloon
import com.skydoves.bindables.BindingActivity
import de.blinkt.openvpn.VpnProfile
import de.blinkt.openvpn.core.ConfigParser
import de.blinkt.openvpn.core.ConfigParser.ConfigParseError
import de.blinkt.openvpn.core.OpenVPNService
import de.blinkt.openvpn.core.OpenVPNThread
import de.blinkt.openvpn.core.ProfileManager
import de.blinkt.openvpn.core.VPNLaunchHelper
import org.json.JSONObject
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import java.io.IOException
import java.io.StringReader
import java.util.Locale


class MainActivity : BindingActivity<ActivityMainBinding>(R.layout.activity_main), CustomAdapter.CustomViewHolder.Delegate, ChangeServer {

    private var attached = true
    var vpnStart = false

    private var vpnProfile: VpnProfile? = null
    lateinit var customListBalloon: Balloon

    private val sharedPrefRepository: SharedPrefRepository by inject { parametersOf(this)  }
    private val serverAdapter: ServerAdapter by inject { parametersOf(this)  }
    private val favoriteServerAdapter: FavoriteServerAdapter by inject { parametersOf(this)  }
    private val viewModel: MainViewModel by viewModel { parametersOf(sharedPrefRepository, serverAdapter, favoriteServerAdapter) }

    private val customAdapter by lazy { CustomAdapter(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding {
            vm = viewModel
        }

        customListBalloon = CustomListBalloonFactory().create(this, this)
        val listRecycler: RecyclerView = customListBalloon.getContentView().findViewById(R.id.list_recyclerView)
        listRecycler.adapter = customAdapter
        customAdapter.addCustomItem(ItemUtils.getCustomSamples(this))

        binding.tbMainBar.setOnTitleBarListener(object : OnTitleBarListener {
            override fun onLeftClick(titleBar: TitleBar) {
                customListBalloon.showAlignBottom(titleBar.leftView, 0, 0)
            }

            override fun onTitleClick(titleBar: TitleBar) { }

            override fun onRightClick(titleBar: TitleBar) {

                if (vpnStart) {
                    MessageDialog.Builder(this@MainActivity)
                        .setTitle("Change VPN Server")
                        .setMessage("Are you sure you want to change the VPN server connection? Your current session will be disconnected.")
                        .setConfirm(getString(R.string.confirm))
                        .setCancel(getString(R.string.cancel))
                        .setListener(object : MessageDialog.OnListener {
                            override fun onConfirm(dialog: BaseDialog?) {
                                if(stopVpn()){
                                    startActivity(Intent(this@MainActivity, ServerPickerActivity::class.java))
                                }
                            }
                            override fun onCancel(dialog: BaseDialog?) {  }
                        }).show()
                }else {
                    startActivity(Intent(this@MainActivity, ServerPickerActivity::class.java))
                }
            }
        })

        onBackPressedDispatcher.addCallback(this@MainActivity, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                MessageDialog.Builder(this@MainActivity)
                    .setTitle("Exit Confirmation")
                    .setMessage("Are you sure you want to exit the app?")
                    .setConfirm(getString(R.string.confirm))
                    .setCancel(getString(R.string.cancel))
                    .setListener(object : MessageDialog.OnListener {
                        override fun onConfirm(dialog: BaseDialog?) {
                            finish()
                        }
                        override fun onCancel(dialog: BaseDialog?) {  }
                    }).show()
            }
        })

        getInstance(this).registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val stage = intent.getStringExtra("state")
                stage?.let { setStage(it) }

                try {
                    var duration = intent.getStringExtra("duration")
                    var lastPacketReceive = intent.getStringExtra("lastPacketReceive")
                    val byteIn = intent.getStringExtra("byteIn")
                    val byteOut = intent.getStringExtra("byteOut")
                    if (duration == null) duration = "00:00:00"
                    if (lastPacketReceive == null) lastPacketReceive = "0"

                    binding.tvServerBytesIn.text = byteIn?.replace(byteIn.first().toString(), "") ?: "0.0 kB - 0.0 B/s"
                    binding.tvServerBytesOut.text = byteOut?.replace(byteOut.first().toString(), "") ?: "0.0 kB - 0.0 B/s"
                    binding.tvServerDuration.text = duration

                    binding.tvServerIpAddress.text = viewModel.selectedServer?.ip

                    val jsonObject = JSONObject()
                    jsonObject.put("duration", duration)
                    jsonObject.put("last_packet_receive", lastPacketReceive)
                    jsonObject.put("byte_in", byteIn)
                    jsonObject.put("byte_out", byteOut)
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }
            }
        }, IntentFilter("connectionState"))

        binding.ivConnectServerButton.setOnClickListener {
            if (vpnStart) {
                MessageDialog.Builder(this@MainActivity)
                    .setTitle("Cancel VPN Connection")
                    .setMessage("Are you sure you want to cancel the current VPN connection?")
                    .setConfirm(getString(R.string.confirm))
                    .setCancel(getString(R.string.cancel))
                    .setListener(object : MessageDialog.OnListener {
                        override fun onConfirm(dialog: BaseDialog?) {
                            stopVpn()
                        }
                        override fun onCancel(dialog: BaseDialog?) {  }
                    }).show()
            }else {
                viewModel.selectedServer?.let {
                    viewModel.connectToVPN()
                    prepareVPN()
                }
            }

        }

        setStage(OpenVPNService.getStatus())

        viewModel.selectedServer?.let {
            if (viewModel.sharedPref.isFavoriteVpnServer(it)) {
                binding.favoriteServerIvIcon.setImageResource(R.drawable.ic_heart_field)
            } else {
                binding.favoriteServerIvIcon.setImageResource(R.drawable.ic_heart_outlined)
            }
        }
    }

    fun onChangeServerClicked(view: View) {
        Toast.makeText(this, "Change Server", Toast.LENGTH_SHORT).show()
    }

    fun onFavServerClicked(view: View) {
        viewModel.selectedServer?.let {
            if (viewModel.sharedPref.isFavoriteVpnServer(it)) {
                viewModel.sharedPref.removeFromFavoriteVpnServers(it)
                (view as ImageView).setImageResource(R.drawable.ic_heart_outlined)
            } else {
                viewModel.sharedPref.addToFavoriteVpnServers(it)
                (view as ImageView).setImageResource(R.drawable.ic_heart_field)
            }
        }
    }


    private fun prepareVPN() {
        if (!vpnStart) {
            if (isNetworkConnected()) {
                setStage("PREPARE")
                try {
                    val configParser = ConfigParser()
                    configParser.parseConfig(StringReader(viewModel.selectedServerConfig?.config.toString()))
                    vpnProfile = configParser.convertProfile()
                } catch (e: IOException) {
                    e.printStackTrace()
                } catch (configParseError: ConfigParseError) {
                    configParseError.printStackTrace()
                }
                val vpnIntent = VpnService.prepare(this)
                if (vpnIntent != null) startActivityForResult(vpnIntent, VPN_REQUEST_ID) else startVPN()
            } else {
                setStage("NONETWORK")
            }
        }else if (stopVpn()) {
            // VPN is stopped, show a Toast message.
            Toast.makeText(this@MainActivity, "Disconnect Successfully", Toast.LENGTH_SHORT).show()
        }
    }


    private fun startVPN() {
        try {
            if(vpnProfile == null) {
                throw RemoteException("Unable to launch")
            }

            vpnProfile?.let {
                setStage("CONNECTING")
                if (it.checkProfile(this) != de.blinkt.openvpn.R.string.no_error_found) {
                    throw RemoteException(getString(it.checkProfile(this)))
                }

                viewModel.selectedServerConfig?.let {config ->
                    it.mName = config.country
                    it.mProfileCreator = packageName
                    it.mUsername = config.username
                    it.mPassword = config.password
                    it.mDNS1 = config.dns1
                    it.mDNS2 = config.dns1

                    if (config.dns1 != null && config.dns2 != null) {
                        it.mOverrideDNS = true
                    }
                    config.bypassPackages?.let { bypassPackages ->
                        it.mAllowedAppsVpn.addAll(bypassPackages)
                        it.mAllowAppVpnBypass = true
                    }
                }

                ProfileManager.setTemporaryProfile(this, vpnProfile)
                VPNLaunchHelper.startOpenVpn(vpnProfile, this)

                vpnStart = true
            }
        } catch (e: RemoteException) {
            setStage("DISCONNECTED")
            e.printStackTrace()
        }
    }

    private fun stopVpn(): Boolean {
        try {
            OpenVPNThread.stop()
            setStage("DISCONNECTED")
            status("connect")
            vpnStart = false
            return true
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }


    override fun onCustomItemClick(customItem: CustomItem) {
        this.customListBalloon.dismiss()
        when (customItem.title) {
            getString(R.string.network_info) -> {
                startActivity(Intent(this@MainActivity, NetworkInfoActivity::class.java))
            }
            getString(R.string.favorite_server) -> {
                startActivity(Intent(this@MainActivity, FavoriteServerPickerActivity::class.java))
            }
            getString(R.string.share) -> {
                ShareCompat.IntentBuilder(this@MainActivity)
                    .setType("text/plain")
                    .setChooserTitle("Share ${getString(R.string.app_name)} with:")
                    .setText(Constants.appStoreBaseURL + this.packageName)
                    .startChooser()
            }
            getString(R.string.rate_us) -> {
                MessageDialog.Builder(this@MainActivity)
                    .setTitle("Enjoying ${getString(R.string.app_name)}?")
                    .setMessage("If you like our app, please give it a 5 stars rating in Google Play, thank you.")
                    .setConfirm(getString(R.string.rate))
                    .setCancel(getString(R.string.cancel))
                    .setListener(object : MessageDialog.OnListener {
                        override fun onConfirm(dialog: BaseDialog?) {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Constants.appStoreId)))
                        }
                        override fun onCancel(dialog: BaseDialog?) {  }
                    }).show()
            }
            getString(R.string.about) -> {
                startActivity(Intent(this@MainActivity, AboutActivity::class.java))
//                AboutDialog.Builder(this@MainActivity)
//                    .setCancelable(true)
//                    .setCanceledOnTouchOutside(true)
//                    .setTitle("About ${getString(R.string.app_name)}")
//                    .setVersionName(BuildConfig.VERSION_NAME)
//                    .setDescription("Enhances your online privacy and security with an ultra-fast connection, providing continuous safeguarding for your digital experience.")
//                    .setListener(object : AboutDialog.OnListener {
//                        override fun onClick(dialog: BaseDialog?) {
//                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
//                            intent.setData(Uri.parse("package:$packageName"))
//                            startActivity(intent)
//                        }
//                    }).show()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == VPN_REQUEST_ID) {
            if (resultCode == RESULT_OK) {
                startVPN()
            } else {
                setStage("denied")
                Toast.makeText(this, "Permission is denied!", Toast.LENGTH_SHORT).show()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase)
        MultiDex.install(this)
    }

    override fun onDetachedFromWindow() {
        attached = false
        super.onDetachedFromWindow()
    }

    override fun newServer(server: VpnServer?) {
        viewModel.selectedServer = server
        server?.let {
            if (viewModel.sharedPref.isFavoriteVpnServer(it)) {
                binding.favoriteServerIvIcon.setImageResource(R.drawable.ic_heart_field)
            } else {
                binding.favoriteServerIvIcon.setImageResource(R.drawable.ic_heart_outlined)
            }
            viewModel.sharedPref.saveSelectedVpnServer(it)
        }
    }

    private fun setStage(stage: String) {
        if (attached){
            when (stage.uppercase(Locale.getDefault())) {
                ServerStatus.CONNECTED -> {
                    vpnStart = true
                    status("connected")
                    binding.ivConnectServerButton.setImageResource(R.drawable.ic_power_connected)
                }
                ServerStatus.DISCONNECTED -> {
                    vpnStart = false
                    binding.ivConnectServerButton.setImageResource(R.drawable.ic_power_not_connected)
                    status("connect")
                    binding.tvServerStatusLog.setText("")
                }
                ServerStatus.WAIT -> {
                    binding.ivConnectServerButton.setImageResource(R.drawable.ic_power_connecting)
                    binding.tvServerStatusLog.setText("waiting for server connection!!")
                }
                ServerStatus.AUTH -> {
                    binding.ivConnectServerButton.setImageResource(R.drawable.ic_power_connecting)
                    binding.tvServerStatusLog.setText("server authenticating!!")
                }
                ServerStatus.RECONNECTING -> {
                    binding.ivConnectServerButton.setImageResource(R.drawable.ic_power_connecting)
                    status("connecting")
                    binding.tvServerStatusLog.setText("Reconnecting...")
                }
                ServerStatus.NO_NETWORK -> {
                    binding.ivConnectServerButton.setImageResource(R.drawable.ic_power_not_connected)
                    binding.tvServerStatusLog.setText("No network connection")
                }
                ServerStatus.CONNECTING -> {
                    binding.ivConnectServerButton.setImageResource(R.drawable.ic_power_connecting)
                    status("connecting")
                }
                ServerStatus.PREPARE -> {
                    binding.ivConnectServerButton.setImageResource(R.drawable.ic_power_connecting)
                }
                ServerStatus.DENIED -> {
                    binding.ivConnectServerButton.setImageResource(R.drawable.ic_power_not_connected)
                }
            }
        }
    }

    fun status(status: String) {
        if (status == "connect") {
            binding.tvServerStatus.setText(getString(R.string.connect))
        } else if (status == "connecting") {
            binding.tvServerStatus.setText(getString(R.string.connecting))
        } else if (status == "connected") {
            binding.tvServerStatus.setText(getString(R.string.disconnect))
        } else if (status == "tryDifferentServer") {
            binding.tvServerStatus.setText("Try Different\nServer")
        } else if (status == "loading") {
            binding.tvServerStatus.setText("Loading Server..")
        } else if (status == "invalidDevice") {
            binding.tvServerStatus.setText("Invalid Device")
        } else if (status == "authenticationCheck") {
            binding.tvServerStatus.setText("Authentication \n Checking...")
        }
    }


}