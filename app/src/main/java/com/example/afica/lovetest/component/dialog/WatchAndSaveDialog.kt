package com.example.afica.lovetest.component.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.ViewGroup
import com.example.afica.lovetest.databinding.DialogWatchAndSaveBinding
import com.example.afica.lovetest.utils.setOnClickWithPressEffect

class WatchAndSaveDialog(context: Context) : Dialog(context) {
    private var onWatchAndSave: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = DialogWatchAndSaveBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        binding.btnCancel.setOnClickWithPressEffect { dismiss() }
        binding.btnWatchAndSave.setOnClickWithPressEffect {
            val action = onWatchAndSave
            dismiss()
            action?.invoke()
        }
    }

    fun show(onWatchAndSave: () -> Unit) {
        if (isShowing) return
        this.onWatchAndSave = onWatchAndSave
        super.show()
        window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.88f).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun dismiss() {
        onWatchAndSave = null
        super.dismiss()
    }

    override fun cancel() {
        onWatchAndSave = null
        super.cancel()
    }
}
