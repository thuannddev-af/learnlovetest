package com.example.afica.lovetest.ads

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Lifecycle
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object InterAdsUtils {
    enum class Placement(val adId: String, val configKey: String) {
        SPLASH(AdsConfig.INTERSTITIAL_SPLASH_ID, RemoteConfigManager.INTERSTITIAL_SPLASH_ENABLE),
        DEFAULT(AdsConfig.INTERSTITIAL_ID, RemoteConfigManager.INTER_ADS)
    }

    private class Request {
        var ad: InterstitialAd? = null
        val callbacks = mutableListOf<(Boolean) -> Unit>()
        var loading = true
    }

    private val requests = mutableMapOf<Pair<FragmentActivity, Placement>, Request>()
    private val remoteConfig by lazy { RemoteConfigManager() }

    /** Preload when a screen has its own waiting animation, such as splash. */
    fun loadInter(
        activity: FragmentActivity,
        placement: Placement = Placement.DEFAULT,
        onComplete: (Boolean) -> Unit = {}
    ) {
        if (activity.isFinishing || activity.isDestroyed) return
        if (!remoteConfig.canShowAd(placement.configKey)) {
            onComplete(false)
            return
        }
        val key = activity to placement
        requests[key]?.let { request ->
            if (request.loading) request.callbacks.add(onComplete)
            else onComplete(request.ad != null)
            return
        }
        val request = Request().apply { callbacks.add(onComplete) }
        requests[key] = request
        activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                requests.remove(key)?.callbacks?.clear()
                activity.lifecycle.removeObserver(this)
            }
        })
        fun complete(ad: InterstitialAd?) {
            if (requests[key] !== request) return
            request.ad = ad
            request.loading = false
            val callbacks = request.callbacks.toList()
            request.callbacks.clear()
            if (ad == null) requests.remove(key)
            callbacks.forEach { it(ad != null) }
        }
        InterstitialAd.load(activity.applicationContext, placement.adId,
            AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) = complete(ad)
                override fun onAdFailedToLoad(error: LoadAdError) = complete(null)
            })
    }

    fun showInter(
        activity: FragmentActivity,
        placement: Placement = Placement.DEFAULT,
        onComplete: () -> Unit
    ) {
        if (activity.isFinishing || activity.isDestroyed) return
        if (!remoteConfig.canShowAd(placement.configKey)) {
            requests.remove(activity to placement)
            onComplete()
            return
        }
        loadInter(activity, placement) { loaded ->
            val ad = requests.remove(activity to placement)?.ad
            if (!loaded || ad == null ||
                !activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                onComplete()
                return@loadInter
            }
            var completed = false
            fun finish() {
                if (completed) return
                completed = true
                ad.fullScreenContentCallback = null
                if (!activity.isFinishing && !activity.isDestroyed) onComplete()
            }
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() = finish()
                override fun onAdFailedToShowFullScreenContent(error: AdError) = finish()
            }
            ad.show(activity)
        }
    }
}
