package com.newagedevs.smartvpn.binding

import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.newagedevs.smartvpn.R
import com.newagedevs.smartvpn.utils.Utils


object ViewBinding {

  @JvmStatic
  @BindingAdapter("loadResource")
  fun bindLoadImage(view: ImageView, src: Int?) {
    Glide.with(view.context)
      .load(src)
      .into(view)
  }

}
