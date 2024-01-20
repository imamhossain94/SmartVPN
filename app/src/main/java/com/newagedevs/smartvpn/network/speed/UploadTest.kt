package com.newagedevs.smartvpn.network.speed

import com.newagedevs.smartvpn.extensions.round
import java.io.DataOutputStream
import java.math.BigDecimal
import java.net.URL
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocketFactory

class UploadTest(fileURL: String) : Thread() {
    var fileURL = ""
    var uploadElapsedTime = 0.0
    var isFinished = false
    var elapsedTime = 0.0
    var finalUploadRate = 0.0
    var startTime: Long = 0

    init {
        this.fileURL = fileURL
    }

    val instantUploadRate: Double
        get() {
            try {
                val bd = BigDecimal(uploadedKByte)
            } catch (ex: Exception) {
                return 0.0
            }
            return if (uploadedKByte >= 0) {
                val now = System.currentTimeMillis()
                elapsedTime = (now - startTime) / 1000.0
                2.round((uploadedKByte / 1000.0 * 8 / elapsedTime))
            } else {
                0.0
            }
        }

    override fun run() {
        try {
            val url = URL(fileURL)
            uploadedKByte = 0
            startTime = System.currentTimeMillis()
            val executor: ExecutorService = Executors.newFixedThreadPool(4)
            for (i in 0..3) {
                executor.execute(HandlerUpload(url))
            }
            executor.shutdown()
            while (!executor.isTerminated) {
                try {
                    sleep(100)
                } catch (_: InterruptedException) {
                }
            }
            val now = System.currentTimeMillis()
            uploadElapsedTime = (now - startTime) / 1000.0
            finalUploadRate = (uploadedKByte / 1000.0 * 8 / uploadElapsedTime)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        isFinished = true
    }

    companion object {
        var uploadedKByte = 0
    }
}

internal class HandlerUpload(var url: URL) : Thread() {
    override fun run() {
        val buffer = ByteArray(150 * 1024)
        val startTime = System.currentTimeMillis()
        val timeout = 8
        while (true) {
            try {
                var conn: HttpsURLConnection?
                conn = url.openConnection() as HttpsURLConnection
                conn.doOutput = true
                conn.requestMethod = "POST"
                conn.setRequestProperty("Connection", "Keep-Alive")
                conn.sslSocketFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
                conn.hostnameVerifier = HostnameVerifier { hostname, _ ->
                    hostname == url.host
                }
                conn.connect()
                val dos = DataOutputStream(conn.outputStream)
                dos.write(buffer, 0, buffer.size)
                dos.flush()
                conn.responseCode
                UploadTest.uploadedKByte += (buffer.size / 1024.0).toInt()
                val endTime = System.currentTimeMillis()
                val uploadElapsedTime = (endTime - startTime) / 1000.0
                if (uploadElapsedTime >= timeout) {
                    break
                }
                dos.close()
                conn.disconnect()
            } catch (ex: Exception) {
                ex.printStackTrace()
                break
            }
        }
    }
}