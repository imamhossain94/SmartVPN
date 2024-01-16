package com.newagedevs.smartvpn.preferences

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.newagedevs.smartvpn.model.VpnServer
import com.newagedevs.smartvpn.utils.Constants
import com.newagedevs.smartvpn.utils.Constants.Companion.clickCountKey
import com.newagedevs.smartvpn.utils.Constants.Companion.isConnectedKey
import com.newagedevs.smartvpn.utils.Constants.Companion.openCountKey
import com.newagedevs.smartvpn.utils.Constants.Companion.selectedVpnServersKey
import com.newagedevs.smartvpn.utils.Constants.Companion.sharedPrefName
import com.newagedevs.smartvpn.utils.Constants.Companion.vpnServersKey

class SharedPrefRepository(private val context: Context) {

    private val gson = Gson()


    // Increment click count
    private fun incrementClickCount() {
        val sharedPref = context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE)
        val currentCount = sharedPref.getInt(clickCountKey, 0)
        val editor = sharedPref.edit()
        editor.putInt(clickCountKey, currentCount + 1)
        editor.apply()
    }

    // Get click count
    private fun getClickCount(): Int {
        val sharedPref = context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE)
        return sharedPref.getInt(clickCountKey, 0)
    }

    // Reset click count
    private fun resetClickCount() {
        val sharedPref = context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putInt(clickCountKey, 0)
        editor.apply()
    }

    // Increment open count
    private fun incrementOpenCount() {
        val sharedPref = context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE)
        val currentCount = sharedPref.getInt(openCountKey, 0)
        val editor = sharedPref.edit()
        editor.putInt(openCountKey, currentCount + 1)
        editor.apply()
    }

    // Get open count
    private fun getOpenCount(): Int {
        val sharedPref = context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE)
        return sharedPref.getInt(openCountKey, 0)
    }

    // Reset open count
    private fun resetOpenCount() {
        val sharedPref = context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putInt(openCountKey, 0)
        editor.apply()
    }

    // Check if interstitial ads should be shown
    fun shouldShowInterstitialAds(): Boolean {
        val clickCount = getClickCount()
        return if (clickCount == 0) {
            true
        } else if (clickCount < Constants.showAdsOnEveryClick) {
            incrementClickCount()
            false
        } else {
            resetClickCount()
            true
        }
    }

    // Check if app open ads should be shown
    fun shouldShowAppOpenAds(): Boolean {
        val clickCount = getOpenCount()
        return if (clickCount == 0) {
            true
        } else if (clickCount < Constants.showAdsOnEveryOpen) {
            incrementOpenCount()
            false
        } else {
            resetOpenCount()
            true
        }
    }


    // New properties

    // Check if the app is running
    fun isConnected(): Boolean {
        val sharedPref = context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE)
        return sharedPref.getBoolean(isConnectedKey, false)
    }

    // Set the app running state
    fun setConnected(isRunning: Boolean) {
        val sharedPref = context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putBoolean(isConnectedKey, isRunning)
        editor.apply()
    }

    fun saveVpnServers(vpnServers: List<VpnServer>) {
        val sharedPref = context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putString(vpnServersKey, gson.toJson(vpnServers))
        editor.apply()
    }

    fun getVpnServers(): List<VpnServer> {
        val sharedPref = context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE)
        val json = sharedPref.getString(vpnServersKey, null)
        return if (json != null) {
            val type = object : TypeToken<List<VpnServer>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyList()
        }
    }

    fun saveSelectedVpnServer(vpnServer: VpnServer) {
        val sharedPref = context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putString(selectedVpnServersKey, gson.toJson(vpnServer))
        editor.apply()
    }

    fun getSelectedVpnServer(): VpnServer? {
        val sharedPref = context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE)
        val json = sharedPref.getString(selectedVpnServersKey, null)
        return if (json != null) {
            val type = object : TypeToken<VpnServer>() {}.type
            gson.fromJson(json, type)
        } else {
            null
        }
    }

}