package com.newagedevs.smartvpn.view.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.newagedevs.smartvpn.R
import com.newagedevs.smartvpn.databinding.ItemServerBinding
import com.newagedevs.smartvpn.model.Server
import com.skydoves.bindables.binding


class ServerAdapter : RecyclerView.Adapter<ServerAdapter.ServerViewHolder>() {

    private val items = mutableListOf<Server>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServerViewHolder {

        val binding = parent.binding<ItemServerBinding>(R.layout.item_server)

        return ServerViewHolder(binding).apply {
            binding.root.setOnClickListener { view ->
                val position =
                    adapterPosition.takeIf { it != RecyclerView.NO_POSITION }
                        ?: return@setOnClickListener

                items.find { it.isConnected }?.isConnected = false
                items.find { items.indexOf(it) == position }?.isConnected = true

                notifyDataSetChanged()
            }
        }

    }

    fun updateServerList(servers: List<Server>) {
        items.clear()
        items.addAll(servers)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ServerViewHolder, position: Int) {
        holder.binding.apply {
            server = items[position]
            activeColor = "F2F2F2"
            inactiveColor = "#FFFFFF"
            executePendingBindings()
        }
    }

    fun getServer(index: Int): Server = items[index]

    class ServerViewHolder(val binding: ItemServerBinding) :
        RecyclerView.ViewHolder(binding.root)

}

