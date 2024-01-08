package com.newagedevs.smartvpn.view.ui


import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hjq.bar.OnTitleBarListener
import com.hjq.bar.TitleBar
import com.newagedevs.smartvpn.R
import com.newagedevs.smartvpn.databinding.ActivityCountryPickerBinding
import com.newagedevs.smartvpn.interfaces.ChangeServer
import com.newagedevs.smartvpn.view.adapter.ServerAdapter
import com.skydoves.bindables.BindingActivity
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf


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