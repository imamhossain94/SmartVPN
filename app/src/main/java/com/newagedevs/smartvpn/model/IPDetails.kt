package com.newagedevs.smartvpn.model

import org.json.JSONObject

data class IPDetails(
    var status: String = "",
    var country: String = "",
    var countryCode: String = "",
    var region: String = "",
    var regionName: String = "",
    var city: String = "",
    var zip: String = " - - - - ",
    var lat: Double = 0.0,
    var lon: Double = 0.0,
    var timezone: String = "Unknown",
    var isp: String = "Unknown",
    var org: String = "",
    var `as`: String = "",
    var ip: String = "0.0.0.0"
) {
    companion object {
        fun fromJson(json: JSONObject): IPDetails {
            return IPDetails(
                status = json.optString("status", ""),
                country = json.optString("country", ""),
                countryCode = json.optString("countryCode", ""),
                region = json.optString("region", ""),
                regionName = json.optString("regionName", ""),
                city = json.optString("city", ""),
                zip = json.optString("zip", " - - - - "),
                lat = json.optDouble("lat", 0.0),
                lon = json.optDouble("lon", 0.0),
                timezone = json.optString("timezone", "Unknown"),
                isp = json.optString("isp", "Unknown"),
                org = json.optString("org", ""),
                `as` = json.optString("as", ""),
                ip = json.optString("query", "0.0.0.0")
            )
        }
    }
}

