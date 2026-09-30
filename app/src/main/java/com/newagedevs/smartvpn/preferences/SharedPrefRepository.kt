package com.newagedevs.smartvpn.preferences

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.newagedevs.smartvpn.model.VpnServer
import com.newagedevs.smartvpn.utils.Constants.Companion.favoriteVpnServersKey
import com.newagedevs.smartvpn.utils.Constants.Companion.isConnectedKey
import com.newagedevs.smartvpn.utils.Constants.Companion.selectedVpnServersKey
import com.newagedevs.smartvpn.utils.Constants.Companion.sharedPrefName
import com.newagedevs.smartvpn.utils.Constants.Companion.vpnServersKey
import timber.log.Timber

class SharedPrefRepository(context: Context) {

    private val gson = Gson()

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE)

    // Connection state

    fun isConnected(): Boolean = prefs.getBoolean(isConnectedKey, false)

    fun setConnected(isRunning: Boolean) {
        prefs.edit().putBoolean(isConnectedKey, isRunning).apply()
    }

    // Server list

    fun saveVpnServers(vpnServers: List<VpnServer>) {
        prefs.edit().putString(vpnServersKey, gson.toJson(vpnServers)).apply()
    }

    fun getVpnServers(): List<VpnServer> =
        decodeList(vpnServersKey, VpnServerListType)
            .sortedBy { it.ping.toIntOrNull() ?: Int.MAX_VALUE }

    // Selected server

    fun saveSelectedVpnServer(vpnServer: VpnServer) {
        prefs.edit().putString(selectedVpnServersKey, gson.toJson(vpnServer)).apply()
    }

    fun getSelectedVpnServer(): VpnServer? {
        val json = prefs.getString(selectedVpnServersKey, null) ?: return null
        return runCatching { gson.fromJson<VpnServer>(json, VpnServerType) }
            .onFailure { Timber.e(it, "Stored VPN server selection was unreadable") }
            .getOrNull()
    }

    // Favorites

    fun addToFavoriteVpnServers(vpnServer: VpnServer) {
        val updated = getFavoriteVpnServers().toMutableList()
        if (updated.contains(vpnServer)) return
        updated.add(vpnServer)
        prefs.edit().putString(favoriteVpnServersKey, gson.toJson(updated)).apply()
    }

    fun removeFromFavoriteVpnServers(vpnServer: VpnServer) {
        val updated = getFavoriteVpnServers().toMutableList()
        if (!updated.remove(vpnServer)) return
        prefs.edit().putString(favoriteVpnServersKey, gson.toJson(updated)).apply()
    }

    fun isFavoriteVpnServer(vpnServer: VpnServer): Boolean =
        getFavoriteVpnServers().contains(vpnServer)

    fun getFavoriteVpnServers(): List<VpnServer> =
        decodeList(favoriteVpnServersKey, VpnServerListType)

    /**
     * Gson happily returns `null` for the literal `"null"` or for a truncated
     * blob, and the Kotlin-side type would otherwise be a platform type that
     * crashes on first use. Anything unreadable degrades to an empty list.
     */
    private fun decodeList(key: String, type: java.lang.reflect.Type): List<VpnServer> {
        val json = prefs.getString(key, null) ?: return emptyList()
        return runCatching { gson.fromJson<List<VpnServer>>(json, type) }
            .onFailure { Timber.e(it, "Stored list for %s was unreadable", key) }
            .getOrNull()
            .orEmpty()
    }

    private companion object {
        val VpnServerType = object : TypeToken<VpnServer>() {}.type
        val VpnServerListType = object : TypeToken<List<VpnServer>>() {}.type
    }
}
