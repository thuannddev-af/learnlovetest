package com.example.afica.lovetest.ads

import android.app.Activity
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import com.ads.admob.AdmobManager
import com.ads.admob.data.ContentAd
import com.ads.admob.helper.interstitial.InterstitialAdConfig
import com.ads.admob.helper.interstitial.InterstitialAdsHelper
import com.ads.admob.listener.InterstitialAdRequestCallBack
import com.ads.admob.listener.InterstitialAdShowCallBack
import com.example.afica.lovetest.BuildConfig
import com.example.afica.lovetest.utils.FirebaseConfigManager
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError
import kotlinx.coroutines.launch

object InterAdsUtils {
    enum class Placement(val adId: String, val placementName: String) {
        SPLASH(BuildConfig.inter_splash, "inter_splash"),
        INTER_SAVE(BuildConfig.inter_save, "inter_save"),
        INTER_DAY(BuildConfig.inter_day, "inter_day"),
        INTER_LOVE(BuildConfig.inter_love, "inter_love"),
        INTER_ADD(BuildConfig.inter_add, "inter_add")
    }

    private class Request(val helper: InterstitialAdsHelper) {
        var loading = true
        var showing = false
        val callbacks = mutableListOf<(Boolean) -> Unit>()
        var observer: DefaultLifecycleObserver? = null
        var releaseFullscreen: (() -> Unit)? = null
    }

    private val requests = mutableMapOf<Pair<FragmentActivity, Placement>, Request>()

    private fun isEnabled(placement: Placement): Boolean {
        val config = FirebaseConfigManager.instance().adConfig
        return if (placement == Placement.SPLASH) config.enableInterSplash else config.enableInterMain
    }

    fun cancelInter(activity: FragmentActivity, placement: Placement) {
        val request = requests.remove(activity to placement) ?: return
        request.callbacks.clear()
        request.releaseFullscreen?.invoke()
        request.observer?.let { activity.lifecycle.removeObserver(it) }
        request.helper.cancelRequestAndShowAllAds(true)
    }

