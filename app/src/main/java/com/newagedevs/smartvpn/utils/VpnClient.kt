//package com.newagedevs.smartvpn.utils
//
//import android.content.Context
//import de.blinkt.openvpn.core.OpenVPNService
//import de.blinkt.openvpn.core.ProfileManager
//import de.blinkt.openvpn.core.VpnStatus
//
//class VpnClient(private val context: Context) {
//
//    init {
//        // Initialize OpenVPNService
//        OpenVPNService.context = context
//    }
//
//    // Start the VPN connection
//    fun startVpnConnection(profilePath: String) {
//        val profileId = ProfileManager.getInstance(context).importProfile(profilePath)
//        if (profileId != -1) {
//            ProfileManager.getInstance(context).saveProfile(context, profileId)
//            VpnStatus.initLogCache(context.cacheDir)
//            VpnStatus.initStatus(context)
//            VpnStatus.init(context)
//            ProfileManager.getInstance(context).addProfile(profileId)
//            ProfileManager.getInstance(context).saveProfile(context, profileId)
//            ProfileManager.getInstance(context).saveProfileList(context)
//            VpnStatus.updateStateString("Connecting")
//            OpenVPNService.startVpn(context, profileId.toString())
//        }
//    }
//
//    // Stop the VPN connection
//    fun stopVpnConnection() {
//        OpenVPNService.stopVPN(context)
//    }
//}
