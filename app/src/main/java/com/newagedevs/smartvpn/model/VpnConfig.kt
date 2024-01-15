package com.newagedevs.smartvpn.model

import de.blinkt.openvpn.VpnProfile

data class VpnConfig(
    val country: String,
    val username: String,
    val password: String,
    val config: String,
    val dns1: String? = VpnProfile.DEFAULT_DNS1,
    val dns2: String? = VpnProfile.DEFAULT_DNS2,
    val bypassPackages: ArrayList<String>? = null
)
