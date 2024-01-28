/*
 * Copyright (C) 2019 skydoves
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.newagedevs.smartvpn.model

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.newagedevs.smartvpn.R

object ItemUtils {
    fun getCustomSamples(context: Context): List<CustomItem> {
        val samples = ArrayList<CustomItem>()
        samples.add(CustomItem(drawable(context, R.drawable.ic_share), context.getString(R.string.share)))
        samples.add(CustomItem(drawable(context, R.drawable.ic_info), context.getString(R.string.network_info)))
        samples.add(CustomItem(drawable(context, R.drawable.ic_heart_outlined), context.getString(R.string.favorite_server)))
        samples.add(CustomItem(drawable(context, R.drawable.ic_star), context.getString(R.string.rate_us)))
        samples.add(CustomItem(drawable(context, R.drawable.ic_alert), context.getString(R.string.about)))
        return samples
    }


    private fun drawable(context: Context, @DrawableRes id: Int): Drawable? {
        return ContextCompat.getDrawable(context, id)
    }
}