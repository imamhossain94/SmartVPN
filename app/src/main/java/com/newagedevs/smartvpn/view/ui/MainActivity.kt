package com.newagedevs.smartvpn.view.ui


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.os.RemoteException
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ShareCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager.*
import androidx.recyclerview.widget.RecyclerView
import com.hjq.bar.OnTitleBarListener
import com.hjq.bar.TitleBar
import com.newagedevs.smartvpn.BuildConfig
import com.newagedevs.smartvpn.R
import com.newagedevs.smartvpn.databinding.ActivityMainBinding
import com.newagedevs.smartvpn.interfaces.ChangeServer
import com.newagedevs.smartvpn.model.CustomItem
import com.newagedevs.smartvpn.model.ItemUtils
import com.newagedevs.smartvpn.model.Server
import com.newagedevs.smartvpn.model.VpnServer
import com.newagedevs.smartvpn.utils.CheckInternetConnection
import com.newagedevs.smartvpn.utils.Constants
import com.newagedevs.smartvpn.utils.NotificationUtil
import com.newagedevs.smartvpn.utils.SharedPreference
import com.newagedevs.smartvpn.view.CustomListBalloonFactory
import com.newagedevs.smartvpn.view.adapter.CustomAdapter
import com.newagedevs.smartvpn.view.adapter.ServerAdapter
import com.newagedevs.smartvpn.view.dialog.AboutDialog
import com.newagedevs.smartvpn.view.dialog.BaseDialog
import com.newagedevs.smartvpn.view.dialog.MessageDialog
import com.skydoves.balloon.Balloon
import com.skydoves.bindables.BindingActivity
import de.blinkt.openvpn.VpnProfile
import de.blinkt.openvpn.core.ConfigParser
import de.blinkt.openvpn.core.OpenVPNService
import de.blinkt.openvpn.core.ProfileManager
import de.blinkt.openvpn.core.VPNLaunchHelper
import org.json.JSONObject
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.StringReader
import java.util.Locale


class MainActivity : BindingActivity<ActivityMainBinding>(R.layout.activity_main), CustomAdapter.CustomViewHolder.Delegate, ChangeServer {

    private val serverAdapter: ServerAdapter by inject { parametersOf(this)  }
    private val viewModel: MainViewModel by viewModel { parametersOf(serverAdapter) }

    private val customAdapter by lazy { CustomAdapter(this) }
    lateinit var customListBalloon: Balloon

    private var server: Server? = null
    private var connection: CheckInternetConnection? = null

    var vpnStart = false
    private var preference: SharedPreference? = null


    private val VPN_REQUEST_ID = 1
    private val TAG = "NVPN"

    private var vpnProfile: VpnProfile? = null

    private var config = ""
    private var username = ""
    private var password = ""
    private var name = ""
    private var dns1 = VpnProfile.DEFAULT_DNS1
    private var dns2 = VpnProfile.DEFAULT_DNS2

    private var bypassPackages: ArrayList<String>? = null

    private val attached = true

    private var localJson: JSONObject? = null

    private lateinit var notificationUtil: NotificationUtil
    private lateinit var notificationPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding {
            vm = viewModel
        }

        notificationUtil = NotificationUtil(this)

        customListBalloon = CustomListBalloonFactory().create(this, this)

        // gets customListBalloon's recyclerView.
        val listRecycler: RecyclerView = customListBalloon.getContentView().findViewById(R.id.list_recyclerView)

        listRecycler.adapter = customAdapter
        customAdapter.addCustomItem(ItemUtils.getCustomSamples(this))


        binding.tbMainBar.setOnTitleBarListener(object : OnTitleBarListener {
            override fun onLeftClick(titleBar: TitleBar) {
                customListBalloon.showAlignBottom(titleBar.leftView, 0, 0)
            }

            override fun onTitleClick(titleBar: TitleBar) {



            }

            override fun onRightClick(titleBar: TitleBar) {
                onChangeCountryClicked(titleBar.rightView)
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




        initializeAll()

        binding.ivConnectServerButton.setOnClickListener {


            val conf: InputStream = this.assets.open("japan.ovpn")
            val isr = InputStreamReader(conf)
            val br = BufferedReader(isr)
            config = ""
            var line: String?
            while (true) {
                line = br.readLine()
                if (line == null) break
                config += line + "\n"
            }
            br.readLine()

            name = "Japan"
            username = "vpn"
            password = "vpn"
            dns1 = ""
            dns2 = ""
            bypassPackages = null
            prepareVPN()

            Toast.makeText(this, "Started ${config}", Toast.LENGTH_SHORT).show()

        }

        getInstance(this).registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val stage = intent.getStringExtra("state")
                stage?.let { setStage(it) }

                try {
                    var duration = intent.getStringExtra("duration")
                    var lastPacketReceive = intent.getStringExtra("lastPacketReceive")
                    var byteIn = intent.getStringExtra("byteIn")
                    var byteOut = intent.getStringExtra("byteOut")
                    if (duration == null) duration = "00:00:00"
                    if (lastPacketReceive == null) lastPacketReceive = "0"
                    if (byteIn == null) byteIn = " "
                    if (byteOut == null) byteOut = " "
                    val jsonObject = JSONObject()
                    jsonObject.put("duration", duration)
                    jsonObject.put("last_packet_receive", lastPacketReceive)
                    jsonObject.put("byte_in", byteIn)
                    jsonObject.put("byte_out", byteOut)
                    localJson = jsonObject

                    if (attached) {
                        // success
                        jsonObject.toString()
                    }
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }

            }
        }, IntentFilter("connectionState"))



        notificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            if(!notificationUtil.isPermissionGranted()) {
                Toast.makeText(this, "Post notification permission not granted", Toast.LENGTH_SHORT).show()
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationUtil.requestPermission(notificationPermissionLauncher)
        }

    }

    override fun onCustomItemClick(customItem: CustomItem) {
        this.customListBalloon.dismiss()


        when (customItem.title) {
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
            getString(R.string.other_app) -> {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Constants.publisherName)))
            }
            getString(R.string.privacy_policy) -> {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Constants.privacyPolicyUrl)))
            }
            getString(R.string.source_code) -> {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Constants.sourceCodeUrl)))
            }
            getString(R.string.about) -> {
                AboutDialog.Builder(this@MainActivity)
                    .setCancelable(true)
                    .setCanceledOnTouchOutside(true)
                    .setTitle("About ${getString(R.string.app_name)}")
                    .setVersionName(BuildConfig.VERSION_NAME)
                    .setDescription("Enhances your online privacy and security with an ultra-fast connection, providing continuous safeguarding for your digital experience.")
                    .setListener(object : AboutDialog.OnListener {
                        override fun onClick(dialog: BaseDialog?) {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            intent.setData(Uri.parse("package:$packageName"))
                            startActivity(intent)
                        }
                    }).show()
            }
        }

    }

    fun onChangeCountryClicked(view: View) {
        startActivity(Intent(this, CountryPickerActivity::class.java))
    }



    private fun initializeAll() {
        preference = SharedPreference(this@MainActivity)
        server = preference?.server

//        val call = "start"
//
//
//
//
//
//        when (call) {
//            "stop" -> {
//                OpenVPNThread.stop()
//                setStage("disconnected")
//            }
//
//            //    _listVpn.add(VpnConfig(config: await rootBundle.loadString("assets/vpn/japan.ovpn"), name: "Japan"));
//            //    _listVpn.add(VpnConfig(config: await rootBundle.loadString("assets/vpn/us.ovpn"), name: "United State"));
//
//
//            "start" -> {
//                config = call.argument("config")
//                name = "Japan"
//                username = call.argument("username")
//                password = call.argument("password")
//                if (call.argument("dns1") != null) dns1 = call.argument("dns1")
//                if (call.argument("dns2") != null) dns2 = call.argument("dns2")
//                bypassPackages = call.argument("bypass_packages")
//                prepareVPN()
//            }
//
//            "refresh" -> updateVPNStages()
//            "refresh_status" -> updateVPNStatus()
//            "stage" -> OpenVPNService.getStatus()
//            "kill_switch" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//                val intent = Intent(Settings.ACTION_VPN_SETTINGS)
//                startActivity(intent)
//            }
//        }

    }


    private fun prepareVPN() {
        if (isConnected()) {
            setStage("prepare")
            try {
                val configParser = ConfigParser()
                configParser.parseConfig(StringReader(config))
                vpnProfile = configParser.convertProfile()
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (configParseError: ConfigParser.ConfigParseError) {
                configParseError.printStackTrace()
            }
            val vpnIntent = VpnService.prepare(this)
            if (vpnIntent != null) startActivityForResult(vpnIntent, VPN_REQUEST_ID) else startVPN()
        } else {
            setStage("nonetwork")
        }
    }

    private fun startVPN() {
        try {
            setStage("connecting")
            if (vpnProfile!!.checkProfile(this) != de.blinkt.openvpn.R.string.no_error_found) {
                throw RemoteException(getString(vpnProfile!!.checkProfile(this)))
            }
            vpnProfile!!.mName = name
            vpnProfile!!.mProfileCreator = packageName
            vpnProfile!!.mUsername = username
            vpnProfile!!.mPassword = password
            vpnProfile!!.mDNS1 = dns1
            vpnProfile!!.mDNS2 = dns2
            if (dns1 != null && dns2 != null) {
                vpnProfile!!.mOverrideDNS = true
            }
            if (bypassPackages != null && (bypassPackages?.size ?: 0) > 0) {
                vpnProfile!!.mAllowedAppsVpn.addAll(bypassPackages!!)
                vpnProfile!!.mAllowAppVpnBypass = true
            }
            ProfileManager.setTemporaryProfile(this, vpnProfile)
            VPNLaunchHelper.startOpenVpn(vpnProfile, this)
        } catch (e: RemoteException) {
            setStage("disconnected")
            e.printStackTrace()
        }
    }


    private fun updateVPNStages() {
        setStage(OpenVPNService.getStatus())
    }

    private fun updateVPNStatus() {
        if (attached) {
            // Success
            localJson.toString()
        }
    }


    @Suppress("DEPRECATION")
    private fun isConnected(): Boolean {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val nInfo = cm.activeNetworkInfo
        return nInfo != null && nInfo.isConnectedOrConnecting
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


    private fun setStage(stage: String) {
        if (attached){
            when (stage.uppercase(Locale.getDefault())) {
                "CONNECTED" -> {
                    //connected
                }
                "DISCONNECTED" -> {
                    //disconnected
                }
                "WAIT" -> {
                    //wait_connection
                }
                "AUTH" -> {
                    //authenticating
                }
                "RECONNECTING" -> {
                    //reconnect
                }
                "NONETWORK" -> {
                    //no_connection
                }
                "CONNECTING" -> {
                    //connecting
                }
                "PREPARE" -> {
                    //prepare
                }
                "DENIED" -> {
                    //denied
                }
            }
        }
    }

    override fun newServer(server: VpnServer?) {
        TODO("Not yet implemented")
    }


}