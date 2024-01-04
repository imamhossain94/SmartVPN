package com.newagedevs.smartvpn.view.ui


import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.hjq.bar.OnTitleBarListener
import com.hjq.bar.TitleBar
import com.newagedevs.smartvpn.view.dialog.BaseDialog
import com.newagedevs.smartvpn.R
import com.newagedevs.smartvpn.databinding.ActivityMainBinding
import com.newagedevs.smartvpn.model.CustomItem
import com.newagedevs.smartvpn.model.ItemUtils
import com.newagedevs.smartvpn.view.CustomListBalloonFactory
import com.newagedevs.smartvpn.view.adapter.CustomAdapter
import com.newagedevs.smartvpn.view.adapter.ServerAdapter
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

                MessageDialog.Builder(this@MainActivity)
                    // Title can be left empty
                    .setTitle("Title")
                    // Content must be filled in
                    .setMessage("Content")
                    // Confirm button text
                    .setConfirm(getString(R.string.common_confirm))
                    // Set to null to hide the cancel button
                    .setCancel(getString(R.string.common_cancel))
                    // Set to false to keep the dialog open after button click
                    //.setAutoDismiss(false)
                    .setListener(object : MessageDialog.OnListener {
                        override fun onConfirm(dialog: BaseDialog?) {
                            Toast.makeText(applicationContext, "Confirmed", Toast.LENGTH_SHORT).show()
                        }

                        override fun onCancel(dialog: BaseDialog?) {
                            Toast.makeText(applicationContext, "Cancelled", Toast.LENGTH_SHORT).show()
                        }
                    }).show()



                //onChangeCountryClicked(titleBar.rightView)
            }
        })

    }

    override fun onCustomItemClick(customItem: CustomItem) {
        this.customListBalloon.dismiss()
        Toast.makeText(applicationContext, customItem.title, Toast.LENGTH_SHORT).show()
    }

    fun onChangeCountryClicked(view: View) {
        startActivity(Intent(this, CountryPickerActivity::class.java))
    }

}