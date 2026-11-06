package com.ads.admob.helper.interstitial

import android.app.Activity
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.ads.admob.AdmobManager
import com.ads.admob.admob.AdmobFactory
import com.ads.admob.data.ContentAd
import com.ads.admob.dialog.LoadingAdsDialog
import com.ads.admob.helper.AdsHelper
import com.ads.admob.helper.interstitial.params.AdInterstitialState
import com.ads.admob.helper.interstitial.params.InterstitialAdParam
import com.ads.admob.listener.InterstitialAdCallback
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.time.Duration.Companion.milliseconds


class InterstitialAdSplashHelper(
    private val activity: Activity,
    private val lifecycleOwner: LifecycleOwner,
    private val config: InterstitialAdSplashConfig
) : AdsHelper<InterstitialAdSplashConfig, InterstitialAdParam>(activity, lifecycleOwner, config) {
    private val dialogLoading by lazy {
        val dialog = LoadingAdsDialog(activity)
        dialog.setOwnerActivity(activity)
        dialog
    }

    private val listAdCallback: CopyOnWriteArrayList<InterstitialAdCallback> =
        CopyOnWriteArrayList()
    private val adInterstitialState: MutableStateFlow<AdInterstitialState> =
        MutableStateFlow(if (canRequestAds()) AdInterstitialState.None else AdInterstitialState.Fail)
    var interstitialAdValue: ContentAd? = null
        private set

    private var requestTimeOutJob: Job? = null
    private var requestDelayJob: Job? = null
    private var showValid = false
    private var loadingJob: Job? = null

    private var isShowing = false
    private var splashFinished = false
    private var timeoutReached = false
    var holdShow: Boolean = false
    override fun requestAds(param: InterstitialAdParam) {
        lifecycleOwner.lifecycleScope.launch {
            if (canRequestAds()) {
                when (param) {
                    is InterstitialAdParam.Request -> {
                        flagActive.compareAndSet(false, true)
                        if (interstitialAdValue == null) {
                            adInterstitialState.emit(AdInterstitialState.Loading)
                        }
                        createInterAds(activity)
                    }

                    is InterstitialAdParam.Show -> {
                        flagActive.compareAndSet(false, true)
                        interstitialAdValue = param.interstitialAd
                        adInterstitialState.emit(AdInterstitialState.Loaded)
                        showInterAds(activity)
                    }

                    else -> {

                    }
                }
            } else {
                invokeAdListener { it.onNextAction() }
            }
        }
    }

    fun releaseHoldShow() {
        holdShow = false
        if (interstitialAdValue != null && !isShowing && !splashFinished) {
            showInterAds(activity)
        }
    }

    private fun showInterAds(activity: Activity) {
        lifecycleOwner.lifecycleScope.launch {
            if (holdShow || isShowing || splashFinished) return@launch
            if (!lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return@launch
            isShowing = true
            if (adInterstitialState.value == AdInterstitialState.Loaded || adInterstitialState.value == AdInterstitialState.ShowFail) {
                requestDelayJob?.cancel()
                AdmobManager.adsShowFullScreen()
                showDialogLoading()
                delay(1500.milliseconds)
                AdmobFactory.INSTANCE
                    .showInterstitial(activity, interstitialAdValue, invokeListenerAdCallback())
                loadingJob = lifecycleOwner.lifecycleScope.launch {
                    delay(2000.milliseconds)
                    dismissDialog()
                }
            } else {
                isShowing = false
            }
        }
    }

    private fun showDialogLoading() {
        if (activity.isFinishing || activity.isDestroyed) return
        try {
            cancelLoadingJob()
            dialogLoading.show()
        } catch (_: Exception) {
        }
    }

    private fun createInterAds(activity: Activity) {
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                if (interstitialAdValue != null && !holdShow && !isShowing && !splashFinished) {
                    showInterAds(activity)
                }
            }
        }
        requestTimeOutJob = lifecycleOwner.lifecycleScope.launch {
            if (config.idAdsPriority != null) {
                requestAdsAlternate(
                    activity,
                    config.idAdsPriority,
                    config.idAds,
                    invokeListenerAdCallback()
                )
            } else {
                AdmobFactory.INSTANCE
                    .requestInterstitialAds(
                        activity,
                        config.idAds,
                        config.adPlacement,
                        config.reloadIfFirstFail,
                        invokeListenerAdCallback()
                    )
            }
            delay(config.timeOut)
            timeoutReached = true
            if (!isShowing && !splashFinished && interstitialAdValue == null) {
                splashFinished = true
                invokeAdListener { it.onNextAction() }
            }
        }
        requestDelayJob = lifecycleOwner.lifecycleScope.launch {
            delay(config.timeDelay)
            showValid = true
            if (interstitialAdValue != null && config.showReady) {
                showInterAds(activity)
            }
        }

    }

    private fun requestAdsAlternate(
        activity: Activity,
        idAdPriority: String,
        idAdNormal: String,
        interstitialAdCallback: InterstitialAdCallback
    ) {
        Log.d(TAG, "requestAdsAlternate: ")
        AdmobFactory.INSTANCE
            .requestInterstitialAds(
                activity,
                idAdPriority,
                config.adPlacement,
                false,
                object : InterstitialAdCallback {
                    override fun onNextAction() {
                        interstitialAdCallback.onNextAction()
                    }

                    override fun onAdClose() {
                        interstitialAdCallback.onAdClose()
                    }

                    override fun onInterstitialShow() {
                        interstitialAdCallback.onInterstitialShow()
                    }

                    override fun onAdLoaded(data: ContentAd) {
                        Log.d(TAG, "requestAdsAlternate onAdLoaded: Priority")
                        interstitialAdCallback.onAdLoaded(data)
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        Log.d(TAG, "requestAdsAlternate: onAdFailedToLoad Priority ${loadAdError.message}")
                        AdmobFactory.INSTANCE
                            .requestInterstitialAds(
                                activity,
                                idAdNormal,
                                config.adPlacement,
                                config.reloadIfFirstFail,
                                object : InterstitialAdCallback {
                                    override fun onNextAction() {
                                        interstitialAdCallback.onNextAction()
                                    }

                                    override fun onAdClose() {
                                        interstitialAdCallback.onAdClose()
                                    }

                                    override fun onInterstitialShow() {
                                        interstitialAdCallback.onInterstitialShow()
                                    }

                                    override fun onAdLoaded(data: ContentAd) {
                                        Log.d(TAG, "requestAdsAlternate onAdLoaded: Normal")
                                        interstitialAdCallback.onAdLoaded(data)
                                    }

                                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                                        Log.d(TAG, "requestAdsAlternate: onAdFailedToLoad Normal ${loadAdError.message}")
                                        interstitialAdCallback.onAdFailedToLoad(loadAdError)
                                    }

                                    override fun onAdClicked() {
                                        interstitialAdCallback.onAdClicked()
                                    }

                                    override fun onAdImpression() {
                                        interstitialAdCallback.onAdImpression()
                                    }

                                    override fun onAdFailedToShow(adError: AdError) {
                                        interstitialAdCallback.onAdFailedToShow(adError)
                                    }

                                })
                    }

                    override fun onAdClicked() {
                        interstitialAdCallback.onAdClicked()
                    }

                    override fun onAdImpression() {
                        interstitialAdCallback.onAdImpression()
                    }

                    override fun onAdFailedToShow(adError: AdError) {
                        interstitialAdCallback.onAdFailedToShow(adError)
                    }

                })
    }

    override fun cancel() {
        requestTimeOutJob?.cancel()
        requestTimeOutJob = null
        requestDelayJob?.cancel()
        requestDelayJob = null
        loadingJob?.cancel()
        loadingJob = null
        splashFinished = true
        dismissDialog()
    }

    fun registerAdListener(adCallback: InterstitialAdCallback) {
        this.listAdCallback.add(adCallback)
    }

    fun unregisterAdListener(adCallback: InterstitialAdCallback) {
        this.listAdCallback.remove(adCallback)
    }

    fun unregisterAllAdListener() {
        this.listAdCallback.clear()
    }

    private fun invokeAdListener(action: (adCallback: InterstitialAdCallback) -> Unit) {
        listAdCallback.forEach(action)
    }

    private fun invokeListenerAdCallback(): InterstitialAdCallback {
        return object : InterstitialAdCallback {
            override fun onNextAction() {
                dismissDialog()
                cancelLoadingJob()
                AdmobManager.adsFullScreenDismiss()
                invokeAdListener { it.onNextAction() }
            }

            override fun onAdClose() {
                isShowing = false
                splashFinished = true
                dismissDialog()
                cancelLoadingJob()
                invokeAdListener { it.onAdClose() }
                AdmobManager.adsFullScreenDismiss()
            }

            override fun onInterstitialShow() {
                AdmobManager.adsShowFullScreen()
                lifecycleOwner.lifecycleScope.launch {
                    adInterstitialState.emit(AdInterstitialState.Showed)
                }
                requestTimeOutJob?.cancel()
                invokeAdListener { it.onInterstitialShow() }
            }

            override fun onAdLoaded(data: ContentAd) {
                interstitialAdValue = data
                lifecycleOwner.lifecycleScope.launch {
                    adInterstitialState.emit(AdInterstitialState.Loaded)
                }
                if (!splashFinished) {
                    showInterAds(activity)
                }
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                invokeAdListener { it.onNextAction() }
                invokeAdListener { it.onAdFailedToLoad(loadAdError) }
            }

            override fun onAdClicked() {
                invokeAdListener { it.onAdClicked() }
            }

            override fun onAdImpression() {
                invokeAdListener { it.onAdImpression() }
            }

            override fun onAdFailedToShow(adError: AdError) {
                isShowing = false
                splashFinished = true
                AdmobManager.adsFullScreenDismiss()
                dismissDialog()
                cancelLoadingJob()
                lifecycleOwner.lifecycleScope.launch {
                    adInterstitialState.emit(AdInterstitialState.ShowFail)
                }
                invokeAdListener { it.onAdFailedToShow(adError) }
                if (lifecycleOwner.lifecycle.currentState == Lifecycle.State.RESUMED) {
                    invokeAdListener { it.onNextAction() }
                }
            }

        }
    }

    private fun dismissDialog() {
        try {
            val activity = dialogLoading.ownerActivity
            if (activity != null && !activity.isDestroyed && activity.windowManager != null) {
                dialogLoading.dismiss()
            }
        } catch (_: Exception) {
        }
    }

    private fun cancelLoadingJob() {
        requestTimeOutJob?.cancel()
        requestDelayJob?.cancel()
        loadingJob?.cancel()
        loadingJob = null
    }

    companion object {
        private val TAG = InterstitialAdSplashHelper::class.simpleName
    }
}