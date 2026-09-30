package com.newagedevs.smartvpn.network.speed

import timber.log.Timber
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Latency probe for the speed test's test server.
 *
 * This used to shell out to `/system/bin/ping`. That cannot work from a normal
 * Android app: SELinux denies the `ping` binary to the `untrusted_app` domain,
 * so the child either died or hung until the watchdog killed it and every run
 * reported "unavailable".
 *
 * A TCP connect against the test server's own port measures the same
 * round-trip latency and is permitted from inside the app sandbox. It is a
 * handshake rather than an ICMP echo, so it is very slightly higher than a true
 * ICMP ping, but it is stable and comparable between runs.
 */
class PingTest(
    private val host: String,
    private val port: Int,
    private val probes: Int = 3,
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
        try {
            repeat(probes) {
                probe()?.let(samples::add)
                if (samples.isNotEmpty()) instantRtt = samples.last()
            }
        } catch (e: Exception) {
            Timber.w(e, "Latency probe failed for %s:%d", host, port)
        } finally {
            if (samples.isNotEmpty()) avgRtt = samples.sum() / samples.size
            isFinished = true
        }
    }

    /** One TCP handshake, in milliseconds, or null when the host does not answer. */
    private fun probe(): Double? {
        val started = System.nanoTime()
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), PROBE_TIMEOUT_MS)
            }
            val elapsedMs = (System.nanoTime() - started) / 1_000_000.0
            // Guard against a zero-millisecond result on a loopback-ish path.
            if (elapsedMs > 0.0) elapsedMs else null
        } catch (e: IOException) {
            Timber.d("Latency probe to %s:%d failed: %s", host, port, e.message)
            null
        }
    }

    private companion object {
        const val PROBE_TIMEOUT_MS = 5_000
    }
}
