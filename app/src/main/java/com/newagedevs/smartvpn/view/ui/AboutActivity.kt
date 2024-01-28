package com.newagedevs.smartvpn.view.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import com.hjq.bar.OnTitleBarListener
import com.hjq.bar.TitleBar
import com.newagedevs.smartvpn.R
import com.newagedevs.smartvpn.databinding.ActivityAboutBinding
import com.newagedevs.smartvpn.utils.Constants
import com.newagedevs.smartvpn.view.dialog.BaseDialog
import com.newagedevs.smartvpn.view.dialog.MessageDialog
import com.skydoves.bindables.BindingActivity
import org.koin.android.viewmodel.ext.android.viewModel


class AboutActivity : BindingActivity<ActivityAboutBinding>(R.layout.activity_about) {

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
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.setData(Uri.parse("package:$packageName"))
                startActivity(intent)
            }
        })
    }

    fun onOtherAppClicked(view: View) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Constants.publisherName)))
    }

    fun onPrivacyPolicyClicked(view: View) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Constants.privacyPolicyUrl)))
    }

    fun onSourceCodeClicked(view: View) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Constants.sourceCodeUrl)))
    }

}