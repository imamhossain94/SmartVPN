package com.newagedevs.vpnthundershield.base

import com.skydoves.bindables.BindingViewModel
import com.skydoves.sandwich.disposables.CompositeDisposable

abstract class DisposableViewModel : BindingViewModel() {

    val disposables: CompositeDisposable = CompositeDisposable()

  override fun onCleared() {
    super.onCleared()
    disposables.clear()
  }
}
