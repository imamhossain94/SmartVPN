package com.newagedevs.smartvpn.extensions

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import androidx.core.app.ActivityCompat
import com.newagedevs.smartvpn.R
import java.lang.Exception

/** makes visible a view. */
fun View.visible() {
  visibility = View.VISIBLE
}

@SuppressLint("DiscouragedApi")
fun Context.getResource(name:String): Drawable? {
  return try{
    val resID = this.resources.getIdentifier(name , "drawable", this.packageName)
    ActivityCompat.getDrawable(this,resID)
  } catch (e:Exception) {
    ActivityCompat.getDrawable(this,R.drawable.flag_common)
  }
}