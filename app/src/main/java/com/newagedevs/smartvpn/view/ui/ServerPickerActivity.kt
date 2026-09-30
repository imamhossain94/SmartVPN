package com.newagedevs.smartvpn.view.ui

import android.os.Bundle
import com.hjq.bar.OnTitleBarListener
import com.hjq.bar.TitleBar
import com.newagedevs.smartvpn.R
import com.newagedevs.smartvpn.databinding.ActivityServerPickerBinding
import com.newagedevs.smartvpn.extensions.applyEdgeToEdgeInsets
import com.newagedevs.smartvpn.view.adapter.ServerAdapter
import com.skydoves.bindables.BindingActivity
import org.koin.androidx.viewmodel.ext.android.viewModel

class ServerPickerActivity : BindingActivity<ActivityServerPickerBinding>(R.layout.activity_server_picker) {

    private val viewModel: MainViewModel by viewModel()

    /**
     * Owned by this Activity rather than by the DI container: a container-cached
     * instance would pin this Activity for the life of the process, and the old
     * container definition was parameterised, so resolving it here used to throw
     * [org.koin.core.error.NoParameterFoundException] outright.
     */
    private val serverAdapter by lazy {
        ServerAdapter { server ->
            viewModel.selectServer(server)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        applyEdgeToEdgeInsets(binding.root)

        binding {
            vm = viewModel
            adapter = serverAdapter
        }

        binding.tbMainBar.setOnTitleBarListener(object : OnTitleBarListener {
            override fun onLeftClick(titleBar: TitleBar) = finish()
            override fun onTitleClick(titleBar: TitleBar) = Unit
            override fun onRightClick(titleBar: TitleBar) = Unit
        })

        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.fetchVpnServer()
        }

        // The list is pushed into the adapter by the `adapterServerList` binding
        // adapter whenever `vm.servers` changes, so no manual update is needed.
        viewModel.loadServersIfNeeded()
    }

    override fun onDestroy() {
        // The spinner is driven by a listener, so clear it explicitly instead of
        // leaving it spinning behind a dead view hierarchy.
        binding.swipeRefreshLayout.isRefreshing = false
        super.onDestroy()
    }
}
