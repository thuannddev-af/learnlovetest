package com.example.afica.lovetest.component.dialog

import com.example.afica.lovetest.utils.setOnClickWithPressEffect

import com.example.afica.lovetest.utils.Constant

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.example.afica.lovetest.databinding.DialogPhotoOptionsBinding

class PhotoOptionsDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogPhotoOptionsBinding.inflate(layoutInflater)
        binding.btnCamera.setOnClickWithPressEffect { select(Constant.PHOTO_OPTION_CAMERA) }
        binding.btnUpload.setOnClickWithPressEffect { select(Constant.PHOTO_OPTION_UPLOAD) }
        return AlertDialog.Builder(requireContext()).setView(binding.root).create()
    }

    private fun select(option: String) {
        parentFragmentManager.setFragmentResult(Constant.RESULT_PHOTO_OPTION, bundleOf(Constant.PHOTO_OPTION to option))
        dismiss()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout((resources.displayMetrics.widthPixels * 0.88f).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    
}
