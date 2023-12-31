package com.newagedevs.smartvpn.view.ui

import android.os.Bundle
import com.newagedevs.smartvpn.R
import com.newagedevs.smartvpn.databinding.ActivityMainBinding
import com.newagedevs.smartvpn.view.adapter.ServerAdapter
import com.skydoves.bindables.BindingActivity
import org.koin.android.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf


class MainActivity : BindingActivity<ActivityMainBinding>(R.layout.activity_main) {

    private lateinit var serverAdapter: ServerAdapter
    private val viewModel: MainViewModel by viewModel { parametersOf(serverAdapter) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        serverAdapter = ServerAdapter()

        binding {
            vm = viewModel
            adapter = serverAdapter
        }

    }


}