package com.ads.admob.helper.appoppen

import android.app.Activity
import android.content.Context
import android.util.Log
import com.ads.admob.admob.AdmobFactory
import com.ads.admob.data.ContentAd
import com.ads.admob.event.AdjustTrackingManager
import com.ads.admob.event.FacebookTrackingManager
import com.ads.admob.event.FirebaseTrackingManager
import com.ads.admob.listener.AppOpenAdCallBack
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdapterResponseInfo
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import java.util.Date


class AppOpenAdManager {
    companion object {
        val TAG = AppOpenAdManager::class.simpleName
    }

    private var appOpenAd: ContentAd? = null
    private var isLoadingAd = false
    var isShowingAd = false
    private var adUnitId = ""
    private var appResumeAdConfig: AppResumeAdConfig? = null
    fun setAppResumeConfig(adConfig: AppResumeAdConfig) {
        appResumeAdConfig = adConfig
    }

    private var appOpenAdCallBack: AppOpenAdCallBack? = null
    fun setAdUnitId(id: String) {
        this.adUnitId = id
    }

    fun registerLister(appOpenAdCallBack: AppOpenAdCallBack) {
        this.appOpenAdCallBack = appOpenAdCallBack
    }

    private var isCancelRequestAndShowAllAds = false
    fun cancelRequestAndShowAllAds(isPurchased: Boolean) {
        isCancelRequestAndShowAllAds = isPurchased
    }

    /** Keep track of the time an app open ad is loaded to ensure you don't show an expired ad. */
    private var loadTime: Long = 0

