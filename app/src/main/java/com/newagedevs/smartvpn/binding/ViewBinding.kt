package com.newagedevs.smartvpn.binding

import android.view.View
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.facebook.shimmer.ShimmerFrameLayout
import com.newagedevs.smartvpn.R
import com.newagedevs.smartvpn.extensions.getResource
import com.newagedevs.smartvpn.model.VpnServer
import com.skydoves.whatif.whatIfNotNull


object ViewBinding {

  @JvmStatic
  @BindingAdapter("loadServerFlag")
  fun bindServerFlagImage(view: ImageView, src: VpnServer?) {
    src?.let {
      val resName = if (src.countryLong.lowercase().contains("united")) {
        "flag_usa"
      } else if (src.countryLong.lowercase().contains("korea")) {
        "flag_korea"
      } else if (src.countryLong.lowercase().contains("viet")) {
        "flag_vietnam"
      } else if (src.countryLong.lowercase().contains("russian")) {
        "flag_russia"
      } else {
        "flag_${src.countryLong.lowercase()}"
      }

      val resDrawable = view.context.getResource(resName)
      Glide.with(view.context)
        .load(resDrawable)
        .into(view)
    }
  }

  @JvmStatic
  @BindingAdapter("loadServerPing")
  fun bindServerPingImage(view: ImageView, src: VpnServer?) {
    src?.let {
      val resDrawable = when (src.ping.toIntOrNull()) {
        in 0..10 -> R.drawable.ic_network_4
        in 11..20 -> R.drawable.ic_network_3
        in 21..50 -> R.drawable.ic_network_2
        in 51..100 -> R.drawable.ic_network_1
        in 101..300 -> R.drawable.ic_network_0
        else -> R.drawable.ic_network_4
      }

      Glide.with(view.context)
        .load(resDrawable)
        .into(view)
    }
  }

  @JvmStatic
  @BindingAdapter("startShimmer")
  fun bindShimmerAnimation(view: ShimmerFrameLayout, value: Boolean?) {
    value.whatIfNotNull {
      if (it) {
        view.visibility = View.VISIBLE
        view.startShimmer()
      } else {
        view.stopShimmer()
        view.visibility = View.GONE
      }
    }
  }
  
  

}
