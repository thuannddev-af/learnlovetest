package com.example.afica.lovetest.component.permission

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings

import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.afica.lovetest.component.main.MainActivity
import com.example.afica.lovetest.R
import com.example.afica.lovetest.base.activity.BaseActivity
import com.example.afica.lovetest.databinding.ActivityPermissionBinding
import com.example.afica.lovetest.utils.setOnClickWithPressEffect

class PermissionActivity : BaseActivity<ActivityPermissionBinding>() {
    private val requestPhotos = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        updatePhotoAccess()
        if (!hasPhotoAccess() && photoPermissions().none { shouldShowRequestPermissionRationale(it) }) {
            AlertDialog.Builder(this)
                .setMessage(R.string.permission_photo_settings_message)
                .setPositiveButton(R.string.permission_open_settings) { _, _ -> openAppSettings() }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    override fun provideViewBinding(): ActivityPermissionBinding = ActivityPermissionBinding.inflate(layoutInflater)

    override fun initViews() {
        updatePhotoAccess()
        viewBaseActivity.switchAccess.setOnClickListener {
            updatePhotoAccess()
            if (hasPhotoAccess()) {
                openAppSettings()
            } else {
                requestPhotos.launch(photoPermissions())
            }
        }
        viewBaseActivity.btnContinue.setOnClickWithPressEffect {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        ViewCompat.setOnApplyWindowInsetsListener(viewBaseActivity.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onResume() {
        super.onResume()
        updatePhotoAccess()
    }

    private fun photoPermissions(): Array<String> = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
        )
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    private fun hasPhotoAccess(): Boolean = photoPermissions().any {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun updatePhotoAccess() {
        viewBaseActivity.switchAccess.isChecked = hasPhotoAccess()
    }

    private fun openAppSettings() {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
    }
}

