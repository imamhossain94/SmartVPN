package com.newagedevs.smartvpn.view.ui

import android.os.Bundle
import com.hjq.bar.OnTitleBarListener
import com.hjq.bar.TitleBar
import com.newagedevs.smartvpn.R
import com.newagedevs.smartvpn.databinding.ActivityFavoriteServerPickerBinding
import com.newagedevs.smartvpn.extensions.applyEdgeToEdgeInsets
import com.newagedevs.smartvpn.view.adapter.FavoriteServerAdapter
import com.skydoves.bindables.BindingActivity
import org.koin.androidx.viewmodel.ext.android.viewModel

class FavoriteServerPickerActivity :
    BindingActivity<ActivityFavoriteServerPickerBinding>(R.layout.activity_favorite_server_picker) {

    private val viewModel: MainViewModel by viewModel()

    private val favoriteServerAdapter by lazy {
        FavoriteServerAdapter { server ->
            viewModel.selectServer(server)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        applyEdgeToEdgeInsets(binding.root)

        binding {
            vm = viewModel
            adapter = favoriteServerAdapter
        }

        binding.tbMainBar.setOnTitleBarListener(object : OnTitleBarListener {
            override fun onLeftClick(titleBar: TitleBar) = finish()
            override fun onTitleClick(titleBar: TitleBar) = Unit
            override fun onRightClick(titleBar: TitleBar) = Unit
        })

        // Re-read on every resume: favourites may have been toggled on the main
        // screen just before this Activity was opened.
        viewModel.refreshFavoriteServerList()
        favoriteServerAdapter.updateServerList(viewModel.favoriteServers)
    }
}
