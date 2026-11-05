package com.example.afica.lovetest.component.splash

import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan

import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.afica.lovetest.R
import com.example.afica.lovetest.base.activity.BaseActivity
import com.example.afica.lovetest.component.language.LanguageActivity
import com.example.afica.lovetest.databinding.ActivitySplashBinding

class SplashActivity : BaseActivity<ActivitySplashBinding>() {
    private val dotsHandler = Handler(Looper.getMainLooper())
    private var activeDotIndex = 0
    private val advanceDot = object : Runnable {
        override fun run() {
            if (isFinishing || isDestroyed) return
            val dots = viewBaseActivity.splashDots
            if (activeDotIndex >= dots.childCount) {
                LanguageActivity.start(this@SplashActivity, isFromSplash = true)
                finish()
                return
            }
            for (index in 0 until dots.childCount) {
                dots.getChildAt(index).isSelected = index == activeDotIndex
            }
            activeDotIndex++
            dotsHandler.postDelayed(this, 350L)
        }
    }

    override fun provideViewBinding(): ActivitySplashBinding = ActivitySplashBinding.inflate(layoutInflater)

    override fun initViews() {
        ViewCompat.setOnApplyWindowInsetsListener(viewBaseActivity.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onStart() {
        super.onStart()
        activeDotIndex = 0
        dotsHandler.post(advanceDot)
    }

    override fun onStop() {
        dotsHandler.removeCallbacks(advanceDot)
        super.onStop()
    }

    override fun initData() {
        viewBaseActivity.splashTitle.text = HtmlCompat.fromHtml(
            getString(R.string.splash),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
    }

}

