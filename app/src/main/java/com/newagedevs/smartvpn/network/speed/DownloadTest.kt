package com.newagedevs.smartvpn.network.speed

import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
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
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        // Ookla's discovery URLs answer 307 and point at a different host, so
        // redirects have to be followed for the transfer to start at all.
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private var startTime = 0L

    private val files = listOf("random4000x4000.jpg", "random3000x3000.jpg")

    override fun run() {
        startTime = System.currentTimeMillis()
        try {
            for (name in files) {
                // The response body must be consumed inside `use`: returning a
                // stream out of that block closes it, and every subsequent
                // read throws, which is why the download used to report 0.
                val read = streamOnce("$fileURL$name") ?: break
                downloadedBytes += read
                if (elapsedSeconds() >= timeoutSeconds) break
            }
        } catch (e: IOException) {
            Timber.w(e, "Download test failed for %s", fileURL)
        } finally {
            val elapsed = elapsedSeconds()
            finalDownloadRate = if (elapsed > 0.0) megabitsPerSecond(downloadedBytes, elapsed) else 0.0
            instantDownloadRate = 0.0
            isFinished = true
        }
    }

    /**
     * Streams [url] to the end (or until the timeout) and returns the byte count.
     *
     * Reading happens inside the `use` block on purpose. Handing the stream back
     * to the caller and closing the response there closes the stream too, and
     * every later read throws -- that is what made the download leg report 0
     * while the upload leg worked.
     */
    private fun streamOnce(url: String): Long {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Timber.w("Download %s returned HTTP %d", url, response.code)
                return 0L
            }

            val stream = response.body.byteStream()
            val buffer = ByteArray(64 * 1024)
            var total = 0L
            while (true) {
                val read = stream.read(buffer)
                if (read == -1) break
                total += read
                downloadedBytes = total
                val elapsed = elapsedSeconds()
                if (elapsed > 0.0) {
                    instantDownloadRate = megabitsPerSecond(total, elapsed)
                }
                if (elapsed >= timeoutSeconds) break
            }
            return total
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

