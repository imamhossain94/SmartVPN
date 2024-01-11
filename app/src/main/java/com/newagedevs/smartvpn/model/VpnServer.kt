package com.newagedevs.smartvpn.model

data class VpnServer(
    var hostname: String = "",
    var ip: String = "",
    var ping: String = "",
    var speed: Int = 0,
    var countryLong: String = "",
    var countryShort: String = "",
    var numVpnSessions: Int = 0,
    var openVPNConfigDataBase64: String = ""
) {
    constructor(json: Map<String, Any>) : this(
        hostname = json["HostName"].toString(),
        ip = json["IP"].toString(),
        ping = json["Ping"].toString(),
        speed = (json["Speed"] as? Int) ?: 0,
        countryLong = json["CountryLong"].toString(),
        countryShort = json["CountryShort"].toString(),
        numVpnSessions = (json["NumVpnSessions"] as? Int) ?: 0,
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
