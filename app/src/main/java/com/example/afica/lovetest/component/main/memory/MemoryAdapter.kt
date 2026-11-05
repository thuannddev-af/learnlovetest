package com.example.afica.lovetest.component.main.memory

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import com.example.afica.lovetest.base.adapter.BaseAdapter
import com.example.afica.lovetest.data.model.Memory
import com.example.afica.lovetest.databinding.ItemMemoryBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class MemoryAdapter : BaseAdapter<Memory, ItemMemoryBinding>() {
    override fun provideViewBinding(parent: ViewGroup): ItemMemoryBinding =
        ItemMemoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)

    override fun binData(viewBinding: ItemMemoryBinding, item: Memory, position: Int) {
        with(viewBinding) {
            imgPhoto.setImageDrawable(null)
            imgPhoto.setImageURI(Uri.fromFile(File(item.photoPath)))
            tvTitle.text = item.title
            tvDate.text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(item.date)
        }
    }
}
