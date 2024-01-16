package com.newagedevs.smartvpn.view.ui

import android.util.Base64
import androidx.databinding.Bindable
import androidx.lifecycle.viewModelScope
import com.newagedevs.smartvpn.base.DisposableViewModel
import com.newagedevs.smartvpn.model.IPDetails
import com.newagedevs.smartvpn.model.VpnConfig
import com.newagedevs.smartvpn.model.VpnServer
import com.newagedevs.smartvpn.network.APIs.Companion.getIPDetails
import com.newagedevs.smartvpn.network.APIs.Companion.getVPNServers
import com.newagedevs.smartvpn.preferences.SharedPrefRepository
import com.newagedevs.smartvpn.view.adapter.ServerAdapter
import com.skydoves.bindables.bindingProperty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.charset.StandardCharsets

class MainViewModel constructor(
    private val sharedPref: SharedPrefRepository,
    private val serverAdapter: ServerAdapter,
) : DisposableViewModel() {

    @get:Bindable
    var isLoading: Boolean by bindingProperty(false)
        private set

    @get:Bindable
    var toast: String? by bindingProperty(null)
        private set

    @get:Bindable
    var servers: List<VpnServer> by bindingProperty(listOf())
        private set


    @get:Bindable
    var selectedServer: VpnServer? by bindingProperty(null)

    @get:Bindable
    var selectedServerConfig: VpnConfig? by bindingProperty(null)
        private set

    @get:Bindable
    var currentIPDetails: IPDetails? by bindingProperty(null)
        private set

    fun connectToVPN() {
        if(selectedServer == null) {
            selectedServerConfig = null
        }
        selectedServer?.let {
            val data = Base64.decode(it.openVPNConfigDataBase64, Base64.DEFAULT)
            val config = String(data, StandardCharsets.UTF_8)
            selectedServerConfig = VpnConfig(
                country = it.countryLong,
                username = "vpn",
                password = "vpn",
                config = config
            )
        }
    }

    fun fetchVpnServer(onComplete: () -> Unit = { }) {
        isLoading = true
        viewModelScope.launch(Dispatchers.IO) {
            getVPNServers(
                onFailure = {
                    println("Error: $it")
                    onComplete.invoke()
                    isLoading = false
                },
                onSuccess = {
                    servers = it
                    selectedServer = it.first()
                    withContext(Dispatchers.Main) {
                        serverAdapter.updateServerList(it)
                        sharedPref.saveVpnServers(it)
                    }
                    onComplete.invoke()
                    isLoading = false
                }
            )
        }
    }

    fun fetchIpDetails(onComplete: () -> Unit = { }) {
        viewModelScope.launch(Dispatchers.IO) {
            currentIPDetails = getIPDetails()
            onComplete.invoke()
        }
    }

    init {
        val servers = sharedPref.getVpnServers()
        if(servers.isNotEmpty()) {
            selectedServer = servers.first()
            serverAdapter.updateServerList(servers)
        } else {
            fetchVpnServer()
        }
    }

}


