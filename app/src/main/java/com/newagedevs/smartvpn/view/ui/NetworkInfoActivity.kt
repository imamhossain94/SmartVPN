package com.newagedevs.smartvpn.view.ui

import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.hjq.bar.OnTitleBarListener
import com.hjq.bar.TitleBar
import com.newagedevs.smartvpn.R
import com.newagedevs.smartvpn.databinding.ActivityNetworkInfoBinding
import com.newagedevs.smartvpn.extensions.applyEdgeToEdgeInsets
import com.newagedevs.smartvpn.model.IPDetails
import com.newagedevs.smartvpn.network.speed.DownloadTest
import com.newagedevs.smartvpn.network.speed.PingTest
import com.newagedevs.smartvpn.network.speed.UploadTest
import com.newagedevs.smartvpn.network.speed.findBestServer
import com.newagedevs.smartvpn.network.speed.getNetworkClient
import com.newagedevs.smartvpn.network.speed.model.Server
import com.skydoves.bindables.BindingActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import java.net.URL
import java.util.Locale

class NetworkInfoActivity : BindingActivity<ActivityNetworkInfoBinding>(R.layout.activity_network_info) {

    private val viewModel: MainViewModel by viewModel()

    private var speedTestJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        applyEdgeToEdgeInsets(binding.root)

        binding { vm = viewModel }

        binding.tbMainBar.setOnTitleBarListener(object : OnTitleBarListener {
            override fun onLeftClick(titleBar: TitleBar) = finish()
            override fun onTitleClick(titleBar: TitleBar) = Unit
            override fun onRightClick(titleBar: TitleBar) = Unit
        })

        binding.gaugeView.setTargetValue(0f)

        binding.speedTestButton.setOnClickListener { startSpeedTest() }

