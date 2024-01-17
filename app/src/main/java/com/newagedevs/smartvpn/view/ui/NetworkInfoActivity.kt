package com.newagedevs.smartvpn.view.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import com.hjq.bar.OnTitleBarListener
import com.hjq.bar.TitleBar
import com.newagedevs.smartvpn.R
import com.newagedevs.smartvpn.databinding.ActivityNetworkInfoBinding
import com.newagedevs.smartvpn.view.dialog.AboutDialog
import com.newagedevs.smartvpn.view.dialog.BaseDialog
import com.skydoves.bindables.BindingActivity
import org.koin.android.viewmodel.ext.android.viewModel


class NetworkInfoActivity : BindingActivity<ActivityNetworkInfoBinding>(R.layout.activity_network_info) {

    private val viewModel: MainViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding {
            vm = viewModel
        }

        binding.tbMainBar.setOnTitleBarListener(object : OnTitleBarListener {
            override fun onLeftClick(titleBar: TitleBar) {
                finish()
            }

            override fun onRightClick(titleBar: TitleBar) {

            }
        })

        viewModel.fetchIpDetails {
            this@NetworkInfoActivity.runOnUiThread {
                viewModel.currentIPDetails?.let {

                    binding.ipAddress.setRightText(it.ip)
                    binding.internetProvider.setRightText(it.isp)
                    binding.location.setRightText("${it.city}, ${it.regionName}, ${it.country}")
                    binding.postalCode.setRightText(it.zip)
                    binding.timezone.setRightText(it.timezone)

//                    AboutDialog.Builder(this@NetworkInfoActivity)
//                        .setCancelable(true)
//                        .setCanceledOnTouchOutside(true)
//                        .setTitle(it.country)
//                        .setVersionName(it.ip)
//                        .setDescription("Internet Provider: ${it.isp}\n" +
//                                "Location: ${it.city}, ${it.regionName}, ${it.country}\n" +
//                                "Timezone: ${it.timezone}")
//                        .setListener(object : AboutDialog.OnListener {
//                            override fun onClick(dialog: BaseDialog?) { }
//                        }).show()




                }
            }
        }

    }
}