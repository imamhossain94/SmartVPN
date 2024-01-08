package com.newagedevs.smartvpn.di

import com.newagedevs.smartvpn.view.adapter.ServerAdapter
import com.newagedevs.smartvpn.view.ui.MainActivity
import com.newagedevs.smartvpn.view.ui.MainViewModel
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {

    single { (activity: MainActivity) -> ServerAdapter(activity) }
    viewModel { MainViewModel(get()) }

    //    viewModel { (adapter:ServerAdapter) -> MainViewModel(adapter) }
}
