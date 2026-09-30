package com.newagedevs.smartvpn.network.speed

import android.location.Location
import com.newagedevs.smartvpn.network.speed.model.Client
import com.newagedevs.smartvpn.network.speed.model.Server
import com.newagedevs.smartvpn.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import timber.log.Timber
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Parses the Ookla `speedtest-config.php` payload and returns its `<client>` element.
 */
internal fun parseClientXML(xmlData: String): Client? = runCatching {
    var result: Client? = null
    forEachElement(xmlData, "client") { parser ->
        result = Client(
            parser.attr("ip").orEmpty(),
            parser.attr("lat").orEmpty(),
            parser.attr("lon").orEmpty(),
            parser.attr("isp").orEmpty(),
            parser.attr("isprating").orEmpty(),
            parser.attr("rating").orEmpty(),
            parser.attr("ispdlavg").orEmpty(),
            parser.attr("ispulavg").orEmpty(),
            parser.attr("loggedin").orEmpty(),
            parser.attr("country").orEmpty(),
        )
    }
    result
}.onFailure { Timber.e(it, "Unable to parse the speed test client config") }.getOrNull()

/** Parses the Ookla `speedtest-servers-static.php` payload into its `<server>` elements. */
internal fun parseServerXML(xmlData: String): List<Server> = runCatching {
    val servers = mutableListOf<Server>()
    forEachElement(xmlData, "server") { parser ->
        // A server without a usable url or host is useless to the test.
        val url = parser.attr("url").orEmpty()
        val host = parser.attr("host").orEmpty()
        if (url.isBlank() || host.isBlank()) return@forEachElement

        servers += Server(
            url,
            parser.attr("lat").orEmpty(),
            parser.attr("lon").orEmpty(),
            parser.attr("name").orEmpty(),
            parser.attr("country").orEmpty(),
            parser.attr("cc").orEmpty(),
            parser.attr("sponsor").orEmpty(),
            parser.attr("id").orEmpty(),
            host,
        )
    }
    servers
}.onFailure { Timber.e(it, "Unable to parse the speed test server list") }.getOrDefault(emptyList())

/** Walks the document, invoking [action] once per `<tag>` start event. */
private inline fun forEachElement(xmlData: String, tag: String, action: (XmlPullParser) -> Unit) {
    val parser = XmlPullParserFactory.newInstance().apply { isNamespaceAware = true }.newPullParser()
    parser.setInput(xmlData.reader())

    var event = parser.eventType
    while (event != XmlPullParser.END_DOCUMENT) {
        if (event == XmlPullParser.START_TAG && parser.name == tag) action(parser)
        event = parser.next()
    }
}

private fun XmlPullParser.attr(name: String): String? =
    getAttributeValue(null, name)?.trim()?.takeIf { it.isNotEmpty() }

suspend fun getNetworkClient(): Client? = withContext(Dispatchers.IO) {
    runCatching { fetchXml(Constants.speedTestClientURL) }
        .onFailure { Timber.e(it, "Unable to reach the speed test config endpoint") }
        .getOrNull()
        ?.let(::parseClientXML)
}

/** Picks the geometrically closest test server to the client. */
suspend fun findBestServer(client: Client): Server? = withContext(Dispatchers.IO) {
    val servers = runCatching { fetchXml(Constants.speedTestServerURL) }
        .onFailure { Timber.e(it, "Unable to reach the speed test server list") }
        .getOrNull()
        ?.let(::parseServerXML)
        .orEmpty()

    val clientLat = client.lat.toDoubleOrNull() ?: return@withContext null
    val clientLon = client.lon.toDoubleOrNull() ?: return@withContext null

    servers
        .filter { it.lat.toDoubleOrNull() != null && it.lon.toDoubleOrNull() != null }
        .minByOrNull { server ->
            val result = FloatArray(1)
            Location.distanceBetween(
                clientLat, clientLon,
                server.lat.toDouble(), server.lon.toDouble(),
                result,
            )
            result[0].toDouble()
        }
}

private fun fetchXml(url: URL): String {
    val connection = (url.openConnection() as HttpURLConnection).apply {
        connectTimeout = 15_000
        readTimeout = 20_000
        requestMethod = "GET"
    }
    try {
        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            throw IOException("HTTP ${connection.responseCode} from $url")
        }
        return connection.inputStream.bufferedReader().use { it.readText() }
    } finally {
        connection.disconnect()
    }
}
