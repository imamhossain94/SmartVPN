package com.newagedevs.smartvpn.utils

import java.net.URL

class Constants {
    companion object {
        val feedbackMail = arrayOf("imamagun94@gmail.com")
        val contactMail = arrayOf("imamagun94@gmail.com")
        const val privacyPolicyUrl = "https://newagedevs-privacy-policy.blogspot.com/2024/01/smart-vpn.html"
        const val sourceCodeUrl = "https://github.com/imamhossain94/SmartVPN"
        const val publisherName = "https://play.google.com/store/apps/developer?id=NewAgeDevs"
        const val appStoreId = "market://details?id=com.newagedevs.smartvpn"
        const val appStoreBaseURL = "http://play.google.com/store/apps/details?id="

        // Shared preferences constants
        const val sharedPrefName = "MyPrefs"
        const val isConnectedKey = "isConnected"
        const val vpnServersKey = "vpn_servers_key"
        const val favoriteVpnServersKey = "favorite_vpn_servers_key"
        const val selectedVpnServersKey: String = "selected_vpn_servers_key"

        // Request Ids
        const val VPN_REQUEST_ID = 1

        // Speed test endpoints (Ookla)
        val speedTestClientURL: URL = URL("https://www.speedtest.net/speedtest-config.php")
        val speedTestServerURL: URL = URL("https://www.speedtest.net/speedtest-servers-static.php")
    }
}
