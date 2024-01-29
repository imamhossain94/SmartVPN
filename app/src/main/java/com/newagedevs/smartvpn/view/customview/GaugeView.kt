/*******************************************************************************
 * Copyright (c) 2015 Sulejman Sarajlija
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */
package com.newagedevs.smartvpn.view.customview

import android.annotation.TargetApi
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ComposeShader
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Paint.Align
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader.TileMode
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.view.View
import com.newagedevs.smartvpn.R

class GaugeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {
    // *--------------------------------------------------------------------- *//
    // Customizable properties
    // *--------------------------------------------------------------------- *//
    private var mShowOuterShadow = false
    private var mShowOuterBorder = false
    private var mShowOuterRim = false
    private var mShowInnerRim = false
    private var mShowScale = false
    private var mShowRanges = false
    private var mShowNeedle = false
    private var mShowText = false
    private var mOuterShadowWidth = 0f
    private var mOuterBorderWidth = 0f
    private var mOuterRimWidth = 0f
    private var mInnerRimWidth = 0f
    private var mInnerRimBorderWidth = 0f
    private var mNeedleWidth = 0f
    private var mNeedleHeight = 0f
    private var mInnerCircleColor = 0
    private var mOuterCircleColor = 0
    private var mScalePosition = 0f
    private var mScaleStartValue = 0f
    private var mScaleEndValue = 0f
    private var mScaleStartAngle = 0f
    private lateinit var mRangeValues: FloatArray
    private lateinit var mRangeColors: IntArray
    private var mDivisions = 0
    private var mSubdivisions = 0
    private var mOuterShadowRect: RectF? = null
    private var mOuterBorderRect: RectF? = null
    private var mOuterRimRect: RectF? = null
    private var mInnerRimRect: RectF? = null
    private var mInnerRimBorderRect: RectF? = null
    private var mFaceRect: RectF? = null
    private var mScaleRect: RectF? = null
    private var mBackground: Bitmap? = null
    private var mBackgroundPaint: Paint? = null
    private var mOuterShadowPaint: Paint? = null
    private var mOuterBorderPaint: Paint? = null
    private var mOuterRimPaint: Paint? = null
    private var mInnerRimPaint: Paint? = null
    private var mInnerRimBorderLightPaint: Paint? = null
    private var mInnerRimBorderDarkPaint: Paint? = null
    private var mFacePaint: Paint? = null
    private var mFaceBorderPaint: Paint? = null
    private var mFaceShadowPaint: Paint? = null
    private lateinit var mRangePaints: Array<Paint?>
    private var mNeedleRightPaint: Paint? = null
    private var mNeedleLeftPaint: Paint? = null
    private var mNeedleScrewPaint: Paint? = null
    private var mNeedleScrewBorderPaint: Paint? = null
    private var mTextValuePaint: Paint? = null
    private var mTextUnitPaint: Paint? = null
    private var mTextValue: String? = null
    private var mTextUnit: String? = null
    private var mTextValueColor = 0
    private var mTextUnitColor = 0
    private var mTextShadowColor = 0
    private var mTextValueSize = 0f
    private var mTextUnitSize = 0f
    private var mNeedleRightPath: Path? = null
    private var mNeedleLeftPath: Path? = null

    // *--------------------------------------------------------------------- *//
    private var mScaleRotation = 0f
    private var mDivisionValue = 0f
    private var mSubdivisionValue = 0f
    private var mSubdivisionAngle = 0f
    private var mTargetValue = 0f
    private var mCurrentValue = 0f
    private var mNeedleVelocity = 0f
    private var mNeedleAcceleration = 0f
    private var mNeedleLastMoved: Long = -1
    private var mNeedleInitialized = false

    init {
        readAttrs(context, attrs, defStyle)
        init()
    }

