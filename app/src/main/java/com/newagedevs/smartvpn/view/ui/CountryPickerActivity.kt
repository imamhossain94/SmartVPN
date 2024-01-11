package com.newagedevs.smartvpn.view.ui


import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hjq.bar.OnTitleBarListener
import com.hjq.bar.TitleBar
import com.newagedevs.smartvpn.R
import com.newagedevs.smartvpn.databinding.ActivityCountryPickerBinding
import com.newagedevs.smartvpn.interfaces.ChangeServer
import com.newagedevs.smartvpn.model.Server
import com.newagedevs.smartvpn.network.APIs
import com.newagedevs.smartvpn.view.adapter.ServerAdapter
import com.skydoves.bindables.BindingActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber
import java.io.IOException


class CountryPickerActivity : BindingActivity<ActivityCountryPickerBinding>(R.layout.activity_country_picker) {

    private val serverAdapter: ServerAdapter by inject()
    private val viewModel: MainViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding {
            vm = viewModel
            adapter = serverAdapter
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



    }

}