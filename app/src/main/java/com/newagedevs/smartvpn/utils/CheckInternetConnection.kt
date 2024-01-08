package com.newagedevs.smartvpn.utils

import android.content.Context
import android.net.ConnectivityManager

/**
 * This class is responsible for internet status checking
 */
class CheckInternetConnection {
    /**
     * Check internet status
     * @param context
     * @return: internet connection status
     */
    @Suppress("DEPRECATION")
    fun netCheck(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val nInfo = cm.activeNetworkInfo
        return nInfo != null && nInfo.isConnectedOrConnecting
    }

}
