package com.newagedevs.smartvpn.extensions

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

/**
 * Pads [this] so its content starts below the status bar and ends above the
 * navigation bar.
 *
 * Targeting SDK 35+ makes Android draw every app edge-to-edge and ignore
 * `android:statusBarColor`, so the title bars and the server lists were being
 * rendered underneath the system bars. The status bar itself is left
 * transparent and the root background shows through, which keeps the
 * light/dark theming intact.
 */
fun Activity.applyEdgeToEdgeInsets(root: View) {
    WindowCompat.setDecorFitsSystemWindows(window, false)

    val initialTop = root.paddingTop
    val initialBottom = root.paddingBottom
    val initialLeft = root.paddingLeft
    val initialRight = root.paddingRight

    ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
        val bars = insets.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        )
        view.updatePadding(
            left = initialLeft + bars.left,
            top = initialTop + bars.top,
            right = initialRight + bars.right,
            bottom = initialBottom + bars.bottom,
        )
        insets
    }
    ViewCompat.requestApplyInsets(root)
}


fun formatSize(v: Long): String {
    if (v < 1024) return "$v B/s"
    val z = (63 - java.lang.Long.numberOfLeadingZeros(v)) / 10
    return String.format(Locale.US, "%.1f %sB", v.toDouble() / (1L shl z * 10), " KMGTPE"[z])
}

/**
 * Reports whether the device currently has a validated internet connection.
 *
 * Replaces `ConnectivityManager.activeNetworkInfo`, which is deprecated and, from
 * API 29, may throw for a VPN app inspecting its own tunnel.
 */
fun Context.isNetworkConnected(): Boolean {
    val manager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return false
    val network = manager.activeNetwork ?: return false
    val capabilities = manager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

fun Int.round(value: Double): Double {
    require(this >= 0)
    if (!value.isFinite()) return 0.0
    return BigDecimal(value).setScale(this, RoundingMode.HALF_UP).toDouble()
}

/**
 * Rounds to [places] decimals.
 *
 * `toBigDecimal()` throws [NumberFormatException] for `NaN` and `Infinity`, which
 * is how a division by a zero elapsed time used to crash the speed test.
 */
fun Double.round(places: Int): Double {
    require(places >= 0)
    if (!isFinite()) return 0.0
    return toBigDecimal().setScale(places, RoundingMode.HALF_UP).toDouble()
}
