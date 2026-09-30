package com.newagedevs.smartvpn.view.ui

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.RecyclerView
import com.hjq.bar.OnTitleBarListener
import com.hjq.bar.TitleBar
import com.newagedevs.smartvpn.R
import com.newagedevs.smartvpn.databinding.ActivityMainBinding
import com.newagedevs.smartvpn.extensions.applyEdgeToEdgeInsets
import com.newagedevs.smartvpn.extensions.isNetworkConnected
import com.newagedevs.smartvpn.model.CustomItem
import com.newagedevs.smartvpn.model.VpnServer
import com.newagedevs.smartvpn.utils.Constants
import com.newagedevs.smartvpn.utils.ItemUtils
import com.newagedevs.smartvpn.view.CustomListBalloonFactory
import com.newagedevs.smartvpn.view.adapter.CustomAdapter
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
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import java.io.IOException
import java.io.StringReader
import java.util.Locale

class MainActivity : BindingActivity<ActivityMainBinding>(R.layout.activity_main),
    CustomAdapter.CustomViewHolder.Delegate {

    private var vpnProfile: VpnProfile? = null
    private lateinit var customListBalloon: Balloon

    private val viewModel: MainViewModel by viewModel()

    private val customAdapter by lazy { CustomAdapter(this) }

    /**
     * Registered in [onCreate] and torn down in [onDestroy]. [LocalBroadcastManager]
     * is application scoped and holds a strong reference to the receiver, so
     * leaving this registered leaked the whole Activity for the life of the
     * process and duplicated every update after a configuration change.
     */
    private val connectionStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            intent.getStringExtra(EXTRA_STATE)?.let(::setStage)
            renderTraffic(intent)
        }
    }

    private val vpnConsentLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                startVpn()
            } else {
                setStage(Stage.DENIED.wire)
                Toast.makeText(this, R.string.vpn_permission_denied, Toast.LENGTH_SHORT).show()
            }
        }

    /**
     * From Android 13 the VPN foreground-service notification is silently dropped
     * without this grant, which leaves the user with no way to pause or cancel
     * the tunnel from the shade. The VPN itself keeps working either way, so a
     * refusal is not treated as an error.
     */
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Timber.i("POST_NOTIFICATIONS denied; the VPN notification will be hidden")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Lets the library build a working notification content intent instead of
        // guessing a class name.
        OpenVPNService.setNotificationActivityClass(MainActivity::class.java)

        applyEdgeToEdgeInsets(binding.root)

        binding { vm = viewModel }

        customListBalloon = CustomListBalloonFactory().create(this, this)
        customListBalloon.getContentView()
            .findViewById<RecyclerView>(R.id.list_recyclerView)
            .adapter = customAdapter
        customAdapter.addCustomItem(ItemUtils.getCustomSamples(this))

        binding.tbMainBar.setOnTitleBarListener(object : OnTitleBarListener {
            override fun onLeftClick(titleBar: TitleBar) {
                customListBalloon.showAlignBottom(titleBar.leftView, 0, 0)
            }

            override fun onTitleClick(titleBar: TitleBar) = Unit

            override fun onRightClick(titleBar: TitleBar) = onChangeServerClicked(titleBar)
        })

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                MessageDialog.Builder(this@MainActivity)
                    .setTitle(R.string.exit_confirmation)
                    .setMessage(R.string.exit_confirmation_message)
                    .setConfirm(getString(R.string.confirm))
                    .setCancel(getString(R.string.cancel))
                    .setListener(object : MessageDialog.OnListener {
                        override fun onConfirm(dialog: BaseDialog?) = finish()
                        override fun onCancel(dialog: BaseDialog?) = Unit
                    }).show()
            }
        })

        LocalBroadcastManager.getInstance(this)
            .registerReceiver(connectionStateReceiver, IntentFilter(ACTION_CONNECTION_STATE))

        binding.ivConnectServerButton.setOnClickListener { onConnectButtonClicked() }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        viewModel.loadServersIfNeeded()

        // Reflect what the service is already doing (e.g. after a rotation while
        // the tunnel is up) instead of assuming "disconnected".
        setStage(OpenVPNService.getStatus())
    }

    override fun onResume() {
        super.onResume()
        // The picker Activities own a different ViewModel instance, so re-read the
        // persisted selection to pick up any change made over there.
        viewModel.syncSelectionFromPrefs()
        refreshFavoriteIcon()
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(connectionStateReceiver)
        super.onDestroy()
    }

    // region Layout callbacks

    fun onChangeServerClicked(view: View) {
        if (isVpnActive()) {
            MessageDialog.Builder(this)
                .setTitle(R.string.change_vpn_title)
                .setMessage(R.string.change_vpn_message)
                .setConfirm(getString(R.string.confirm))
                .setCancel(getString(R.string.cancel))
                .setListener(object : MessageDialog.OnListener {
                    override fun onConfirm(dialog: BaseDialog?) {
                        if (stopVpn()) {
                            startActivity(Intent(this@MainActivity, ServerPickerActivity::class.java))
                        }
                    }

                    override fun onCancel(dialog: BaseDialog?) = Unit
                }).show()
        } else {
            startActivity(Intent(this, ServerPickerActivity::class.java))
        }
    }

    fun onFavServerClicked(view: View) {
        viewModel.selectedServer?.let { server ->
            viewModel.toggleFavoriteVpnServer(server)
            (view as? ImageView)?.setImageResource(
                if (viewModel.isFavoriteVpnServer(server)) {
                    R.drawable.ic_heart_field
                } else {
                    R.drawable.ic_heart_outlined
                }
            )
        }
    }

    // endregion

    private fun onConnectButtonClicked() {
        if (isVpnActive()) {
            MessageDialog.Builder(this)
                .setTitle(R.string.cancel_vpn_title)
                .setMessage(R.string.cancel_vpn_message)
                .setConfirm(getString(R.string.confirm))
                .setCancel(getString(R.string.cancel))
                .setListener(object : MessageDialog.OnListener {
                    override fun onConfirm(dialog: BaseDialog?) {
                        stopVpn()
                    }

                    override fun onCancel(dialog: BaseDialog?) = Unit
                }).show()
            return
        }

        if (viewModel.selectedServer == null) {
            Toast.makeText(this, R.string.no_server_selected, Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, ServerPickerActivity::class.java))
            return
        }

        viewModel.connectToVPN()
        prepareVpn()
    }

    private fun prepareVpn() {
        if (!isNetworkConnected()) {
            setStage(Stage.NO_NETWORK.wire)
            return
        }

        setStage(Stage.PREPARE.wire)

        val config = viewModel.selectedServerConfig?.config
        if (config.isNullOrBlank()) {
            Timber.e("Selected server has no usable OpenVPN configuration")
            setStage(Stage.DISCONNECTED.wire)
            return
        }

        vpnProfile = try {
            ConfigParser().apply { parseConfig(StringReader(config)) }.convertProfile()
        } catch (e: ConfigParseError) {
            Timber.e(e, "Unable to parse the OpenVPN configuration")
            null
        } catch (e: IOException) {
            Timber.e(e, "Unable to read the OpenVPN configuration")
            null
        }

        if (vpnProfile == null) {
            setStage(Stage.DISCONNECTED.wire)
            Toast.makeText(this, R.string.invalid_server_config, Toast.LENGTH_LONG).show()
            return
        }

        val consent = VpnService.prepare(this)
        if (consent != null) {
            vpnConsentLauncher.launch(consent)
        } else {
            startVpn()
        }
    }

    private fun startVpn() {
        val profile = vpnProfile
        val config = viewModel.selectedServerConfig
        if (profile == null || config == null) {
            Timber.w("startVpn() called without a parsed profile and configuration")
            setStage(Stage.DISCONNECTED.wire)
            return
        }

        setStage(Stage.CONNECTING.wire)

        val profileError = profile.checkProfile(this)
        if (profileError != de.blinkt.openvpn.R.string.no_error_found) {
            Timber.e("Profile check failed: %s", getString(profileError))
            setStage(Stage.DISCONNECTED.wire)
            Toast.makeText(this, profileError, Toast.LENGTH_LONG).show()
            return
        }

        profile.mName = config.country
        profile.mProfileCreator = packageName
        profile.mUsername = config.username
        profile.mPassword = config.password
        // DNS is intentionally left to the OpenVPN configuration. Overriding it
        // here leaked lookups outside the tunnel and, because the defaults were
        // never null, also forced mOverrideDNS on unconditionally.

        ProfileManager.setTemporaryProfile(this, profile)
        VPNLaunchHelper.startOpenVpn(profile, this)
    }

    /**
     * Always reports success: the UI has to leave the connecting state even when
     * the native process was never spawned and [OpenVPNThread.stop] had nothing
     * to destroy. Returning false here used to strand the button on "connecting"
     * and stop "Change server" from ever opening the picker.
     */
    private fun stopVpn(): Boolean {
        vpnProfile = null
        try {
            OpenVPNThread.stop()
        } catch (e: Exception) {
            Timber.e(e, "Error while stopping the VPN service")
        }
        setStage(Stage.DISCONNECTED.wire)
        return true
    }

    private fun refreshFavoriteIcon() {
        val server = viewModel.selectedServer
        binding.favoriteServerIvIcon.setImageResource(
            if (server != null && viewModel.isFavoriteVpnServer(server)) {
                R.drawable.ic_heart_field
            } else {
                R.drawable.ic_heart_outlined
            }
        )
    }

    override fun onCustomItemClick(customItem: CustomItem) {
        customListBalloon.dismiss()
        when (customItem.title) {
            getString(R.string.network_info) ->
                startActivity(Intent(this, NetworkInfoActivity::class.java))

            getString(R.string.favorite_server) ->
                startActivity(Intent(this, FavoriteServerPickerActivity::class.java))

            getString(R.string.share) ->
                ShareCompat.IntentBuilder(this)
                    .setType("text/plain")
                    .setChooserTitle(getString(R.string.share_app, getString(R.string.app_name)))
                    .setText(Constants.appStoreBaseURL + packageName)
                    .startChooser()

            getString(R.string.rate_us) ->
                MessageDialog.Builder(this)
                    .setTitle(getString(R.string.rate_title, getString(R.string.app_name)))
                    .setMessage(R.string.rate_message)
                    .setConfirm(getString(R.string.rate))
                    .setCancel(getString(R.string.cancel))
                    .setListener(object : MessageDialog.OnListener {
                        override fun onConfirm(dialog: BaseDialog?) {
                            startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(Constants.appStoreId))
                            )
                        }

                        override fun onCancel(dialog: BaseDialog?) = Unit
                    }).show()

            getString(R.string.about) ->
                startActivity(Intent(this, AboutActivity::class.java))
        }
    }

    /** Renders the counters broadcast by the library's status service. */
    private fun renderTraffic(intent: Intent) {
        binding.tvServerDuration.text =
            intent.getStringExtra("duration")?.takeIf { it.isNotBlank() } ?: "00:00:00"
        binding.tvServerBytesIn.text = trafficLabel(intent.getStringExtra("byteIn"))
        binding.tvServerBytesOut.text = trafficLabel(intent.getStringExtra("byteOut"))
        viewModel.selectedServer?.let { binding.tvServerIpAddress.text = it.ip }
    }

    /**
     * The library prefixes these strings with an arrow glyph, e.g. "↓2.0 kB".
     * Only that first character is removed: the previous code called `first()`
     * on the value (which throws for an empty string) and then `replace(glyph, "")`,
     * which also stripped every later occurrence of the glyph.
     */
    private fun trafficLabel(raw: String?): String =
        raw?.trim()?.removePrefix("↓")?.removePrefix("↑")?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: getString(R.string.no_traffic)

    /** True while the tunnel is up or on its way there. */
    private fun isVpnActive(): Boolean = Stage.isActive(Stage.from(OpenVPNService.getStatus()))

    private fun setStage(stage: String) {
        val normalized = stage.uppercase(Locale.US)
        when (Stage.from(normalized)) {
            Stage.CONNECTED -> {
                binding.ivConnectServerButton.setImageResource(R.drawable.ic_power_connected)
                binding.tvServerStatus.setText(R.string.disconnect)
                binding.tvServerStatusLog.setText(R.string.vpn_connected)
            }

            Stage.DISCONNECTED, Stage.NOPROCESS, null -> {
                binding.ivConnectServerButton.setImageResource(R.drawable.ic_power_not_connected)
                binding.tvServerStatus.setText(R.string.connect)
                binding.tvServerStatusLog.setText("")
            }

            // The library spells this "NONETWORK"; the app used "NO_NETWORK".
            Stage.NO_NETWORK, Stage.NONETWORK -> {
                binding.ivConnectServerButton.setImageResource(R.drawable.ic_power_not_connected)
                binding.tvServerStatus.setText(R.string.connect)
                binding.tvServerStatusLog.setText(R.string.no_network)
            }

            Stage.AUTH_FAILED -> {
                binding.ivConnectServerButton.setImageResource(R.drawable.ic_power_not_connected)
                binding.tvServerStatus.setText(R.string.connect)
                binding.tvServerStatusLog.setText(R.string.auth_failed)
            }

            Stage.DENIED -> {
                binding.ivConnectServerButton.setImageResource(R.drawable.ic_power_not_connected)
                binding.tvServerStatus.setText(R.string.connect)
                binding.tvServerStatusLog.setText(R.string.permission_denied)
            }

            Stage.PAUSED -> {
                binding.ivConnectServerButton.setImageResource(R.drawable.ic_power_connecting)
                binding.tvServerStatus.setText(R.string.disconnect)
                binding.tvServerStatusLog.setText(R.string.vpn_paused)
            }

            Stage.PREPARE -> {
                binding.ivConnectServerButton.setImageResource(R.drawable.ic_power_connecting)
                binding.tvServerStatus.setText(R.string.connecting)
            }

            else -> {
                // CONNECTING / WAIT / AUTH / RECONNECTING / CONNECTRETRY /
                // WAIT_ORBOT / VPN_GENERATE_CONFIG / NEED / USER_INPUT
                binding.ivConnectServerButton.setImageResource(R.drawable.ic_power_connecting)
                binding.tvServerStatus.setText(R.string.connecting)
                binding.tvServerStatusLog.setText(logFor(normalized))
            }
        }
    }

    private fun logFor(normalized: String): String = when (Stage.from(normalized)) {
        Stage.WAIT -> getString(R.string.waiting_for_server)
        Stage.AUTH -> getString(R.string.server_authenticating)
        Stage.RECONNECTING, Stage.CONNECTRETRY -> getString(R.string.reconnecting)
        Stage.WAIT_ORBOT -> getString(R.string.waiting_for_orbot)
        Stage.NEED, Stage.USER_INPUT, Stage.USER_VPN_PASSWORD, Stage.USER_VPN_PERMISSION ->
            getString(R.string.user_input_required)
        else -> getString(R.string.connecting)
    }

    companion object {
        /**
         * Read reflectively by the VPN library when it builds the notification
         * content intent. Must stay a public static field of this class.
         */
        const val TYPE_START = "TYPE_START"
        const val TYPE_FROM_NOTIFY = 1

        /** Matches the action used by the library's status broadcasts. */
        private const val ACTION_CONNECTION_STATE = "connectionState"
        private const val EXTRA_STATE = "state"
    }
}

