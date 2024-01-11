package com.newagedevs.smartvpn.view.ui

import androidx.databinding.Bindable
import androidx.lifecycle.viewModelScope
import com.newagedevs.smartvpn.R
import com.newagedevs.smartvpn.base.DisposableViewModel
import com.newagedevs.smartvpn.model.Server
import com.newagedevs.smartvpn.network.APIs.Companion.getVPNServers
import com.newagedevs.smartvpn.view.adapter.ServerAdapter
import com.skydoves.bindables.bindingProperty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import android.util.Base64
import com.newagedevs.smartvpn.model.VpnServer
import com.newagedevs.smartvpn.model.VpnConfig
import java.nio.charset.StandardCharsets

class MainViewModel constructor(
    serverAdapter: ServerAdapter,
) : DisposableViewModel() {

    @get:Bindable
    var isLoading: Boolean by bindingProperty(false)
        private set

    @get:Bindable
    var toast: String? by bindingProperty(null)
        private set

    @get:Bindable
    var servers: List<VpnServer>? by bindingProperty(listOf())


    @get:Bindable
    var selectedServer: VpnServer? by bindingProperty(null)
        private set

    fun connectToVPN() {
        selectedServer?.let {
            val data = Base64.decode(it.openVPNConfigDataBase64, Base64.DEFAULT)
            val config = String(data, StandardCharsets.UTF_8)
            val vpnConfig = VpnConfig(
                country = it.countryLong,
                username = "vpn",
                password = "vpn",
                config = config
            )
        }
    }


    init {
        Timber.d("injection DashboardViewModel")

        viewModelScope.launch(Dispatchers.IO) {
            servers = getVPNServers()
            servers?.let {
                withContext(Dispatchers.Main) {
                    serverAdapter.updateServerList(it)
                }
            }
        }
    }

}


