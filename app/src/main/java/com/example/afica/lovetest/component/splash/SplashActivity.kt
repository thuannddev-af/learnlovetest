package com.example.afica.lovetest.component.splash

import android.app.ActivityOptions
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import androidx.core.text.HtmlCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.withResumed
import com.ads.admob.data.ContentAd
import com.ads.admob.helper.interstitial.InterstitialAdSplashConfig
import com.ads.admob.helper.interstitial.InterstitialAdSplashHelper
import com.ads.admob.helper.interstitial.params.InterstitialAdParam
import com.ads.admob.listener.InterstitialAdCallback
import com.example.afica.lovetest.App
import com.example.afica.lovetest.BuildConfig
import com.example.afica.lovetest.R
import com.example.afica.lovetest.ads.NativeAdsUtils
import com.example.afica.lovetest.base.activity.BaseActivity
import com.example.afica.lovetest.component.language.LanguageActivity
import com.example.afica.lovetest.component.onboarding.OnboardingActivity
import com.example.afica.lovetest.databinding.ActivitySplashBinding
import com.example.afica.lovetest.utils.Constant
import com.example.afica.lovetest.utils.FirebaseConfigManager
import com.example.afica.lovetest.utils.SpManager
import com.example.afica.lovetest.utils.Utils
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : BaseActivity<ActivitySplashBinding>() {
    private var dotsFinished = false
    private var nextScreenPending = false
    private var hasNavigated = false
    private var networkDialog: AlertDialog? = null
    private val interSplashLazy = lazy { initInterAdSplash() }
    private val interAdSplashHelper by interSplashLazy

    override fun provideViewBinding() = ActivitySplashBinding.inflate(layoutInflater)

    override fun initData() {
        viewBaseActivity.splashTitle.text = HtmlCompat.fromHtml(
            getString(R.string.splash), HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        startDotAnimation()
        checkConnection()
    }

    private fun checkConnection() {
        if (isFinishing || isDestroyed) return
        if (Utils.isConnected(this)) {
            (application as App).initConsentManager(this) {
                FirebaseConfigManager.instance().doAfterFetch {
                    if (!hasNavigated && !isFinishing && !isDestroyed) {
                        loadInterSplash()
                        loadLanguageNativeAd()
                    }
                }
            }
        } else {
            networkDialog = AlertDialog.Builder(this)
                .setTitle(R.string.network_error)
                .setMessage(R.string.network_error_message)
                .setCancelable(false)
                .setPositiveButton(R.string.try_again) { _, _ -> checkConnection() }
                .setNegativeButton(R.string.exit) { _, _ -> finish() }
                .show()
        }
    }

    private fun loadInterSplash() {
        interAdSplashHelper.holdShow = !dotsFinished
        interAdSplashHelper.requestAds(InterstitialAdParam.Request)
    }

    private fun initInterAdSplash(): InterstitialAdSplashHelper {
        val config = InterstitialAdSplashConfig(
            idAds = BuildConfig.inter_splash,
            canShowAds = FirebaseConfigManager.instance().adConfig.enableInterSplash,
            canReloadAds = false,
            timeDelay = 0L,
            timeOut = 15000L,
            showReady = true,
            adPlacement = "inter_splash",
            reloadIfFirstFail = true,
            loadingDelayMs = 0L
        )
        return InterstitialAdSplashHelper(this, this, config).apply {
            registerAdListener(interAdCallBack)
        }
    }

    private val interAdCallBack = object : InterstitialAdCallback {
        override fun onNextAction() { completeSplash() }
        override fun onAdClose() { completeSplash() }
        override fun onInterstitialShow() = Unit
        override fun onAdLoaded(data: ContentAd) = Unit
        override fun onAdFailedToLoad(loadAdError: LoadAdError) = Unit
        override fun onAdClicked() = Unit
        override fun onAdImpression() = Unit
        override fun onAdFailedToShow(adError: AdError) { completeSplash() }
    }

    private fun loadLanguageNativeAd() {
        if (FirebaseConfigManager.instance().adConfig.enableNativeLanguage &&
            !SpManager.getInstance(this).isLanguageChosen()) {
            NativeAdsUtils.preloadNative(applicationContext)
        }
    }

    // Luồng riêng của app: dot chạy 1 -> 2 -> 3 -> 4, lặp đủ 4 lượt.
    // Helper tải inter với holdShow = true; khi dot chạy xong mới cho phép hiện ads.
    private fun startDotAnimation() {
        lifecycleScope.launch {
            var step = 0
            val dots = viewBaseActivity.splashDots
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (step < dots.childCount * 4) {
                    for (index in 0 until dots.childCount) {
                        dots.getChildAt(index).isSelected = index == step % dots.childCount
                    }
                    delay(500L)
                    step++
                }
                if (!dotsFinished) {
                    dotsFinished = true
                    if (nextScreenPending) goToMainScreen()
                    else if (interSplashLazy.isInitialized()) interAdSplashHelper.releaseHoldShow()
                }
            }
        }
    }

    private fun completeSplash() {
        nextScreenPending = true
        lifecycleScope.launch { lifecycle.withResumed { goToMainScreen() } }
    }

    private fun goToMainScreen() {
        if (!dotsFinished || !nextScreenPending || hasNavigated || isFinishing || isDestroyed) return
        hasNavigated = true
        val next = if (!SpManager.getInstance(this).isLanguageChosen()) {
            Intent(this, LanguageActivity::class.java).putExtra(Constant.EXTRA_FROM_SPLASH, true)
        } else {
            Intent(this, OnboardingActivity::class.java)
        }
        window.setWindowAnimations(0)
        disableTransitionAnimations()
        startActivity(next.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION),
            ActivityOptions.makeCustomAnimation(this, 0, 0).toBundle())
        finish()
        disableTransitionAnimations()
    }

    override fun onDestroy() {
        networkDialog?.dismiss()
        if (interSplashLazy.isInitialized()) {
            interAdSplashHelper.unregisterAllAdListener()
            interAdSplashHelper.cancel()
        }
        super.onDestroy()
    }
}