    private fun readAttrs(context: Context, attrs: AttributeSet?, defStyle: Int) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.GaugeView, defStyle, 0)
        mShowOuterShadow = a.getBoolean(R.styleable.GaugeView_showOuterShadow, SHOW_OUTER_SHADOW)
        mShowOuterBorder = a.getBoolean(R.styleable.GaugeView_showOuterBorder, SHOW_OUTER_BORDER)
        mShowOuterRim = a.getBoolean(R.styleable.GaugeView_showOuterRim, SHOW_OUTER_RIM)
        mShowInnerRim = a.getBoolean(R.styleable.GaugeView_showInnerRim, SHOW_INNER_RIM)
        mShowNeedle = a.getBoolean(R.styleable.GaugeView_showNeedle, SHOW_NEEDLE)
        mShowScale = a.getBoolean(R.styleable.GaugeView_showScale, SHOW_SCALE)
        mShowRanges = a.getBoolean(R.styleable.GaugeView_showRanges, SHOW_RANGES)
        mShowText = a.getBoolean(R.styleable.GaugeView_showText, SHOW_TEXT)
        mOuterShadowWidth = if (mShowOuterShadow) a.getFloat(
            R.styleable.GaugeView_outerShadowWidth,
            OUTER_SHADOW_WIDTH
        ) else 0.0f
        mOuterBorderWidth = if (mShowOuterBorder) a.getFloat(
            R.styleable.GaugeView_outerBorderWidth,
            OUTER_BORDER_WIDTH
        ) else 0.0f
        mOuterRimWidth = if (mShowOuterRim) a.getFloat(
            R.styleable.GaugeView_outerRimWidth,
            OUTER_RIM_WIDTH
        ) else 0.0f
        mInnerRimWidth = if (mShowInnerRim) a.getFloat(
            R.styleable.GaugeView_innerRimWidth,
            INNER_RIM_WIDTH
        ) else 0.0f
        mInnerRimBorderWidth = if (mShowInnerRim) a.getFloat(
            R.styleable.GaugeView_innerRimBorderWidth,
            INNER_RIM_BORDER_WIDTH
        ) else 0.0f
        mNeedleWidth = a.getFloat(R.styleable.GaugeView_needleWidth, NEEDLE_WIDTH)
        mNeedleHeight = a.getFloat(R.styleable.GaugeView_needleHeight, NEEDLE_HEIGHT)
        mInnerCircleColor = a.getColor(R.styleable.GaugeView_innerCircleColor, INNER_CIRCLE_COLOR)
        mOuterCircleColor = a.getColor(R.styleable.GaugeView_outerCircleColor, OUTER_CIRCLE_COLOR)
        mScalePosition = if (mShowScale || mShowRanges) a.getFloat(
            R.styleable.GaugeView_scalePosition,
            SCALE_POSITION
        ) else 0.0f
        mScaleStartValue = a.getFloat(R.styleable.GaugeView_scaleStartValue, SCALE_START_VALUE)
        mScaleEndValue = a.getFloat(R.styleable.GaugeView_scaleEndValue, SCALE_END_VALUE)
        mScaleStartAngle = a.getFloat(R.styleable.GaugeView_scaleStartAngle, SCALE_START_ANGLE)
        mDivisions = a.getInteger(R.styleable.GaugeView_divisions, SCALE_DIVISIONS)
        mSubdivisions = a.getInteger(R.styleable.GaugeView_subdivisions, SCALE_SUBDIVISIONS)
        if (mShowRanges) {
            mTextShadowColor = a.getColor(R.styleable.GaugeView_textShadowColor, TEXT_SHADOW_COLOR)
            val rangesId = a.getResourceId(R.styleable.GaugeView_rangeValues, 0)
            val colorsId = a.getResourceId(R.styleable.GaugeView_rangeColors, 0)
            readRanges(context.resources, rangesId, colorsId)
        }
        if (mShowText) {
            val textValueId = a.getResourceId(R.styleable.GaugeView_textValue, 0)
            val textValue = a.getString(R.styleable.GaugeView_textValue)
            mTextValue = if (0 < textValueId) context.getString(textValueId) else textValue ?: ""
            val textUnitId = a.getResourceId(R.styleable.GaugeView_textUnit, 0)
            val textUnit = a.getString(R.styleable.GaugeView_textUnit)
            mTextUnit = if (0 < textUnitId) context.getString(textUnitId) else textUnit ?: ""
            mTextValueColor = a.getColor(R.styleable.GaugeView_textValueColor, TEXT_VALUE_COLOR)
            mTextUnitColor = a.getColor(R.styleable.GaugeView_textUnitColor, TEXT_UNIT_COLOR)
            mTextShadowColor = a.getColor(R.styleable.GaugeView_textShadowColor, TEXT_SHADOW_COLOR)
            mTextValueSize = a.getFloat(R.styleable.GaugeView_textValueSize, TEXT_VALUE_SIZE)
            mTextUnitSize = a.getFloat(R.styleable.GaugeView_textUnitSize, TEXT_UNIT_SIZE)
        }
        a.recycle()
    }

    private fun readRanges(res: Resources, rangesId: Int, colorsId: Int) {
        if (rangesId > 0 && colorsId > 0) {
            val ranges = res.getStringArray(R.array.ranges)
            val colors = res.getStringArray(R.array.rangeColors)
            require(ranges.size == colors.size) { "The ranges and colors arrays must have the same length." }
            val length = ranges.size
            mRangeValues = FloatArray(length)
            mRangeColors = IntArray(length)
            for (i in 0 until length) {
                mRangeValues[i] = ranges[i].toFloat()
                mRangeColors[i] = Color.parseColor(colors[i])
            }
        } else {
            mRangeValues = RANGE_VALUES
            mRangeColors = RANGE_COLORS
        }
    }

    @TargetApi(11)
    private fun init() {
        // TODO Why isn't this working with HA layer?
        // The needle is not displayed although the onDraw() is being triggered by invalidate()
        // calls.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            setLayerType(LAYER_TYPE_SOFTWARE, null)
        }
        initDrawingRects()
        initDrawingTools()

        // Compute the scale properties
        if (mShowRanges) {
            initScale()
        }
    }

    fun initDrawingRects() {
        // The drawing area is a rectangle of width 1 and height 1,
        // where (0,0) is the top left corner of the canvas.
        // Note that on Canvas X axis points to right, while the Y axis points downwards.
        mOuterShadowRect = RectF(LEFT, TOP, RIGHT, BOTTOM)
        mOuterBorderRect = RectF(
            mOuterShadowRect!!.left + mOuterShadowWidth,
            mOuterShadowRect!!.top + mOuterShadowWidth,
            mOuterShadowRect!!.right - mOuterShadowWidth,
            mOuterShadowRect!!.bottom - mOuterShadowWidth
        )
        mOuterRimRect = RectF(
            mOuterBorderRect!!.left + mOuterBorderWidth,
            mOuterBorderRect!!.top + mOuterBorderWidth,
            mOuterBorderRect!!.right - mOuterBorderWidth,
            mOuterBorderRect!!.bottom - mOuterBorderWidth
        )
        mInnerRimRect = RectF(
            mOuterRimRect!!.left + mOuterRimWidth,
            mOuterRimRect!!.top + mOuterRimWidth,
            mOuterRimRect!!.right
                    - mOuterRimWidth,
            mOuterRimRect!!.bottom - mOuterRimWidth
        )
        mInnerRimBorderRect = RectF(
            mInnerRimRect!!.left + mInnerRimBorderWidth,
            mInnerRimRect!!.top + mInnerRimBorderWidth,
            mInnerRimRect!!.right - mInnerRimBorderWidth,
            mInnerRimRect!!.bottom - mInnerRimBorderWidth
        )
        mFaceRect = RectF(
            mInnerRimRect!!.left + mInnerRimWidth, mInnerRimRect!!.top + mInnerRimWidth,
            mInnerRimRect!!.right - mInnerRimWidth, mInnerRimRect!!.bottom - mInnerRimWidth
        )
        mScaleRect = RectF(
            mFaceRect!!.left + mScalePosition,
            mFaceRect!!.top + mScalePosition,
            mFaceRect!!.right - mScalePosition,
            mFaceRect!!.bottom - mScalePosition
        )
    }

    private fun initDrawingTools() {
        mBackgroundPaint = Paint()
        mBackgroundPaint!!.isFilterBitmap = true
        if (mShowOuterShadow) {
            mOuterShadowPaint = defaultOuterShadowPaint
        }
        if (mShowOuterBorder) {
            mOuterBorderPaint = defaultOuterBorderPaint
        }
        if (mShowOuterRim) {
            mOuterRimPaint = defaultOuterRimPaint
        }
        if (mShowInnerRim) {
            mInnerRimPaint = defaultInnerRimPaint
            mInnerRimBorderLightPaint = defaultInnerRimBorderLightPaint
            mInnerRimBorderDarkPaint = defaultInnerRimBorderDarkPaint
        }
        if (mShowRanges) {
            setDefaultScaleRangePaints()
        }
        if (mShowNeedle) {
            setDefaultNeedlePaths()
            mNeedleLeftPaint = defaultNeedleLeftPaint
            mNeedleRightPaint = defaultNeedleRightPaint
            mNeedleScrewPaint = defaultNeedleScrewPaint
            mNeedleScrewBorderPaint = defaultNeedleScrewBorderPaint
        }
        if (mShowText) {
            mTextValuePaint = defaultTextValuePaint
            mTextUnitPaint = defaultTextUnitPaint
        }
        mFacePaint = defaultFacePaint
        mFaceBorderPaint = defaultFaceBorderPaint
        mFaceShadowPaint = defaultFaceShadowPaint
    }

    val defaultOuterShadowPaint: Paint
        get() {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.style = Paint.Style.FILL
            paint.setShader(
                RadialGradient(
                    CENTER,
                    CENTER,
                    mOuterShadowRect!!.width() / 2.0f,
                    OUTER_SHADOW_COLORS,
                    OUTER_SHADOW_POS,
                    TileMode.MIRROR
                )
            )
            return paint
        }
    private val defaultOuterBorderPaint: Paint
        private get() {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.style = Paint.Style.FILL
            paint.color = Color.argb(245, 0, 0, 0)
            return paint
        }
    val defaultOuterRimPaint: Paint
        get() {
            // Use a linear gradient to create the 3D effect
            val verticalGradient = LinearGradient(
                mOuterRimRect!!.left,
                mOuterRimRect!!.top,
                mOuterRimRect!!.left,
                mOuterRimRect!!.bottom,
                Color.rgb(255, 255, 255),
                Color.rgb(84, 90, 100),
                TileMode.REPEAT
            )

            // Use a Bitmap shader for the metallic style
            val bitmap = BitmapFactory.decodeResource(resources, R.drawable.light_alu)
            val aluminiumTile = BitmapShader(bitmap, TileMode.REPEAT, TileMode.REPEAT)
            val matrix = Matrix()
            matrix.setScale(1.0f / bitmap.width, 1.0f / bitmap.height)
            aluminiumTile.setLocalMatrix(matrix)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.setShader(
                ComposeShader(
                    verticalGradient,
                    aluminiumTile,
                    PorterDuff.Mode.MULTIPLY
                )
            )
            paint.isFilterBitmap = true
            return paint
        }
    private val defaultInnerRimPaint: Paint
        private get() {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.setShader(
                LinearGradient(
                    mInnerRimRect!!.left,
                    mInnerRimRect!!.top,
                    mInnerRimRect!!.left,
                    mInnerRimRect!!.bottom,
                    intArrayOf(
                        Color.argb(255, 68, 73, 80),
                        Color.argb(255, 91, 97, 105),
                        Color.argb(255, 178, 180, 183),
                        Color.argb(255, 188, 188, 190),
                        Color.argb(255, 84, 90, 100),
                        Color.argb(255, 137, 137, 137)
                    ),
                    floatArrayOf(0f, 0.1f, 0.2f, 0.4f, 0.8f, 1f),
                    TileMode.CLAMP
                )
            )
            return paint
        }
    private val defaultInnerRimBorderLightPaint: Paint
        private get() {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.style = Paint.Style.STROKE
            paint.color = Color.argb(100, 255, 255, 255)
            paint.strokeWidth = 0.005f
            return paint
        }
    private val defaultInnerRimBorderDarkPaint: Paint
        private get() {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.style = Paint.Style.STROKE
            paint.color = Color.argb(100, 81, 84, 89)
            paint.strokeWidth = 0.005f
            return paint
        }
    val defaultFacePaint: Paint
        get() {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.color = Color.rgb(255, 255, 255)
            return paint
        }
    val defaultFaceBorderPaint: Paint
        get() {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.style = Paint.Style.STROKE
            paint.color = Color.rgb(255, 255, 255)
            paint.strokeWidth = 0f
            return paint
        }
    val defaultFaceShadowPaint: Paint
        get() {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.setShader(
                RadialGradient(
                    0.5f, 0.5f, mFaceRect!!.width() / 2.0f, intArrayOf(
                        Color.argb(60, 40, 96, 170),
                        Color.argb(80, 15, 34, 98),
                        Color.argb(120, 0, 0, 0),
                        Color.argb(140, 0, 0, 0)
                    ), floatArrayOf(0.60f, 0.85f, 0.96f, 0.99f), TileMode.MIRROR
                )
            )
            return paint
        }

    fun setDefaultNeedlePaths() {
        val x = 0.5f
        val y = 0.5f
        mNeedleLeftPath = Path()
        mNeedleLeftPath!!.moveTo(x, y)
        mNeedleLeftPath!!.lineTo(x - mNeedleWidth, y)
        mNeedleLeftPath!!.lineTo(x, y - mNeedleHeight)
        mNeedleLeftPath!!.lineTo(x, y)
        mNeedleLeftPath!!.lineTo(x - mNeedleWidth, y)
        mNeedleRightPath = Path()
        mNeedleRightPath!!.moveTo(x, y)
        mNeedleRightPath!!.lineTo(x + mNeedleWidth, y)
        mNeedleRightPath!!.lineTo(x, y - mNeedleHeight)
        mNeedleRightPath!!.lineTo(x, y)
        mNeedleRightPath!!.lineTo(x + mNeedleWidth, y)
    }

    val defaultNeedleLeftPaint: Paint
        get() {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.color = Color.rgb(0, 0, 0)
            return paint
        }
    val defaultNeedleRightPaint: Paint
        get() {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.color = Color.rgb(0, 0, 0)
            //paint.setShadowLayer(0.01f, 0.005f, -0.005f, Color.argb(127, 0, 0, 0));
            return paint
        }
    val defaultNeedleScrewPaint: Paint
        get() {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            //paint.setShader(new RadialGradient(0.5f, 0.5f, 0.07f, new int[]{Color.rgb(171, 171, 171), Color.WHITE}, new float[]{0.05f,
            //        0.9f}, TileMode.MIRROR));
            paint.color = Color.rgb(0, 0, 0)
            return paint
        }
    val defaultNeedleScrewBorderPaint: Paint
        get() {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.style = Paint.Style.STROKE
            paint.color = Color.argb(100, 81, 84, 89)
            paint.strokeWidth = 0.005f
            return paint
        }

    fun setDefaultRanges() {
        mRangeValues = floatArrayOf(16f, 25f, 40f, 100f)
        mRangeColors = intArrayOf(
            Color.rgb(231, 32, 43),
            Color.rgb(232, 111, 33),
            Color.rgb(232, 231, 33),
            Color.rgb(27, 202, 33)
        )
    }

    fun setDefaultScaleRangePaints() {
        val length = mRangeValues.size
        mRangePaints = arrayOfNulls(length)
        for (i in 0 until length) {
            mRangePaints[i] = Paint(Paint.LINEAR_TEXT_FLAG or Paint.ANTI_ALIAS_FLAG)
            mRangePaints[i]!!.color = mRangeColors[i]
            mRangePaints[i]!!.style = Paint.Style.STROKE
            mRangePaints[i]!!.strokeWidth = 0.005f
            mRangePaints[i]!!.textSize = 0.05f
            mRangePaints[i]!!.setTypeface(Typeface.SANS_SERIF)
            mRangePaints[i]!!.textAlign = Align.CENTER
            mRangePaints[i]!!.setShadowLayer(0.005f, 0.002f, 0.002f, mTextShadowColor)
        }
    }

    val defaultTextValuePaint: Paint
        get() {
            val paint = Paint(Paint.LINEAR_TEXT_FLAG or Paint.ANTI_ALIAS_FLAG)
            paint.color = mTextValueColor
            paint.style = Paint.Style.FILL_AND_STROKE
            paint.strokeWidth = 0.005f
            paint.textSize = mTextValueSize
            paint.textAlign = Align.CENTER
            paint.setTypeface(Typeface.SANS_SERIF)
            paint.setShadowLayer(0.01f, 0.002f, 0.002f, mTextShadowColor)
            return paint
        }
    val defaultTextUnitPaint: Paint
        get() {
            val paint = Paint(Paint.LINEAR_TEXT_FLAG or Paint.ANTI_ALIAS_FLAG)
            paint.color = mTextUnitColor
            paint.style = Paint.Style.FILL_AND_STROKE
            paint.strokeWidth = 0.005f
            paint.textSize = mTextUnitSize
            paint.textAlign = Align.CENTER
            paint.setShadowLayer(0.01f, 0.002f, 0.002f, mTextShadowColor)
            return paint
        }

    override fun onRestoreInstanceState(state: Parcelable) {
        val bundle = state as Bundle
        val superState = bundle.getParcelable<Parcelable>("superState")
        super.onRestoreInstanceState(superState)
        mNeedleInitialized = bundle.getBoolean("needleInitialized")
        mNeedleVelocity = bundle.getFloat("needleVelocity")
        mNeedleAcceleration = bundle.getFloat("needleAcceleration")
        mNeedleLastMoved = bundle.getLong("needleLastMoved")
        mCurrentValue = bundle.getFloat("currentValue")
        mTargetValue = bundle.getFloat("targetValue")
    }

    private fun initScale() {
        mScaleRotation = (mScaleStartAngle + 180) % 360
        mDivisionValue = (mScaleEndValue - mScaleStartValue) / mDivisions
        Log.d("mDivisionValue:", mDivisionValue.toString())
        mSubdivisionValue = mDivisionValue / mSubdivisions
        mSubdivisionAngle = (360 - 2 * mScaleStartAngle) / (mDivisions * mSubdivisions)
    }

    override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()
        val state = Bundle()
        state.putParcelable("superState", superState)
        state.putBoolean("needleInitialized", mNeedleInitialized)
        state.putFloat("needleVelocity", mNeedleVelocity)
        state.putFloat("needleAcceleration", mNeedleAcceleration)
        state.putLong("needleLastMoved", mNeedleLastMoved)
        state.putFloat("currentValue", mCurrentValue)
        state.putFloat("targetValue", mTargetValue)
        return state
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Loggable.log.debug(String.format("widthMeasureSpec=%s, heightMeasureSpec=%s",
        // View.MeasureSpec.toString(widthMeasureSpec),
        // View.MeasureSpec.toString(heightMeasureSpec)));
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val chosenWidth = chooseDimension(widthMode, widthSize)
        val chosenHeight = chooseDimension(heightMode, heightSize)
        setMeasuredDimension(chosenWidth, chosenHeight)
    }

    private fun chooseDimension(mode: Int, size: Int): Int {
        return when (mode) {
            MeasureSpec.AT_MOST, MeasureSpec.EXACTLY -> size
            MeasureSpec.UNSPECIFIED -> defaultDimension
            else -> defaultDimension
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        drawGauge()
    }

    private fun drawGauge() {
        if (null != mBackground) {
            // Let go of the old background
            mBackground!!.recycle()
        }
        // Create a new background according to the new width and height
        mBackground = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(mBackground!!)
        val scale = Math.min(width, height).toFloat()
        canvas.scale(scale, scale)
        canvas.translate(
            if (scale == height.toFloat()) (width - scale) / 2 / scale else 0f,
            if (scale == width.toFloat()) (height - scale) / 2 / scale else 0f
        )

        //drawRim(canvas);
        //drawFace(canvas);
        if (mShowRanges) {
            drawScale(canvas)
        }
    }

    override fun onDraw(canvas: Canvas) {
        drawBackground(canvas)
        val scale = Math.min(width, height).toFloat()
        canvas.scale(scale, scale)
        canvas.translate(
            if (scale == height.toFloat()) (width - scale) / 2 / scale else 0f,
            if (scale == width.toFloat()) (height - scale) / 2 / scale else 0f
        )
        if (mShowNeedle) {
            drawNeedle(canvas)
        }
        if (mShowText) {
            drawText(canvas)
        }
        computeCurrentValue()
    }

    private fun drawBackground(canvas: Canvas) {
        if (null != mBackground) {
            canvas.drawBitmap(mBackground!!, 0f, 0f, mBackgroundPaint)
        }
    }

    private fun drawRim(canvas: Canvas) {
        if (mShowOuterShadow) {
            canvas.drawOval(mOuterShadowRect!!, mOuterShadowPaint!!)
        }
        if (mShowOuterBorder) {
            canvas.drawOval(mOuterBorderRect!!, mOuterBorderPaint!!)
        }
        if (mShowOuterRim) {
            canvas.drawOval(mOuterRimRect!!, mOuterRimPaint!!)
        }
        if (mShowInnerRim) {
            canvas.drawOval(mInnerRimRect!!, mInnerRimPaint!!)
            canvas.drawOval(mInnerRimRect!!, mInnerRimBorderLightPaint!!)
            canvas.drawOval(mInnerRimBorderRect!!, mInnerRimBorderDarkPaint!!)
        }
    }

    private fun drawFace(canvas: Canvas) {
        // Draw the face gradient
        canvas.drawOval(mFaceRect!!, mFacePaint!!)
        // Draw the face border
        //canvas.drawOval(mFaceRect, mFaceBorderPaint);
        // Draw the inner face shadow
        //canvas.drawOval(mFaceRect, mFaceShadowPaint);
    }

    private fun drawText(canvas: Canvas) {
        val textValue =
            if (!TextUtils.isEmpty(mTextValue)) mTextValue else valueString(mCurrentValue)
        val textValueWidth = mTextValuePaint!!.measureText(textValue)
        val textUnitWidth: Float =
            if (!TextUtils.isEmpty(mTextUnit)) mTextUnitPaint!!.measureText(mTextUnit) else 0f
        val startX = CENTER - textUnitWidth / 2
        val startY = CENTER + 0.1f
        drawText(canvas, textValue, startX, startY, mTextValuePaint)
        if (!TextUtils.isEmpty(mTextUnit)) {
            canvas.drawText(
                mTextUnit!!,
                CENTER + textValueWidth / 2 + 0.03f,
                CENTER,
                mTextUnitPaint!!
            )
        }
    }

    private fun drawScale(canvas: Canvas) {
        canvas.save()
        // On canvas, North is 0 degrees, East is 90 degrees, South is 180 etc.
        // We start the scale somewhere South-West so we need to first rotate the canvas.
        canvas.rotate(mScaleRotation, 0.5f, 0.5f)
        Log.d("mScaleRotation: ", mScaleRotation.toString())
        val totalTicks = mDivisions * mSubdivisions + 1
        for (i in 0 until totalTicks) {
            val y1 = mScaleRect!!.top
            Log.d("mScaleRect.top: ", mScaleRect!!.top.toString())
            val y2 = y1 + 0.045f // height of division
            val y3 = y1 + 0.090f // height of subdivision
            val value = getValueForTick(i)
            val paint = getRangePaint(value)
            val div = mScaleEndValue / mDivisions.toFloat()
            val mod = value % div
            if (Math.abs(mod - 0) < 0.001 || Math.abs(mod - div) < 0.001) {
                // Draw a division tick
                paint!!.strokeWidth = 0.01f
                paint.color = Color.rgb(87, 97, 114)
                canvas.drawLine(0.5f, y1 - 0.015f, 0.5f, y3 - 0.03f, paint)
                // Draw the text 0.15 away from the division tick
                //paint.setTextSize(1F);
                paint.style = Paint.Style.FILL
                //canvas.drawText(valueString(value), 0.49f, y3 + 0.05f, paint);
                drawText(canvas, valueString(value), 0.5f, y3 + 0.05f, paint)
                Log.d("TEXT:", valueString(value))
            } else {
                // Draw a subdivision tick
                paint!!.strokeWidth = 0.002f
                paint.color = Color.rgb(209, 209, 209)
                canvas.drawLine(0.5f, y1, 0.5f, y2, paint)
            }
            canvas.rotate(mSubdivisionAngle, 0.5f, 0.5f)
            Log.d("mSubdivisionAngle: ", mSubdivisionAngle.toString())
        }
        canvas.restore()
    }

    private fun drawText(canvas: Canvas, value: String?, x: Float, y: Float, paint: Paint?) {
        //Save original font size
        val originalTextSize = paint!!.textSize

        // set a magnification factor
        val magnifier = 100f

        // Scale the canvas
        canvas.save()
        canvas.scale(1f / magnifier, 1f / magnifier)

        // increase the font size
        paint.textSize = originalTextSize * magnifier
        canvas.drawText(value!!, x * magnifier, y * magnifier, paint)

        //canvas.drawTextOnPath(value, textPath, 0.0f, 0.0f, paint);

        // bring everything back to normal
        canvas.restore()
        paint.textSize = originalTextSize
    }

    private fun valueString(value: Float): String {
        return String.format("%d", value.toInt())
    }

    private fun getValueForTick(tick: Int): Float {
        return tick * (mDivisionValue / mSubdivisions)
    }

    private fun getRangePaint(value: Float): Paint? {
        val length = mRangeValues.size
        for (i in 0 until length - 1) {
            if (value < mRangeValues[i]) return mRangePaints[i]
        }
        if (value <= mRangeValues[length - 1]) return mRangePaints[length - 1]
        throw IllegalArgumentException("Value $value out of range!")
    }

    private fun drawNeedle(canvas: Canvas) {
        if (mNeedleInitialized) {
            val angle = getAngleForValue(mCurrentValue)
            // Logger.log.info(String.format("value=%f -> angle=%f", mCurrentValue, angle));
            canvas.save()
            canvas.rotate(angle, 0.5f, 0.5f)
            val outerCircle = Paint()
            outerCircle.color = mOuterCircleColor
            outerCircle.style = Paint.Style.FILL
            canvas.drawCircle(0.5f, 0.5f, 0.23f, outerCircle)
            val innerCircle = Paint()
            innerCircle.color = mInnerCircleColor
            canvas.drawCircle(0.5f, 0.5f, 0.1f, innerCircle)
            setNeedleShadowPosition(angle)
            canvas.drawPath(mNeedleLeftPath!!, mNeedleLeftPaint!!)
            canvas.drawPath(mNeedleRightPath!!, mNeedleRightPaint!!)
            canvas.restore()

            // Draw the needle screw and its border
            canvas.drawCircle(0.5f, 0.5f, 0.04f, mNeedleScrewPaint!!)
            Log.d("NEEDLE", "Needle drawn :/")
            //canvas.drawCircle(0.5f, 0.5f, 0.04f, mNeedleScrewBorderPaint);
        }
    }

    private fun setNeedleShadowPosition(angle: Float) {
        if (angle > 180 && angle < 360) {
            // Move shadow from right to left
            mNeedleRightPaint!!.setShadowLayer(0f, 0f, 0f, Color.BLACK)
            mNeedleLeftPaint!!.setShadowLayer(0.01f, -0.005f, 0.005f, Color.argb(127, 0, 0, 0))
        } else {
            // Move shadow from left to right
            mNeedleLeftPaint!!.setShadowLayer(0f, 0f, 0f, Color.BLACK)
            mNeedleRightPaint!!.setShadowLayer(0.01f, 0.005f, -0.005f, Color.argb(127, 0, 0, 0))
        }
    }

    private fun getAngleForValue(value: Float): Float {
        return (mScaleRotation + value / mSubdivisionValue * mSubdivisionAngle) % 360
    }

    private fun computeCurrentValue() {
        // Logger.log.warn(String.format("velocity=%f, acceleration=%f", mNeedleVelocity,
        // mNeedleAcceleration));
        if (!(Math.abs(mCurrentValue - mTargetValue) > 0.01f)) {
            return
        }
        if (-1L != mNeedleLastMoved) {
            val time = (System.currentTimeMillis() - mNeedleLastMoved) / 1000.0f
            val direction = Math.signum(mNeedleVelocity)
            mNeedleAcceleration = if (Math.abs(mNeedleVelocity) < 90.0f) {
                5.0f * (mTargetValue - mCurrentValue)
            } else {
                0.0f
            }
            mNeedleAcceleration = 5.0f * (mTargetValue - mCurrentValue)
            mCurrentValue += mNeedleVelocity * time
            mNeedleVelocity += mNeedleAcceleration * time
            if ((mTargetValue - mCurrentValue) * direction < 0.01f * direction) {
                mCurrentValue = mTargetValue
                mNeedleVelocity = 0.0f
                mNeedleAcceleration = 0.0f
                mNeedleLastMoved = -1L
            } else {
                mNeedleLastMoved = System.currentTimeMillis()
            }
            invalidate()
        } else {
            mNeedleLastMoved = System.currentTimeMillis()
            computeCurrentValue()
        }
    }

    fun setTargetValue(value: Float) {
        mTargetValue = if (mShowScale || mShowRanges) {
            if (value < mScaleStartValue) {
                mScaleStartValue
            } else if (value > mScaleEndValue) {
                mScaleEndValue
            } else {
                value
            }
        } else {
            value
        }
        mNeedleInitialized = true
        invalidate()
    }



    companion object {
        const val defaultDimension = 300
        const val TOP = 0.0f
        const val LEFT = 0.0f
        const val RIGHT = 1.0f
        const val BOTTOM = 1.0f
        const val CENTER = 0.5f
        const val SHOW_OUTER_SHADOW = true
        const val SHOW_OUTER_BORDER = true
        const val SHOW_OUTER_RIM = true
        const val SHOW_INNER_RIM = true
        const val SHOW_NEEDLE = true
        const val SHOW_SCALE = false
        const val SHOW_RANGES = true
        const val SHOW_TEXT = false
        const val OUTER_SHADOW_WIDTH = 0.03f
        const val OUTER_BORDER_WIDTH = 0.04f
        const val OUTER_RIM_WIDTH = 0.05f
        const val INNER_RIM_WIDTH = 0.06f
        const val INNER_RIM_BORDER_WIDTH = 0.005f
        const val NEEDLE_WIDTH = 0.025f
        const val NEEDLE_HEIGHT = 0.32f
        val INNER_CIRCLE_COLOR = Color.rgb(190, 215, 123)
        val OUTER_CIRCLE_COLOR = Color.rgb(205, 231, 132)
        const val SCALE_POSITION = 0.015f
        const val SCALE_START_VALUE = 0.0f
        const val SCALE_END_VALUE = 100.0f
        const val SCALE_START_ANGLE = 60.0f
        const val SCALE_DIVISIONS = 10
        const val SCALE_SUBDIVISIONS = 5
        val OUTER_SHADOW_COLORS = intArrayOf(
            Color.argb(40, 255, 254, 187), Color.argb(20, 255, 247, 219),
            Color.argb(5, 255, 255, 255)
        )
        val OUTER_SHADOW_POS = floatArrayOf(0.90f, 0.95f, 0.99f)
        val RANGE_VALUES = floatArrayOf(16.0f, 25.0f, 40.0f, 100.0f)
        val RANGE_COLORS = intArrayOf(
            Color.rgb(0, 0, 0), Color.rgb(0, 0, 0), Color.rgb(0, 0, 0),
            Color.rgb(0, 0, 0)
        )
        val TEXT_SHADOW_COLOR = Color.argb(100, 0, 0, 0)
        const val TEXT_VALUE_COLOR = Color.WHITE
        const val TEXT_UNIT_COLOR = Color.WHITE
        const val TEXT_VALUE_SIZE = 0.3f
        const val TEXT_UNIT_SIZE = 0.1f

        /**
         * Draws a text in the canvas with spacing between each letter.
         * Basically what this method does is it split's the given text into individual letters
         * and draws each letter independently using Canvas.drawText with a separation of
         * `spacingX` between each letter.
         * @param canvas the canvas where the text will be drawn
         * @param text the text what will be drawn
         * @param left the left position of the text
         * @param top the top position of the text
         * @param paint holds styling information for the text
         * @param spacingPx the number of pixels between each letter that will be drawn
         */
        fun drawSpacedText(
            canvas: Canvas,
            text: String,
            left: Float,
            top: Float,
            paint: Paint,
            spacingPx: Float
        ) {
            var currentLeft = left
            for (i in 0 until text.length) {
                val c = text[i].toString() + ""
                canvas.drawText(c, currentLeft, top, paint)
                currentLeft += spacingPx
                currentLeft += paint.measureText(c)
            }
        }

        /**
         * returns the width of a text drawn by drawSpacedText
         */
        fun getSpacedTextWidth(paint: Paint, text: String, spacingX: Float): Float {
            return paint.measureText(text) + spacingX * (text.length - 1)
        }
    }
}
