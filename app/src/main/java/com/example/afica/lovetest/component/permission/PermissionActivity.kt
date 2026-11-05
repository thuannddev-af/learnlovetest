package com.example.afica.lovetest.component.permission

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
    private val requestCamera = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        updateCameraAccess()
        if (!granted && !shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            showCameraPermissionDialog()
        }
    }

    override fun provideViewBinding(): ActivityPermissionBinding = ActivityPermissionBinding.inflate(layoutInflater)

    override fun initViews() {
        updateCameraAccess()
        viewBaseActivity.switchAccess.setOnClickListener {
            updateCameraAccess()
            if (hasCameraAccess()) {
                openAppSettings()
            } else {
                requestCamera.launch(Manifest.permission.CAMERA)
            }
        }
        viewBaseActivity.btnContinue.setOnClickWithPressEffect {
            if (hasCameraAccess()) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                showCameraPermissionDialog()
            }
        }
//        ViewCompat.setOnApplyWindowInsetsListener(viewBaseActivity.root) { view, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
//            insets
//        }
    }

    override fun onResume() {
        super.onResume()
        updateCameraAccess()
    }

    private fun hasCameraAccess(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun updateCameraAccess() {
        viewBaseActivity.switchAccess.isChecked = hasCameraAccess()
    }

    private fun showCameraPermissionDialog() {
        AlertDialog.Builder(this)
            .setMessage(R.string.permission_camera_settings_message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.permission_open_settings) { _, _ -> openAppSettings() }
            .show()
    }

    private fun openAppSettings() {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
    }
}

