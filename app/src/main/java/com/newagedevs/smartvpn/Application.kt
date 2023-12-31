@file:Suppress("unused")

package com.newagedevs.smartvpn

import android.app.Application
import androidx.databinding.ktx.BuildConfig
import com.newagedevs.smartvpn.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import timber.log.Timber

class Application : Application() {

  override fun onCreate() {
    super.onCreate()

    startKoin {
      androidContext(this@Application)

      //Adding Module
      modules(viewModelModule)

    }

    if (BuildConfig.DEBUG) {
      Timber.plant(Timber.DebugTree())
    }
  }
}
