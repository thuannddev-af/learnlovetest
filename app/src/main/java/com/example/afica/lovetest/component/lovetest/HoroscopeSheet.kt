package com.example.afica.lovetest.component.lovetest

import com.example.afica.lovetest.utils.Constant

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.example.afica.lovetest.databinding.SheetHoroscopeBinding
import com.example.afica.lovetest.domain.usecase.GetZodiacSignsUseCase
import com.google.android.material.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
@AndroidEntryPoint
class HoroscopeSheet : BottomSheetDialogFragment() {

    override fun getTheme(): Int = com.example.afica.lovetest.R.style.ThemeOverlay_Lovetest_BackgroundSheet

    @Inject lateinit var getSigns: GetZodiacSignsUseCase
    private var binding: SheetHoroscopeBinding? = null
    private val horoscopeAdapter = HoroscopeAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        SheetHoroscopeBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding?.rclSigns?.adapter = horoscopeAdapter
        horoscopeAdapter.selectedCode = requireArguments().getString(Constant.SIGN_CODE).orEmpty()
        horoscopeAdapter.onClick = { sign ->
            parentFragmentManager.setFragmentResult(
                requireArguments().getString(REQUEST_KEY)!!,
                Bundle().apply { putString(Constant.SIGN_CODE, sign.code) }
            )
            dismiss()
        }
        viewLifecycleOwner.lifecycleScope.launch {
            horoscopeAdapter.setData(ArrayList(getSigns.execute(GetZodiacSignsUseCase.Param())))
        }
    }

    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.findViewById<View>(
            R.id.design_bottom_sheet
        )?.apply {
            background = ColorDrawable(Color.TRANSPARENT)
            layoutParams = layoutParams.apply { height = (resources.displayMetrics.heightPixels * 0.66f).toInt() }
            BottomSheetBehavior.from(this).apply {
                skipCollapsed = true
                state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
    }

    override fun onDestroyView() {
        binding?.rclSigns?.adapter = null
        horoscopeAdapter.onClick = null
        binding = null
        super.onDestroyView()
    }

    companion object {
        private const val REQUEST_KEY = "request_key"
        fun newInstance(selectedCode: String, requestKey: String) = HoroscopeSheet().apply {
            arguments = Bundle().apply {
                putString(Constant.SIGN_CODE, selectedCode)
                putString(REQUEST_KEY, requestKey)
            }
        }
    }
}
