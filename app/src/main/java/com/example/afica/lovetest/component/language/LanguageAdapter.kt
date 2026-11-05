package com.example.afica.lovetest.component.language

import android.view.LayoutInflater
import android.view.ViewGroup
import com.example.afica.lovetest.base.adapter.BaseAdapter
import com.example.afica.lovetest.data.model.Language
import com.example.afica.lovetest.databinding.ItemLanguageBinding

class LanguageAdapter : BaseAdapter<Language, ItemLanguageBinding>() {

    fun selectedLanguage(): Language? = dataSet.firstOrNull { it.selected }

    override fun provideViewBinding(parent: ViewGroup): ItemLanguageBinding {
        return ItemLanguageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    }

    fun selectLanguage(languageCode: String) {
        var index = dataSet.indexOfFirst { it.selected }
        if (index > -1) {
            dataSet[index].selected = false
            notifyItemChanged(index)
        }
        index = dataSet.indexOfFirst { it.languageCode == languageCode }
        if (index > -1) {
            dataSet[index].selected = true
            notifyItemChanged(index)
        }
    }

    override fun binData(viewBinding: ItemLanguageBinding, item: Language, position: Int) {
        with(viewBinding) {
            imgLanguage.setImageResource(item.iconRes)
            tvTitle.setText(item.nameRes)
            root.isSelected = item.selected
            root.setOnClickListener { onClick?.invoke(item) }
        }
    }
}
