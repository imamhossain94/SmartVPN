package com.newagedevs.vpnthundershield.model

data class Server(
    var countryName: String?,
    var ipAddress: String?,
    var flagDrawable: Int?,
    var openVpn: String?,
    var openVpnUserName: String?,
    var openVpnUserPassword: String?,
    var isConnected: Boolean
)