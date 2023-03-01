package com.newagedevs.vpnthundershield.extensions

import android.view.View
import android.view.ViewAnimationUtils
import androidx.annotation.ColorInt
import androidx.core.graphics.drawable.DrawableCompat
import kotlin.math.hypot

/** makes visible a view. */
fun View.visible() {
  visibility = View.VISIBLE
}
