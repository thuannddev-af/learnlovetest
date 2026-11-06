package com.ads.admob.helper.reward

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
import com.ads.admob.helper.reward.params.AdRewardState
import com.ads.admob.listener.RewardAdCallBack
import com.ads.admob.listener.RewardAdRequestCallBack
import com.ads.admob.listener.RewardAdShowCallBack
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RewardAdHelper private constructor(private val adPlacement: String) {
    private var isCancelRequestAndShowAllAds = false

    private var rewardAdConfig: RewardAdConfig =
        RewardAdConfig(idAds = "", canShowAds = true, canReloadAds = true, adPlacement = "reward")

    fun setRewardAdConfig(config: RewardAdConfig) {
        rewardAdConfig = config
    }

    private var adRewardState: AdRewardState = AdRewardState.None
    var rewardAdValue: ContentAd? = null
        private set

    private var loadingAdsDialog: LoadingAdsDialog? = null
    private var requestShowCount = 0
    private var loadingJob: Job? = null
    private fun cancelLoadingJob() {
        loadingJob?.cancel()
        loadingJob = null
    }

    fun cancelRequestAndShowAllAds(isPurchased: Boolean) {
        adRewardState.emit(AdRewardState.None)
        rewardAdValue = null
        isCancelRequestAndShowAllAds = isPurchased
        if (isPurchased) {
            synchronized(RewardAdHelper::class.java) {
                instances.remove(adPlacement)
            }
        }
    }


    private fun canRequestAds(activity: Activity?): Boolean {
        val consentOk = activity?.let {
            ConsentManager.Companion.getInstance(it).getConsentResult(it)
        } ?: true
        return rewardAdConfig.canShowAds && !isCancelRequestAndShowAllAds && consentOk
    }

    fun AdRewardState.emit(state: AdRewardState) = apply {
        adRewardState = state
    }

    private fun requestAdsAlternate(
        activity: Context,
        idAdPriority: String,
        idAdNormal: String,
        rewardAdCallBack: RewardAdCallBack
    ) {
        Log.d(TAG, "requestAdsAlternate: ")
        AdmobFactory.Companion.INSTANCE
            .requestRewardAd(
                activity,
                idAdPriority,
                rewardAdConfig.adPlacement,
                false,
                object : RewardAdCallBack {

                    override fun onAdClose() {
                        rewardAdCallBack.onAdClose()
                    }

                    override fun onUserEarnedReward(rewardItem: RewardItem?) {

                    }

                    override fun onRewardShow() {
                        adRewardState.emit(AdRewardState.Showed)
                    }


                    override fun onAdLoaded(data: ContentAd) {
                        Log.d(TAG, "requestAdsAlternate onAdLoaded: Priority")
                        rewardAdCallBack.onAdLoaded(data)
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        Log.d(TAG, "requestAdsAlternate: onAdFailedToLoad Priority ${loadAdError.message}")
                        AdmobFactory.Companion.INSTANCE
                            .requestRewardAd(
                                activity,
                                idAdNormal,
                                rewardAdConfig.adPlacement,
                                rewardAdConfig.reloadIfFirstFail,
                                object : RewardAdCallBack {

                                    override fun onAdClose() {
                                        rewardAdCallBack.onAdClose()
                                    }

                                    override fun onUserEarnedReward(rewardItem: RewardItem?) {
                                    }

                                    override fun onRewardShow() {
                                        rewardAdCallBack.onRewardShow()
                                    }

                                    override fun onAdLoaded(data: ContentAd) {
                                        Log.d(TAG, "requestAdsAlternate onAdLoaded: Normal")
                                        rewardAdCallBack.onAdLoaded(data)
                                    }

                                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                                        Log.d(TAG, "requestAdsAlternate: onAdFailedToLoad Normal ${loadAdError.message}")
                                        rewardAdCallBack.onAdFailedToLoad(loadAdError)
                                    }

                                    override fun onAdClicked() {
                                        rewardAdCallBack.onAdClicked()
                                    }

                                    override fun onAdImpression() {
                                        rewardAdCallBack.onAdImpression()
                                    }

                                    override fun onAdFailedToShow(adError: AdError) {
                                        rewardAdCallBack.onAdFailedToShow(adError)
                                    }

                                })
                    }

                    override fun onAdClicked() {
                        rewardAdCallBack.onAdClicked()
                    }

                    override fun onAdImpression() {
                        rewardAdCallBack.onAdImpression()
                    }

                    override fun onAdFailedToShow(adError: AdError) {
                        rewardAdCallBack.onAdFailedToShow(adError)
                    }

                })
    }

    fun requestRewardAds(
        activity: Activity,
        isLoadAndShow: Boolean = false,
        rewardAdRequestCallBack: RewardAdRequestCallBack
    ) {
        if (canRequestAds(activity)) {
            if (requestValid(activity)) {
                Log.e(TAG, "requestInterAds: $adPlacement")
                adRewardState.emit(AdRewardState.Loading)
                if(isLoadAndShow) showDialogLoading(activity)
                if (rewardAdConfig.idAdsPriority != null) {
                    requestAdsAlternate(
                        activity,
                        rewardAdConfig.idAdsPriority!!,
                        rewardAdConfig.idAds,
                        object : RewardAdCallBack {

                            override fun onAdLoaded(data: ContentAd) {
                                rewardAdValue = data
                                adRewardState.emit(AdRewardState.Loaded)
                                rewardAdRequestCallBack.onAdLoaded(data)
                            }

                            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                                adRewardState.emit(AdRewardState.Fail)
                                dismissDialog()
                                rewardAdRequestCallBack.onAdFailedToLoad(loadAdError)
                            }

                            override fun onAdClicked() {
                            }

                            override fun onAdImpression() {
                                adRewardState.emit(AdRewardState.Showed)
                            }

                            override fun onAdFailedToShow(adError: AdError) {
                            }

                            override fun onAdClose() {

                            }

                            override fun onUserEarnedReward(rewardItem: RewardItem?) {

                            }

                            override fun onRewardShow() {
                                adRewardState.emit(AdRewardState.Showed)
                            }

                        })
                } else {
                    AdmobFactory.Companion.INSTANCE
                        .requestRewardAd(
                            activity,
                            rewardAdConfig.idAds,
                            rewardAdConfig.adPlacement,
                            rewardAdConfig.reloadIfFirstFail,
                            object : RewardAdCallBack {
                                override fun onAdClose() {
                                }

                                override fun onUserEarnedReward(rewardItem: RewardItem?) {
                                }

                                override fun onRewardShow() {
                                    adRewardState.emit(AdRewardState.Showed)
                                }

                                override fun onAdLoaded(data: ContentAd) {
                                    rewardAdValue = data
                                    adRewardState.emit(AdRewardState.Loaded)
                                    rewardAdRequestCallBack.onAdLoaded(data)
                                }

                                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                                    adRewardState.emit(AdRewardState.Fail)
                                    dismissDialog()
                                    rewardAdRequestCallBack.onAdFailedToLoad(loadAdError)
                                }

                                override fun onAdClicked() {
                                }

                                override fun onAdImpression() {
                                    adRewardState.emit(AdRewardState.Showed)
                                }

                                override fun onAdFailedToShow(adError: AdError) {
                                }

                            }
                        )
                }
            } else {
                Log.e(TAG, "requestInterAds $adPlacement: Invalid")
                rewardAdRequestCallBack.onAdFailedToLoad(
                    LoadAdError(
                        99,
                        "request Invalid",
                        "",
                        null,
                        null
                    )
                )
            }
        } else {
            Log.e(TAG, "requestInterAds $adPlacement: canRequestAds = false")
            adRewardState.emit(AdRewardState.Fail)
            rewardAdRequestCallBack.onAdFailedToLoad(
                LoadAdError(
                    99,
                    "can request = false",
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
                adRewardState == AdRewardState.Loaded &&
                rewardAdValue != null
            ) {
                return true
            }
            delay(50)
        }

        return false
    }

    fun forceShowRewardAd(
        activity: Activity,
        lifecycleOwner: LifecycleOwner,
        rewardAdShowCallBack: RewardAdShowCallBack
    ) {
        if (isCancelRequestAndShowAllAds) {
            Log.e(TAG, "$adPlacement forceShowRewardAd:Cancel Request And Show All Ads ")
            rewardAdShowCallBack.onAdClose()
            return
        }
        if (rewardAdConfig.showByTime != 1) {
            requestShowCount++
        }
        if (rewardAdValue != null && adRewardState == AdRewardState.Loaded) {
            try {
                lifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                    val ready = waitForAdReady(1200)

                    if (!ready) {
                        dismissDialog()
                        AdmobManager.adsFullScreenDismiss()
                        rewardAdShowCallBack.onAdClose()
                        return@launch
                    }
                    AdmobManager.adsShowFullScreen()
                    showDialogLoading(activity)
                    delay(800)
                    showRewardInternal(activity, rewardAdShowCallBack)

                    loadingJob = lifecycleOwner.lifecycleScope.launch {
                        delay(1000)
                        dismissDialog()
                    }

                }
            } catch (ex: Exception) {
                cancelLoadingJob()
                dismissDialog()
                AdmobManager.adsFullScreenDismiss()
                rewardAdShowCallBack.onAdFailedToShow(
                    AdError(
                        1999,
                        "reward show Exception : ${ex.message}",
                        ""
                    )
                )
            }
        } else if (adRewardState != AdRewardState.Loading) {
            AdmobManager.adsFullScreenDismiss()
            rewardAdShowCallBack.onAdClose()
        } else {
            rewardAdShowCallBack.onAdFailedToShow(AdError(1999, "ads requesting", ""))
        }
    }

    private fun showRewardInternal(
        activity: Activity,
        callback: RewardAdShowCallBack
    ) {
        AdmobFactory.Companion.INSTANCE
            .showRewardAd(
                activity,
                rewardAdValue!!,
                object : RewardAdCallBack {

                    override fun onAdClose() {
                        AdmobManager.adsFullScreenDismiss()
                        callback.onAdClose()
                        cancelLoadingJob()
                        dismissDialog()
                    }

                    override fun onUserEarnedReward(rewardItem: RewardItem?) {
                    }

                    override fun onRewardShow() {
                        cancelLoadingJob()
                        dismissDialog()
                        callback.onRewardShow()
                        adRewardState.emit(AdRewardState.Showed)
                    }


                    override fun onAdLoaded(data: ContentAd) {
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    }

                    override fun onAdClicked() {

                    }

                    override fun onAdImpression() {
                        cancelLoadingJob()
                        dismissDialog()
                        adRewardState.emit(AdRewardState.Showed)
                    }

                    override fun onAdFailedToShow(adError: AdError) {
                        cancelLoadingJob()
                        dismissDialog()
                        AdmobManager.adsFullScreenDismiss()
                        callback.onAdFailedToShow(adError)
                        adRewardState.emit(AdRewardState.ShowFail)

                    }
                })
    }

    private fun dismissDialog() {
        try {
            Log.e(TAG, "dismissDialog: wqe")
            val activity = loadingAdsDialog?.ownerActivity
            if (activity != null && !activity.isDestroyed && activity.windowManager != null) {
                Log.e(TAG, "dismissDialog: ffkkk")
                loadingAdsDialog?.dismiss()
                loadingAdsDialog = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "dismissDialog: ${e.message}")
        }
    }

    private fun showDialogLoading(context: Activity) {
        if (loadingAdsDialog == null) {
            loadingAdsDialog = LoadingAdsDialog(context)
        }
//        if (loadingAdsDialog?.isShowing == true) {
//            loadingAdsDialog?.dismiss()
//        }
        loadingAdsDialog?.setOwnerActivity(context)
        loadingAdsDialog?.show()
    }

    private fun requestValid(activity: Activity?): Boolean {
        val showConfigValid = (rewardAdConfig.showByTime == 1
                || requestShowCount % rewardAdConfig.showByTime ==
                if (rewardAdConfig.showByTime <= 2) {
                    1
                } else {
                    rewardAdConfig.showByTime - 1
                })
        val valueValid =
            (rewardAdValue == null
                    && (adRewardState != AdRewardState.Loading && adRewardState != AdRewardState.Loaded)
                    )
                    || adRewardState == AdRewardState.Showed
        return canRequestAds(activity) && showConfigValid && valueValid
    }

    companion object {
        private val TAG = RewardAdHelper::class.simpleName

        // Map to hold singleton instances with their associated IDs
        private val instances = mutableMapOf<String, RewardAdHelper>()

        // Method to get or create singleton instances
        @Synchronized
        fun getInstance(adPlacement: String): RewardAdHelper {
            return instances.getOrPut(adPlacement) { RewardAdHelper(adPlacement) }
        }
    }
}