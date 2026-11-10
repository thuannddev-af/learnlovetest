package com.example.afica.lovetest.component.splash

import android.Manifest
import android.app.ActivityOptions
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import com.example.afica.lovetest.R
import com.example.afica.lovetest.ads.InterAdsUtils
import com.example.afica.lovetest.ads.AppOpenAdsUtils
import com.example.afica.lovetest.ads.NativeAdsUtils
import com.example.afica.lovetest.ads.RemoteConfigManager
import com.example.afica.lovetest.base.activity.BaseActivity
import com.example.afica.lovetest.component.language.LanguageActivity
import com.example.afica.lovetest.component.onboarding.OnboardingActivity
import com.example.afica.lovetest.databinding.ActivitySplashBinding
import com.example.afica.lovetest.utils.Constant
import com.example.afica.lovetest.utils.SharedPreferenceUtils
import com.google.android.gms.ads.MobileAds

class SplashActivity : BaseActivity<ActivitySplashBinding>() {


    companion object {
        private const val ADS_TIMEOUT = 15000L
        private const val DOT_CYCLES = 4
        private const val DOT_STEP_DELAY = 500L
    }

    private val dotsHandler = Handler(Looper.getMainLooper())
    private var activeDotIndex = 0
    private var completedDotCycles = 0

    //ads
    private var splashFinished = false
    private var adsFinished = false
    private var hasNavigated = false
    private var isShowingAd = false
    private var adReady = false
    private var isResumed = false
    private val remoteConfigManager by lazy {
        RemoteConfigManager()
    }

    private val adsTimeoutRunnable = Runnable {
        if (!adsFinished && !isShowingAd) {
            Log.d("SplashAds", "Ads timeout")
            adsFinished = true
            adReady = false
            tryShowAds()
        }
    }
    private val advanceDot = object : Runnable {
        override fun run() {
            if (isFinishing || isDestroyed) return
            val dots = viewBaseActivity.splashDots
            if (activeDotIndex >= dots.childCount) {
                completedDotCycles++
                if (completedDotCycles >= DOT_CYCLES) {
                    splashFinished = true
                    Log.d("SplashAds", "Splash finished")
                    tryShowAds()
                    return
                }
                activeDotIndex = 0
            }
            for (index in 0 until dots.childCount) {
                dots.getChildAt(index).isSelected = index == activeDotIndex
            }
            activeDotIndex++
            dotsHandler.postDelayed(this, DOT_STEP_DELAY)
        }
    }

    override fun provideViewBinding(): ActivitySplashBinding =
        ActivitySplashBinding.inflate(layoutInflater)

    private fun openNextScreen() {
        val next = if (!SharedPreferenceUtils.getInstance(this).isLanguageChosen()) {
            Intent(this, LanguageActivity::class.java)
                .putExtra(Constant.EXTRA_FROM_SPLASH, true)
        } else {
            val hasCamera = ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
            Intent(
                this,
                if (hasCamera) OnboardingActivity::class.java else OnboardingActivity::class.java
            )
        }
        window.setWindowAnimations(0)
        disableTransitionAnimations()
        startActivity(
            next.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION),
            ActivityOptions.makeCustomAnimation(this, 0, 0).toBundle()
        )
        finish()
        disableTransitionAnimations()
    }


    override fun onStart() {
        super.onStart()
        if (!splashFinished) dotsHandler.post(advanceDot)
    }

    override fun onResume() {
        super.onResume()
        isResumed = true
        tryShowAds()
    }

    override fun onPause() {
        isResumed = false
        super.onPause()
    }

    override fun onStop() {
        dotsHandler.removeCallbacks(advanceDot)
        super.onStop()
    }

    override fun onDestroy() {
        dotsHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun initData() {
        viewBaseActivity.splashTitle.text = HtmlCompat.fromHtml(
            getString(R.string.splash),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )

        initAds()

    }

    private fun initAds() {
        dotsHandler.postDelayed(
            adsTimeoutRunnable,
            ADS_TIMEOUT
        )

        remoteConfigManager.init { success ->
            if (isFinishing || isDestroyed ||
                hasNavigated || adsFinished
            ) {
                return@init
            }

            val canShowInter = remoteConfigManager.canShowAd(
                RemoteConfigManager.INTER_SPLASH
            )
            val shouldPreloadLanguage =
                !SharedPreferenceUtils.getInstance(this).isLanguageChosen() &&
                        remoteConfigManager.canShowAd(RemoteConfigManager.NATIVE_LANGUAGE)
            if (!canShowInter && !shouldPreloadLanguage &&
                !remoteConfigManager.canShowAd(RemoteConfigManager.APP_OPEN_ENABLE)) {
                onAdsLoaded(false)
                return@init
            }

            MobileAds.initialize(applicationContext) {
                runOnUiThread {
                    AppOpenAdsUtils.onAdsInitialized(applicationContext)
                    if (isFinishing || isDestroyed ||
                        hasNavigated || adsFinished
                    ) {
                        return@runOnUiThread
                    }

                    if (shouldPreloadLanguage) {
                        NativeAdsUtils.preloadNative(applicationContext)
                    }
                    if (!canShowInter) {
                        onAdsLoaded(false)
                        return@runOnUiThread
                    }

                    InterAdsUtils.loadInter(this, InterAdsUtils.Placement.SPLASH) { loaded ->
                        if (!isFinishing && !isDestroyed &&
                            !hasNavigated && !adsFinished
                        ) {
                            onAdsLoaded(loaded)
                        }
                    }
                }
            }
        }
    }

    private fun onAdsLoaded(loaded: Boolean) {
        if (adsFinished || hasNavigated || isShowingAd) return

        adsFinished = true
        adReady = loaded

        dotsHandler.removeCallbacks(adsTimeoutRunnable)
        tryShowAds()
    }

    private fun tryShowAds() {

        if (!splashFinished ||
            !adsFinished ||
            !isResumed ||
            hasNavigated ||
            isShowingAd ||
            isFinishing ||
            isDestroyed
        ) {
            return
        }

        if (adReady) {
            isShowingAd = true
            adReady = false

            InterAdsUtils.showInter(this, InterAdsUtils.Placement.SPLASH) {

                isShowingAd = false
                navigateOnce()
            }
        } else {

            navigateOnce()
        }
    }

    private fun navigateOnce() {
        if (hasNavigated || isFinishing || isDestroyed) return

        hasNavigated = true
        dotsHandler.removeCallbacks(adsTimeoutRunnable)
        dotsHandler.removeCallbacks(advanceDot)

        openNextScreen()
    }
}

