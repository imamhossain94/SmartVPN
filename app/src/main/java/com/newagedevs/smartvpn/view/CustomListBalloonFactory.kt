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

package com.newagedevs.smartvpn.view

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.newagedevs.smartvpn.R
import com.skydoves.balloon.ArrowOrientation
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.BalloonSizeSpec

class CustomListBalloonFactory : Balloon.Factory() {

  override fun create(context: Context, lifecycle: LifecycleOwner?): Balloon {
    val balloonBuilder = Balloon.Builder(context)
      .setLayout(R.layout.layout_custom_list)
      .setWidth(BalloonSizeSpec.WRAP)
      .setHeight(BalloonSizeSpec.WRAP)
      .setArrowOrientation(ArrowOrientation.TOP)
      .setArrowPosition(0.15f)
      .setArrowSize(10)
      .setTextSize(12f)
      .setCornerRadius(10f)
      .setMarginRight(12)
      .setElevation(6)
      .setBalloonAnimation(BalloonAnimation.FADE)
      .setIsVisibleOverlay(true)
      .setOverlayPadding(12.5f)
      .setDismissWhenShowAgain(true)
      .setDismissWhenTouchOutside(true)
      .setDismissWhenOverlayClicked(false)
      .setLifecycleOwner(lifecycle)

    // Check the theme and set the background color accordingly
    val theme = context.theme
    val typedArray = theme.obtainStyledAttributes(intArrayOf(R.attr.cardBgColor))
    val backgroundColor = typedArray.getColor(0, 0)
    typedArray.recycle()

    balloonBuilder.setBackgroundColor(backgroundColor)

    return balloonBuilder.build()
  }
}