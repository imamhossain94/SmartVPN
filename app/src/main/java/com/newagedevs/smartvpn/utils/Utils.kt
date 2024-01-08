package com.newagedevs.smartvpn.utils

import android.net.Uri
import com.newagedevs.smartvpn.BuildConfig
import com.newagedevs.smartvpn.R

object Utils {
    /**
     * Convert drawable image resource to string
     *
     * @param resourceId drawable image resource
     * @return image path
     */
//    fun getImgURL(resourceId: Int): String {
//        return Uri.parse(
//            "android.resource://" + R::class.java.getPackage()?.name + "/" + resourceId
//        ).toString()
//    }

    fun getImgURL(resourceId: Int): String {
        return Uri.Builder()
            .scheme("android.resource")
            .authority(R::class.java.getPackage()?.name)
            .path(resourceId.toString())
            .build()
            .toString()
    }
}
