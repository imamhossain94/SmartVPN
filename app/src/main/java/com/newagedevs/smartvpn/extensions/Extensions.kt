package com.newagedevs.smartvpn.extensions

import kotlin.math.log10
import kotlin.math.pow

fun formatBytes(bytes: Int, decimals: Int = 1): String {
    if (bytes <= 0) return "0 B"
    val suffixes = listOf("Bps", "Kbps", "Mbps", "Gbps", "Tbps")
    val i = (log10(bytes.toDouble()) / log10(1024.0))
    return "${"%.${decimals}f".format(bytes / 1024.0.pow(i))} ${suffixes[i.toInt()]}"
}

fun formatSize(v: Long): String {
    if (v < 1024) return "$v B"
    val z = (63 - java.lang.Long.numberOfLeadingZeros(v)) / 10
    return String.format("%.1f %sB", v.toDouble() / (1L shl z * 10), " KMGTPE"[z])
}


