package com.newagedevs.smartvpn.di

import com.newagedevs.smartvpn.preferences.SharedPrefRepository
import com.newagedevs.smartvpn.view.ui.MainViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    // The repository is backed by SharedPreferences and holds no Activity state,
    // so a process-wide singleton is safe.
    single { SharedPrefRepository(androidContext()) }

    // Adapters are intentionally absent: they are bound to the Activity that owns
    // the list, so they are created directly by that Activity.
    viewModel { MainViewModel(get(), androidContext()) }
}
