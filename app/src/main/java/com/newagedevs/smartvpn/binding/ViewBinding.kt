package com.newagedevs.smartvpn.binding

import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide


object ViewBinding {
  @JvmStatic
  @BindingAdapter("loadImage")
  fun bindLoadImage(view: AppCompatImageView, url: String?) {
    Glide.with(view.context)
      .load(url)
      .into(view)
  }


  @JvmStatic
  @BindingAdapter("android:src")
  fun setImageViewResource(imageView: ImageView, resource: Int) {
    imageView.setImageResource(resource)
  }


}
