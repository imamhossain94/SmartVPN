package com.newagedevs.smartvpn.view.ui


import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.hjq.bar.OnTitleBarListener
import com.hjq.bar.TitleBar
import com.newagedevs.smartvpn.R
import com.newagedevs.smartvpn.databinding.ActivityMainBinding
import com.newagedevs.smartvpn.model.CustomItem
import com.newagedevs.smartvpn.model.ItemUtils
import com.newagedevs.smartvpn.view.CustomListBalloonFactory
import com.newagedevs.smartvpn.view.adapter.CustomAdapter
import com.newagedevs.smartvpn.view.adapter.ServerAdapter
import com.skydoves.balloon.ArrowOrientation
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.bindables.BindingActivity
import org.koin.android.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf


class MainActivity : BindingActivity<ActivityMainBinding>(R.layout.activity_main), CustomAdapter.CustomViewHolder.Delegate {

    private lateinit var serverAdapter: ServerAdapter
    private val viewModel: MainViewModel by viewModel { parametersOf(serverAdapter) }

    var balloon: Balloon? = null
    private val handler = Handler(Looper.getMainLooper())


    private val customAdapter by lazy { CustomAdapter(this) }
    lateinit var customListBalloon: Balloon

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        serverAdapter = ServerAdapter()

        binding {
            vm = viewModel
            adapter = serverAdapter
        }

        balloon = Balloon.Builder(applicationContext)
            .setArrowSize(10)
            .setArrowOrientation(ArrowOrientation.TOP)
            .setIsVisibleArrow(true)
            .setArrowPosition(0.5f)
            .setWidthRatio(0.6f)
            .setHeight(65)
            .setTextSize(15f)
            .setCornerRadius(4f)
            .setAlpha(0.9f)
            .setBackgroundColorResource(R.color.black)
            .setTextColorResource(R.color.white)
            .setText("Click to connect!!")
            .setBalloonAnimation(BalloonAnimation.FADE)
            .setDismissWhenTouchOutside(false)
            .setDismissWhenClicked(false)
            .build()

        customListBalloon = CustomListBalloonFactory().create(this, this)


        // gets customListBalloon's recyclerView.
        val listRecycler: RecyclerView = customListBalloon.getContentView().findViewById(R.id.list_recyclerView)

        listRecycler.adapter = customAdapter
        customAdapter.addCustomItem(ItemUtils.getCustomSamples(this))


        binding.tbMainBar.setOnTitleBarListener(object : OnTitleBarListener {
            override fun onLeftClick(titleBar: TitleBar) {
//                balloon?.showAlignBottom(binding.imagePower)
                customListBalloon.showAlignBottom(titleBar.leftView, 0, 0)
            }

            override fun onTitleClick(titleBar: TitleBar) {

            }

            override fun onRightClick(titleBar: TitleBar) {

            }
        })

//        handler.postDelayed({
//            balloon?.showAlignBottom(binding.imagePower)
//        }, 1000)

    }

    override fun onCustomItemClick(customItem: CustomItem) {
        this.customListBalloon.dismiss()
        Toast.makeText(applicationContext, customItem.title, Toast.LENGTH_SHORT).show()
    }

    fun onChangeCountryClicked(view: View) {
        startActivity(Intent(this, CountryPickerActivity::class.java))
    }

}