package com.newagedevs.smartvpn.interfaces

import com.newagedevs.smartvpn.model.VpnServer

interface ChangeServer {
    fun newServer(server: VpnServer?)
}
