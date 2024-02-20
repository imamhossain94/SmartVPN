@file:Suppress("unused")

package com.newagedevs.smartvpn

import android.app.Application
import androidx.databinding.ktx.BuildConfig
import com.applovin.sdk.AppLovinSdk
import com.applovin.sdk.AppLovinSdkSettings
import com.newagedevs.smartvpn.di.viewModelModule
import com.newagedevs.smartvpn.utils.AppOpenManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import timber.log.Timber

class Application : Application() {

  private lateinit var appOpenManager: AppOpenManager

  override fun onCreate() {
    super.onCreate()

    initializeAppLovinSdk()

    startKoin {
      androidContext(this@Application)
      //Adding Module
      modules(viewModelModule)
    }

    if (BuildConfig.DEBUG) {
      Timber.plant(Timber.DebugTree())
    }
  }


  private fun initializeAppLovinSdk() {
//        val settings = AppLovinSdkSettings(this@Application)
//        settings.testDeviceAdvertisingIds = listOf("REMOVED_TEST_DEVICE_ID")
//        val sdk = AppLovinSdk.getInstance(settings, this@Application)
//        sdk.showMediationDebugger()

    val sdk = AppLovinSdk.getInstance(this@Application)
    sdk.mediationProvider = "max"
    sdk.initializeSdk {
      appOpenManager = AppOpenManager(this@Application)
    }
  }

}
