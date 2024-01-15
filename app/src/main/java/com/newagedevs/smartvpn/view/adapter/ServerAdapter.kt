package com.newagedevs.smartvpn.view.adapter

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.newagedevs.smartvpn.R
import com.newagedevs.smartvpn.databinding.ItemServerBinding
import com.newagedevs.smartvpn.interfaces.ChangeServer
import com.newagedevs.smartvpn.model.Server
import com.newagedevs.smartvpn.model.VpnServer
import com.newagedevs.smartvpn.view.ui.CountryPickerActivity
import com.skydoves.bindables.binding


class ServerAdapter(private val changeServer: ChangeServer) : RecyclerView.Adapter<ServerAdapter.ServerViewHolder>() {

    private val items = mutableListOf<VpnServer>()
    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServerViewHolder {

        val binding = parent.binding<ItemServerBinding>(R.layout.item_server)

        return ServerViewHolder(binding).apply {
            binding.root.setOnClickListener { view ->
                val position = adapterPosition.takeIf { it != RecyclerView.NO_POSITION } ?: return@setOnClickListener
                val server = items[position]
                changeServer.newServer(server)
                (view.context as CountryPickerActivity).finish()
                //notifyDataSetChanged()
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

