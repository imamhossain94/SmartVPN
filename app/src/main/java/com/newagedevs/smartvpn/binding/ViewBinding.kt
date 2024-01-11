package com.newagedevs.smartvpn.binding

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.newagedevs.smartvpn.R
import com.newagedevs.smartvpn.extensions.getResource
import com.newagedevs.smartvpn.model.VpnServer


object ViewBinding {

  @JvmStatic
  @BindingAdapter("loadResource")
  fun bindLoadImage(view: ImageView, src: VpnServer?) {

    src?.let {

      val resName = if(src.countryLong.lowercase().contains("united")) {
        "flag_usa"
      } else if(src.countryLong.lowercase().contains("korea")) {
        "flag_korea"
      } else if(src.countryLong.lowercase().contains("viet")) {
        "flag_vietnam"
      }  else if(src.countryLong.lowercase().contains("russian")) {
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

}