    /**
     * Load an ad.
     *
     * @param context the context of the activity that loads the ad
     */
    fun loadAd(context: Context) {
        // Do not load ad if there is an unused ad or one is already loading.
        if (isLoadingAd || isAdAvailable() || appResumeAdConfig?.canShowAds == false) {
            Log.e(TAG, "loadAd: invalid")
            return
        }
        Log.d(TAG, "request AOA: ")
        isLoadingAd = true
        FirebaseTrackingManager.getInstance().logEvent("ad_open_request")
        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            context,
            adUnitId,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                /**
                 * Called when an app open ad has loaded.
                 *
                 * @param ad the loaded app open ad.
                 */
                override fun onAdLoaded(ad: AppOpenAd) {
                    FirebaseTrackingManager.getInstance().logEvent("ad_open_loaded")
                    appOpenAdCallBack?.onAdLoaded(ContentAd.AdmobAd.ApAppResumeAd(ad))
                    appOpenAd = ContentAd.AdmobAd.ApAppOpenAd(ad)
                    isLoadingAd = false
                    loadTime = Date().time
                    Log.e(TAG, "onAdLoaded: ")

                    try {
                        ad.setOnPaidEventListener {
                            val loadedAdapterResponseInfo: AdapterResponseInfo? =
                                ad.responseInfo.loadedAdapterResponseInfo
                            AdjustTrackingManager.pushTrackEventAdmob(it, loadedAdapterResponseInfo, appResumeAdConfig?.adPlacement)
                            AdjustTrackingManager.pushTrackEvenAdjustRevenueAdMod(it, AdmobFactory.INSTANCE.getConfig().afAdjustConfig.adRevenueKey)
                            AdjustTrackingManager.pushTrackEvenAdjustRevenueMintegral(
                                it,
                                AdmobFactory.INSTANCE.getConfig().afAdjustConfig.adRevenueMintegralKey
                            )
                            FacebookTrackingManager.getInstance().logPurchase(it.valueMicros / 1000000.0, it.currencyCode)
                            FacebookTrackingManager.getInstance().logAdImpression(it.valueMicros / 1000000.0, it.currencyCode)
                            FirebaseTrackingManager.getInstance()
                                .logRevenue(
                                    it,
                                    appResumeAdConfig?.adPlacement,
                                    loadedAdapterResponseInfo?.adapterClassName,
                                    appResumeAdConfig?.idAds,
                                    "app_open"
                                )
                        }
                    } catch (_: Exception) {
                    }


                }

                /**
                 * Called when an app open ad has failed to load.
                 *
                 * @param loadAdError the error.
                 */
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    FirebaseTrackingManager.getInstance().logEvent("ad_open_load_failed")
                    isLoadingAd = false
                    appOpenAdCallBack?.onAdFailedToLoad(loadAdError)
                    Log.d(TAG, "onAdFailedToLoad: " + loadAdError.message)
                }
            }
        )
    }

    /** Check if ad was loaded more than n hours ago. */
    private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
        val dateDifference: Long = Date().time - loadTime
        val numMilliSecondsPerHour: Long = 3600000
        return dateDifference < numMilliSecondsPerHour * numHours
    }

    /** Check if ad exists and can be shown. */
    fun isAdAvailable(): Boolean {
        // Ad references in the app open beta will time out after four hours, but this time limit
        // may change in future beta versions. For details, see:
        // https://support.google.com/admob/answer/9341964?hl=en
        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4) && !isCancelRequestAndShowAllAds
    }

    /**
     * Show the ad if one isn't already showing.
     *
     * @param activity the activity that shows the app open ad
     * @param onShowAdCompleteListener the listener to be notified when an app open ad is complete
     */
    fun showAdIfAvailable(activity: Activity, adCallback: AppOpenAdCallBack) {
        Log.e(TAG, "showAdIfAvailable: ")
        // If the app open ad is already showing, do not show the ad again.
        if (isShowingAd) {
            Log.d(TAG, "The app open ad is already showing.")
            return
        }

        // If the app open ad is not available yet, invoke the callback.
        if (!isAdAvailable()) {
            Log.d(TAG, "The app open ad is not ready yet.")
            loadAd(activity)
            return
        }
        FirebaseTrackingManager.getInstance().logEvent("ad_open_call_show")

        Log.d(TAG, "Will show ad.")
        when (appOpenAd) {
            is ContentAd.AdmobAd.ApAppOpenAd -> {
                (appOpenAd as ContentAd.AdmobAd.ApAppOpenAd).appOpenAd.fullScreenContentCallback =
                    object : FullScreenContentCallback() {
                        /** Called when full screen content is dismissed. */
                        override fun onAdDismissedFullScreenContent() {
                            // Set the reference to null so isAdAvailable() returns false.
                            FirebaseTrackingManager.getInstance().logEvent("ad_open_closed")
                            appOpenAd = null
                            isShowingAd = false
                            adCallback.onAppOpenAdClose()
                            Log.d(TAG, "onAdDismissedFullScreenContent.")
                            loadAd(activity)
                        }

                        /** Called when fullscreen content failed to show. */
                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            FirebaseTrackingManager.getInstance().logEvent("ad_open_show_failed")
                            appOpenAd = null
                            isShowingAd = false
                            adCallback.onAdFailedToShow(adError)
                            Log.d(TAG, "onAdFailedToShowFullScreenContent: " + adError.message)
                            loadAd(activity)
                        }

                        /** Called when fullscreen content is shown. */
                        override fun onAdShowedFullScreenContent() {
                            FirebaseTrackingManager.getInstance().logEvent("ad_open_open")
                            adCallback.onAppOpenAdShow()
                            Log.d(TAG, "onAdShowedFullScreenContent.")
                        }

                        override fun onAdClicked() {
                            super.onAdClicked()
                            adCallback.onAdClicked()
                            FirebaseTrackingManager.getInstance().logEvent("ad_open_clicked")
                        }

                        override fun onAdImpression() {
                            super.onAdImpression()
                            adCallback.onAdImpression()
                            FirebaseTrackingManager.getInstance().logEvent("ad_open_impression")
                        }
                    }
                isShowingAd = true
                (appOpenAd as ContentAd.AdmobAd.ApAppOpenAd).appOpenAd.show(activity)
            }

            else -> {
                Log.d(TAG, "Not Show Ads")
            }
        }
    }

    fun destroyAds(){
        appOpenAd = null
        isLoadingAd = false
        isShowingAd = false
        loadTime = 0L

        isCancelRequestAndShowAllAds = false
        appOpenAdCallBack = null
    }
}
