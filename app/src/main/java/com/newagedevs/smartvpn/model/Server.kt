package com.newagedevs.smartvpn.model

class Server {
    var ipAddress: String? = null
    var countryName: String? = null
    var flagDrawable: Int = 0
    var openVpn: String? = null
    var openVpnUserName: String? = null
    var openVpnUserPassword: String? = null

    constructor()
    constructor(countryName: String?, flagDrawable: Int, openVpn: String?) {
        this.countryName = countryName
        this.flagDrawable = flagDrawable
        this.openVpn = openVpn
    }

    constructor(
        ipAddress: String?,
        country: String?,
        flagDrawable: Int,
        openVpn: String?,
        openVpnUserName: String?,
        openVpnUserPassword: String?
    ) {
        this.ipAddress = ipAddress
        this.countryName = country
        this.flagDrawable = flagDrawable
        this.openVpn = openVpn
        this.openVpnUserName = openVpnUserName
        this.openVpnUserPassword = openVpnUserPassword
    }
}
