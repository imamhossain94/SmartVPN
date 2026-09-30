package com.newagedevs.smartvpn.view.adapter

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.newagedevs.smartvpn.R
import com.newagedevs.smartvpn.databinding.ItemServerBinding
import com.newagedevs.smartvpn.model.VpnServer
import com.skydoves.bindables.binding

/** Renders the user's favourite VPN servers. See [ServerAdapter] for the DI rationale. */
class FavoriteServerAdapter(
    private val onServerSelected: (VpnServer) -> Unit,
) : RecyclerView.Adapter<FavoriteServerAdapter.ServerViewHolder>() {

    private val items = mutableListOf<VpnServer>()

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServerViewHolder {
        val binding = parent.binding<ItemServerBinding>(R.layout.item_server)

        return ServerViewHolder(binding).apply {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) return@setOnClickListener
                onServerSelected(items[position])
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateServerList(servers: List<VpnServer>) {
        items.clear()
        items.addAll(servers)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ServerViewHolder, position: Int) {
        holder.binding.apply {
            server = items[position]
            executePendingBindings()
        }
    }

    fun getServer(index: Int): VpnServer = items[index]

    class ServerViewHolder(val binding: ItemServerBinding) :
        RecyclerView.ViewHolder(binding.root)
}
