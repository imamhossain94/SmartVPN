package com.newagedevs.smartvpn.binding


import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.newagedevs.smartvpn.model.Server
import com.newagedevs.smartvpn.model.VpnServer
import com.newagedevs.smartvpn.view.adapter.FavoriteServerAdapter
import com.newagedevs.smartvpn.view.adapter.ServerAdapter
import com.skydoves.whatif.whatIfNotNullAs
import com.skydoves.whatif.whatIfNotNullOrEmpty
import jp.wasabeef.recyclerview.adapters.AlphaInAnimationAdapter
import jp.wasabeef.recyclerview.animators.SlideInLeftAnimator

object RecyclerViewBinding {
    @JvmStatic
    @BindingAdapter("adapter")
    fun bindAdapter(view: RecyclerView, baseAdapter: RecyclerView.Adapter<*>) {
        view.adapter = baseAdapter//AlphaInAnimationAdapter(baseAdapter)
    }

    @JvmStatic
    @BindingAdapter("toast")
    fun bindToast(view: ConstraintLayout, text: String?) {
        text.whatIfNotNullOrEmpty {
            Toast.makeText(view.context, it, Toast.LENGTH_SHORT).show()
        }
    }

    @JvmStatic
    @BindingAdapter("adapterServerList")
    fun bindAdapterServerList(view: RecyclerView, items: List<VpnServer>?) {
        items.whatIfNotNullOrEmpty {
            view.adapter.whatIfNotNullAs<ServerAdapter> { adapter ->
                adapter.updateServerList(it)
            }
        }
    }

    @JvmStatic
    @BindingAdapter("favoriteServerList")
    fun bindFavoriteServerList(view: RecyclerView, items: List<VpnServer>?) {
        items.whatIfNotNullOrEmpty {
            view.adapter.whatIfNotNullAs<FavoriteServerAdapter> { adapter ->
                adapter.updateServerList(it)
            }
        }
    }

}
