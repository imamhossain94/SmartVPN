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
        // VPNGate labels the first column "Filename"; fall back to the other
        // spelling so both API revisions resolve.
        hostname = json.string("HostName", "Filename"),
        ip = json.string("IP"),
        ping = json.string("Ping"),
        speed = json["Speed"].toString().toLongOrNull()?.let(::formatSize) ?: "-",
        countryLong = json.string("CountryLong"),
        countryShort = json.string("CountryShort"),
        numVpnSessions = json.string("NumVpnSessions"),
        openVPNConfigDataBase64 = json.string("OpenVPN_ConfigData_Base64")
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

/**
 * Reads a column, tolerating the trailing-whitespace and naming differences
 * between VPNGate API revisions instead of throwing on a malformed row.
 */
private fun Map<String, Any>.string(vararg names: String): String =
    names.firstNotNullOfOrNull { this[it] }?.toString()?.trim().orEmpty()
