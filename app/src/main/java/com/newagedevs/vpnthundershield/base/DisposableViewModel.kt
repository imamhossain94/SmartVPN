package com.newagedevs.vpnthundershield.base

import com.skydoves.bindables.BindingViewModel

abstract class DisposableViewModel : BindingViewModel() {
  override fun onCleared() {
    super.onCleared()

  }
}