    fun loadInter(
        activity: FragmentActivity,
        placement: Placement,
        onComplete: (Boolean) -> Unit = {}
    ) {
        if (activity.isFinishing || activity.isDestroyed) return
        if (!isEnabled(placement)) {
            cancelInter(activity, placement)
            onComplete(false)
            return
        }
        val key = activity to placement
        requests[key]?.let {
            if (it.loading) it.callbacks.add(onComplete)
            else onComplete(it.helper.interstitialAdValue != null)
            return
        }
        val helperKey = "${placement.placementName}_${System.identityHashCode(activity)}"
        val helper = InterstitialAdsHelper.getInstance(helperKey).apply {
            setInterstitialAdConfig(
                InterstitialAdConfig(
                    idAds = placement.adId,
                    canShowAds = isEnabled(placement),
                    canReloadAds = false,
                    adPlacement = placement.placementName,
                    reloadIfFirstFail = true
                )
            )
        }
        val request = Request(helper).apply { callbacks.add(onComplete) }
        requests[key] = request
        request.observer = object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                cancelInter(activity, placement)
            }
        }.also { activity.lifecycle.addObserver(it) }
        fun complete(loaded: Boolean) {
            if (requests[key] !== request) return
            request.loading = false
            val callbacks = request.callbacks.toList()
            request.callbacks.clear()
            if (!loaded) cancelInter(activity, placement)
            callbacks.forEach { it(loaded) }
        }
        helper.requestInterAds(activity, false, object : InterstitialAdRequestCallBack {
            override fun onAdLoaded(data: ContentAd) {
                Log.d("InterAds", "${placement.placementName} loaded")
                complete(true)
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                Log.e("InterAds", "${placement.placementName} load failed: ${loadAdError.message}")
                complete(false)
            }
        })
    }
    fun showInter(activity: FragmentActivity, placement: Placement, onComplete: () -> Unit) {
        if (activity.isFinishing || activity.isDestroyed) return
        if (!isEnabled(placement)) {
            cancelInter(activity, placement)
            onComplete()
            return
        }
        loadInter(activity, placement) { loaded ->
            val request = requests[activity to placement]
            if (!loaded || request == null) {
                onComplete()
                return@loadInter
            }
            if (request.showing) return@loadInter
            request.showing = true
            activity.lifecycleScope.launch {
                activity.lifecycle.withResumed {
                    if (requests[activity to placement] !== request) return@withResumed
                    val token = Any()
                    if (!isEnabled(placement) || AdmobManager.isShowAdsFullScreen || !FullscreenAdGate.acquire(
                            token
                        )
                    ) {
                        request.showing = false
                        onComplete()
                        return@withResumed
                    }
                    request.releaseFullscreen = {
                        FullscreenAdGate.release(token)
                        AdmobManager.adsFullScreenDismiss()
                    }
                    var completed = false
                    fun finish() {
                        if (completed) return
                        completed = true
                        cancelInter(activity, placement)
                        if (!activity.isFinishing && !activity.isDestroyed) onComplete()
                    }
                    try {
                        request.helper.forceShowInterstitial(
                            activity,
                            activity,
                            object : InterstitialAdShowCallBack {
                                override fun onNextAction() = finish()
                                override fun onAdClose() = finish()
                                override fun onInterstitialShow() {
                                    Log.d("InterAds", "${placement.placementName} shown")
                                }

                                override fun onAdClicked() = Unit
                                override fun onAdImpression() = Unit
                                override fun onAdFailedToShow(adError: AdError) {
                                    Log.e("InterAds", adError.message)
                                    finish()
                                }
                            })
                    } catch (error: RuntimeException) {
                        Log.e("InterAds", "Cannot show ${placement.placementName}", error)
                        finish()
                    }
                }
            }
        }
    }



    //////////// ads theo module
    fun loadInterAds(
        activity: Activity,
        idAds: String,
        adPlacement: String,
        isEnable: Boolean,
        onAdLoaded: () -> Unit = {},
        onAdLoadFail: () -> Unit = {}
    ) {
        val helper = InterstitialAdsHelper.getInstance(adPlacement)

        helper.setInterstitialAdConfig(
            InterstitialAdConfig(
                idAds = idAds,
                canShowAds = isEnable,
                canReloadAds = false,
                adPlacement = adPlacement,
                reloadIfFirstFail = true
            )
        )

        helper.requestInterAds(
            activity,
            false,
            object : InterstitialAdRequestCallBack {
                override fun onAdLoaded(data: ContentAd) {
                    onAdLoaded()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    onAdLoadFail()
                }
            }
        )
    }

    fun showInterAds(
        activity: Activity,
        lifecycleOwner: LifecycleOwner,
        adPlacement: String,
        action: () -> Unit
    ) {
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.lifecycle.withResumed {
                val token = Any()

                if (AdmobManager.isShowAdsFullScreen ||
                    !FullscreenAdGate.acquire(token)
                ) {
                    action()
                    return@withResumed
                }

                var completed = false

                fun finish() {
                    if (completed) return
                    completed = true
                    FullscreenAdGate.release(token)

                    if (!activity.isFinishing && !activity.isDestroyed) {
                        action()
                    }
                }

                InterstitialAdsHelper.getInstance(adPlacement)
                    .forceShowInterstitial(
                        activity,
                        lifecycleOwner,
                        object : InterstitialAdShowCallBack {
                            override fun onNextAction() = finish()

                            override fun onAdClose() = finish()

                            override fun onAdFailedToShow(
                                adError: AdError
                            ) = finish()

                            override fun onInterstitialShow() {}
                            override fun onAdClicked() {}
                            override fun onAdImpression() {}
                        }
                    )
            }
        }
    }

    fun loadandShowInterAds(
        activity: Activity,
        lifecycleOwner: LifecycleOwner,
        idAds: String,
        adPlacement: String,
        isEnable: Boolean,
        action: () -> Unit
    ) {
        if(activity.isFinishing || activity.isDestroyed) return

        if(!isEnable) {
            action()
            return
        }
        loadInterAds(
            activity,
            idAds,
            adPlacement,
            isEnable,
            onAdLoaded = {
                if(!activity.isFinishing && !activity.isDestroyed && lifecycleOwner.lifecycle.currentState != Lifecycle.State.DESTROYED) {
                    showInterAds(
                        activity,
                        lifecycleOwner,
                        adPlacement,
                        action
                    )
                }
            },
            onAdLoadFail = {
                if(!activity.isFinishing && !activity.isDestroyed && lifecycleOwner.lifecycle.currentState != Lifecycle.State.DESTROYED) {
                    action()
                }
            }
        )
    }
    fun cancelInter(adPlacement: String) {
        InterstitialAdsHelper.getInstance(adPlacement).cancelRequestAndShowAllAds(true)
    }
}
