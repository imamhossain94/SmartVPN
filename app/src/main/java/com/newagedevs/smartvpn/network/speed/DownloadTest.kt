package com.newagedevs.smartvpn.network.speed

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.max

/**
 * Measures download throughput against an Ookla-compatible test server.
 *
 * The reported rate is always finite: the previous implementation did the
 * bits-to-Mbit conversion in `Int` arithmetic (truncating to 0 for the first
 * second and overflowing past 256 MB) and then divided by an elapsed time that
 * starts at 0.0, which produced `NaN` and made `round(2)` throw.
 */
class DownloadTest(private val fileURL: String, private val timeoutSeconds: Int = 8) : Thread() {

    @Volatile
    var finalDownloadRate = 0.0
        private set

    @Volatile
    var instantDownloadRate = 0.0
        private set

    @Volatile
    var downloadedBytes = 0L
        private set

    @Volatile
    var isFinished = false
        private set

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private var startTime = 0L

    private val files = listOf("random4000x4000.jpg", "random3000x3000.jpg")

    override fun run() {
        startTime = System.currentTimeMillis()
        try {
            files.forEach { name ->
                val body = downloadOnce("$fileURL$name") ?: return
                try {
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = body.read(buffer)
                        if (read == -1) break
                        downloadedBytes += read
                        val elapsed = elapsedSeconds()
                        if (elapsed <= 0.0) continue
                        instantDownloadRate = megabitsPerSecond(downloadedBytes, elapsed)
                        if (elapsed >= timeoutSeconds) break
                    }
                } finally {
                    body.closeQuietly()
                }
                if (elapsedSeconds() >= timeoutSeconds) return
            }
        } catch (e: IOException) {
            // A failed transfer is reported as a zero rate, not as a stuck test.
        } finally {
            val elapsed = elapsedSeconds()
            finalDownloadRate = if (elapsed > 0.0) megabitsPerSecond(downloadedBytes, elapsed) else 0.0
            instantDownloadRate = 0.0
            isFinished = true
        }
    }

    private fun downloadOnce(url: String): InputStream? {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            return response.body.byteStream()
        }
    }

    private fun elapsedSeconds(): Double = (System.currentTimeMillis() - startTime) / 1000.0

    private fun megabitsPerSecond(bytes: Long, seconds: Double): Double {
        if (bytes <= 0L || seconds <= 0.0) return 0.0
        // bytes * 8 -> bits, / 1_000_000 -> Mbit. Done in Double to avoid both the
        // integer truncation and the Int overflow of the original expression.
        return max(0.0, bytes.toDouble() * 8.0 / 1_000_000.0 / seconds)
    }
}

internal fun java.io.Closeable.closeQuietly() {
    runCatching { close() }
}
