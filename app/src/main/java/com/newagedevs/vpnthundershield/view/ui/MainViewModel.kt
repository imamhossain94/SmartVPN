package com.newagedevs.vpnthundershield.view.ui

import androidx.databinding.Bindable
import com.newagedevs.vpnthundershield.base.DisposableViewModel
import com.skydoves.bindables.bindingProperty
import timber.log.Timber


class MainViewModel constructor() : DisposableViewModel() {

    @get:Bindable
    var isLoading: Boolean by bindingProperty(false)
        private set

    @get:Bindable
    var toast: String? by bindingProperty(null)
        private set


    init {
        Timber.d("injection DashboardViewModel")
    }

}


