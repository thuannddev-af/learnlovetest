package com.example.afica.lovetest.component.language

import com.example.afica.lovetest.utils.Constant

import android.content.Context
import android.content.Intent
import android.app.ActivityOptions
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.afica.lovetest.R
import com.example.afica.lovetest.base.activity.BaseActivity
import com.example.afica.lovetest.component.onboarding.OnboardingActivity
import com.example.afica.lovetest.data.model.Language
import com.example.afica.lovetest.databinding.ActivityLanguageBinding
import com.example.afica.lovetest.utils.LocaleHelper
import com.example.afica.lovetest.utils.SharedPreferenceUtils
import com.example.afica.lovetest.utils.setOnClickWithPressEffect
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@AndroidEntryPoint
class LanguageActivity : BaseActivity<ActivityLanguageBinding>() {
    private var selectLanguageModel: Language? = null
    private var selectedLanguageCode: String? = null
    private val viewModel: LanguageViewModel by viewModels()
    private val languageAdapter = LanguageAdapter()
    private val preferences by lazy { SharedPreferenceUtils.getInstance(this) }
    private val isFromSplash: Boolean
        get() = intent.getBooleanExtra(Constant.EXTRA_FROM_SPLASH,
            !intent.getBooleanExtra(Constant.EXTRA_FROM_SETTINGS, false))

    override fun provideViewBinding() = ActivityLanguageBinding.inflate(layoutInflater)

    override fun initViews() {
        selectedLanguageCode = restoredState?.getString(STATE_SELECTED_LANGUAGE)
        viewBaseActivity.rclLanguage.adapter = languageAdapter
        viewBaseActivity.toolBar.btnBack.isVisible = !isFromSplash
        viewBaseActivity.toolBar.btnAction.visibility = View.INVISIBLE
        viewBaseActivity.toolBar.tvTitle.setText(R.string.language)

        languageAdapter.onClick = { selected ->
            languageAdapter.selectLanguage(selected.languageCode)
            selectLanguageModel = languageAdapter.selectedLanguage()
            selectedLanguageCode = selectLanguageModel?.languageCode
            viewBaseActivity.toolBar.btnAction.isVisible = selectLanguageModel != null
        }
        viewBaseActivity.toolBar.btnBack.setOnClickWithPressEffect { onBack() }
        viewBaseActivity.toolBar.btnAction.setOnClickWithPressEffect {
            selectLanguageModel?.let { selected ->
                viewBaseActivity.toolBar.btnAction.isEnabled = false
                preferences.saveLanguage(selected)
                preferences.setLanguageChosen()
                LocaleHelper.setLocale(this, selected.languageCode)
                if (isFromSplash) {
                    startActivity(
                        Intent(this, OnboardingActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION),
                        ActivityOptions.makeCustomAnimation(this, 0, 0).toBundle()
                    )
                }
                window.setWindowAnimations(0)
                finish()
                disableTransitionAnimations()
            }
        }
//        ViewCompat.setOnApplyWindowInsetsListener(viewBaseActivity.root) { view, insets ->
//            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
//            insets
//        }
    }

    override fun initData() {
        viewModel.loadListLanguage()
    }

    override fun initObserver() {
        viewModel.listLanguage.onEach { languages ->
            languageAdapter.setData(ArrayList(languages.map { it.copy(selected = false) }))
            val code = selectedLanguageCode
                ?: if (!isFromSplash) preferences.getLanguage().languageCode else null
            code?.let { languageAdapter.selectLanguage(it) }
            selectLanguageModel = if (selectedLanguageCode != null) {
                languageAdapter.selectedLanguage()
            } else null
            viewBaseActivity.toolBar.btnAction.visibility =
                if (selectLanguageModel != null) View.VISIBLE else View.INVISIBLE
        }.flowWithLifecycle(lifecycle, Lifecycle.State.CREATED).launchIn(lifecycleScope)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_SELECTED_LANGUAGE, selectedLanguageCode)
        super.onSaveInstanceState(outState)
    }

    companion object {
        private const val STATE_SELECTED_LANGUAGE = "selected_language"

        fun start(context: Context, isFromSplash: Boolean) {
            context.startActivity(Intent(context, LanguageActivity::class.java)
                .putExtra(Constant.EXTRA_FROM_SPLASH, isFromSplash))
        }
    }
}
