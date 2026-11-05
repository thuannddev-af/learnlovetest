package com.example.afica.lovetest.component.startdate

import com.example.afica.lovetest.utils.Constant

import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan

import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.afica.lovetest.R
import com.example.afica.lovetest.base.activity.BaseActivity
import com.example.afica.lovetest.component.dialog.DatePickerDialog
import com.example.afica.lovetest.databinding.ActivityStartDateBinding
import com.example.afica.lovetest.utils.SharedPreferenceUtils
import java.text.SimpleDateFormat
import java.util.Locale

class StartDateActivity : BaseActivity<ActivityStartDateBinding>() {
    private val preferences by lazy { SharedPreferenceUtils(this, "home") }
    private var selectedDate = 0L

    override fun provideViewBinding(): ActivityStartDateBinding = ActivityStartDateBinding.inflate(layoutInflater)

    override fun initViews() {

        val title = getString(R.string.start_date_title)
        viewBaseActivity.tvTitle.text = SpannableString(title).apply {
            val start = title.indexOf(' ') + 1
            setSpan(ForegroundColorSpan(ContextCompat.getColor(this@StartDateActivity, R.color.splash_accent)),
                start, title.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        WindowInsetsControllerCompat(window, viewBaseActivity.root).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        ViewCompat.setOnApplyWindowInsetsListener(viewBaseActivity.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        viewBaseActivity.btnBack.setOnClickListener { finish() }
        viewBaseActivity.btnDateOfLove.setOnClickListener {
            if (supportFragmentManager.findFragmentByTag(DATE_RESULT) == null) {
                DatePickerDialog.newInstance(selectedDate, DATE_RESULT).show(supportFragmentManager, DATE_RESULT)
            }
        }
        viewBaseActivity.btnDone.setOnClickListener {
            preferences.putLong(Constant.KEY_SP_START_DATE, selectedDate)
            finish()
        }
    }

    private fun renderDate() {
        viewBaseActivity.tvSelectedDate.text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedDate)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putLong(Constant.SELECTED_DATE, selectedDate)
        super.onSaveInstanceState(outState)
    }

    companion object {
        private const val DATE_RESULT = "start_date_result"
    }

    override fun initData() {
        selectedDate = restoredState?.getLong(Constant.SELECTED_DATE)
            ?: preferences.getLong(Constant.KEY_SP_START_DATE, System.currentTimeMillis())
        renderDate()
    }

    override fun initObserver() {
        supportFragmentManager.setFragmentResultListener(DATE_RESULT, this) { _, result ->
            selectedDate = result.getLong(Constant.SELECTED_DATE)
            renderDate()
        }
    }

}

