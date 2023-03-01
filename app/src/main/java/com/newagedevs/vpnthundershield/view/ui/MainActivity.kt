package com.newagedevs.vpnthundershield.view.ui

import android.os.Bundle
import com.newagedevs.vpnthundershield.R
import com.newagedevs.vpnthundershield.databinding.ActivityMainBinding
import com.skydoves.bindables.BindingActivity
import org.koin.android.viewmodel.ext.android.getViewModel


class MainActivity : BindingActivity<ActivityMainBinding>(R.layout.activity_main) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding {
            vm = getViewModel()
        }

    }


}