package com.newagedevs.smartvpn.network.speed

import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Measures upload throughput against an Ookla-compatible test server.
 *
 * The byte counter is an instance field: it used to live in a `companion object`,
 * so every subsequent test kept adding to the previous run's total.
 */
class UploadTest(
    private val fileURL: String,
    private val timeoutSeconds: Int = 8,
) : Thread() {

    @Volatile
    var finalUploadRate = 0.0
        private set

    @Volatile
    var instantUploadRate = 0.0
        private set

    @Volatile
    var uploadedKBytes = 0L
        private set

    @Volatile
    var isFinished = false
        private set

    private var startTime = 0L

    private val workers = 4

    override fun run() {
        startTime = System.currentTimeMillis()
        try {
            val url = URL("${fileURL}upload.php")
            val executor = Executors.newFixedThreadPool(workers)
            try {
                repeat(workers) { executor.execute { uploadLoop(url) } }
            } finally {
                executor.shutdown()
                if (!executor.awaitTermination(timeoutSeconds + 5L, TimeUnit.SECONDS)) {
                    executor.shutdownNow()
                }
            }
        } catch (e: Exception) {
            // Reported as a zero rate below.
        } finally {
            val elapsed = elapsedSeconds()
            finalUploadRate = megabitsPerSecond(uploadedKBytes, elapsed)
            instantUploadRate = 0.0
            isFinished = true
        }
    }

    private fun uploadLoop(url: URL) {
        val payload = ByteArray(150 * 1024)
        while (elapsedSeconds() < timeoutSeconds) {
            var connection: HttpURLConnection? = null
            try {
                // Ookla test servers answer on plain HTTP; casting to
                // HttpsURLConnection (as the previous code did) threw
                // ClassCastException before a single byte was sent.
                connection = (url.openConnection() as HttpURLConnection).apply {
                    doOutput = true
                    requestMethod = "POST"
                    connectTimeout = 10_000
                    readTimeout = 10_000
                    setRequestProperty("Connection", "Keep-Alive")
                    setRequestProperty("Content-Type", "application/octet-stream")
                }
                DataOutputStream(connection.outputStream).use { it.write(payload) }
                connection.responseCode
                uploadedKBytes += payload.size / 1024L
            } catch (e: Exception) {
                return
            } finally {
                connection?.disconnect()
            }
        }
    }

    private fun elapsedSeconds(): Double = (System.currentTimeMillis() - startTime) / 1000.0

    private fun megabitsPerSecond(kbytes: Long, seconds: Double): Double {
        if (kbytes <= 0L || seconds <= 0.0) return 0.0
        return kbytes.toDouble() * 1024.0 * 8.0 / 1_000_000.0 / seconds
    }
}
