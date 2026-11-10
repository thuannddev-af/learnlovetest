package com.example.afica.lovetest.ads

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object RewardAdsUtils {
    private class Config(val idAds: String, val isEnable: Boolean) {
        var ad: RewardedAd? = null
        var loadedAt = 0L
        var loading = false
        var showing = false
        val callbacks = mutableListOf<Pair<Boolean, () -> Unit>>()
    }

    private val placements = mutableMapOf<String, Config>()
    private var cancelled = false

    fun loadRewardAds(
        activity: Activity,
        idAds: String,
        adPlacement: String,
        isEnable: Boolean,
        isLoadAndShow: Boolean = false,
        onAdLoaded: (() -> Unit)? = null
    ) {
        val previous = placements[adPlacement]
        if (previous == null || previous.idAds != idAds || previous.isEnable != isEnable) {
            previous?.callbacks?.clear()
            placements[adPlacement] = Config(idAds, isEnable)
        }
        loadAds(activity.applicationContext, adPlacement, isLoadAndShow, onAdLoaded)
    }

    fun showRewardAds(
        activity: Activity,
        lifecycleOwner: LifecycleOwner,
        adPlacement: String,
        isReloadWhenClose: Boolean = false,
        onRewardEarned: ((RewardItem) -> Unit)? = null,
        action: () -> Unit
    ) {
        val config = placements[adPlacement]
        if (activity.isFinishing || activity.isDestroyed ||
            lifecycleOwner.lifecycle.currentState == Lifecycle.State.DESTROYED) return
        if (config?.showing == true) return
        val ad = config?.ad
        if (cancelled || config == null || !config.isEnable || ad == null ||
            SystemClock.elapsedRealtime() - config.loadedAt >= 55 * 60 * 1000L ||
            !lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            config?.ad = null
            action()
            return
        }
        val fullscreenToken = Any()
        if (!FullscreenAdGate.acquire(fullscreenToken)) {
            action()
            return
        }
        config.ad = null
        config.showing = true
        var finished = false
        var rewarded = false
        fun finish() {
            if (finished) return
            finished = true
            FullscreenAdGate.release(fullscreenToken)
            config.showing = false
            ad.fullScreenContentCallback = null
            if (cancelled || placements[adPlacement] !== config || activity.isFinishing ||
                activity.isDestroyed || lifecycleOwner.lifecycle.currentState == Lifecycle.State.DESTROYED) return
            checkReload(activity.applicationContext, adPlacement, isReloadWhenClose)
            action()
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() = finish()
            override fun onAdFailedToShowFullScreenContent(error: AdError) = finish()
        }
        try {
        ad.show(activity) { reward ->
            if (!rewarded && !finished && !cancelled && placements[adPlacement] === config &&
                !activity.isFinishing && !activity.isDestroyed &&
                lifecycleOwner.lifecycle.currentState != Lifecycle.State.DESTROYED) {
                rewarded = true
                onRewardEarned?.invoke(reward)
            }
        }
        } catch (_: RuntimeException) { finish() }
    }

    fun loadAndShowRewardAds(
        activity: Activity,
        lifecycleOwner: LifecycleOwner,
        idAds: String,
        adPlacement: String,
        isEnable: Boolean,
        onRewardEarned: ((RewardItem) -> Unit)? = null,
        action: () -> Unit
    ) {
        if (cancelled || !isEnable) {
            if (!activity.isFinishing && !activity.isDestroyed &&
                lifecycleOwner.lifecycle.currentState != Lifecycle.State.DESTROYED) action()
            return
        }
        loadRewardAds(activity, idAds, adPlacement, isEnable, true, onAdLoaded = {
            showRewardAds(activity, lifecycleOwner, adPlacement,
                onRewardEarned = onRewardEarned, action = action)
        })
    }

    private fun loadAds(
        context: Context,
        adPlacement: String,
        isLoadAndShow: Boolean = false,
        onAdLoaded: (() -> Unit)? = null
    ) {
        val config = placements[adPlacement] ?: return
        if (cancelled || !config.isEnable) return
        if (config.ad != null && SystemClock.elapsedRealtime() - config.loadedAt < 55 * 60 * 1000L) {
            onAdLoaded?.invoke()
            return
        }
        config.ad = null
        onAdLoaded?.let { config.callbacks.add(isLoadAndShow to it) }
        if (config.loading || config.showing) return
        config.loading = true
        fun request(retry: Boolean) {
            RewardedAd.load(context.applicationContext, config.idAds, AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        if (cancelled || placements[adPlacement] !== config) return
                        config.ad = ad
                        config.loadedAt = SystemClock.elapsedRealtime()
                        complete()
                    }
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        if (cancelled || placements[adPlacement] !== config) return
                        if (retry) request(false) else complete()
                    }
                    private fun complete() {
                        config.loading = false
                        val callbacks = config.callbacks.toList()
                        config.callbacks.clear()
                        // Load-and-show also continues on load failure instead of leaving the UI waiting.
                        callbacks.forEach { (continueOnFailure, callback) ->
                            if (config.ad != null || continueOnFailure) callback()
                        }
                    }
                })
        }
        request(true)
    }

    private fun checkReload(context: Context, adPlacement: String, isReloadWhenClose: Boolean) {
        if (isReloadWhenClose) loadAds(context, adPlacement)
    }

    @Suppress("UNUSED_PARAMETER")
    fun cancelAllAds(context: Context, isPurchased: Boolean) {
        placements.values.forEach { config ->
            config.ad?.fullScreenContentCallback = null
            config.ad = null
            config.callbacks.clear()
        }
        placements.clear()
        cancelled = isPurchased
    }
}
