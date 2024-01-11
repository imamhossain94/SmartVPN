package com.newagedevs.smartvpn.model

import org.json.JSONObject

data class IPDetails(
    var country: String = "",
    var regionName: String = "",
    var city: String = "",
    var zip: String = " - - - - ",
    var timezone: String = "Unknown",
    var isp: String = "Unknown",
    var query: String = "Not available"
) {
    companion object {
        fun fromJson(json: JSONObject): IPDetails {
            return IPDetails(
                country = json.optString("country", ""),
                regionName = json.optString("regionName", ""),
                city = json.optString("city", ""),
                zip = json.optString("zip", " - - - - "),
                timezone = json.optString("timezone", "Unknown"),
                isp = json.optString("isp", "Unknown"),
                query = json.optString("query", "Not available")
            )
        }
    }
}
