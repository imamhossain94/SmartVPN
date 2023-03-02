package com.newagedevs.vpnthundershield.view.ui

import androidx.databinding.Bindable
import com.newagedevs.vpnthundershield.R
import com.newagedevs.vpnthundershield.base.DisposableViewModel
import com.newagedevs.vpnthundershield.model.Server
import com.newagedevs.vpnthundershield.view.adapter.ServerAdapter
import com.skydoves.bindables.bindingProperty
import timber.log.Timber


class MainViewModel constructor(
    private val serverAdapter: ServerAdapter,
) : DisposableViewModel() {

    @get:Bindable
    var isLoading: Boolean by bindingProperty(false)
        private set

    @get:Bindable
    var toast: String? by bindingProperty(null)
        private set

    @get:Bindable
    var servers: List<Server>? by bindingProperty(listOf())

    init {
        Timber.d("injection DashboardViewModel")

        val serverList: ArrayList<Server> = ArrayList()

        serverList.add(Server(
                "United States", "192.168.22.36", R.drawable.flag_usa,
                "us.ovpn", "freeopenvpn", "416248023",
                true
            ))

        serverList.add(Server(
            "Japan", "192.168.22.36", R.drawable.flag_japan,
            "japan.ovpn", "vpn", "vpn",
            false
        ))

        serverList.add(Server(
            "Sweden", "192.168.22.36", R.drawable.flag_sweden,
            "sweden.ovpn", "vpn", "vpn",
            false
        ))

        serverList.add(Server(
            "Korea", "192.168.22.36", R.drawable.flag_korea,
            "korea.ovpn", "vpn", "vpn",
            false
        ))

        serverList.add(Server(
            "Thailand", "192.168.22.36", R.drawable.flag_thailand,
            "thailand.ovpn", "vpn", "vpn",
            false
        ))


        servers = serverList
        serverAdapter.updateServerList(servers!!)

    }

}


