package com.newagedevs.smartvpn.model

import com.newagedevs.smartvpn.extensions.formatSize

data class VpnServer(
    var hostname: String = "",
    var ip: String = "",
    var ping: String = "",
    var speed: String = "",
    var countryLong: String = "",
    var countryShort: String = "",
    var numVpnSessions: String = "",
    var openVPNConfigDataBase64: String = ""
) {
    constructor(json: Map<String, Any>) : this(
        hostname = json["HostName"].toString(),
        ip = json["IP"].toString(),
        ping = json["Ping"].toString(),
        speed = formatSize(json["Speed"].toString().toLong()),
        countryLong = json["CountryLong"].toString(),
        countryShort = json["CountryShort"].toString(),
        numVpnSessions = json["NumVpnSessions"].toString(),
        openVPNConfigDataBase64 = json["OpenVPN_ConfigData_Base64"].toString()
    )

    fun toJson(): Map<String, Any> {
        return mapOf(
            "HostName" to hostname,
            "IP" to ip,
            "Ping" to ping,
            "Speed" to speed,
            "CountryLong" to countryLong,
            "CountryShort" to countryShort,
            "NumVpnSessions" to numVpnSessions,
            "OpenVPN_ConfigData_Base64" to openVPNConfigDataBase64
        )
    }
}
