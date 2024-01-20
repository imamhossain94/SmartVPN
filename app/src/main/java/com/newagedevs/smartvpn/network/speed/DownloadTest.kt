package com.newagedevs.smartvpn.network.speed

import com.newagedevs.smartvpn.extensions.round
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.StringReader

class DownloadTest(private val fileURL: String) : Thread() {
    private var downloadedByte = 0
    var finalDownloadRate = 0.0
    var isFinished = false
    var instantDownloadRate = 0.0
    private var timeout = 8
    private var client: OkHttpClient = OkHttpClient.Builder()
        .hostnameVerifier { _, _ -> true }
        .build()
    private var startTime = 0L
    private var downloadElapsedTime = 0.0


    private fun setInstantDownloadRate(downloadedByte: Int, elapsedTime: Double) {
        instantDownloadRate = if (downloadedByte >= 0) {
            (downloadedByte * 8 / (1000 * 1000) / elapsedTime).round(2)
        } else {
            0.0
        }
    }

    override fun run() {
        val fileUrls = listOf(
            "$fileURL/random4000x4000.jpg",
            "$fileURL/random3000x3000.jpg"
        )

        startTime = System.currentTimeMillis()

        try {
            for (link in fileUrls) {
                val request = Request.Builder().url(link).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw Exception("Unexpected code $response")

                    val inputString: BufferedReader = BufferedReader(StringReader(link))
                    val myhostname = inputString.readLine()
                    val myVeryfyhost = myhostname.split("://")[1].split(":")[0]

                    if (myVeryfyhost != response.request.url.host) throw Exception("Hostname mismatch")

                    val inputStream = response.body?.byteStream()
                    val buffer = ByteArray(10240)
                    var len: Int
                    while (inputStream?.read(buffer).also { len = it!! } != -1) {
                        downloadedByte += len
                        downloadElapsedTime = (System.currentTimeMillis() - startTime) / 1000.0
                        setInstantDownloadRate(downloadedByte, downloadElapsedTime)
                        if (downloadElapsedTime >= timeout) {
                            break
                        }
                    }
                    inputStream?.close()
                }
            }
            finalDownloadRate = downloadedByte * 8 / (1000 * 1000.0) / downloadElapsedTime
            isFinished = true
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
}
