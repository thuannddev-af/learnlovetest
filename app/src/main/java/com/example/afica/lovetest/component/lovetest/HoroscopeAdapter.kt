package com.example.afica.lovetest.component.lovetest

import android.view.LayoutInflater
import android.view.ViewGroup
import com.example.afica.lovetest.base.adapter.BaseAdapter
import com.example.afica.lovetest.data.model.ZodiacSign
import com.example.afica.lovetest.databinding.ItemHoroscopeBinding

class HoroscopeAdapter : BaseAdapter<ZodiacSign, ItemHoroscopeBinding>() {
    var selectedCode = ""

    override fun provideViewBinding(parent: ViewGroup): ItemHoroscopeBinding =
        ItemHoroscopeBinding.inflate(LayoutInflater.from(parent.context), parent, false)

    override fun binData(viewBinding: ItemHoroscopeBinding, item: ZodiacSign, position: Int) {
        with(viewBinding) {
            imgSign.setImageResource(item.iconRes)
            root.contentDescription = root.context.getString(item.nameRes)
            root.isSelected = item.code == selectedCode
            root.setOnClickListener { onClick?.invoke(item) }
        }
    }
}
