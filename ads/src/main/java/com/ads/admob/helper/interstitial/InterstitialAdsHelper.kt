package com.ads.admob.helper.interstitial

import android.app.Activity
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.ads.admob.AdmobManager
import com.ads.admob.admob.AdmobFactory
import com.ads.admob.cmp.ConsentManager
import com.ads.admob.data.ContentAd
import com.ads.admob.dialog.LoadingAdsDialog
import com.ads.admob.helper.interstitial.params.AdInterstitialState
import com.ads.admob.listener.InterstitialAdCallback
import com.ads.admob.listener.InterstitialAdRequestCallBack
import com.ads.admob.listener.InterstitialAdShowCallBack
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class InterstitialAdsHelper private constructor(private val adPlacement: String) {
    private var interstitialAdConfig: InterstitialAdConfig =
        InterstitialAdConfig(idAds = "", canShowAds = true, canReloadAds = true, adPlacement = "inter")

    fun setInterstitialAdConfig(config: InterstitialAdConfig) {
        interstitialAdConfig = config
    }

    private var adInterstitialState: AdInterstitialState = AdInterstitialState.None

    fun AdInterstitialState.emit(state: AdInterstitialState) = apply {
        adInterstitialState = state
    }

    private var loadingJob: Job? = null
    var interstitialAdValue: ContentAd? = null
        private set
    private var requestShowCount = 0
    private var loadingAdsDialog: LoadingAdsDialog? = null
    private var isCancelRequestAndShowAllAds = false

    fun cancelRequestAndShowAllAds(isPurchased: Boolean) {
        adInterstitialState.emit(AdInterstitialState.None)
        interstitialAdValue = null
        isCancelRequestAndShowAllAds = isPurchased
        if (isPurchased) {
            synchronized(InterstitialAdsHelper::class.java) {
                instances.remove(adPlacement)
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
        AdmobFactory.Companion.INSTANCE
            .requestInterstitialAds(
                activity,
                idAdPriority,
                interstitialAdConfig.adPlacement,
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
                        AdmobFactory.Companion.INSTANCE
                            .requestInterstitialAds(
                                activity,
                                idAdNormal,
                                interstitialAdConfig.adPlacement,
                                interstitialAdConfig.reloadIfFirstFail,
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

    fun requestInterAds(
        activity: Activity,
        isLoadAndShow: Boolean = false,
        interstitialAdRequestCallBack: InterstitialAdRequestCallBack
    ) {
        if (canRequestAds(activity)) {
            if (requestValid(activity)) {
                Log.e(TAG, "requestInterAds: $adPlacement")
                adInterstitialState.emit(AdInterstitialState.Loading)
                if(isLoadAndShow) showDialogLoading(activity)
                if (interstitialAdConfig.idAdsPriority != null) {
                    requestAdsAlternate(
                        activity,
                        interstitialAdConfig.idAdsPriority!!,
                        interstitialAdConfig.idAds,
                        object : InterstitialAdCallback {
                            override fun onNextAction() {
                            }

                            override fun onAdClose() {
                            }

                            override fun onInterstitialShow() {
                                adInterstitialState.emit(AdInterstitialState.Showed)
                            }

                            override fun onAdLoaded(data: ContentAd) {
                                interstitialAdValue = data
                                adInterstitialState.emit(AdInterstitialState.Loaded)
                                interstitialAdRequestCallBack.onAdLoaded(data)
                            }

                            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                                adInterstitialState.emit(AdInterstitialState.Fail)
                                dismissDialog()
                                interstitialAdRequestCallBack.onAdFailedToLoad(loadAdError)
                            }

                            override fun onAdClicked() {
                            }

                            override fun onAdImpression() {
                                adInterstitialState.emit(AdInterstitialState.Showed)
                            }

                            override fun onAdFailedToShow(adError: AdError) {
                            }

                        })
                } else {
                    AdmobFactory.Companion.INSTANCE
                        .requestInterstitialAds(
                            activity,
                            interstitialAdConfig.idAds,
                            interstitialAdConfig.adPlacement,
                            interstitialAdConfig.reloadIfFirstFail,
                            object : InterstitialAdCallback {
                                override fun onNextAction() {
                                }

                                override fun onAdClose() {
                                }

                                override fun onInterstitialShow() {
                                    adInterstitialState.emit(AdInterstitialState.Showed)
                                }

                                override fun onAdLoaded(data: ContentAd) {
                                    interstitialAdValue = data
                                    adInterstitialState.emit(AdInterstitialState.Loaded)
                                    interstitialAdRequestCallBack.onAdLoaded(data)
                                }

                                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                                    adInterstitialState.emit(AdInterstitialState.Fail)
                                    dismissDialog()
                                    interstitialAdRequestCallBack.onAdFailedToLoad(loadAdError)
                                }

                                override fun onAdClicked() {
                                }

                                override fun onAdImpression() {
                                    adInterstitialState.emit(AdInterstitialState.Showed)
                                }

                                override fun onAdFailedToShow(adError: AdError) {
                                }

                            }
                        )
                }
            } else {
                Log.e(TAG, "requestInterAds $adPlacement: Invalid")
            }
        } else {
            Log.e(TAG, "requestInterAds $adPlacement: canRequestAds = false")
            adInterstitialState.emit(AdInterstitialState.Fail)
            interstitialAdRequestCallBack.onAdFailedToLoad(
                LoadAdError(
                    99,
                    "Request Invalid",
                    "",
                    null,
                    null
                )
            )
        }
    }

    private suspend fun waitForAdReady(timeoutMs: Long = 1200L): Boolean {
        val start = System.currentTimeMillis()

        while (System.currentTimeMillis() - start < timeoutMs) {
            if (
                adInterstitialState == AdInterstitialState.Loaded &&
                interstitialAdValue != null
            ) {
                return true
            }
            delay(50)
        }

        return false
    }

    fun forceShowInterstitial(
        activity: Activity,
        lifecycleOwner: LifecycleOwner,
        interstitialAdShowCallBack: InterstitialAdShowCallBack
    ) {
        if (isCancelRequestAndShowAllAds) {
            Log.e(TAG, "forceShowInterstitial: InValid")
            interstitialAdShowCallBack.onNextAction()
            interstitialAdShowCallBack.onAdFailedToShow(AdError(1999, "Show ads InValid isCancelRequestAndShowAllAds", ""))
            return
        }
        if (interstitialAdConfig.showByTime != 1) {
            requestShowCount++
        }
        if (requestShowCount % interstitialAdConfig.showByTime == 0 && interstitialAdValue != null && adInterstitialState == AdInterstitialState.Loaded) {
            if (!AdmobFactory.Companion.INSTANCE.isShowAdsIntervalValid()) {
                Log.e(TAG, "forceShowInterstitial: interval InValid")
                interstitialAdShowCallBack.onNextAction()
                interstitialAdShowCallBack.onAdFailedToShow(AdError(1999, "Show ads interval InValid", ""))
                return
            }
            try {
                lifecycleOwner.lifecycleScope.launch {
                    Log.e(TAG, "forceShowInterstitial: dwqdwq")

                    val ready = waitForAdReady(1200)

                    if (!ready) {
                        dismissDialog()
                        AdmobManager.adsFullScreenDismiss()
                        interstitialAdShowCallBack.onNextAction()
                        return@launch
                    }
                    AdmobManager.adsShowFullScreen()
                    showDialogLoading(activity)
                    delay(800)
                    showInterstitialInternal(activity, interstitialAdShowCallBack)

                    loadingJob = lifecycleOwner.lifecycleScope.launch {
                        delay(1000)
                        dismissDialog()
                    }
                }
            } catch (ex: Exception) {
                cancelLoadingJob()
                dismissDialog()
                AdmobManager.adsFullScreenDismiss()
                Log.e(TAG, "forceShowInterstitial: Exception ${ex.message}")
                interstitialAdShowCallBack.onNextAction()
                interstitialAdShowCallBack.onAdFailedToShow(AdError(1999, "${ex.message}", ""))
            }

        } else if (requestShowCount % interstitialAdConfig.showByTime ==
            if (interstitialAdConfig.showByTime <= 2) {
                1
            } else {
                interstitialAdConfig.showByTime - 1
            }
            && adInterstitialState != AdInterstitialState.Loading
        ) {
            AdmobManager.adsFullScreenDismiss()
            Log.e(TAG, "forceShowInterstitial: InValid")
            interstitialAdShowCallBack.onNextAction()
            interstitialAdShowCallBack.onAdFailedToShow(AdError(1999, "Show ads InValid", ""))
        } else {
            AdmobManager.adsFullScreenDismiss()
            Log.e(TAG, "forceShowInterstitial: InValid")
            interstitialAdShowCallBack.onNextAction()
            interstitialAdShowCallBack.onAdFailedToShow(AdError(1999, "Show ads InValid", ""))
        }
    }

    private fun showInterstitialInternal(
        activity: Activity,
        callback: InterstitialAdShowCallBack
    ) {
        AdmobFactory.Companion.INSTANCE
            .showInterstitial(
                activity,
                interstitialAdValue,
                object : InterstitialAdCallback {
                    override fun onNextAction() {
                        AdmobManager.adsFullScreenDismiss()
                        callback.onNextAction()
                        cancelLoadingJob()
                        dismissDialog()
                    }

                    override fun onAdClose() {
                        AdmobManager.adsFullScreenDismiss()
                        callback.onAdClose()
                        cancelLoadingJob()
                        dismissDialog()
                    }

                    override fun onInterstitialShow() {
                        callback.onInterstitialShow()
                        adInterstitialState.emit(AdInterstitialState.Showed)
                    }

                    override fun onAdLoaded(data: ContentAd) {
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    }

                    override fun onAdClicked() {

                    }

                    override fun onAdImpression() {
                        adInterstitialState.emit(AdInterstitialState.Showed)
                    }

                    override fun onAdFailedToShow(adError: AdError) {
                        cancelLoadingJob()
                        dismissDialog()
                        AdmobManager.adsFullScreenDismiss()
                        callback.onAdFailedToShow(adError)
                        callback.onNextAction()
                        adInterstitialState.emit(AdInterstitialState.ShowFail)

                    }
                })
    }

    private fun cancelLoadingJob() {
        loadingJob?.cancel()
        loadingJob = null
    }

    private fun dismissDialog() {
        try {
            val activity = loadingAdsDialog?.ownerActivity
            if (activity != null && !activity.isDestroyed && activity.windowManager != null) {
                loadingAdsDialog?.dismiss()
                loadingAdsDialog = null
            }
        } catch (_: Exception) {
        }
    }

    private fun showDialogLoading(context: Activity) {
        if (context.isFinishing || context.isDestroyed) return
        try {
            if (loadingAdsDialog == null) {
                loadingAdsDialog = LoadingAdsDialog(context)
            }
//            if (loadingAdsDialog?.isShowing == true) {
//                loadingAdsDialog?.dismiss()
//            }
            loadingAdsDialog?.setOwnerActivity(context)
            loadingAdsDialog?.show()
        } catch (_: Exception) {
        }

    }

    private fun requestValid(activity: Activity?): Boolean {
        val showConfigValid = (interstitialAdConfig.showByTime == 1
                || requestShowCount % interstitialAdConfig.showByTime ==
                if (interstitialAdConfig.showByTime <= 2) {
                    1
                } else {
                    interstitialAdConfig.showByTime - 1
                })
        val valueValid =
            (interstitialAdValue == null
                    && (adInterstitialState != AdInterstitialState.Loading && adInterstitialState != AdInterstitialState.Loaded)
                    )
                    || adInterstitialState == AdInterstitialState.Showed
        return canRequestAds(activity) && showConfigValid && valueValid
    }

    private fun canRequestAds(activity: Activity?): Boolean {
        val consentOk = activity?.let {
            ConsentManager.Companion.getInstance(it).getConsentResult(it)
        } ?: true

        return interstitialAdConfig.canShowAds &&
                !isCancelRequestAndShowAllAds &&
                consentOk
    }

    companion object {
        private val TAG = InterstitialAdsHelper::class.simpleName

        // Map to hold singleton instances with their associated IDs
        private val instances = mutableMapOf<String, InterstitialAdsHelper>()

        // Method to get or create singleton instances
        @Synchronized
        fun getInstance(adPlacement: String): InterstitialAdsHelper {
            return instances.getOrPut(adPlacement) { InterstitialAdsHelper(adPlacement) }
        }
    }
}