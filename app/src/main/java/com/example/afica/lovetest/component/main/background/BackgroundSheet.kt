package com.example.afica.lovetest.component.main.background

import com.example.afica.lovetest.utils.Constant

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.example.afica.lovetest.R
import com.example.afica.lovetest.databinding.SheetBackgroundBinding
import com.example.afica.lovetest.domain.usecase.GetListBackgroundUseCase
import com.example.afica.lovetest.utils.ProfilePhotos
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BackgroundSheet : BottomSheetDialogFragment() {
    override fun getTheme(): Int = R.style.ThemeOverlay_Lovetest_BackgroundSheet

    @Inject lateinit var getListBackgroundUseCase: GetListBackgroundUseCase
    private var defaultBackgrounds: List<Uri> = emptyList()
    private var binding: SheetBackgroundBinding? = null
    private val backgroundAdapter = BackgroundAdapter()
    private var importing = false

    private val uploadPhoto = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) importPhoto(uri)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        SheetBackgroundBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding?.rclBackgrounds?.adapter = backgroundAdapter
        backgroundAdapter.onClick = { uri ->
            if (!importing) {
                if (uri == null) {
                    uploadPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                } else {
                    applyBackground(uri)
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            defaultBackgrounds = getListBackgroundUseCase.execute(GetListBackgroundUseCase.Param()).map {
                Uri.parse("android.resource://" + requireContext().packageName + "/drawable/" + resources.getResourceEntryName(it))
            }
            renderBackgrounds()
        }
    }

    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.findViewById<View>(
            com.google.android.material.R.id.design_bottom_sheet
        )?.apply {
            background = ColorDrawable(Color.TRANSPARENT)
            layoutParams = layoutParams.apply { height = (resources.displayMetrics.heightPixels * 0.62f).toInt() }
            BottomSheetBehavior.from(this).apply {
                skipCollapsed = true
                state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
    }

    private fun renderBackgrounds() {
        val items = arrayListOf<Uri?>(null)
        items.addAll(defaultBackgrounds)
        backgroundAdapter.setData(items)
        backgroundAdapter.selectBackground(
            BackgroundStore(requireContext()).selected() ?: defaultBackgrounds.firstOrNull()
        )
    }

    private fun importPhoto(uri: Uri) {
        val context = requireContext().applicationContext
        importing = true
        binding?.progressImport?.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val path = withContext(Dispatchers.IO) { ProfilePhotos.importPhoto(context, uri) }
                applyBackground(Uri.fromFile(File(path)))
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                Toast.makeText(context, R.string.profile_photo_error, Toast.LENGTH_SHORT).show()
            } finally {
                importing = false
                binding?.progressImport?.visibility = View.GONE
            }
        }
    }

    private fun applyBackground(uri: Uri) {
        backgroundAdapter.selectBackground(uri)
        BackgroundStore(requireContext()).select(uri)
        parentFragmentManager.setFragmentResult(
            Constant.RESULT_BACKGROUND_UPDATED, Bundle().apply { putString(Constant.BACKGROUND_URI, uri.toString()) }
        )
        dismiss()
    }

    override fun onDestroyView() {
        binding?.rclBackgrounds?.adapter = null
        backgroundAdapter.onClick = null
        binding = null
        super.onDestroyView()
    }

    
}
