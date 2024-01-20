package com.newagedevs.smartvpn.extensions

import android.content.Context
import android.net.ConnectivityManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.newagedevs.smartvpn.R
import java.lang.Exception
import java.math.BigDecimal
import java.math.RoundingMode


fun formatSize(v: Long): String {
    if (v < 1024) return "$v B/s"
    val z = (63 - java.lang.Long.numberOfLeadingZeros(v)) / 10
    return String.format("%.1f %sB", v.toDouble() / (1L shl z * 10), " KMGTPE"[z])
}

@Suppress("DEPRECATION")
fun Context.isNetworkConnected(): Boolean {
    return try{
        val cm = getSystemService(AppCompatActivity.CONNECTIVITY_SERVICE) as ConnectivityManager
        val nInfo = cm.activeNetworkInfo
        nInfo != null && nInfo.isConnectedOrConnecting
    } catch (e: Exception) {
        false
    }
}


fun Int.round(value: Double): Double {
    require(this >= 0)
    var bd: BigDecimal = try {
        BigDecimal(value)
    } catch (ex: Exception) {
        return 0.0
    }
    bd = bd.setScale(this, RoundingMode.HALF_UP)
    return bd.toDouble()
}

fun Double.round(places: Int): Double {
    require(places >= 0)
    return toBigDecimal().setScale(places, RoundingMode.HALF_UP).toDouble()
}

