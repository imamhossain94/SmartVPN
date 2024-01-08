package com.newagedevs.smartvpn.view.ui


import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.net.VpnService
import android.os.Bundle
import android.os.RemoteException
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ShareCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.hjq.bar.OnTitleBarListener
import com.hjq.bar.TitleBar
import com.newagedevs.smartvpn.BuildConfig
import com.newagedevs.smartvpn.R
import com.newagedevs.smartvpn.databinding.ActivityMainBinding
import com.newagedevs.smartvpn.interfaces.ChangeServer
import com.newagedevs.smartvpn.model.CustomItem
import com.newagedevs.smartvpn.model.ItemUtils
import com.newagedevs.smartvpn.model.Server
import com.newagedevs.smartvpn.utils.CheckInternetConnection
import com.newagedevs.smartvpn.utils.Constants
import com.newagedevs.smartvpn.utils.SharedPreference
import com.newagedevs.smartvpn.utils.Utils
import com.newagedevs.smartvpn.view.CustomListBalloonFactory
import com.newagedevs.smartvpn.view.adapter.CustomAdapter
import com.newagedevs.smartvpn.view.adapter.ServerAdapter
import com.newagedevs.smartvpn.view.dialog.AboutDialog
import com.newagedevs.smartvpn.view.dialog.BaseDialog
import com.newagedevs.smartvpn.view.dialog.MessageDialog
import com.skydoves.balloon.Balloon
import com.skydoves.bindables.BindingActivity
import de.blinkt.openvpn.OpenVpnApi
import de.blinkt.openvpn.core.OpenVPNService
import de.blinkt.openvpn.core.OpenVPNThread
import de.blinkt.openvpn.core.VpnStatus
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader


class MainActivity : BindingActivity<ActivityMainBinding>(R.layout.activity_main), CustomAdapter.CustomViewHolder.Delegate, ChangeServer {

    private val serverAdapter: ServerAdapter by inject { parametersOf(this)  }
    private val viewModel: MainViewModel by viewModel { parametersOf(serverAdapter) }

    private val customAdapter by lazy { CustomAdapter(this) }
    lateinit var customListBalloon: Balloon

    private var server: Server? = null
    private var connection: CheckInternetConnection? = null

    var vpnStart = false
    private var preference: SharedPreference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding {
            vm = viewModel
        }

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
                updateCurrentServerIcon(server?.flagDrawable)

                // Stop previous connection

                // Stop previous connection
                if (vpnStart) {
                    stopVpn()
                }

                prepareVpn()
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


        isServiceRunning()
        VpnStatus.initLogCache(this.cacheDir)


        initializeAll()

        binding.ivConnectServerButton.setOnClickListener {
            // Vpn is running, user would like to disconnect current connection.
            if (vpnStart) {
                confirmDisconnect()
            } else {
                prepareVpn()
            }
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

        // Update current selected server icon
        updateCurrentServerIcon(server?.flagDrawable)
        connection = CheckInternetConnection()
    }


    /**
     * Show show disconnect confirm dialog
     */
    fun confirmDisconnect() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(getString(R.string.connection_close_confirm))
        builder.setPositiveButton(getString(R.string.yes),
            DialogInterface.OnClickListener { dialog, id -> stopVpn() })
        builder.setNegativeButton(getString(R.string.no),
            DialogInterface.OnClickListener { dialog, id ->
                // User cancelled the dialog
            })

