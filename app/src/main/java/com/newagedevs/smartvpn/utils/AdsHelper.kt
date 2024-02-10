@file:Suppress("DEPRECATION")

package com.newagedevs.smartvpn.utils

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdListener
import com.applovin.mediation.MaxAdViewAdListener
import com.applovin.mediation.MaxError
import com.applovin.mediation.ads.MaxAdView
import com.applovin.mediation.ads.MaxAppOpenAd
import com.applovin.mediation.ads.MaxInterstitialAd
import com.applovin.sdk.AppLovinSdk
import com.newagedevs.smartvpn.BuildConfig
import com.newagedevs.smartvpn.R
import java.util.concurrent.TimeUnit
import kotlin.math.pow

class AdsHelper {

    companion object {

        var retryAttempt = 0.0

        fun createBannerAd(context: Context, adsContainer: LinearLayout) {
            val bannerAdsListener = object : MaxAdViewAdListener {
                override fun onAdLoaded(p0: MaxAd) {
                    adsContainer.visibility = View.VISIBLE
                }

                override fun onAdDisplayed(p0: MaxAd) {
                    adsContainer.visibility = View.VISIBLE
                }

                override fun onAdHidden(p0: MaxAd) {
                    adsContainer.visibility = View.GONE
                }

                override fun onAdClicked(p0: MaxAd) { }

                override fun onAdLoadFailed(p0: String, p1: MaxError) {
                    adsContainer.visibility = View.GONE
                }

                override fun onAdDisplayFailed(p0: MaxAd, p1: MaxError) {
                    adsContainer.visibility = View.GONE
                }

                override fun onAdExpanded(p0: MaxAd) { }

                override fun onAdCollapsed(p0: MaxAd) { }
            }

            val bannerId = BuildConfig.banner_AdUnit
            val adView = MaxAdView(bannerId, context).apply {
                setListener(bannerAdsListener)
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    resources.getDimensionPixelSize(R.dimen.banner_height)
                )
            }
            adsContainer.addView(adView)
            adView.loadAd()
        }

        fun createInterstitialAd(context: Activity) : MaxInterstitialAd {

            val interstitialId = BuildConfig.interstitial_AdUnit
            val interstitialAd = MaxInterstitialAd(interstitialId, context)

            val interstitialAdsListener = object : MaxAdListener {
                override fun onAdLoaded(maxAd: MaxAd) {
                    retryAttempt = 0.0
                }

                override fun onAdLoadFailed(p0: String, p1: MaxError) {
                    retryAttempt++
                    val delayMillis = TimeUnit.SECONDS.toMillis( 2.0.pow(6.0.coerceAtMost(retryAttempt)).toLong() )
                    Handler(Looper.getMainLooper()).postDelayed( { interstitialAd.loadAd() }, delayMillis )
                }

                override fun onAdDisplayFailed(p0: MaxAd, p1: MaxError) {
                    interstitialAd.loadAd()
                }

                override fun onAdDisplayed(maxAd: MaxAd) { }

                override fun onAdClicked(maxAd: MaxAd) { }

                override fun onAdHidden(maxAd: MaxAd) {
                    interstitialAd.loadAd()
                }
            }


            interstitialAd.setListener(interstitialAdsListener)
            interstitialAd.loadAd()

            return interstitialAd
        }
    }
}

@Suppress("PrivatePropertyName", "DEPRECATION")
class AppOpenManager(context: Context) : LifecycleObserver,
    MaxAdListener {
    private val appOpenAd: MaxAppOpenAd?
    private val context: Context

    //Ads ID here
    private val ADS_UNIT = BuildConfig.appOpen_AdUnit

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        this.context = context
        appOpenAd = MaxAppOpenAd(ADS_UNIT, context)
        appOpenAd.setListener(this)
        appOpenAd.loadAd()
    }

    private fun showAdIfReady() {
        if (appOpenAd == null || !AppLovinSdk.getInstance(context).isInitialized) return

        if (appOpenAd.isReady) {
            appOpenAd.showAd(ADS_UNIT)
        } else {
            appOpenAd.loadAd()
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onStart() {
        showAdIfReady()
    }

    override fun onAdLoaded(ad: MaxAd) { }
    override fun onAdLoadFailed(adUnitId: String, error: MaxError) {}
    override fun onAdDisplayed(ad: MaxAd) { }
    override fun onAdClicked(ad: MaxAd) {}
    override fun onAdHidden(ad: MaxAd) {
        appOpenAd!!.loadAd()
    }

    override fun onAdDisplayFailed(ad: MaxAd, error: MaxError) {
        appOpenAd!!.loadAd()
    }
}