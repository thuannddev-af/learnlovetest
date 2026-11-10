package com.example.afica.lovetest.ads

import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnPreDraw
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.launch
import com.example.afica.lovetest.base.activity.BaseActivity
import com.example.afica.lovetest.component.lovetest.HoroscopeTestActivity
import com.example.afica.lovetest.component.lovetest.NameTestActivity
import com.example.afica.lovetest.component.lovetest.NameTestResultActivity
import com.example.afica.lovetest.component.main.MainActivity
import com.example.afica.lovetest.component.main.memory.MemoryActivity
import com.example.afica.lovetest.component.startdate.StartDateActivity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd

/** Resume-only ads. Cold starts and utility/system flows never trigger a show. */
object AppOpenAdsUtils : Application.ActivityLifecycleCallbacks {
    private var initialized = false
    private var sdkReady = false
    private var ad: AppOpenAd? = null
    private var loadedAt = 0L
    private var loading = false
    private val resumePolicy = AppResumeAdPolicy()
    private val remoteConfig by lazy { RemoteConfigManager() }

    fun initialize(application: Application) {
        if (initialized) return
        initialized = true
        application.registerActivityLifecycleCallbacks(this)
    }

    /** Call after MobileAds initialization and Remote Config fetch. */
    fun onAdsInitialized(context: Context) {
        sdkReady = true
        preloadAppOpen(context)
    }

    /** Call before launching permission, picker, camera, sharing, or settings UI. */
    fun skipNextResume() {
        resumePolicy.skipNextResume()
    }

    fun preloadAppOpen(context: Context) {
        if (!sdkReady || loading || !remoteConfig.canShowAd(RemoteConfigManager.APP_OPEN_ENABLE)) return
        if (ad != null && SystemClock.elapsedRealtime() - loadedAt < 4 * 60 * 60 * 1000L) return
        ad = null
        loading = true
        AppOpenAd.load(
            context.applicationContext, AdsConfig.APP_OPEN_ID, AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(loadedAd: AppOpenAd) {
                    loading = false
                    ad = loadedAd
                    loadedAt = SystemClock.elapsedRealtime()
                    // Do not show from a load callback: the resume opportunity may already be over.
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loading = false
                }
            })
    }

    private fun isContent(activity: Activity): Boolean = activity is MainActivity ||
            activity is MemoryActivity || activity is StartDateActivity ||
            activity is NameTestActivity || activity is HoroscopeTestActivity ||
            activity is NameTestResultActivity

    /** Explicit opt-in for a chosen screen. Does not change automatic resume eligibility. */
    fun showAppOpenIfAvailable(activity: FragmentActivity) {
        activity.lifecycleScope.launch {
            activity.lifecycle.withResumed {
                showAppOpen(activity, requireContent = false)
            }
        }
    }

    private fun showAppOpen(activity: Activity, requireContent: Boolean = true) {
        if (!sdkReady || (requireContent && !isContent(activity)) || activity.isFinishing || activity.isDestroyed ||
            !remoteConfig.canShowAd(RemoteConfigManager.APP_OPEN_ENABLE)
        ) return
        val readyAd = ad
        if (readyAd == null || SystemClock.elapsedRealtime() - loadedAt >= 4 * 60 * 60 * 1000L) {
            ad = null
            preloadAppOpen(activity)
            return
        }
        val token = Any()
        if (!FullscreenAdGate.acquire(token)) return
        ad = null
        val decor = activity.window.decorView as ViewGroup
        val whiteBackground = View(activity).apply {
            setBackgroundColor(Color.WHITE)
            isClickable = true
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        decor.addView(
            whiteBackground, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        val owner = activity as LifecycleOwner
        var destroyObserver: DefaultLifecycleObserver? = null
        var finished = false
        fun finish() {
            if (finished) return
            finished = true
            decor.removeView(whiteBackground)
            destroyObserver?.let { owner.lifecycle.removeObserver(it) }
            FullscreenAdGate.release(token)
            readyAd.fullScreenContentCallback = null
            preloadAppOpen(activity.applicationContext)
        }
        destroyObserver = object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) = finish()
        }.also { owner.lifecycle.addObserver(it) }
        readyAd.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() = finish()
            override fun onAdFailedToShowFullScreenContent(error: AdError) = finish()
        }
        // Draw the white app background before the SDK opens its fullscreen window.
        decor.doOnPreDraw {
            decor.post {
                if (!finished) {
                    if (activity.isFinishing || activity.isDestroyed ||
                        !owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) finish()
                    else try {
                        readyAd.show(activity)
                    } catch (_: RuntimeException) {
                        finish()
                    }
                }
            }
        }
    }

    override fun onActivityStarted(activity: Activity) {
        if (activity !is BaseActivity<*>) return
        resumePolicy.onStarted(FullscreenAdGate.isBusy)
    }

    override fun onActivityResumed(activity: Activity) {
        if (activity !is BaseActivity<*>) return
        val shouldShow = resumePolicy.onResumed(isContent(activity), FullscreenAdGate.isBusy)
        if (shouldShow) showAppOpen(activity)
        else if (isContent(activity)) preloadAppOpen(activity)
    }

    override fun onActivityStopped(activity: Activity) {
        if (activity !is BaseActivity<*>) return
        resumePolicy.onStopped(
            isContent(activity), activity.isFinishing,
            activity.isChangingConfigurations, FullscreenAdGate.isBusy
        )
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
