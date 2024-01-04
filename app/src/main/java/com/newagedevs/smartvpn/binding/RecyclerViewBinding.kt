package com.newagedevs.smartvpn.binding


import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.newagedevs.smartvpn.model.Server
import com.newagedevs.smartvpn.view.adapter.ServerAdapter
import com.skydoves.whatif.whatIfNotNullAs
import com.skydoves.whatif.whatIfNotNullOrEmpty

object RecyclerViewBinding {
    @JvmStatic
    @BindingAdapter("adapter")
    fun bindAdapter(view: RecyclerView, baseAdapter: RecyclerView.Adapter<*>) {
        view.adapter = baseAdapter
//        val dividerItemDecoration = DividerItemDecoration(view.context, LinearLayoutManager.VERTICAL)
//        view.addItemDecoration(dividerItemDecoration)
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
    fun bindAdapterServerList(view: RecyclerView, medications: List<Server>?) {
        medications.whatIfNotNullOrEmpty { items ->
            view.adapter.whatIfNotNullAs<ServerAdapter> { adapter ->
                adapter.updateServerList(items)
            }
        }
    }

}
