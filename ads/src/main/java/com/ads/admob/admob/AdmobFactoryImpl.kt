package com.ads.admob.admob

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.annotation.IntDef
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustConfig
import com.adjust.sdk.LogLevel
import com.ads.admob.config.AfAdConfig
import com.ads.admob.config.AfAdjustConfig
import com.ads.admob.config.EventConfig
import com.ads.admob.data.ContentAd
import com.ads.admob.event.AdjustTrackingManager
import com.ads.admob.event.FacebookTrackingManager
import com.ads.admob.event.FirebaseTrackingManager
import com.ads.admob.helper.adnative.factory.admob.AdmobNativeFactory
import com.ads.admob.helper.appoppen.factory.AdmobAppOpenAdFactory
import com.ads.admob.helper.banner.factory.admob.AdmobBannerFactory
import com.ads.admob.helper.interstitial.factory.admob.AdmobInterstitialAdFactory
import com.ads.admob.helper.reward.factory.admob.AdmobRewardAdFactory
import com.ads.admob.listener.AdmobCallBack
import com.ads.admob.listener.AppOpenAdCallBack
import com.ads.admob.listener.BannerAdCallBack
import com.ads.admob.listener.InterstitialAdCallback
import com.ads.admob.listener.NativeAdCallback
import com.ads.admob.listener.RewardAdCallBack
import com.applovin.sdk.AppLovinPrivacySettings
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdapterResponseInfo
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.initialization.InitializationStatus
import com.google.android.gms.ads.rewarded.RewardItem
import com.mbridge.msdk.MBridgeConstans
import com.mbridge.msdk.out.MBridgeSDKFactory
import com.unity3d.ads.metadata.MetaData
import com.vungle.ads.VunglePrivacySettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AdmobFactoryImpl : AdmobFactory {

    private lateinit var afAdConfig: AfAdConfig
    private val TAG = AdmobFactoryImpl::class.simpleName
    private var isCancelRequestAndShowAllAds = false
    private var previousAdCloseTime = 0L

    override fun initAdmob(
        context: Application,
        afAdConfig: AfAdConfig,
        adCallback: AdmobCallBack
    ) {
        this.afAdConfig = afAdConfig
        CoroutineScope(Dispatchers.IO).launch {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val processName = Application.getProcessName()
                val packageName = context.packageName
                if (packageName != processName) {
                    WebView.setDataDirectorySuffix(processName)
                }
            }
            try {
                FirebaseTrackingManager.initialize(context)
            } catch (_: Exception) {
            }
            try {
                FacebookTrackingManager.initialize(context)
                afAdConfig.eventConfig?.let {
                    FacebookTrackingManager.getInstance().setEventConfig(
                        it
                    )
                }
            } catch (_: Exception) {
            }

            MobileAds.initialize(context) { initializationStatus: InitializationStatus ->
                val statusMap = initializationStatus.adapterStatusMap
                for (adapterClass in statusMap.keys) {
                    val status = statusMap[adapterClass]
                    Log.d(
                        TAG, String.format(
                            "Adapter name: %s, Description: %s, Latency: %d",
                            adapterClass, status!!.description, status.latency
                        )
                    )
                }
            }
            MobileAds.setRequestConfiguration(
                RequestConfiguration.Builder().setTestDeviceIds(afAdConfig.listDevices)
                    .build()
            )
            setupAdjust(context, afAdConfig.afAdjustConfig)
            initMediation(context)
            adCallback.initialized()
        }
    }

    override fun cancelRequestAndShowAllAds() {
        isCancelRequestAndShowAllAds = true
    }

    private fun setupAdjust(application: Application, adjustConfig: AfAdjustConfig) {
        val environment = if (adjustConfig.environmentProduct) {
            AdjustConfig.ENVIRONMENT_PRODUCTION
        } else {
            AdjustConfig.ENVIRONMENT_SANDBOX
        }
        val config = AdjustConfig(application, adjustConfig.adjustToken, environment)

        // Change the log level.
        config.setLogLevel(LogLevel.VERBOSE)
        config.setOnAttributionChangedListener { attribution ->
            Log.d(TAG, "Attribution callback called!")
            Log.d(TAG, "Attribution: $attribution")
        }

        // Set event success tracking delegate.
        config.setOnEventTrackingSucceededListener { eventSuccessResponseData ->
            Log.d(TAG, "Event success callback called!")
            Log.d(
                TAG,
                "Event success data: $eventSuccessResponseData"
            )
        }
        // Set event failure tracking delegate.
        config.setOnEventTrackingFailedListener { eventFailureResponseData ->
            Log.d(TAG, "Event failure callback called!")
            Log.d(
                TAG,
                "Event failure data: $eventFailureResponseData"
            )
        }

        // Set session success tracking delegate.
        config.setOnSessionTrackingSucceededListener { sessionSuccessResponseData ->
            Log.d(
                TAG,
                "Session success callback called!"
            )
            Log.d(
                TAG,
                "Session success data: $sessionSuccessResponseData"
            )
        }

        // Set session failure tracking delegate.
        config.setOnSessionTrackingFailedListener { sessionFailureResponseData ->
            Log.d(
                TAG,
                "Session failure callback called!"
            )
            Log.d(
                TAG,
                "Session failure data: $sessionFailureResponseData"
            )
        }
        application.registerActivityLifecycleCallbacks(AdjustLifecycleCallbacks())
        Adjust.initSdk(config)
    }

    private class AdjustLifecycleCallbacks : Application.ActivityLifecycleCallbacks {
        override fun onActivityResumed(activity: Activity) {
            Adjust.onResume()
        }

        override fun onActivityPaused(activity: Activity) {
            Adjust.onPause()
        }

        override fun onActivityStopped(activity: Activity) {
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        }

        override fun onActivityDestroyed(activity: Activity) {
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        }

        override fun onActivityStarted(activity: Activity) {
        }
    }

    override fun requestBannerAd(
        context: Context,
        adId: String,
        adPlacement: String,
        collapsibleGravity: String?,
        bannerInlineStyle: Int,
        useInlineAdaptive: Boolean,
        reloadIfFirstFail: Boolean,
        adCallback: BannerAdCallBack
    ) {
        if (isCancelRequestAndShowAllAds) {
            adCallback.onAdFailedToLoad(LoadAdError(99, "BannerAd cancel Request And Show All Ads", "", null, null))
            return
        }
        FirebaseTrackingManager.getInstance().logEvent("ad_banner_load")
        AdmobBannerFactory.getInstance()
            .requestBannerAd(
                context,
                adId,
                adPlacement,
                collapsibleGravity,
                bannerInlineStyle,
                useInlineAdaptive,
                object : BannerAdCallBack {
                    override fun onAdLoaded(data: ContentAd) {
                        adCallback.onAdLoaded(data)
                        FirebaseTrackingManager.getInstance().logEvent("ad_banner_loaded")

                        if (data is ContentAd.AdmobAd.ApBannerAd) {
                            data.adView.setOnPaidEventListener { adValue ->
                                val loadedAdapterResponseInfo: AdapterResponseInfo? =
                                    data.adView.responseInfo?.loadedAdapterResponseInfo
                                AdjustTrackingManager.pushTrackEventAdmob(adValue, loadedAdapterResponseInfo, adPlacement)
                                AdjustTrackingManager.pushTrackEvenAdjustRevenueAdMod(adValue, afAdConfig.afAdjustConfig.adRevenueKey)
                                AdjustTrackingManager.pushTrackEvenAdjustRevenueMintegral(adValue, afAdConfig.afAdjustConfig.adRevenueMintegralKey)
                                FacebookTrackingManager.getInstance().logPurchase(adValue.valueMicros / 1000000.0, adValue.currencyCode)
                                FacebookTrackingManager.getInstance().logAdImpression(adValue.valueMicros / 1000000.0, adValue.currencyCode)
                                FirebaseTrackingManager.getInstance()
                                    .logRevenue(adValue, adPlacement, loadedAdapterResponseInfo?.adapterClassName, adId, "banner")
                            }
                        }
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        if (reloadIfFirstFail) {
                            Log.d(TAG, "$adPlacement Fail 1")
                            requestBannerAd(context, adId, adPlacement, collapsibleGravity, bannerInlineStyle, useInlineAdaptive, false, adCallback)
                        } else {
                            Log.d(TAG, "$adPlacement Fail 2")
                            adCallback.onAdFailedToLoad(loadAdError)
                            FirebaseTrackingManager.getInstance()
                                .logEvent("ad_banner_load_failed")
                        }
                    }

                    override fun onAdClicked() {
                        adCallback.onAdClicked()
                        FirebaseTrackingManager.getInstance().logEvent("ad_banner_clicked")
                    }

                    override fun onAdImpression() {
                        adCallback.onAdImpression()
                        FirebaseTrackingManager.getInstance()
                            .logEvent("ad_banner_impression")
                    }

                    override fun onAdFailedToShow(adError: AdError) {
                        adCallback.onAdFailedToShow(adError)
                        FirebaseTrackingManager.getInstance()
                            .logEvent("ad_banner_show_fail")
                    }

                }
            )
    }

    override fun requestNativeAd(
        context: Context,
        adId: String,
        adPlacement: String,
        reloadIfFirstFail: Boolean,
        adCallback: NativeAdCallback
    ) {
        if (isCancelRequestAndShowAllAds) {
            adCallback.onAdFailedToLoad(LoadAdError(99, "ApNativeAd cancel Request And Show All Ads", "", null, null))
            return
        }
        FirebaseTrackingManager.getInstance().logEvent("ad_native_request")
        AdmobNativeFactory.getInstance().requestNativeAd(context, adId, object : NativeAdCallback {
            override fun populateNativeAd() {
                adCallback.populateNativeAd()
            }

            override fun onAdLoaded(data: ContentAd) {
                adCallback.onAdLoaded(data)
                FirebaseTrackingManager.getInstance().logEvent("ad_native_loaded")
                if (data is ContentAd.AdmobAd.ApNativeAd) {
                    data.nativeAd.setOnPaidEventListener { adValue ->
                        val loadedAdapterResponseInfo: AdapterResponseInfo? = data.nativeAd.responseInfo?.loadedAdapterResponseInfo
                        AdjustTrackingManager.pushTrackEventAdmob(adValue, loadedAdapterResponseInfo, adPlacement)
                        AdjustTrackingManager.pushTrackEvenAdjustRevenueAdMod(adValue, afAdConfig.afAdjustConfig.adRevenueKey)
                        AdjustTrackingManager.pushTrackEvenAdjustRevenueMintegral(adValue, afAdConfig.afAdjustConfig.adRevenueMintegralKey)
                        FacebookTrackingManager.getInstance().logPurchase(adValue.valueMicros / 1000000.0, adValue.currencyCode)
                        FacebookTrackingManager.getInstance().logAdImpression(adValue.valueMicros / 1000000.0, adValue.currencyCode)
                        FirebaseTrackingManager.getInstance()
                            .logRevenue(adValue, adPlacement, loadedAdapterResponseInfo?.adapterClassName, adId, "native")
                    }
                }
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                if (reloadIfFirstFail) {
                    Log.d(TAG, "$adPlacement Fail 1")
                    requestNativeAd(context, adId, adPlacement, false, adCallback)
                } else {
                    Log.d(TAG, "$adPlacement Fail 2")
                    adCallback.onAdFailedToLoad(loadAdError)
                    FirebaseTrackingManager.getInstance().logEvent("ad_native_load_failed")
                }
            }

            override fun onAdClicked() {
                adCallback.onAdClicked()
                FirebaseTrackingManager.getInstance().logEvent("ad_native_clicked")
            }

            override fun onAdImpression() {
                adCallback.onAdImpression()
                FirebaseTrackingManager.getInstance().logEvent("ad_native_impression")

            }

            override fun onAdFailedToShow(adError: AdError) {
                adCallback.onAdFailedToShow(adError)
                FirebaseTrackingManager.getInstance().logEvent("ad_native_show_fail")
            }

        })
    }

    override fun populateNativeAdView(
        context: Context,
        nativeAd: ContentAd,
        nativeAdViewId: Int,
        adPlaceHolder: FrameLayout,
        containerShimmerLoading: ShimmerFrameLayout?,
        adCallback: NativeAdCallback
    ) {
        if (isCancelRequestAndShowAllAds) {
            adCallback.onAdFailedToLoad(LoadAdError(99, "ApNativeAd cancel Request And Show All Ads", "", null, null))
            return
        }
        FirebaseTrackingManager.getInstance().logEvent("ad_native_call_show")
        when (nativeAd) {
            is ContentAd.AdmobAd.ApNativeAd -> {
                AdmobNativeFactory.getInstance().populateNativeAdView(
                    context,
                    nativeAd.nativeAd,
                    nativeAdViewId,
                    adPlaceHolder,
                    containerShimmerLoading,
                    adCallback
                )
            }

            else -> {
                adCallback.onAdFailedToShow(AdError(1999, "Ad Not support", ""))
            }
        }

    }

    override fun requestInterstitialAds(
        context: Context,
        adId: String,
        adPlacement: String,
        reloadIfFirstFail: Boolean,
        adCallback: InterstitialAdCallback
    ) {
        if (isCancelRequestAndShowAllAds) {
            adCallback.onAdFailedToLoad(LoadAdError(99, "ApInterstitialAd cancel Request And Show All Ads", "", null, null))
            return
        }
        FirebaseTrackingManager.getInstance().logEvent("ad_inter_request")
        Log.d(TAG, "load $adPlacement")
        AdmobInterstitialAdFactory.getInstance()
            .requestInterstitialAd(context, adId, adPlacement, object : InterstitialAdCallback {
                override fun onNextAction() {
                    adCallback.onNextAction()
                }

                override fun onAdClose() {
                    adCallback.onAdClose()
                    FirebaseTrackingManager.getInstance().logEvent("ad_inter_closed")
                }

                override fun onInterstitialShow() {
                    adCallback.onInterstitialShow()
                    FirebaseTrackingManager.getInstance().logEvent("ad_inter_open")
                }

                override fun onAdLoaded(data: ContentAd) {
                    Log.d(TAG, "load $adPlacement success")
                    FirebaseTrackingManager.getInstance().logEvent("ad_inter_loaded")
                    if (data is ContentAd.AdmobAd.ApInterstitialAd) {
                        adCallback.onAdLoaded(data)
                        data.interstitialAd.setOnPaidEventListener { adValue ->
                            val loadedAdapterResponseInfo: AdapterResponseInfo? =
                                data.interstitialAd.responseInfo.loadedAdapterResponseInfo
                            AdjustTrackingManager.pushTrackEventAdmob(adValue, loadedAdapterResponseInfo, adPlacement)
                            AdjustTrackingManager.pushTrackEvenAdjustRevenueAdMod(adValue, afAdConfig.afAdjustConfig.adRevenueKey)
                            AdjustTrackingManager.pushTrackEvenAdjustRevenueMintegral(adValue, afAdConfig.afAdjustConfig.adRevenueMintegralKey)
                            FacebookTrackingManager.getInstance().logPurchase(adValue.valueMicros / 1000000.0, adValue.currencyCode)
                            FacebookTrackingManager.getInstance().logAdImpression(adValue.valueMicros / 1000000.0, adValue.currencyCode)
                            FirebaseTrackingManager.getInstance()
                                .logRevenue(adValue, adPlacement, loadedAdapterResponseInfo?.adapterClassName, adId, "inter")
                        }
                    }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    if (reloadIfFirstFail) {
                        Log.d(TAG, "$adPlacement Fail 1")
                        requestInterstitialAds(context, adId, adPlacement, false, adCallback)
                    } else {
                        Log.d(TAG, "$adPlacement Fail 2")
                        adCallback.onAdFailedToLoad(loadAdError)
                        FirebaseTrackingManager.getInstance().logEvent("ad_inter_load_failed")
                    }
                }

                override fun onAdClicked() {
                    adCallback.onAdClicked()
                    FirebaseTrackingManager.getInstance().logEvent("ad_inter_clicked")
                }

                override fun onAdImpression() {
                    adCallback.onAdImpression()
                    FirebaseTrackingManager.getInstance().logEvent("ad_inter_impression")
                }

                override fun onAdFailedToShow(adError: AdError) {
                    adCallback.onAdFailedToShow(adError)
                    FirebaseTrackingManager.getInstance().logEvent("ad_inter_show_failed")
                }

            })
    }

    override fun showInterstitial(
        context: Context,
        interstitialAd: ContentAd?,
        adCallback: InterstitialAdCallback
    ) {
        if (isCancelRequestAndShowAllAds) {
            adCallback.onAdFailedToShow(AdError(99, "ApInterstitialAd cancel Request And Show All Ads", "", null))
            return
        }
        when (interstitialAd) {
            is ContentAd.AdmobAd.ApInterstitialAd -> {
                AdmobInterstitialAdFactory.getInstance()
                    .showInterstitial(context, interstitialAd.interstitialAd, object : InterstitialAdCallback {
                        override fun onNextAction() {
                            adCallback.onNextAction()
                        }

                        override fun onAdClose() {
                            previousAdCloseTime = System.currentTimeMillis()
                            adCallback.onAdClose()
                            FirebaseTrackingManager.getInstance().logEvent("ad_inter_closed")
                        }

                        override fun onInterstitialShow() {
                            adCallback.onInterstitialShow()
                            FirebaseTrackingManager.getInstance().logEvent("ad_inter_open")
                        }

                        override fun onAdLoaded(data: ContentAd) {
                            FirebaseTrackingManager.getInstance().logEvent("ad_inter_loaded")
                            adCallback.onAdLoaded(data)
                        }

                        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                            adCallback.onAdFailedToLoad(loadAdError)
                            FirebaseTrackingManager.getInstance().logEvent("ad_inter_load_failed")
                        }

                        override fun onAdClicked() {
                            adCallback.onAdClicked()
                            FirebaseTrackingManager.getInstance().logEvent("ad_inter_clicked")
                        }

                        override fun onAdImpression() {
                            adCallback.onAdImpression()
                            FirebaseTrackingManager.getInstance().logEvent("ad_inter_impression")
                        }

                        override fun onAdFailedToShow(adError: AdError) {
                            adCallback.onAdFailedToShow(adError)
                            FirebaseTrackingManager.getInstance().logEvent("ad_inter_show_failed")
                        }

                    })
            }

            else -> {
                adCallback.onAdFailedToShow(AdError(1999, "Ad Not support", ""))
            }
        }

    }

    override fun requestAppOpenAds(
        context: Context,
        adId: String,
        adPlacement: String,
        reloadIfFirstFail: Boolean,
        adCallback: AppOpenAdCallBack
    ) {
        if (isCancelRequestAndShowAllAds) {
            adCallback.onAdFailedToLoad(LoadAdError(99, "ApAppOpenAd cancel Request And Show All Ads", "", null, null))
            return
        }
        FirebaseTrackingManager.getInstance().logEvent("ad_app_open_request")
        Log.d(TAG, "load $adPlacement")
        AdmobAppOpenAdFactory.getInstance()
            .requestAppOpenAd(context, adId, adPlacement, object : AppOpenAdCallBack {
                override fun onAppOpenAdShow() {
                    adCallback.onAppOpenAdShow()
                    FirebaseTrackingManager.getInstance().logEvent("ad_app_open_open")
                }

                override fun onAppOpenAdClose() {
                    adCallback.onAppOpenAdClose()
                    FirebaseTrackingManager.getInstance().logEvent("ad_app_open_closed")
                }

                override fun onAdLoaded(data: ContentAd) {
                    Log.d(TAG, "load $adPlacement success")
                    FirebaseTrackingManager.getInstance().logEvent("ad_app_open_loaded")
                    if (data is ContentAd.AdmobAd.ApAppOpenAd) {
                        adCallback.onAdLoaded(data)
                        data.appOpenAd.setOnPaidEventListener { adValue ->
                            val loadedAdapterResponseInfo: AdapterResponseInfo? =
                                data.appOpenAd.responseInfo.loadedAdapterResponseInfo
                            AdjustTrackingManager.pushTrackEventAdmob(adValue, loadedAdapterResponseInfo, adPlacement)
                            AdjustTrackingManager.pushTrackEvenAdjustRevenueAdMod(adValue, afAdConfig.afAdjustConfig.adRevenueKey)
                            AdjustTrackingManager.pushTrackEvenAdjustRevenueMintegral(adValue, afAdConfig.afAdjustConfig.adRevenueMintegralKey)
                            FacebookTrackingManager.getInstance().logPurchase(adValue.valueMicros / 1000000.0, adValue.currencyCode)
                            FacebookTrackingManager.getInstance().logAdImpression(adValue.valueMicros / 1000000.0, adValue.currencyCode)
                            FirebaseTrackingManager.getInstance()
                                .logRevenue(adValue, adPlacement, loadedAdapterResponseInfo?.adapterClassName, adId, "app_open")
                        }
                    }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    if (reloadIfFirstFail) {
                        Log.d(TAG, "$adPlacement Fail 1")
                        requestAppOpenAds(context, adId, adPlacement, false, adCallback)
                    } else {
                        Log.d(TAG, "$adPlacement Fail 2")
                        adCallback.onAdFailedToLoad(loadAdError)
                        FirebaseTrackingManager.getInstance().logEvent("ad_app_open_load_failed")
                    }
                }

                override fun onAdClicked() {
                    adCallback.onAdClicked()
                    FirebaseTrackingManager.getInstance().logEvent("ad_app_open_clicked")
                }

                override fun onAdImpression() {
                    adCallback.onAdImpression()
                    FirebaseTrackingManager.getInstance().logEvent("ad_app_open_impression")
                }

                override fun onAdFailedToShow(adError: AdError) {
                    adCallback.onAdFailedToShow(adError)
                    FirebaseTrackingManager.getInstance().logEvent("ad_app_open_show_failed")
                }
            })
    }

    override fun showAppOpen(
        context: Context,
        appOpenAd: ContentAd?,
        adCallback: AppOpenAdCallBack
    ) {
        if (isCancelRequestAndShowAllAds) {
            adCallback.onAdFailedToShow(AdError(99, "ApAppOpenAd cancel Request And Show All Ads", "", null))
            return
        }
        when (appOpenAd) {
            is ContentAd.AdmobAd.ApAppOpenAd -> {
                AdmobAppOpenAdFactory.getInstance()
                    .showAppOpen(context, appOpenAd.appOpenAd, object : AppOpenAdCallBack {


                        override fun onAdLoaded(data: ContentAd) {
                            FirebaseTrackingManager.getInstance().logEvent("ad_app_open_loaded")
                            adCallback.onAdLoaded(data)
                        }

                        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                            adCallback.onAdFailedToLoad(loadAdError)
                            FirebaseTrackingManager.getInstance().logEvent("ad_app_open_load_failed")
                        }

                        override fun onAdClicked() {
                            adCallback.onAdClicked()
                            FirebaseTrackingManager.getInstance().logEvent("ad_app_open_clicked")
                        }

                        override fun onAdImpression() {
                            adCallback.onAdImpression()
                            FirebaseTrackingManager.getInstance().logEvent("ad_app_open_impression")
                        }

                        override fun onAdFailedToShow(adError: AdError) {
                            adCallback.onAdFailedToShow(adError)
                            FirebaseTrackingManager.getInstance().logEvent("ad_app_open_show_failed")
                        }

                        override fun onAppOpenAdShow() {
                            adCallback.onAppOpenAdShow()
                            FirebaseTrackingManager.getInstance().logEvent("ad_app_open_show")
                        }

                        override fun onAppOpenAdClose() {
                            adCallback.onAppOpenAdClose()
                            FirebaseTrackingManager.getInstance().logEvent("ad_app_open_closed")
                        }

                    })
            }

            else -> {
                adCallback.onAdFailedToShow(AdError(1999, "Ad Not support", ""))
            }
        }
    }

    override fun requestRewardAd(
        context: Context,
        adId: String,
        adPlacement: String,
        reloadIfFirstFail: Boolean,
        adCallback: RewardAdCallBack
    ) {
        if (isCancelRequestAndShowAllAds) {
            adCallback.onAdFailedToLoad(LoadAdError(99, "ApRewardAd cancel Request And Show All Ads", "", null, null))
            return
        }
        Log.d(TAG, "load $adPlacement")
        FirebaseTrackingManager.getInstance().logEvent("ad_reward_load")
        AdmobRewardAdFactory.getInstance()
            .requestRewardAd(context, adId, object : RewardAdCallBack {
                override fun onAdClose() {
                    adCallback.onAdClose()
                    FirebaseTrackingManager.getInstance().logEvent("ad_reward_closed")
                }

                override fun onUserEarnedReward(rewardItem: RewardItem?) {
                    adCallback.onUserEarnedReward(rewardItem)
                    FirebaseTrackingManager.getInstance().logEvent("ad_reward_user_earned")
                }

                override fun onRewardShow() {
                    adCallback.onRewardShow()
                    FirebaseTrackingManager.getInstance().logEvent("ad_reward_show")
                }

                override fun onAdLoaded(data: ContentAd) {
                    Log.d(TAG, "load $adPlacement success")
                    adCallback.onAdLoaded(data)
                    FirebaseTrackingManager.getInstance().logEvent("ad_reward_loaded")
                    if (data is ContentAd.AdmobAd.ApRewardAd) {
                        data.rewardAd.setOnPaidEventListener { adValue ->
                            val loadedAdapterResponseInfo: AdapterResponseInfo? =
                                data.rewardAd.responseInfo.loadedAdapterResponseInfo
                            AdjustTrackingManager.pushTrackEventAdmob(adValue, loadedAdapterResponseInfo, adPlacement)
                            AdjustTrackingManager.pushTrackEvenAdjustRevenueAdMod(adValue, afAdConfig.afAdjustConfig.adRevenueKey)
                            AdjustTrackingManager.pushTrackEvenAdjustRevenueMintegral(adValue, afAdConfig.afAdjustConfig.adRevenueMintegralKey)
                            FacebookTrackingManager.getInstance().logPurchase(adValue.valueMicros / 1000000.0, adValue.currencyCode)
                            FacebookTrackingManager.getInstance().logAdImpression(adValue.valueMicros / 1000000.0, adValue.currencyCode)
                            FirebaseTrackingManager.getInstance()
                                .logRevenue(adValue, adPlacement, loadedAdapterResponseInfo?.adapterClassName, adId, "reward")
                        }
                    }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    if (reloadIfFirstFail) {
                        Log.d(TAG, "$adPlacement Fail 1")
                        requestRewardAd(context, adId, adPlacement, false, adCallback)
                    } else {
                        Log.d(TAG, "$adPlacement Fail 2")
                        adCallback.onAdFailedToLoad(loadAdError)
                        FirebaseTrackingManager.getInstance().logEvent("ad_reward_load_failed")
                    }
                }

                override fun onAdClicked() {
                    adCallback.onAdClicked()
                    FirebaseTrackingManager.getInstance().logEvent("ad_reward_clicked")
                }

                override fun onAdImpression() {
                    adCallback.onAdImpression()
                    FirebaseTrackingManager.getInstance().logEvent("ad_reward_impression")
                }

                override fun onAdFailedToShow(adError: AdError) {
                    adCallback.onAdFailedToShow(adError)
                    FirebaseTrackingManager.getInstance().logEvent("ad_reward_show_failed")
                }
            })
    }

    override fun showRewardAd(
        activity: Activity,
        rewardedAd: ContentAd,
        adCallback: RewardAdCallBack
    ) {
        if (isCancelRequestAndShowAllAds) {
            adCallback.onAdFailedToShow(AdError(99, "ApRewardAd cancel Request And Show All Ads", "", null))
            return
        }
        when (rewardedAd) {
            is ContentAd.AdmobAd.ApRewardAd -> {
                AdmobRewardAdFactory.getInstance()
                    .showRewardAd(activity, rewardedAd.rewardAd, object : RewardAdCallBack {
                        override fun onAdClose() {
                            adCallback.onAdClose()
                            FirebaseTrackingManager.getInstance().logEvent("ad_mrec_closed")
                        }

                        override fun onUserEarnedReward(rewardItem: RewardItem?) {
                            adCallback.onUserEarnedReward(rewardItem)
                            FirebaseTrackingManager.getInstance().logEvent("ad_mrec_user_earned")
                        }

                        override fun onRewardShow() {
                            adCallback.onRewardShow()
                            FirebaseTrackingManager.getInstance().logEvent("ad_mrec_show")
                        }

                        override fun onAdLoaded(data: ContentAd) {
                            adCallback.onAdLoaded(data)
                            FirebaseTrackingManager.getInstance().logEvent("ad_mrec_load")
//                            if (data is ContentAd.AdmobAd.ApRewardAd) {
//                                data.rewardAd.setOnPaidEventListener { adValue ->
//                                    FacebookTrackingManager.getInstance().logPurchase(adValue.valueMicros / 1000000.0, adValue.currencyCode)
//                                    FacebookTrackingManager.getInstance().logAdImpression(adValue.valueMicros / 1000000.0, adValue.currencyCode)
//                                }
//                            }
                        }

                        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                            adCallback.onAdFailedToLoad(loadAdError)
                            FirebaseTrackingManager.getInstance().logEvent("ad_mrec_load_failed")
                        }

                        override fun onAdClicked() {
                            adCallback.onAdClicked()
                            FirebaseTrackingManager.getInstance().logEvent("ad_mrec_clicked")
                        }

                        override fun onAdImpression() {
                            adCallback.onAdImpression()
                            FirebaseTrackingManager.getInstance().logEvent("ad_mrec_impression")
                        }

                        override fun onAdFailedToShow(adError: AdError) {
                            adCallback.onAdFailedToShow(adError)
                            FirebaseTrackingManager.getInstance().logEvent("ad_mrec_show_failed")
                        }
                    })

            }

            else -> {
                adCallback.onAdFailedToShow(AdError(1999, "Ad Not support", ""))
            }
        }
    }

    override fun isShowAdsIntervalValid(): Boolean {
        return System.currentTimeMillis() - previousAdCloseTime > afAdConfig.intervalBetweenInterstitial
    }

    override fun getConfig(): AfAdConfig {
        return afAdConfig
    }

    override fun setEventConfig(eventConfig: EventConfig) {
        FacebookTrackingManager.getInstance().setEventConfig(eventConfig)
    }

    override fun initMediation(context: Context) {
        //Applovin
        AppLovinPrivacySettings.setDoNotSell(true, context)
        VunglePrivacySettings.setGDPRStatus(true, "v1.0.0")
        VunglePrivacySettings.setCCPAStatus(true)
        //Mintegral
        val sdk = MBridgeSDKFactory.getMBridgeSDK()
        sdk.setConsentStatus(context, MBridgeConstans.IS_SWITCH_ON)
        val mBridgeSDK = MBridgeSDKFactory.getMBridgeSDK()
        mBridgeSDK.setDoNotTrackStatus(false)
        //Unity
        val gdprMetaData = MetaData(context)
        gdprMetaData["gdpr.consent"] = true
        gdprMetaData.commit()
        val ccpaMetaData = MetaData(context)
        ccpaMetaData["privacy.consent"] = true
        ccpaMetaData.commit()
    }

    companion object {
        private val TAG = AdmobFactoryImpl::class.simpleName
    }
}

@IntDef(BannerInlineStyle.SMALL_STYLE, BannerInlineStyle.LARGE_STYLE)
annotation class BannerInlineStyle {
    companion object {
        const val SMALL_STYLE = 0
        const val LARGE_STYLE = 1
    }
}