package com.example.afica.lovetest.component.main.memory

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.afica.lovetest.databinding.ItemMemoryAddBinding

class MemoryAddAdapter : RecyclerView.Adapter<MemoryAddAdapter.AddViewHolder>() {
    var onAddClick: (() -> Unit)? = null

    override fun getItemCount() = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddViewHolder =
        AddViewHolder(ItemMemoryAddBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: AddViewHolder, position: Int) {
        holder.binding.btnAddMemory.setOnClickListener { onAddClick?.invoke() }
    }

    class AddViewHolder(val binding: ItemMemoryAddBinding) : RecyclerView.ViewHolder(binding.root)
}
