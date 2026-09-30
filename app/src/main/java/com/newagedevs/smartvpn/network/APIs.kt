package com.newagedevs.smartvpn.network

import android.content.Context
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
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

object APIs {

    private val client by lazy {
        OkHttpClient.Builder()
            // The VPNGate feed is ~1.3 MB of base64 configs and is regularly slow
            // on mobile links, so the read budget has to be generous. Anything
            // tighter means the list never arrives and the user sees no servers.
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .callTimeout(150, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    /**
     * Reads the bundled server snapshot from `assets/vpngate_fallback.csv`.
     *
     * The live endpoint is unreliable (it has been observed taking over two
     * minutes and timing out outright), so this guarantees the app always has
     * something to offer instead of an empty list.
     */
    suspend fun getBundledServers(context: Context): List<VpnServer> = withContext(Dispatchers.IO) {
        runCatching {
            val csv = context.assets.open(FALLBACK_ASSET).use { it.readBytes().toString(StandardCharsets.UTF_8) }
            parseVpnGateCsv(csv)
        }.onFailure { Timber.e(it, "Bundled server snapshot could not be read") }
            .getOrDefault(emptyList())
    }

    /**
     * Downloads the public VPNGate server list and maps it onto [VpnServer].
     *
     * @throws IOException when the endpoint is unreachable or answers with a
     *   non-success status, and [IllegalStateException] when the payload cannot be
     *   parsed into at least one server row.
     */
    suspend fun getVPNServers(): List<VpnServer> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://www.vpngate.net/api/iphone/")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("VPNGate responded with HTTP ${response.code}")
            }
            val body = response.body.string()
            parseVpnGateCsv(body)
        }
    }

    /**
     * Parses the VPNGate CSV format.
     *
     * The live endpoint introduces its header with `#*` and prefixes data rows
     * with `*`; the bundled snapshot uses plain `#` comment lines. Rather than
     * depending on either exact shape, this locates the header row (the first
     * line naming a known column) and parses everything after it. Base64
     * payloads never contain `#` or `*`, so the remaining text is safe to use
     * verbatim.
     */
    private fun parseVpnGateCsv(payload: String): List<VpnServer> {
        val lines = payload.lineSequence()
            .map { it.removePrefix("#").removePrefix("*").trim() }
            .filter { it.isNotEmpty() }
            .toList()

        val headerIndex = lines.indexOfFirst { line ->
            line.startsWith(HEADER_FIRST_COLUMN) || line.startsWith(HEADER_ALT_FIRST_COLUMN)
        }
        if (headerIndex < 0) throw IllegalStateException("VPNGate payload had no recognisable header row")

        val header = CsvReader().readAll(lines[headerIndex]).first().map { it.trim() }
        if (header.size < 8) throw IllegalStateException("VPNGate CSV header was truncated")

        return lines.drop(headerIndex + 1)
            .mapNotNull { line -> CsvReader().readAll(line).firstOrNull() }
            .filter { it.size >= header.size }
            .map { row ->
                mutableMapOf<String, Any>().apply {
                    header.forEachIndexed { index, column -> put(column, row[index]) }
                }
            }
            .map { row -> VpnServer(row) }
    }

    /** Best-effort public IP lookup; returns an empty [IPDetails] on failure. */
    suspend fun getIPDetails(): IPDetails = withContext(Dispatchers.IO) {
        runCatching {
            val res = URL("http://ip-api.com/json/").readText(StandardCharsets.UTF_8)
            IPDetails.fromJson(JSONObject(res))
        }.onFailure { Timber.e(it, "Unable to resolve public IP details") }
            .getOrElse { IPDetails() }
    }

    /** Name of the bundled server snapshot in `assets/`. */
    private const val FALLBACK_ASSET = "vpngate_fallback.csv"

    /** First column of the VPNGate header row, per API revision. */
    private const val HEADER_FIRST_COLUMN = "HostName"
    private const val HEADER_ALT_FIRST_COLUMN = "Filename"
}
