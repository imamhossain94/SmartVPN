package com.newagedevs.smartvpn.view.ui

import androidx.databinding.Bindable
import com.newagedevs.smartvpn.R
import com.newagedevs.smartvpn.base.DisposableViewModel
import com.newagedevs.smartvpn.model.Server
import com.newagedevs.smartvpn.utils.Utils
import com.newagedevs.smartvpn.view.adapter.ServerAdapter
import com.skydoves.bindables.bindingProperty
import timber.log.Timber


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
    var servers: List<Server>? by bindingProperty(listOf())

    init {
        Timber.d("injection DashboardViewModel")

        val serverList: ArrayList<Server> = ArrayList()

        serverList.add(
            Server(
                "192.168.22.36", "United States", R.drawable.flag_usa,
                "us.ovpn", "freeopenvpn", "416248023",
            )
        )

        serverList.add(
            Server(
                "192.168.22.36", "Japan", R.drawable.flag_japan,
                "japan.ovpn", "vpn", "vpn"
            )
        )

        serverList.add(
            Server(
                "192.168.22.36", "Sweden", R.drawable.flag_sweden,
                "sweden.ovpn", "vpn", "vpn"
            )
        )

        serverList.add(
            Server(
                "192.168.22.36", "Korea", R.drawable.flag_korea,
                "korea.ovpn", "vpn", "vpn"
            )
        )

        serverList.add(
            Server(
                "192.168.22.36", "Thailand", R.drawable.flag_thailand,
                "thailand.ovpn", "vpn", "vpn"
            )
        )

        servers = serverList
        serverAdapter.updateServerList(servers!!)

    }

}


