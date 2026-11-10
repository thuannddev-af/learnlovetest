package com.example.afica.lovetest.component.splash

import android.app.ActivityOptions
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.text.HtmlCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import com.ads.admob.AdmobManager
import com.ads.admob.data.ContentAd
import com.ads.admob.helper.interstitial.InterstitialAdSplashConfig
import com.ads.admob.helper.interstitial.InterstitialAdSplashHelper
import com.ads.admob.helper.interstitial.params.InterstitialAdParam
import com.ads.admob.listener.InterstitialAdCallback
import com.example.afica.lovetest.App
import com.example.afica.lovetest.BuildConfig
import com.example.afica.lovetest.R
import com.example.afica.lovetest.ads.FullscreenAdGate
import com.example.afica.lovetest.ads.NativeAdsUtils
import com.example.afica.lovetest.base.activity.BaseActivity
import com.example.afica.lovetest.component.language.LanguageActivity
import com.example.afica.lovetest.component.onboarding.OnboardingActivity
import com.example.afica.lovetest.databinding.ActivitySplashBinding
import com.example.afica.lovetest.utils.Constant
import com.example.afica.lovetest.utils.FirebaseConfigManager
import com.example.afica.lovetest.utils.SharedPreferenceUtils
import com.example.afica.lovetest.utils.SpManager
import com.example.afica.lovetest.utils.Utils
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError
import kotlinx.coroutines.launch

class SplashActivity : BaseActivity<ActivitySplashBinding>() {
    companion object {
        private const val ADS_TIMEOUT = 15000L
        private const val DOT_CYCLES = 4
        private const val DOT_STEP_DELAY = 500L
    }

    private val handler = Handler(Looper.getMainLooper())
    private var activeDotIndex = 0
    private var completedDotCycles = 0
    private var splashFinished = false
    private var adsFinished = false
    private var hasNavigated = false
    private var isShowingAd = false
    private var adReady = false
    private var isResumed = false
    private var initializingAds = false
    private var interSplashHelper: InterstitialAdSplashHelper? = null
    private var fullscreenToken: Any? = null
    private var networkDialog: AlertDialog? = null

    private val adsTimeout = Runnable {
        if (!hasNavigated && !adsFinished && !isShowingAd) {
            Log.d("SplashAds", "Splash config/inter timeout")
            interSplashHelper?.cancel()
            onAdsLoaded(false)
        }
    }
    private val advanceDot = object : Runnable {
        override fun run() {
            if (isFinishing || isDestroyed || splashFinished) return
            val dots = viewBaseActivity.splashDots
            if (activeDotIndex >= dots.childCount) {
                completedDotCycles++
                if (completedDotCycles >= DOT_CYCLES) {
                    splashFinished = true
                    tryShowAds()
                    return
                }
                activeDotIndex = 0
            }
            for (index in 0 until dots.childCount) dots.getChildAt(index).isSelected = index == activeDotIndex
            activeDotIndex++
            handler.postDelayed(this, DOT_STEP_DELAY)
        }
    }

    override fun provideViewBinding() = ActivitySplashBinding.inflate(layoutInflater)

    override fun initData() {
        viewBaseActivity.splashTitle.text = HtmlCompat.fromHtml(getString(R.string.splash), HtmlCompat.FROM_HTML_MODE_LEGACY)
        checkConnection()
    }

    private fun checkConnection() {
        if (isFinishing || isDestroyed) return
        if (Utils.isConnected(this)) {
            val app = application as? App
            if (app == null) {
                Log.e("SplashAds", "Application must use the same App class declared in the manifest")
                onAdsLoaded(false)
                return
            }
            app.initConsentManager(this) { runOnUiThread { initAds() } }
        } else if (networkDialog?.isShowing != true) {
            networkDialog = AlertDialog.Builder(this)
                .setTitle(R.string.network_error)
                .setMessage(R.string.network_error_message)
                .setCancelable(false)
                .setPositiveButton(R.string.try_again) { _, _ -> checkConnection() }
                .setNegativeButton(R.string.exit) { _, _ -> finish() }
                .create().also { it.show() }
        }
    }

