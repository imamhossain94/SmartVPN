package com.newagedevs.smartvpn.network.speed

import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * @author erdigurbuz
 */
class PingTest(serverIpAddress: String, pingTryCount: Int) : Thread() {
    var result = HashMap<String, Any>()
    var server = ""
    var count: Int
    var instantRtt = 0.0
    var avgRtt = 0.0
    var isFinished = false
    var started = false

    init {
        server = serverIpAddress
        count = pingTryCount
    }

    override fun run() {
        try {
            val ps = ProcessBuilder("ping", "-c $count", server)
            ps.redirectErrorStream(true)
            val pr = ps.start()
            val `in` = BufferedReader(InputStreamReader(pr.inputStream))
            var line: String
            while (`in`.readLine().also { line = it } != null) {
                if (line.contains("icmp_seq")) {
                    instantRtt = line.split(" ".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()[line.split(" ".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray().size - 2].replace("time=", "").toDouble()
                }
                if (line.startsWith("rtt ")) {
                    avgRtt = line.split("/".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()[4].toDouble()
                    break
                }
                if (line.contains("Unreachable") || line.contains("Unknown") || line.contains("%100 packet loss")) {
                    return
                }
            }
            pr.waitFor()
            `in`.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        isFinished = true
    }
}