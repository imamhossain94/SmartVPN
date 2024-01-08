package com.newagedevs.smartvpn.utils

import android.content.Context
import android.content.SharedPreferences
import com.newagedevs.smartvpn.R
import com.newagedevs.smartvpn.model.Server
import com.newagedevs.smartvpn.utils.Utils.getImgURL

class SharedPreference(private val context: Context) {
    private val mPreference: SharedPreferences = context.getSharedPreferences(APP_PREFS_NAME, Context.MODE_PRIVATE)
    private val mPrefEditor: SharedPreferences.Editor = mPreference.edit()

    /**
     * Save server details
     * @param server details of ovpn server
     */
    fun saveServer(server: Server) {
        mPrefEditor.putString(SERVER_COUNTRY, server.countryName)
        mPrefEditor.putInt(SERVER_FLAG, server.flagDrawable)
        mPrefEditor.putString(SERVER_OVPN, server.openVpn)
        mPrefEditor.putString(SERVER_OVPN_USER, server.openVpnUserName)
        mPrefEditor.putString(SERVER_OVPN_PASSWORD, server.openVpnUserPassword)
        mPrefEditor.commit()
    }

    val server: Server
        /**
         * Get server data from shared preference
         * @return server model object
         */
        get() = Server(
            mPreference.getString(SERVER_IP, "192.168.22.36"),
            mPreference.getString(SERVER_COUNTRY, "Japan"),
            mPreference.getInt(SERVER_FLAG, R.drawable.flag_japan),
            mPreference.getString(SERVER_OVPN, "japan.ovpn"),
            mPreference.getString(SERVER_OVPN_USER, "vpn"),
            mPreference.getString(SERVER_OVPN_PASSWORD, "vpn")
        )

    companion object {
        private const val APP_PREFS_NAME = "SmartVPNPreference"
        private const val SERVER_IP = "server_ip"
        private const val SERVER_COUNTRY = "server_country"
        private const val SERVER_FLAG = "server_flag"
        private const val SERVER_OVPN = "server_ovpn"
        private const val SERVER_OVPN_USER = "server_ovpn_user"
        private const val SERVER_OVPN_PASSWORD = "server_ovpn_password"
    }
}
