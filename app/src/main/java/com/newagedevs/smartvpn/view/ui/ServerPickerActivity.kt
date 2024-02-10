package com.newagedevs.smartvpn.view.ui


import android.os.Bundle
import com.hjq.bar.OnTitleBarListener
import com.hjq.bar.TitleBar
import com.newagedevs.smartvpn.R
import com.newagedevs.smartvpn.databinding.ActivityServerPickerBinding
import com.newagedevs.smartvpn.utils.AdsHelper
import com.newagedevs.smartvpn.view.adapter.ServerAdapter
import com.skydoves.bindables.BindingActivity
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel


class ServerPickerActivity : BindingActivity<ActivityServerPickerBinding>(R.layout.activity_server_picker) {

    private val serverAdapter: ServerAdapter by inject()
    private val viewModel: MainViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding {
            vm = viewModel
            adapter = serverAdapter
        }

        AdsHelper.createBannerAd(this, binding.adsContainer)

        binding.tbMainBar.setOnTitleBarListener(object : OnTitleBarListener {
            override fun onLeftClick(titleBar: TitleBar) {
                finish()
            }

            override fun onTitleClick(titleBar: TitleBar) {

            }

            override fun onRightClick(titleBar: TitleBar) {

            }
        })

        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.fetchVpnServer {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }


    }

}