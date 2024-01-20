package com.newagedevs.smartvpn.network.speed

import android.location.Location
import com.newagedevs.smartvpn.network.speed.model.Client
import com.newagedevs.smartvpn.network.speed.model.Server
import com.newagedevs.smartvpn.utils.Constants.Companion.speedTestClientURL
import com.newagedevs.smartvpn.utils.Constants.Companion.speedTestServerURL
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection

fun parseClientXML(xmlData: String): Client? {
    try {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val parser = factory.newPullParser()
        parser.setInput(xmlData.reader())

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    val tagName = parser.name
                    if (tagName == "client") {
                        val ip = parser.getAttributeValue(null, "ip")
                        val lat = parser.getAttributeValue(null, "lat")
                        val lon = parser.getAttributeValue(null, "lon")
                        val isp = parser.getAttributeValue(null, "isp")
                        val isprating = parser.getAttributeValue(null, "isprating")
                        val rating = parser.getAttributeValue(null, "rating")
                        val ispdlavg = parser.getAttributeValue(null, "ispdlavg")
                        val ispulavg = parser.getAttributeValue(null, "ispulavg")
                        val loggedin = parser.getAttributeValue(null, "loggedin")
                        val country = parser.getAttributeValue(null, "country")

                        return Client(
                            ip,
                            lat,
                            lon,
                            isp,
                            isprating,
                            rating,
                            ispdlavg,
                            ispulavg,
                            loggedin,
                            country
                        )
                    }
                }
            }
            eventType = parser.next()
        }

    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}

fun parseServerXML(xmlData: String): ArrayList<Server>? {
    try {
        val servers = ArrayList<Server>()
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val parser = factory.newPullParser()
        parser.setInput(xmlData.reader())

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    val tagName = parser.name
                    if (tagName == "server") {
                        val serverUrl = parser.getAttributeValue(null, "url")
                        val lat = parser.getAttributeValue(null, "lat")
                        val lon = parser.getAttributeValue(null, "lon")
                        val name = parser.getAttributeValue(null, "name")
                        val country = parser.getAttributeValue(null, "country")
                        val cc = parser.getAttributeValue(null, "cc")
                        val sponsor = parser.getAttributeValue(null, "sponsor")
                        val id = parser.getAttributeValue(null, "id")
                        val host = parser.getAttributeValue(null, "host")

                        val server = Server(
                            serverUrl,
                            lat,
                            lon,
                            name,
                            country,
                            cc,
                            sponsor,
                            id,
                            host
                        )

                        servers.add(server)
                    }
                }
            }
            eventType = parser.next()
        }
        return servers
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}

fun getNetworkClient(): Client? {
    val clientConnection = speedTestClientURL.openConnection() as HttpURLConnection
    if (clientConnection.responseCode == 200) {
        val clientBr = BufferedReader(InputStreamReader(clientConnection.inputStream))
        val client = parseClientXML(clientBr.readText())
        clientBr.close()
        return client
    }
    return null
}

fun findBestServer(client: Client): Server? {
    val serverConnection = speedTestServerURL.openConnection() as HttpURLConnection
    if (serverConnection.responseCode == 200) {
        val serverBr = BufferedReader(InputStreamReader(serverConnection.inputStream))
        val servers = parseServerXML(serverBr.readText())
        serverBr.close()

        val source = Location("Source").apply {
            latitude = client.lat.toDouble()
            longitude = client.lon.toDouble()
        }

        var tmp = 19349458.0
        var bestServer: Server? = null

        if (servers != null) {
            for (server in servers) {
                val dest = Location("Dest").apply {
                    latitude = server.lat.toDouble()
                    longitude = server.lon.toDouble()
                }

                val distance = source.distanceTo(dest).toDouble()
                if (tmp > distance) {
                    tmp = distance
                    bestServer = server
                }
            }
        }
        return bestServer
    }
    return null
}
