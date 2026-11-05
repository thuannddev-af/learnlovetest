package com.example.afica.lovetest.component.main.background

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.afica.lovetest.R
import com.example.afica.lovetest.base.adapter.BaseAdapter
import com.example.afica.lovetest.databinding.ItemBackgroundBinding

class BackgroundAdapter : BaseAdapter<Uri?, ItemBackgroundBinding>() {
    private var selectedUri: Uri? = null

    fun selectBackground(uri: Uri?) {
        val previous = dataSet.indexOf(selectedUri)
        selectedUri = uri
        val selected = dataSet.indexOf(uri)
        if (previous >= 0) notifyItemChanged(previous)
        if (selected >= 0 && selected != previous) notifyItemChanged(selected)
    }

    override fun provideViewBinding(parent: ViewGroup): ItemBackgroundBinding =
        ItemBackgroundBinding.inflate(LayoutInflater.from(parent.context), parent, false)

    override fun binData(viewBinding: ItemBackgroundBinding, item: Uri?, position: Int) {
        with(viewBinding) {
            root.isSelected = item != null && item == selectedUri
            tvPlus.visibility = if (item == null) View.VISIBLE else View.GONE
            imgBackground.visibility = if (item == null) View.INVISIBLE else View.VISIBLE
            imgBackground.setImageDrawable(null)
            if (item != null) {
                try {
                    imgBackground.setImageURI(item)
                } catch (_: SecurityException) {
                    imgBackground.setImageResource(R.drawable.default1)
                }
                if (imgBackground.drawable == null) imgBackground.setImageResource(R.drawable.default1)
            }
            root.contentDescription = root.context.getString(
                if (item == null) R.string.background_add_photo else R.string.background_photo_number,
                position
            )
            root.setOnClickListener { onClick?.invoke(item) }
        }
    }
}
