package com.newagedevs.vpnthundershield.di

import com.newagedevs.vpnthundershield.view.adapter.ServerAdapter
import com.newagedevs.vpnthundershield.view.ui.MainViewModel
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {

    viewModel { (adapter:ServerAdapter) -> MainViewModel(adapter) }

}