        // Create the AlertDialog
        val dialog = builder.create()
        dialog.show()
    }

    /**
     * Prepare for vpn connect with required permission
     */
    private fun prepareVpn() {
        if (!vpnStart) {
            if (getInternetStatus()) {

                // Checking permission for network monitor
                val intent = VpnService.prepare(this)
                if (intent != null) {
                    startActivityForResult(intent, 1)
                } else startVpn() //have already permission

                // Update confection status
                status("connecting")
            } else {

                // No internet connection available
                showToast("you have no internet connection !!")
            }
        } else if (stopVpn()) {

            // VPN is stopped, show a Toast message.
            showToast("Disconnect Successfully")
        }
    }

    /**
     * Stop vpn
     * @return boolean: VPN status
     */
    fun stopVpn(): Boolean {
        try {
            OpenVPNThread.stop()
            status("connect")
            vpnStart = false
            return true
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    /**
     * Taking permission for network access
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {

            //Permission granted, start the VPN
            startVpn()
        } else {
            showToast("Permission Deny !! ")
        }
    }

    /**
     * Internet connection status.
     */
    fun getInternetStatus(): Boolean {
        return connection!!.netCheck(this)
    }

    /**
     * Get service status
     */
    fun isServiceRunning() {
        setStatus(OpenVPNService.getStatus())
    }

    /**
     * Start the VPN
     */
    private fun startVpn() {
        try {
            // .ovpn file
            val conf: InputStream = this.getAssets().open(server?.openVpn!!)
            val isr = InputStreamReader(conf)
            val br = BufferedReader(isr)
            var config = ""
            var line: String?
            while (true) {
                line = br.readLine()
                if (line == null) break
                config += line + "\n"
            }
            br.readLine()
            OpenVpnApi.startVpn(
                this,
                config,
                server?.countryName,
                server?.openVpnUserName,
                server?.openVpnUserPassword
            )

            // Update log
            binding.tvServerStatusLog.setText("Connecting...")
            vpnStart = true
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    /**
     * Status change with corresponding vpn connection status
     * @param connectionState
     */
    fun setStatus(connectionState: String?) {
        if (connectionState != null) when (connectionState) {
            "DISCONNECTED" -> {
                status("connect")
                vpnStart = false
                OpenVPNService.setDefaultStatus()
                binding.tvServerStatusLog.setText("")
            }

            "CONNECTED" -> {
                vpnStart = true // it will use after restart this activity
                status("connected")
                binding.tvServerStatusLog.setText("")
            }

            "WAIT" -> binding.tvServerStatusLog.setText("waiting for server connection!!")
            "AUTH" -> binding.tvServerStatusLog.setText("server authenticating!!")
            "RECONNECTING" -> {
                status("connecting")
                binding.tvServerStatusLog.setText("Reconnecting...")
            }

            "NONETWORK" -> binding.tvServerStatusLog.setText("No network connection")
        }
    }

    /**
     * Change button background color and text
     * @param status: VPN current status
     */
    fun status(status: String) {
        if (status == "connect") {
            binding.tvServerStatus.setText(getString(R.string.connect))
        } else if (status == "connecting") {
            binding.tvServerStatus.setText(getString(R.string.connecting))
        } else if (status == "connected") {
            binding.tvServerStatus.setText(getString(R.string.disconnect))
        } else if (status == "tryDifferentServer") {
//            binding.vpnBtn.setBackgroundResource(R.drawable.button_connected)
            binding.tvServerStatus.setText("Try Different\nServer")
        } else if (status == "loading") {
//            binding.tvServerStatus.setBackgroundResource(R.drawable.button)
            binding.tvServerStatus.setText("Loading Server..")
        } else if (status == "invalidDevice") {
//            binding.tvServerStatus.setBackgroundResource(R.drawable.button_connected)
            binding.tvServerStatus.setText("Invalid Device")
        } else if (status == "authenticationCheck") {
//            binding.tvServerStatus.setBackgroundResource(R.drawable.button_connecting)
            binding.tvServerStatus.setText("Authentication \n Checking...")
        }
    }

    /**
     * Receive broadcast message
     */
    var broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            try {
                setStatus(intent.getStringExtra("state"))
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                var duration = intent.getStringExtra("duration")
                var lastPacketReceive = intent.getStringExtra("lastPacketReceive")
                var byteIn = intent.getStringExtra("byteIn")
                var byteOut = intent.getStringExtra("byteOut")
                if (duration == null) duration = "00:00:00"
                if (lastPacketReceive == null) lastPacketReceive = "0"
                if (byteIn == null) byteIn = " "
                if (byteOut == null) byteOut = " "
                updateConnectionStatus(duration, lastPacketReceive, byteIn, byteOut)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Update status UI
     * @param duration: running time
     * @param lastPacketReceive: last packet receive time
     * @param byteIn: incoming data
     * @param byteOut: outgoing data
     */
    fun updateConnectionStatus(
        duration: String,
        lastPacketReceive: String,
        byteIn: String,
        byteOut: String
    ) {
        binding.tvServerDuration.setText("Duration: $duration")
        //binding.lastPacketReceiveTv.setText("Packet Received: $lastPacketReceive second ago")
        binding.tvServerBytesIn.setText("Bytes In: $byteIn")
        binding.tvServerBytesOut.setText("Bytes Out: $byteOut")
    }

    /**
     * Show toast message
     * @param message: toast message
     */
    fun showToast(message: String?) {
        Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
    }

    /**
     * VPN server country icon change
     * @param serverIcon: icon URL
     */
    fun updateCurrentServerIcon(serverIcon: Int?) {
//        Glide.with(this@MainActivity)
//            .load(serverIcon)
//            .into<Target<Drawable>>(binding.selectedServerIcon)
        Glide.with(this@MainActivity)
            .load(serverIcon)
            .into(binding.ivSelectedServerFlag)
    }

    /**
     * Change server when user select new server
     * @param server ovpn server details
     */
    override fun newServer(server: Server?) {
        this.server = server
        updateCurrentServerIcon(server?.flagDrawable)

        // Stop previous connection
        if (vpnStart) {
            stopVpn()
        }
        prepareVpn()
    }

    override fun onResume() {
        LocalBroadcastManager.getInstance(this@MainActivity)
            .registerReceiver(broadcastReceiver, IntentFilter("connectionState"))
        if (server == null) {
            server = preference?.server
        }
        super.onResume()
    }

    override fun onPause() {
        LocalBroadcastManager.getInstance(this@MainActivity).unregisterReceiver(broadcastReceiver)
        super.onPause()
    }

    /**
     * Save current selected server on local shared preference
     */
    override fun onStop() {
        if (server != null) {
            preference!!.saveServer(server!!)
        }
        super.onStop()
    }

}