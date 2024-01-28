package com.newagedevs.smartvpn.view.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.hjq.bar.OnTitleBarListener
import com.hjq.bar.TitleBar
import com.newagedevs.smartvpn.R
import com.newagedevs.smartvpn.databinding.ActivityNetworkInfoBinding
import com.newagedevs.smartvpn.network.speed.DownloadTest
import com.newagedevs.smartvpn.network.speed.PingTest
import com.newagedevs.smartvpn.network.speed.UploadTest
import com.newagedevs.smartvpn.network.speed.findBestServer
import com.newagedevs.smartvpn.network.speed.getNetworkClient
import com.newagedevs.smartvpn.network.speed.model.Server
import com.skydoves.bindables.BindingActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.viewmodel.ext.android.viewModel
import java.net.URL
import com.newagedevs.smartvpn.extensions.round

class NetworkInfoActivity : BindingActivity<ActivityNetworkInfoBinding>(R.layout.activity_network_info) {

    private val viewModel: MainViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding {
            vm = viewModel
        }

        binding.tbMainBar.setOnTitleBarListener(object : OnTitleBarListener {
            override fun onLeftClick(titleBar: TitleBar) {
                finish()
            }

            override fun onRightClick(titleBar: TitleBar) {

            }
        })

        binding.gaugeView.setTargetValue(0f)

        viewModel.fetchIpDetails {
            this@NetworkInfoActivity.runOnUiThread {
                viewModel.currentIPDetails?.let {

                    binding.ipAddress.setRightText(it.ip)
                    binding.internetProvider.setRightText(it.isp)
                    binding.location.setRightText("${it.city}, ${it.regionName}, ${it.country}")
                    binding.postalCode.setRightText(it.zip)
                    binding.timezone.setRightText(it.timezone)

                }
            }
        }

        binding.speedTestButton.setOnClickListener {
            networkSpeedTest()
        }


    }

    private fun networkSpeedTest() {
        val defaultMbps = "---"
        val defaultMs = "---"

        binding.speedTestButton.text = "Test Running"
        binding.bestServer.setRightText("---")
        binding.ping.setRightText(defaultMs)
        binding.downloadSpeed.setRightText(defaultMbps)
        binding.uploadSpeed.setRightText(defaultMbps)

        //recheckButton.visibility = View.INVISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = getNetworkClient()
                if (client != null) {
                    val bestServer = findBestServer(client)
                    if (bestServer != null) {
                        withContext(Dispatchers.Main) {
                            binding.bestServer.setRightText(bestServer.sponsor)
                        }

                        runNetworkTests(bestServer)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun runNetworkTests(bestServer: Server) {
        withContext(Dispatchers.Main) {
            try {
                binding.bestServer.setRightText(bestServer.sponsor)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        var pingTestStarted = false
        var pingTestFinished = false
        var downloadTestStarted = false
        var downloadTestFinished = false
        var uploadTestStarted = false
        var uploadTestFinished = false

        val url = URL(bestServer.serverUrl)
        val baseUrl = url.protocol + "://" + url.host + ":" + url.port + "/speedtest/"

        val pingTest = PingTest(bestServer.host.replace(":8080", ""), 3)
        val downloadTest = DownloadTest(baseUrl.replace("http://", "https://"))
        val uploadTest = UploadTest(baseUrl.replace("http://", "https://"))

        while (true) {
            if (!pingTestStarted) {
                pingTest.start()
                pingTestStarted = true
            }
            if (pingTestFinished && !downloadTestStarted) {
                downloadTest.start()
                downloadTestStarted = true
            }
            if (downloadTestFinished && !uploadTestStarted) {
                uploadTest.start()
                uploadTestStarted = true
            }

            //Ping Test
            withContext(Dispatchers.Main) {
                if (pingTestFinished) {
                    if (pingTest.avgRtt.toInt() == 0) {
                        println("Ping error...")
                    } else {
                        binding.ping.setRightText(String.format("%s ms", pingTest.avgRtt))
                    }
                } else {
                    binding.ping.setRightText(String.format("%s ms", pingTest.instantRtt))
                }
            }

            //Download Test
            withContext(Dispatchers.Main) {
                if (pingTestFinished) {
                    if (downloadTestFinished) {
                        if (downloadTest.finalDownloadRate.toInt() == 0) {
                            println("Download error...")
                        } else {
                            binding.gaugeView.setTargetValue(0f)
                            binding.downloadSpeed.setRightText(String.format("%s Mbps", downloadTest.finalDownloadRate.round(2)))
                        }
                    } else {
                        binding.gaugeView.setTargetValue(downloadTest.instantDownloadRate.toFloat())
                        binding.downloadSpeed.setRightText(String.format("%s Mbps", downloadTest.instantDownloadRate.round(2)))
                    }
                }
            }

            //Upload Test
            withContext(Dispatchers.Main) {
                if (downloadTestFinished) {
                    if (uploadTestFinished) {
                        if (uploadTest.finalUploadRate.toInt() == 0) {
                            println("Upload error...")
                        } else {
                            binding.gaugeView.setTargetValue(0f)
                            binding.uploadSpeed.setRightText(String.format("%s Mbps", uploadTest.finalUploadRate.round(2)))
                        }
                        binding.speedTestButton.text = "Start Test"
                        Toast.makeText(this@NetworkInfoActivity, "Speed test completed", Toast.LENGTH_SHORT).show()
                    } else {
                        binding.gaugeView.setTargetValue(uploadTest.instantUploadRate.toFloat())
                        binding.uploadSpeed.setRightText(String.format("%s Mbps", uploadTest.instantUploadRate.round(2)))
                    }
                }
            }

            if (pingTestFinished && downloadTestFinished && uploadTest.isFinished) {
                withContext(Dispatchers.Main) {
                    binding.gaugeView.setTargetValue(0f)
                }
                break
            }
            if (pingTest.isFinished) {
                pingTestFinished = true
            }
            if (downloadTest.isFinished) {
                downloadTestFinished = true

            }
            if (uploadTest.isFinished) {
                uploadTestFinished = true
            }

            if (!pingTestFinished) {
                try {
                    delay(300)
                } catch (_: InterruptedException) { }
            } else {
                try {
                    delay(100)
                } catch (_: InterruptedException) { }
            }
        }

    }

}