package com.ads.admob.helper.appoppen

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.ads.admob.AdmobManager
import com.ads.admob.admob.AdmobFactory
import com.ads.admob.cmp.ConsentManager
import com.ads.admob.data.ContentAd
import com.ads.admob.dialog.LoadingAdsDialog
import com.ads.admob.helper.appoppen.params.AdAppOpenState
import com.ads.admob.listener.AppOpenAdCallBack
import com.ads.admob.listener.AppOpenAdRequestCallBack
import com.ads.admob.listener.AppOpenAdShowCallBack
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AppOpenAdsHelper private constructor(private val adPlacement: String) {
    private var appOpenAdConfig: AppOpenAdConfig =
        AppOpenAdConfig(idAds = "", canShowAds = true, canReloadAds = true, adPlacement = "app_open")

    fun setAppOpenAdConfig(config: AppOpenAdConfig) {
        appOpenAdConfig = config
    }

    private var adAppOpenState: AdAppOpenState = AdAppOpenState.None

    fun AdAppOpenState.emit(state: AdAppOpenState) = apply {
        adAppOpenState = state
    }

    private var loadingJob: Job? = null
    var appOpenAdValue: ContentAd? = null
        private set
    private var loadingAdsDialog: LoadingAdsDialog? = null
    private var isCancelRequestAndShowAllAds = false

    fun cancelRequestAndShowAllAds(isPurchased: Boolean) {
        adAppOpenState.emit(AdAppOpenState.None)
        appOpenAdValue = null
        isCancelRequestAndShowAllAds = isPurchased
        if (isPurchased) {
            synchronized(AppOpenAdsHelper::class.java) {
                instances.remove(adPlacement)
            }
        }
    }

    private fun requestAdsAlternate(
        activity: Context,
        idAdPriority: String,
        idAdNormal: String,
        appOpenAdCallback: AppOpenAdCallBack
    ) {
        Log.d(TAG, "requestAdsAlternate: ")
        AdmobFactory.INSTANCE
            .requestAppOpenAds(
                activity,
                idAdPriority,
                appOpenAdConfig.adPlacement,
                false,
                object : AppOpenAdCallBack {
                    override fun onAppOpenAdShow() {
                        appOpenAdCallback.onAppOpenAdShow()
                    }

                    override fun onAppOpenAdClose() {
                        appOpenAdCallback.onAppOpenAdClose()
                    }

                    override fun onAdLoaded(data: ContentAd) {
                        appOpenAdValue = data
                        appOpenAdCallback.onAdLoaded(data)
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        AdmobFactory.INSTANCE
                            .requestAppOpenAds(
                                activity,
                                idAdNormal,
                                appOpenAdConfig.adPlacement,
                                appOpenAdConfig.reloadIfFirstFail,
                                object : AppOpenAdCallBack {
                                    override fun onAppOpenAdShow() {
                                        appOpenAdCallback.onAppOpenAdShow()
                                    }

                                    override fun onAppOpenAdClose() {
                                        appOpenAdCallback.onAppOpenAdClose()
                                    }

                                    override fun onAdLoaded(data: ContentAd) {
                                        appOpenAdCallback.onAdLoaded(data)
                                    }

                                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                                        appOpenAdCallback.onAdFailedToLoad(loadAdError)
                                    }

                                    override fun onAdClicked() {
                                        appOpenAdCallback.onAdClicked()
                                    }

                                    override fun onAdImpression() {
                                        appOpenAdCallback.onAdImpression()
                                    }

                                    override fun onAdFailedToShow(adError: AdError) {
                                        appOpenAdCallback.onAdFailedToShow(adError)
                                    }

                                })
                    }

                    override fun onAdClicked() {
                        appOpenAdCallback.onAdClicked()
                    }

                    override fun onAdImpression() {
                        appOpenAdCallback.onAdImpression()
                    }

                    override fun onAdFailedToShow(adError: AdError) {
                        appOpenAdCallback.onAdFailedToShow(adError)
                    }

                })
    }

    fun requestAppOpenAds(
        activity: Activity,
        isLoadAndShow: Boolean = false,
        appOpenAdRequestCallBack: AppOpenAdRequestCallBack
    ) {
        if (canRequestAds(activity)) {
            if (requestValid(activity)) {
                Log.e(TAG, "requestAppOpenAds: $adPlacement")
                adAppOpenState.emit(AdAppOpenState.Loading)
                if (isLoadAndShow) showDialogLoading(activity)
                if (appOpenAdConfig.idAdsPriority != null) {
                    requestAdsAlternate(
                        activity,
                        appOpenAdConfig.idAdsPriority!!,
                        appOpenAdConfig.idAds,
                        object : AppOpenAdCallBack {
                            override fun onAppOpenAdShow() {
                                adAppOpenState.emit(AdAppOpenState.Showed)
                            }

                            override fun onAppOpenAdClose() {

                            }

                            override fun onAdLoaded(data: ContentAd) {
                                appOpenAdValue = data
                                adAppOpenState.emit(AdAppOpenState.Loaded)
                                appOpenAdRequestCallBack.onAdLoaded(data)
                            }

                            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                                adAppOpenState.emit(AdAppOpenState.Fail)
                                dismissDialog()
                                appOpenAdRequestCallBack.onAdFailedToLoad(loadAdError)
                            }

                            override fun onAdClicked() {
                            }

                            override fun onAdImpression() {
                                adAppOpenState.emit(AdAppOpenState.Showed)
                            }

                            override fun onAdFailedToShow(adError: AdError) {
                            }

                        })
                } else {
                    AdmobFactory.INSTANCE.requestAppOpenAds(
                        activity,
                        appOpenAdConfig.idAds,
                        appOpenAdConfig.adPlacement,
                        appOpenAdConfig.reloadIfFirstFail,
                        object : AppOpenAdCallBack {
                            override fun onAppOpenAdShow() {
                                adAppOpenState.emit(AdAppOpenState.Showed)
                            }

                            override fun onAppOpenAdClose() {

                            }

                            override fun onAdLoaded(data: ContentAd) {
                                appOpenAdValue = data
                                adAppOpenState.emit(AdAppOpenState.Loaded)
                                appOpenAdRequestCallBack.onAdLoaded(data)
                            }

                            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                                adAppOpenState.emit(AdAppOpenState.Fail)
                                dismissDialog()
                                appOpenAdRequestCallBack.onAdFailedToLoad(loadAdError)
                            }

                            override fun onAdClicked() {
                            }

                            override fun onAdImpression() {
                                adAppOpenState.emit(AdAppOpenState.Showed)
                            }

                            override fun onAdFailedToShow(adError: AdError) {
                            }

                        })
                }
            } else {
                Log.e(TAG, "requestAppOpenAds $adPlacement: Invalid")
            }
        } else {
            Log.e(TAG, "requestAppOpenAds $adPlacement: canRequestAds = false")
            adAppOpenState.emit(AdAppOpenState.Fail)
            appOpenAdRequestCallBack.onAdFailedToLoad(
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
                adAppOpenState == AdAppOpenState.Loaded &&
                appOpenAdValue != null
            ) {
                return true
            }
            delay(50)
        }

        return false
    }

    fun forceShowAppOpen(
        activity: Activity,
        lifecycleOwner: LifecycleOwner,
        appOpenAdShowCallBack: AppOpenAdShowCallBack
    ) {
        if (isCancelRequestAndShowAllAds) {
            Log.e(TAG, "forceShowAppOpen: InValid")
            appOpenAdShowCallBack.onNextAction()
            appOpenAdShowCallBack.onAdFailedToShow(AdError(1999, "Show ads InValid isCancelRequestAndShowAllAds", ""))
            return
        }

        if (appOpenAdValue != null && adAppOpenState == AdAppOpenState.Loaded) {
            try {
                lifecycleOwner.lifecycleScope.launch {
                    Log.e(TAG, "forceShowAppOpen: dwqdwq")
                    val ready = waitForAdReady(1200)

                    if (!ready) {
                        dismissDialog()
                        AdmobManager.adsFullScreenDismiss()
                        appOpenAdShowCallBack.onNextAction()
                        return@launch
                    }
                    AdmobManager.adsShowFullScreen()
                    showDialogLoading(activity)
                    delay(800)
                    showAppOpenInternal(activity, appOpenAdShowCallBack)

                    loadingJob = lifecycleOwner.lifecycleScope.launch {
                        delay(1000)
                        dismissDialog()
                    }
                }
            } catch (ex: Exception) {
                cancelLoadingJob()
                dismissDialog()
                AdmobManager.adsFullScreenDismiss()
                Log.e(TAG, "forceShowAppOpen: Exception ${ex.message}")
                appOpenAdShowCallBack.onNextAction()
                appOpenAdShowCallBack.onAdFailedToShow(AdError(1999, "${ex.message}", ""))
            }

        } else if (adAppOpenState != AdAppOpenState.Loading) {
            AdmobManager.adsFullScreenDismiss()
            Log.e(TAG, "forceShowAppOpen: InValid")
            appOpenAdShowCallBack.onNextAction()
            appOpenAdShowCallBack.onAdFailedToShow(AdError(1999, "Show ads InValid", ""))
        } else {
            AdmobManager.adsFullScreenDismiss()
            Log.e(TAG, "forceShowAppOpen: InValid")
            appOpenAdShowCallBack.onNextAction()
            appOpenAdShowCallBack.onAdFailedToShow(AdError(1999, "Show ads InValid", ""))
        }
    }

    private fun showAppOpenInternal(
        activity: Activity,
        callback: AppOpenAdShowCallBack
    ) {
        AdmobFactory.INSTANCE
            .showAppOpen(
                activity,
                appOpenAdValue,
                object : AppOpenAdCallBack {
                    override fun onAppOpenAdShow() {
                        Log.e(TAG, "onAppOpenAdShow: ")
                        callback.onAppOpenShow()
                        adAppOpenState.emit(AdAppOpenState.Showed)
                    }

                    override fun onAppOpenAdClose() {
                        Log.e(TAG, "onAppOpenAdClose: ")
                        AdmobManager.adsFullScreenDismiss()
                        callback.onNextAction()
                        cancelLoadingJob()
                        dismissDialog()
                    }

                    override fun onAdLoaded(data: ContentAd) {
                        Log.e(TAG, "onAppOpenAdLoaded: ")
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        Log.e(TAG, "onAppOpenAdLoadFail: ")
                    }

                    override fun onAdClicked() {
                    }

                    override fun onAdImpression() {
                        adAppOpenState.emit(AdAppOpenState.Showed)
                    }

                    override fun onAdFailedToShow(adError: AdError) {
                        cancelLoadingJob()
                        dismissDialog()
                        AdmobManager.adsFullScreenDismiss()
                        callback.onAdFailedToShow(adError)
                        callback.onNextAction()
                        adAppOpenState.emit(AdAppOpenState.ShowFail)
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

        val valueValid =
            (appOpenAdValue == null
                    && (adAppOpenState != AdAppOpenState.Loading && adAppOpenState != AdAppOpenState.Loaded)
                    )
                    || adAppOpenState == AdAppOpenState.Showed
        return canRequestAds(activity) && valueValid
    }

    private fun canRequestAds(activity: Activity?): Boolean {
        val consentOk = activity?.let {
            ConsentManager.getInstance(it).getConsentResult(it)
        } ?: true
        return appOpenAdConfig.canShowAds && !isCancelRequestAndShowAllAds && consentOk
    }

    companion object {
        private val TAG = AppOpenAdsHelper::class.simpleName

        // Map to hold singleton instances with their associated IDs
        private val instances = mutableMapOf<String, AppOpenAdsHelper>()

        // Method to get or create singleton instances
        @Synchronized
        fun getInstance(adPlacement: String): AppOpenAdsHelper {
            return instances.getOrPut(adPlacement) { AppOpenAdsHelper(adPlacement) }
        }
    }
}