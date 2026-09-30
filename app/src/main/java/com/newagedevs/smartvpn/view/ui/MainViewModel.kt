package com.newagedevs.smartvpn.view.ui

import android.content.Context
import android.util.Base64
import androidx.databinding.Bindable
import androidx.lifecycle.viewModelScope
import com.newagedevs.smartvpn.base.DisposableViewModel
import com.newagedevs.smartvpn.model.IPDetails
import com.newagedevs.smartvpn.model.VpnConfig
import com.newagedevs.smartvpn.model.VpnServer
import com.newagedevs.smartvpn.network.APIs
import com.newagedevs.smartvpn.preferences.SharedPrefRepository
import com.skydoves.bindables.bindingProperty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.nio.charset.StandardCharsets

class MainViewModel(
    private val sharedPref: SharedPrefRepository,
    private val appContext: Context,
) : DisposableViewModel() {

    @get:Bindable
    var isLoading: Boolean by bindingProperty(false)
        private set

    /** Guards against a refresh being triggered while one is already in flight. */
    private var isFetching = false

    /** Set when the live fetch fails, so the UI can explain the stale list. */
    @get:Bindable
    var loadError: String? by bindingProperty(null)
        private set

    @get:Bindable
    var servers: List<VpnServer> by bindingProperty(emptyList())
        private set

    @get:Bindable
    var favoriteServers: List<VpnServer> by bindingProperty(emptyList())
        private set

    @get:Bindable
    var selectedServer: VpnServer? by bindingProperty(null)

    @get:Bindable
    var selectedServerConfig: VpnConfig? by bindingProperty(null)
        private set

    /**
     * Exposed as a [kotlinx.coroutines.flow.StateFlow] rather than a bindable
     * property: it is not referenced from any layout, and the Activity collects
     * it lifecycle-aware so a late response cannot touch a detached view.
     */
    val currentIPDetails = MutableStateFlow<IPDetails?>(null)

    /**
     * Called when the user taps the connect button. Decodes the base64 OpenVPN
     * configuration shipped by the server into something [ConfigParser] accepts.
     */
    fun connectToVPN() {
        selectedServer?.let { server ->
            val raw = try {
                String(Base64.decode(server.openVPNConfigDataBase64, Base64.DEFAULT), StandardCharsets.UTF_8)
            } catch (e: IllegalArgumentException) {
                Timber.e(e, "Malformed base64 config for %s", server.hostname)
                null
            }
            selectedServerConfig = raw?.let {
                VpnConfig(
                    country = server.countryLong,
                    username = "vpn",
                    password = "vpn",
                    config = it,
                )
            }
        }
    }

    /**
     * Loads the cached server list, falling back to a network fetch. Safe to call
     * from several Activities: only the first call does any work.
     */
    fun loadServersIfNeeded(forceRefresh: Boolean = false) {
        if (isLoading) return
        if (!forceRefresh && servers.isNotEmpty()) return

        isLoading = true
        viewModelScope.launch {
            val cached = if (forceRefresh) emptyList() else withContext(Dispatchers.IO) {
                sharedPref.getVpnServers()
            }

            if (cached.isNotEmpty()) {
                applyServerList(cached)
                restoreSelection()
                isLoading = false
            } else {
                // Nothing cached: fall back to the bundled snapshot so the list is
                // never empty, then refresh from the network in the background.
                loadBundledServersThenRefresh()
            }
        }
    }

    /**
     * Shows the bundled snapshot immediately and then attempts a live refresh.
     *
     * The live VPNGate feed is a ~1.3 MB payload that has been measured taking
     * over two minutes on a mobile link, so waiting on it before rendering
     * anything left the user staring at an empty screen.
     */
    private fun loadBundledServersThenRefresh() {
        viewModelScope.launch {
            val bundled = withContext(Dispatchers.IO) { APIs.getBundledServers(appContext) }
            if (bundled.isNotEmpty()) {
                applyServerList(bundled)
                restoreSelection()
            }
            isLoading = false
            fetchVpnServer()
        }
    }

    fun fetchVpnServer() {
        if (isFetching) return
        isFetching = true
        isLoading = true
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { APIs.getVPNServers() }
            }
            result
                .onSuccess { list ->
                    if (list.isEmpty()) {
                        // The upstream CSV can legitimately parse to zero rows;
                        // treat it as a failure instead of crashing on first().
                        Timber.w("VPN server list came back empty")
                        loadError = "The server list came back empty. Pull to refresh."
                        return@onSuccess
                    }
                    loadError = null
                    applyServerList(list)
                    withContext(Dispatchers.IO) { sharedPref.saveVpnServers(list) }
                    restoreSelection()
                }
                .onFailure {
                    Timber.e(it, "Unable to fetch VPN server list")
                    loadError = "Could not reach VPNGate. Showing the bundled list."
                }
            isLoading = false
            isFetching = false
        }
    }

    fun fetchIpDetails() {
        viewModelScope.launch {
            currentIPDetails.value = withContext(Dispatchers.IO) { APIs.getIPDetails() }
        }
    }

    fun refreshFavoriteServerList() {
        favoriteServers = sharedPref.getFavoriteVpnServers()
    }

    fun isFavoriteVpnServer(vpnServer: VpnServer): Boolean =
        sharedPref.getFavoriteVpnServers().contains(vpnServer)

    fun toggleFavoriteVpnServer(vpnServer: VpnServer) {
        if (isFavoriteVpnServer(vpnServer)) {
            sharedPref.removeFromFavoriteVpnServers(vpnServer)
        } else {
            sharedPref.addToFavoriteVpnServers(vpnServer)
        }
        refreshFavoriteServerList()
    }

    /**
     * Persists [server] as the current selection. The picker Activities own their
     * own ViewModel instance, so the selection is stored in SharedPreferences and
     * [syncSelectionFromPrefs] re-reads it when returning to the main screen.
     */
    fun selectServer(server: VpnServer) {
        selectedServer = server
        connectToVPN()
        sharedPref.saveSelectedVpnServer(server)
    }

    fun syncSelectionFromPrefs() {
        val persisted = sharedPref.getSelectedVpnServer() ?: return
        if (persisted == selectedServer) return
        selectedServer = persisted
        connectToVPN()
    }

    private fun applyServerList(list: List<VpnServer>) {
        servers = list.sortedBy { it.ping.toIntOrNull() ?: Int.MAX_VALUE }
    }

    private fun restoreSelection() {
        val list = servers
        if (list.isEmpty()) return
        selectedServer = sharedPref.getSelectedVpnServer()?.takeIf { persisted ->
            list.any { it.hostname == persisted.hostname }
        } ?: list.first()
        connectToVPN()
    }
}
