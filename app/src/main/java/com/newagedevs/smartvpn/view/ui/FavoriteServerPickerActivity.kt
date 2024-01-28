package com.newagedevs.smartvpn.view.ui


import android.os.Bundle
import com.hjq.bar.OnTitleBarListener
import com.hjq.bar.TitleBar
import com.newagedevs.smartvpn.R
import com.newagedevs.smartvpn.databinding.ActivityFavoriteServerPickerBinding
import com.newagedevs.smartvpn.view.adapter.FavoriteServerAdapter
import com.skydoves.bindables.BindingActivity
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel


class FavoriteServerPickerActivity : BindingActivity<ActivityFavoriteServerPickerBinding>(R.layout.activity_favorite_server_picker) {

    private val favoriteServerAdapter: FavoriteServerAdapter by inject()
    private val viewModel: MainViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding {
            vm = viewModel
            adapter = favoriteServerAdapter
        }

        binding.tbMainBar.setOnTitleBarListener(object : OnTitleBarListener {
            override fun onLeftClick(titleBar: TitleBar) {
                finish()
            }

            override fun onTitleClick(titleBar: TitleBar) {

            }

            override fun onRightClick(titleBar: TitleBar) {

            }
        })

        viewModel.refreshFavoriteServerList()
    }

}