/** Every state string the VPN library broadcasts, normalised for [MainActivity.setStage]. */
private enum class Stage(val wire: String) {
    CONNECTED("CONNECTED"),
    DISCONNECTED("DISCONNECTED"),
    NOPROCESS("NOPROCESS"),
    WAIT("WAIT"),
    AUTH("AUTH"),
    RECONNECTING("RECONNECTING"),
    CONNECTING("CONNECTING"),
    CONNECTRETRY("CONNECTRETRY"),
    WAIT_ORBOT("WAIT_ORBOT"),
    PREPARE("PREPARE"),
    DENIED("DENIED"),
    NO_NETWORK("NO_NETWORK"),
    NONETWORK("NONETWORK"),
    AUTH_FAILED("AUTH_FAILED"),
    PAUSED("SCREENOFF"),
    USERPAUSE("USERPAUSE"),
    VPN_GENERATE_CONFIG("VPN_GENERATE_CONFIG"),
    NEED("NEED"),
    USER_INPUT("USER_INPUT"),
    USER_VPN_PASSWORD("USER_VPN_PASSWORD"),
    USER_VPN_PERMISSION("USER_VPN_PERMISSION");

    companion object {
        fun from(value: String): Stage? = entries.firstOrNull { it.wire == value }

        /** States in which the connect button must offer to disconnect. */
        fun isActive(stage: Stage?): Boolean = stage != null && stage in ACTIVE_STATES

        private val ACTIVE_STATES = setOf(
            CONNECTED, WAIT, AUTH, RECONNECTING, CONNECTING, CONNECTRETRY, WAIT_ORBOT,
            PAUSED, USERPAUSE, NEED, USER_INPUT, USER_VPN_PASSWORD, USER_VPN_PERMISSION,
            VPN_GENERATE_CONFIG,
        )
    }
}
