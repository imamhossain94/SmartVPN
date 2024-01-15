package com.newagedevs.smartvpn.network

import com.github.doyaaaaaken.kotlincsv.client.CsvReader
import com.newagedevs.smartvpn.model.IPDetails
import com.newagedevs.smartvpn.model.VpnServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import timber.log.Timber
import java.io.IOException
import java.net.URL
import java.nio.charset.Charset

class APIs {
    companion object {
        suspend fun getVPNServers(
            onFailure: (message: String) -> Unit,
            onSuccess: suspend (servers: List<VpnServer>) -> Unit
        ) {
            try {
                val vpnList = mutableListOf<VpnServer>()

                val request = Request.Builder()
                    .url("https://www.vpngate.net/api/iphone/")
                    .build()

                val client = OkHttpClient()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        onFailure("Unexpected code ${response.code}")
                        return
                    }

                    val csvString = response.body!!.string().split("#")[1].replace("*", "").trim()

                    csvString.let {
                        val list = CsvReader().readAll(it)
                        val header = list[0]

                        for (i in 1 until list.size) {
                            val tempJson = mutableMapOf<String, Any>()

                            for (j in header.indices) {
                                tempJson[header[j]] = list[i][j]
                            }

                            vpnList.add(VpnServer(tempJson))
                        }
                    }

                    onSuccess(vpnList)
                }
            } catch (e: Exception) {
                onFailure(e.message ?: "Unknown error")
            }
        }

        suspend fun getIPDetails(): IPDetails {
            var ipDetails = IPDetails()
            try {
                val url = URL("http://ip-api.com/json/")
                val res = withContext(Dispatchers.IO) {
                    url.readText(Charset.defaultCharset())
                }
                val data = JSONObject(res)
                Timber.d(data.toString())
                ipDetails = IPDetails.fromJson(data)
            } catch (e: Exception) {
                Timber.d("\ngetIPDetailsE: $e")
            }
            return ipDetails
        }
    }
}
