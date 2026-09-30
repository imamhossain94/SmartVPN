package com.newagedevs.smartvpn.network.speed

import android.os.Build
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/**
 * Latency probe built on the system `ping` binary.
 *
 * `/system/bin/ping` is not guaranteed to exist on every Android 12+ image, and
 * the previous implementation returned early on an unreachable host without
 * closing the process, the reader, or setting `isFinished` -- which left the
 * caller's polling loop running for the life of the process.
 */
class PingTest(
    private val server: String,
    private val pingTryCount: Int = 3,
) : Thread() {

    @Volatile
    var instantRtt = 0.0
        private set

    @Volatile
    var avgRtt = 0.0
        private set

    @Volatile
    var isFinished = false
        private set

    private val samples = mutableListOf<Double>()

    override fun run() {
        var process: Process? = null
        try {
            val builder = ProcessBuilder("ping", "-c", pingTryCount.toString(), server)
            builder.redirectErrorStream(true)
            process = builder.start()

            process.inputStream.bufferedReader().use { reader: BufferedReader ->
                var line = reader.readLine()
                while (line != null) {
                    parseLine(line)
                    line = reader.readLine()
                }
            }

            // Process.waitFor(long, TimeUnit) only exists from API 26, but
            // minSdk is 23. The reader above already drains the output to EOF,
            // so waiting is a best-effort guard against a child that never exits.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                !process.waitFor(10, TimeUnit.SECONDS)
            ) {
                process.destroyForcibly()
            }
        } catch (e: Exception) {
            // No `ping` binary, or the host refused. The result simply stays 0,
            // which the UI already renders as "unknown".
            Timber.w(e, "Ping test unavailable for %s", server)
        } finally {
            process?.destroy()
            if (samples.isNotEmpty()) {
                avgRtt = samples.sum() / samples.size
                instantRtt = samples.last()
            }
            isFinished = true
        }
    }

    private fun parseLine(line: String) {
        when {
            line.contains("%100 packet loss") -> Timber.d("Ping: 100%% packet loss")

            // "64 bytes from 1.2.3.4: icmp_seq=1 ttl=54 time=12.3 ms"
            line.contains("icmp_seq") -> extractTime(line)?.let { samples += it }

            // "rtt min/avg/max/mdev = 11.2/12.4/13.0/0.8 ms"
            line.startsWith("rtt ") || line.contains("min/avg/max") -> {
                val summary = line.substringAfter("=", "").trim().split("/")
                summary.getOrNull(1)?.toDoubleOrNull()?.let { avgRtt = it }
                summary.getOrNull(0)?.toDoubleOrNull()?.let { samples += it }
            }
        }
    }

    private fun extractTime(line: String): Double? {
        val marker = "time="
        val index = line.indexOf(marker)
        if (index < 0) return null
        val rest = line.substring(index + marker.length)
        val value = rest.takeWhile { it.isDigit() || it == '.' }
        return value.toDoubleOrNull()
    }
}
