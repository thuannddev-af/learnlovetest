package com.example.afica.lovetest.component.lovetest

import com.example.afica.lovetest.utils.Constant

import android.content.Intent
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.afica.lovetest.R
import com.example.afica.lovetest.base.activity.BaseActivity
import com.example.afica.lovetest.component.main.profile.ProfileStore
import com.example.afica.lovetest.databinding.ActivityNameTestBinding
import com.example.afica.lovetest.utils.SharedPreferenceUtils

class NameTestActivity : BaseActivity<ActivityNameTestBinding>() {
    private val preferences by lazy { SharedPreferenceUtils(this, "name_test") }

    override fun provideViewBinding(): ActivityNameTestBinding = ActivityNameTestBinding.inflate(layoutInflater)

    override fun initViews() {
        WindowInsetsControllerCompat(window, viewBaseActivity.root).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
//        ViewCompat.setOnApplyWindowInsetsListener(viewBaseActivity.root) { view, insets ->
//            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            val keyboard = insets.getInsets(WindowInsetsCompat.Type.ime())
//            view.setPadding(bars.left, bars.top, bars.right, maxOf(bars.bottom, keyboard.bottom))
//            insets
//        }
        viewBaseActivity.btnBack.setOnClickListener { onBack() }
        viewBaseActivity.btnLoveTest.setOnClickListener { prepareNameTest() }
    }

    override fun initData() {
        val profiles = ProfileStore(this)
        viewBaseActivity.edtYourName.setText(preferences.getString(YOUR_NAME, profiles.load(Constant.PROFILE_BOY).name))
        viewBaseActivity.edtPartnerName.setText(preferences.getString(PARTNER_NAME, profiles.load(Constant.PROFILE_GIRL).name))
    }

    private fun prepareNameTest() {
        val yourName = viewBaseActivity.edtYourName.text?.toString()?.trim().orEmpty()
        val partnerName = viewBaseActivity.edtPartnerName.text?.toString()?.trim().orEmpty()
        viewBaseActivity.edtYourName.error = null
        viewBaseActivity.edtPartnerName.error = null
        if (yourName.isEmpty()) {
            viewBaseActivity.edtYourName.error = getString(R.string.profile_name_required)
            viewBaseActivity.edtYourName.requestFocus()
            return
        }
        if (partnerName.isEmpty()) {
            viewBaseActivity.edtPartnerName.error = getString(R.string.profile_name_required)
            viewBaseActivity.edtPartnerName.requestFocus()
            return
        }
        preferences.edit {
            putString(YOUR_NAME, yourName)
            putString(PARTNER_NAME, partnerName)
        }
        viewBaseActivity.edtYourName.clearFocus()
        viewBaseActivity.edtPartnerName.clearFocus()
        WindowInsetsControllerCompat(window, viewBaseActivity.root).hide(WindowInsetsCompat.Type.ime())
        startActivity(Intent(this, NameTestResultActivity::class.java).apply {
            putExtra(Constant.EXTRA_YOUR_NAME, yourName)
            putExtra(Constant.EXTRA_PARTNER_NAME, partnerName)
        })
    }

    companion object {
        private const val YOUR_NAME = "your_name"
        private const val PARTNER_NAME = "partner_name"
    }
}
