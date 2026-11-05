package com.example.afica.lovetest.component.main.profile

import com.example.afica.lovetest.utils.Constant

import android.content.ActivityNotFoundException
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.ListPopupWindow
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import com.example.afica.lovetest.R
import com.example.afica.lovetest.component.dialog.PhotoOptionsDialog
import com.example.afica.lovetest.component.dialog.DatePickerDialog
import com.example.afica.lovetest.data.model.ProfileInformation
import com.example.afica.lovetest.databinding.SheetProfileInformationBinding
import com.example.afica.lovetest.utils.ProfilePhotos
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileInformationSheet : BottomSheetDialogFragment() {
    private var binding: SheetProfileInformationBinding? = null
    private val profileId: String get() = requireArguments().getString(PROFILE_ID)!!
    private var birthday = 0L
    private var photoPath: String? = null
    private var cameraPath: String? = null
    private var draftName = ""
    private var draftMale = true
    private var loadingPhoto = false
    private var genderPopup: ListPopupWindow? = null

    private val pickPhoto = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) importPhoto(uri)
    }
    private val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val capturedPath = cameraPath
        cameraPath = null
        if (success && capturedPath != null) {
            importPhoto(Uri.fromFile(File(capturedPath)), capturedPath)
        } else {
            capturedPath?.let { File(it).delete() }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val profile = ProfileStore(requireContext()).load(profileId)
        birthday = savedInstanceState?.getLong("birthday") ?: profile.birthday
        photoPath = if (savedInstanceState != null) savedInstanceState.getString("photo") else profile.photoPath
        cameraPath = savedInstanceState?.getString("camera")
        draftName = savedInstanceState?.getString("name") ?: profile.name
        draftMale = savedInstanceState?.getBoolean("male") ?: profile.isMale
        childFragmentManager.setFragmentResultListener(BIRTHDAY_RESULT, this) { _, result ->
            birthday = result.getLong(Constant.SELECTED_DATE)
            updateBirthday()
        }
        childFragmentManager.setFragmentResultListener(Constant.RESULT_PHOTO_OPTION, this) { _, result ->
            when (result.getString(Constant.PHOTO_OPTION)) {
                Constant.PHOTO_OPTION_CAMERA -> launchCamera()
                Constant.PHOTO_OPTION_UPLOAD -> pickPhoto.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return SheetProfileInformationBinding.inflate(inflater, container, false).also { binding = it }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val views = binding ?: return
        views.edtName.setText(draftName)
        updateGender()
        views.btnGender.setOnClickListener { showGenderOptions() }
        updateAvatar()
        updateBirthday()
        views.imgAvatar.setOnClickListener { showPhotoOptions() }
        views.btnChangePhoto.setOnClickListener { showPhotoOptions() }
        views.btnBirthday.setOnClickListener { chooseBirthday() }
        views.btnSave.setOnClickListener { saveProfile() }
    }

    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.apply {
            window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.apply {
                background = ColorDrawable(Color.TRANSPARENT)
                BottomSheetBehavior.from(this).apply {
                    skipCollapsed = true
                    state = BottomSheetBehavior.STATE_EXPANDED
                }
            }
        }
    }

    private fun showPhotoOptions() {
        if (!loadingPhoto && childFragmentManager.findFragmentByTag("photo_options") == null) {
            PhotoOptionsDialog().show(childFragmentManager, "photo_options")
        }
    }

    private fun updateGender() {
        binding?.btnGender?.setText(if (draftMale) R.string.home_male else R.string.home_female)
    }

    private fun showGenderOptions() {
        val anchor = binding?.btnGender ?: return
        if (genderPopup?.isShowing == true) return
        genderPopup = ListPopupWindow(requireContext()).apply {
            anchorView = anchor
            width = anchor.width
            isModal = true
            setAdapter(ArrayAdapter(
                requireContext(), android.R.layout.simple_list_item_1,
                listOf(getString(R.string.home_male), getString(R.string.home_female))
            ))
            setOnItemClickListener { _, _, position, _ ->
                draftMale = position == 0
                updateGender()
                dismiss()
            }
            show()
        }
    }

    private fun launchCamera() {
        try {
            val directory = File(requireContext().cacheDir, "profile_camera").apply { mkdirs() }
            val file = File.createTempFile("avatar_", ".jpg", directory)
            cameraPath = file.absolutePath
            val uri = FileProvider.getUriForFile(requireContext(), requireContext().packageName + ".profile.photos", file)
            takePhoto.launch(uri)
        } catch (_: ActivityNotFoundException) {
            cameraPath?.let { File(it).delete() }
            cameraPath = null
            Toast.makeText(requireContext(), R.string.profile_camera_unavailable, Toast.LENGTH_SHORT).show()
        }
    }

    private fun importPhoto(uri: Uri, capturedPath: String? = null) {
        val context = requireContext().applicationContext
        loadingPhoto = true
        binding?.btnSave?.isEnabled = false
        binding?.btnChangePhoto?.setText(R.string.profile_loading_photo)
        lifecycleScope.launch {
            try {
                photoPath = withContext(Dispatchers.IO) { ProfilePhotos.importPhoto(context, uri) }
                updateAvatar()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                Toast.makeText(context, R.string.profile_photo_error, Toast.LENGTH_SHORT).show()
            } finally {
                capturedPath?.let { File(it).delete() }
                loadingPhoto = false
                binding?.btnSave?.isEnabled = true
                binding?.btnChangePhoto?.setText(R.string.profile_change_photo)
            }
        }
    }

    private fun updateAvatar() {
        val avatar = binding?.imgAvatar ?: return
        avatar.setImageResource(if (profileId == Constant.PROFILE_BOY) R.drawable.boy1 else R.drawable.girl1)
        photoPath?.let { path ->
            if (File(path).isFile) avatar.setImageURI(Uri.fromFile(File(path)))
        }
    }

    private fun updateBirthday() {
        binding?.btnBirthday?.text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(birthday)
    }

    private fun chooseBirthday() {
        if (childFragmentManager.findFragmentByTag(BIRTHDAY_RESULT) == null) {
            DatePickerDialog.newInstance(birthday, BIRTHDAY_RESULT).show(childFragmentManager, BIRTHDAY_RESULT)
        }
    }

    private fun saveProfile() {
        val views = binding ?: return
        val name = views.edtName.text?.toString()?.trim().orEmpty()
        if (name.isEmpty()) {
            views.edtName.error = getString(R.string.profile_name_required)
            views.edtName.requestFocus()
            return
        }
        ProfileStore(requireContext()).save(
            profileId,
            ProfileInformation(name, draftMale, birthday, photoPath)
        )
        parentFragmentManager.setFragmentResult(Constant.RESULT_PROFILE_UPDATED, bundleOf(PROFILE_ID to profileId))
        dismiss()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("name", binding?.edtName?.text?.toString() ?: draftName)
        outState.putBoolean("male", draftMale)
        outState.putLong("birthday", birthday)
        outState.putString("photo", photoPath)
        outState.putString("camera", cameraPath)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        genderPopup?.dismiss()
        genderPopup = null
        binding = null
        super.onDestroyView()
    }

    companion object {
        private const val BIRTHDAY_RESULT = "birthday_result"
        private const val PROFILE_ID = "profile_id"
        fun newInstance(id: String) = ProfileInformationSheet().apply { arguments = bundleOf(PROFILE_ID to id) }
    }
}