    private fun initAds() {
        if (initializingAds || hasNavigated || isFinishing || isDestroyed) return
        initializingAds = true
        handler.postDelayed(adsTimeout, ADS_TIMEOUT)
        // App starts the fetch. Continue once the new JSON AdConfig is available.
        FirebaseConfigManager.instance().doAfterFetch {
            runOnUiThread {
                if (hasNavigated || adsFinished || isFinishing || isDestroyed) return@runOnUiThread
                val config = FirebaseConfigManager.instance().adConfig
                if (config.enableNativeLanguage && !SpManager.getInstance(this).isLanguageChosen()) {
                    NativeAdsUtils.preloadNative(applicationContext)
                }
                if (config.enableInterSplash) loadInterSplash() else onAdsLoaded(false)
            }
        }
    }

    private fun loadInterSplash() {
        val helper = InterstitialAdSplashHelper(this, this, InterstitialAdSplashConfig(
            idAds = BuildConfig.inter_splash,
            canShowAds = FirebaseConfigManager.instance().adConfig.enableInterSplash,
            canReloadAds = false,
            adPlacement = "inter_splash",
            timeDelay = 0L,
            timeOut = ADS_TIMEOUT,
            showReady = true,
            reloadIfFirstFail = true,
            loadingDelayMs = 0L
        ))
        interSplashHelper = helper
        helper.holdShow = true
        helper.registerAdListener(object : InterstitialAdCallback {
            override fun onAdLoaded(data: ContentAd) { onAdsLoaded(true) }
            override fun onNextAction() {
                if (isShowingAd) navigateOnce() else onAdsLoaded(false)
            }
            override fun onAdClose() { navigateOnce() }
            override fun onInterstitialShow() { Log.d("SplashAds", "Inter splash shown") }
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                Log.e("SplashAds", "Inter load failed: ${loadAdError.message}")
                onAdsLoaded(false)
            }
            override fun onAdFailedToShow(adError: AdError) {
                Log.e("SplashAds", "Inter show failed: ${adError.message}")
                navigateOnce()
            }
            override fun onAdClicked() = Unit
            override fun onAdImpression() = Unit
        })
        helper.requestAds(InterstitialAdParam.Request)
    }

    private fun onAdsLoaded(loaded: Boolean) {
        if (adsFinished || hasNavigated || isShowingAd || isFinishing || isDestroyed) return
        adsFinished = true
        adReady = loaded
        handler.removeCallbacks(adsTimeout)
        Log.d("SplashAds", "Inter ready=$loaded, dots complete=$splashFinished")
        tryShowAds()
    }

    private fun tryShowAds() {
        if (!splashFinished || !adsFinished || !isResumed || hasNavigated || isShowingAd ||
            isFinishing || isDestroyed) return
        if (!adReady) { navigateOnce(); return }
        isShowingAd = true
        adReady = false
        lifecycleScope.launch {
            lifecycle.withResumed {
                if (hasNavigated) return@withResumed
                val helper = interSplashHelper
                val token = Any()
                if (helper == null || AdmobManager.isShowAdsFullScreen || !FullscreenAdGate.acquire(token)) {
                    navigateOnce()
                    return@withResumed
                }
                fullscreenToken = token
                helper.releaseHoldShow()
            }
        }
    }

    private fun releaseFullscreen() {
        fullscreenToken?.let {
            FullscreenAdGate.release(it)
            AdmobManager.adsFullScreenDismiss()
        }
        fullscreenToken = null
    }

    private fun navigateOnce() {
        if (hasNavigated || isFinishing || isDestroyed) return
        hasNavigated = true
        releaseFullscreen()
        handler.removeCallbacksAndMessages(null)
        interSplashHelper?.unregisterAllAdListener()
        interSplashHelper?.cancel()
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

    override fun onStart() {
        super.onStart()
        if (!splashFinished) { handler.removeCallbacks(advanceDot); handler.post(advanceDot) }
    }
    override fun onResume() { super.onResume(); isResumed = true; tryShowAds() }
    override fun onPause() { isResumed = false; super.onPause() }
    override fun onStop() { handler.removeCallbacks(advanceDot); super.onStop() }
    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        networkDialog?.dismiss()
        interSplashHelper?.unregisterAllAdListener()
        interSplashHelper?.cancel()
        releaseFullscreen()
        super.onDestroy()
    }
}
