package com.newagedevs.smartvpn.view.ui


import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.app.ShareCompat
import androidx.recyclerview.widget.RecyclerView
import com.hjq.bar.OnTitleBarListener
import com.hjq.bar.TitleBar
import com.newagedevs.smartvpn.BuildConfig
import com.newagedevs.smartvpn.R
import com.newagedevs.smartvpn.databinding.ActivityMainBinding
import com.newagedevs.smartvpn.model.CustomItem
import com.newagedevs.smartvpn.model.ItemUtils
import com.newagedevs.smartvpn.utils.Constants
import com.newagedevs.smartvpn.view.CustomListBalloonFactory
import com.newagedevs.smartvpn.view.adapter.CustomAdapter
import com.newagedevs.smartvpn.view.adapter.ServerAdapter
import com.newagedevs.smartvpn.view.dialog.AboutDialog
import com.newagedevs.smartvpn.view.dialog.BaseDialog
import com.newagedevs.smartvpn.view.dialog.MessageDialog
import com.skydoves.balloon.Balloon
import com.skydoves.bindables.BindingActivity
import org.koin.android.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf


class MainActivity : BindingActivity<ActivityMainBinding>(R.layout.activity_main), CustomAdapter.CustomViewHolder.Delegate {

    private lateinit var serverAdapter: ServerAdapter
    private val viewModel: MainViewModel by viewModel { parametersOf(serverAdapter) }

    private val customAdapter by lazy { CustomAdapter(this) }
    lateinit var customListBalloon: Balloon

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        serverAdapter = ServerAdapter()

        binding {
            vm = viewModel
            adapter = serverAdapter
        }

        customListBalloon = CustomListBalloonFactory().create(this, this)

        // gets customListBalloon's recyclerView.
        val listRecycler: RecyclerView = customListBalloon.getContentView().findViewById(R.id.list_recyclerView)

        listRecycler.adapter = customAdapter
        customAdapter.addCustomItem(ItemUtils.getCustomSamples(this))


        binding.tbMainBar.setOnTitleBarListener(object : OnTitleBarListener {
            override fun onLeftClick(titleBar: TitleBar) {
                customListBalloon.showAlignBottom(titleBar.leftView, 0, 0)
            }

            override fun onTitleClick(titleBar: TitleBar) {

            }

            override fun onRightClick(titleBar: TitleBar) {
                onChangeCountryClicked(titleBar.rightView)
            }
        })


        onBackPressedDispatcher.addCallback(this@MainActivity, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                MessageDialog.Builder(this@MainActivity)
                    .setTitle("Exit Confirmation")
                    .setMessage("Are you sure you want to exit the app?")
                    .setConfirm(getString(R.string.confirm))
                    .setCancel(getString(R.string.cancel))
                    .setListener(object : MessageDialog.OnListener {
                        override fun onConfirm(dialog: BaseDialog?) {
                            finish()
                        }

                        override fun onCancel(dialog: BaseDialog?) {  }
                    }).show()
            }
        })

    }

    override fun onCustomItemClick(customItem: CustomItem) {
        this.customListBalloon.dismiss()


        when (customItem.title) {
            getString(R.string.share) -> {
                ShareCompat.IntentBuilder(this@MainActivity)
                    .setType("text/plain")
                    .setChooserTitle("Share ${getString(R.string.app_name)} with:")
                    .setText(Constants.appStoreBaseURL + this.packageName)
                    .startChooser()
            }
            getString(R.string.rate_us) -> {
                MessageDialog.Builder(this@MainActivity)
                    .setTitle("Enjoying ${getString(R.string.app_name)}?")
                    .setMessage("If you like our app, please give it a 5 stars rating in Google Play, thank you.")
                    .setConfirm(getString(R.string.rate))
                    .setCancel(getString(R.string.cancel))
                    .setListener(object : MessageDialog.OnListener {
                        override fun onConfirm(dialog: BaseDialog?) {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Constants.appStoreId)))
                        }
                        override fun onCancel(dialog: BaseDialog?) {  }
                    }).show()
            }
            getString(R.string.other_app) -> {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Constants.publisherName)))
            }
            getString(R.string.privacy_policy) -> {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Constants.privacyPolicyUrl)))
            }
            getString(R.string.source_code) -> {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Constants.sourceCodeUrl)))
            }
            getString(R.string.about) -> {
                AboutDialog.Builder(this@MainActivity)
                    .setCancelable(true)
                    .setCanceledOnTouchOutside(true)
                    .setTitle("About ${getString(R.string.app_name)}")
                    .setVersionName(BuildConfig.VERSION_NAME)
                    .setDescription("Enhances your online privacy and security with an ultra-fast connection, providing continuous safeguarding for your digital experience.")
                    .setListener(object : AboutDialog.OnListener {
                        override fun onClick(dialog: BaseDialog?) {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            intent.setData(Uri.parse("package:$packageName"))
                            startActivity(intent)
                        }
                    }).show()
            }
        }

    }

    fun onChangeCountryClicked(view: View) {
        startActivity(Intent(this, CountryPickerActivity::class.java))
    }

}