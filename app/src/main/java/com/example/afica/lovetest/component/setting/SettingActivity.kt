package com.example.afica.lovetest.component.setting

import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.afica.lovetest.base.activity.BaseActivity
import com.example.afica.lovetest.component.language.LanguageActivity
import com.example.afica.lovetest.databinding.ActivitySettingBinding
import com.example.afica.lovetest.utils.setOnClickWithPressEffect

class SettingActivity : BaseActivity<ActivitySettingBinding>() {
    override fun provideViewBinding() = ActivitySettingBinding.inflate(layoutInflater)

    override fun initViews() {
        WindowInsetsControllerCompat(window, viewBaseActivity.root).isAppearanceLightStatusBars = true
//        ViewCompat.setOnApplyWindowInsetsListener(viewBaseActivity.root) { view, insets ->
//            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
//            insets
//        }
        viewBaseActivity.btnBack.setOnClickWithPressEffect { onBack() }
        viewBaseActivity.btnLanguage.setOnClickListener {
            LanguageActivity.start(this, isFromSplash = false)
        }
    }
}