        // Collect only while STARTED, so a late response can never touch a
        // detached view hierarchy.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.currentIPDetails.collectLatest(::renderIpDetails)
            }
        }

        viewModel.fetchIpDetails()
    }

    override fun onDestroy() {
        speedTestJob?.cancel()
        super.onDestroy()
    }

    private fun renderIpDetails(details: IPDetails?) {
        if (details == null) return
        binding.ipAddress.setRightText(details.ip)
        binding.internetProvider.setRightText(details.isp)
        binding.location.setRightText(
            listOf(details.city, details.regionName, details.country)
                .filter { it.isNotBlank() }
                .joinToString(", ")
                .ifBlank { "-" }
        )
        binding.postalCode.setRightText(details.zip.ifBlank { "-" })
        binding.timezone.setRightText(details.timezone.ifBlank { "-" })
    }

    private fun startSpeedTest() {
        if (speedTestJob?.isActive == true) return

        speedTestJob = lifecycleScope.launch {
            binding.speedTestButton.setText(R.string.test_running)
            binding.bestServer.setRightText("-")
            binding.ping.setRightText("-")
            binding.downloadSpeed.setRightText("-")
            binding.uploadSpeed.setRightText("-")

            val server = resolveBestServer()
            if (server == null) {
                finishSpeedTestUi()
                toast(getString(R.string.speed_test_unavailable))
                return@launch
            }

            binding.bestServer.setRightText(server.sponsor.ifBlank { "-" })
            val measured = runTests(server)

            finishSpeedTestUi()
            toast(
                getString(
                    if (measured) R.string.speed_test_completed
                    else R.string.speed_test_failed
                )
            )
        }
    }

    private suspend fun resolveBestServer(): Server? {
        val client = runCatching { getNetworkClient() }
            .onFailure { Timber.e(it, "Speed test client lookup failed") }
            .getOrNull() ?: return null
        return runCatching { findBestServer(client) }
            .onFailure { Timber.e(it, "Speed test server lookup failed") }
            .getOrNull()
    }

    private fun finishSpeedTestUi() {
        binding.gaugeView.setTargetValue(0f)
        binding.speedTestButton.setText(R.string.start_test)
    }

    private fun toast(message: String) =
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    /**
     * Runs ping, then download, then upload, polling each measurement while it is
     * in flight.
     *
     * The previous version polled in `while (true)` from a scope that outlived the
     * Activity and never terminated, because the error paths of `PingTest` and
     * `DownloadTest` left their `isFinished` flag false forever. The loop is now
     * bounded by [TEST_TIMEOUT_MS], runs on [lifecycleScope], and exits as soon as
     * the Activity is destroyed.
     */
    private suspend fun runTests(server: Server): Boolean {
        val baseUrl = baseUrlOf(server)
        val pingHost = server.host.substringBefore(':').ifBlank { null }
        if (baseUrl == null || pingHost == null) {
            Timber.w("Skipping speed test for a server with an unusable URL/host")
            return false
        }

        val ping = PingTest(pingHost, PING_COUNT)
        val download = DownloadTest(baseUrl)
        val upload = UploadTest(baseUrl)

        val deadline = System.currentTimeMillis() + TEST_TIMEOUT_MS
        var phase = Phase.PING
        ping.start()

        while (currentCoroutineContext().isActive && System.currentTimeMillis() < deadline) {
            when (phase) {
                Phase.PING -> {
                    // Ping needs the system binary, which many Android builds
                    // deny, so "unavailable" is a normal outcome, not a failure.
                    val rtt = ping.avgRtt.takeIf { it > 0.0 } ?: ping.instantRtt
                    binding.ping.setRightText(
                        if (rtt > 0.0) getString(R.string.ping_ms, rtt.format1())
                        else getString(R.string.ping_unavailable)
                    )
                    if (ping.isFinished) {
                        download.start()
                        phase = Phase.DOWNLOAD
                    }
                }

                Phase.DOWNLOAD -> {
                    val live = download.instantDownloadRate
                    binding.gaugeView.setTargetValue(live.coerceIn(0.0, GAUGE_MAX.toDouble()).toFloat())
                    binding.downloadSpeed.setRightText(formatRate(live))
                    if (download.isFinished) {
                        binding.gaugeView.setTargetValue(0f)
                        binding.downloadSpeed.setRightText(formatRate(download.finalDownloadRate))
                        upload.start()
                        phase = Phase.UPLOAD
                    }
                }

                Phase.UPLOAD -> {
                    val live = upload.instantUploadRate
                    binding.gaugeView.setTargetValue(live.coerceIn(0.0, GAUGE_MAX.toDouble()).toFloat())
                    binding.uploadSpeed.setRightText(formatRate(live))
                    if (upload.isFinished) {
                        binding.gaugeView.setTargetValue(0f)
                        binding.uploadSpeed.setRightText(formatRate(upload.finalUploadRate))
                        phase = Phase.DONE
                    }
                }

                Phase.DONE -> return download.downloadedBytes > 0L || upload.uploadedKBytes > 0L
            }

            delay(POLL_INTERVAL_MS)
        }

        // Ran out of time before every phase reported.
        Timber.w("Speed test hit the ${TEST_TIMEOUT_MS}ms budget before completing")
        return download.downloadedBytes > 0L || upload.uploadedKBytes > 0L
    }

    /** Renders a rate, distinguishing "no data" from a genuine 0.00 Mbps. */
    private fun formatRate(mbps: Double): String =
        if (mbps > 0.0) getString(R.string.rate_mbps, mbps.format2())
        else getString(R.string.rate_unavailable)

    /**
     * Builds the `/speedtest/` base URL for an Ookla server.
     *
     * The previous code rewrote the scheme to `https://` while keeping port 8080.
     * Ookla test servers only speak plain HTTP there, so every request failed and
     * the test could never complete. Returns null when `url` is malformed.
     */
    private fun baseUrlOf(server: Server): String? = runCatching {
        val parsed = URL(server.serverUrl)
        val port = if (parsed.port > 0) ":${parsed.port}" else ""
        "${parsed.protocol}://${parsed.host}$port/speedtest/"
    }.onFailure { Timber.e(it, "Malformed test server URL: %s", server.serverUrl) }
        .getOrNull()

    private fun Double.format1(): String =
        if (isNaN() || isInfinite()) "0.0" else String.format(Locale.US, "%.1f", this)

    private fun Double.format2(): String =
        if (isNaN() || isInfinite()) "0.00" else String.format(Locale.US, "%.2f", this)

    private enum class Phase { PING, DOWNLOAD, UPLOAD, DONE }

    private companion object {
        const val PING_COUNT = 3
        const val POLL_INTERVAL_MS = 300L
        const val TEST_TIMEOUT_MS = 45_000L

        /** Matches `app:scaleEndValue` on the gauge so the needle never overruns. */
        const val GAUGE_MAX = 100.0
    }
}
