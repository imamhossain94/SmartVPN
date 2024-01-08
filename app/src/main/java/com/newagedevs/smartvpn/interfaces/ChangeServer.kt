package com.newagedevs.smartvpn.interfaces

import com.newagedevs.smartvpn.model.Server

interface ChangeServer {
    fun newServer(server: Server?)
}
