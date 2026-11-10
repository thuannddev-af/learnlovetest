package com.example.afica.lovetest.ads

import android.app.Activity
import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.ads.admob.data.ContentAd
import com.ads.admob.helper.appoppen.AppOpenAdConfig
import com.ads.admob.helper.appoppen.AppOpenAdsHelper
import com.ads.admob.listener.AppOpenAdRequestCallBack
import com.ads.admob.listener.AppOpenAdShowCallBack
import com.example.afica.lovetest.utils.Logger
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError

object AppOpenAdsUtils {

    fun loadAppOpenAds(
        activity: Activity, idAdsPriority: String? = null, idAds: String, adPlacement: String, isEnable: Boolean, isLoadAndShow: Boolean = false,
        onAdLoaded: (() -> Unit)? = null
    ) {
        AppOpenAdsHelper.getInstance(adPlacement).setAppOpenAdConfig(
            AppOpenAdConfig(
                idAdsPriority = idAdsPriority,
                idAds = idAds,
                canShowAds = isEnable,
                canReloadAds = false,
                adPlacement = adPlacement,
                reloadIfFirstFail = true
            )
        )
        loadAppOpen(activity, adPlacement, isLoadAndShow, onAdLoaded)
    }


    fun showAppOpenAds(activity: Activity, lifecycleOwner: LifecycleOwner, adPlacement: String, isReloadWhenClose: Boolean = false, action: () -> Unit) {
        AppOpenAdsHelper.getInstance(adPlacement)
            .forceShowAppOpen(activity, lifecycleOwner, object : AppOpenAdShowCallBack {
                override fun onNextAction() {
                    Logger.e("AppOpenAdsUtils onNextAction")
                    checkReloadAppOpen(activity, adPlacement, isReloadWhenClose)
                    action.invoke()
                }

                override fun onAdClose() {
                    Logger.e("AppOpenAdsUtils onAdClose")
                }

                override fun onAppOpenShow() {
                    Logger.e("AppOpenAdsUtils onAppOpenShow $adPlacement")
                }

                override fun onAdClicked() {
                }

                override fun onAdImpression() {
                }

                override fun onAdFailedToShow(adError: AdError) {
                    Logger.e("AppOpenAdsUtils onAdFailedToShow ${adError.message}")
                    checkReloadAppOpen(activity, adPlacement, isReloadWhenClose)
                    action.invoke()
                }

            })
    }

    fun loadAndShowAppOpenAds(
        activity: Activity,
        lifecycleOwner: LifecycleOwner,
        idAds: String,
        adPlacement: String,
        isEnable: Boolean,
        action: () -> Unit
    ) {
        loadAppOpenAds(activity = activity, idAds = idAds, adPlacement = adPlacement, isEnable = isEnable, isLoadAndShow = true, onAdLoaded = {
            showAppOpenAds(activity, lifecycleOwner, adPlacement, action = action)
        })
    }

    private fun loadAppOpen(activity: Activity, adPlacement: String, isLoadAndShow: Boolean = false, onAdLoaded: (() -> Unit)? = null) {
        AppOpenAdsHelper.getInstance(adPlacement).requestAppOpenAds(activity, isLoadAndShow, object : AppOpenAdRequestCallBack {
            override fun onAdLoaded(data: ContentAd) {
                Logger.e("$adPlacement : onAdLoaded")
                onAdLoaded?.invoke()
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                Logger.e("$adPlacement : onAdFailedToLoad: ${loadAdError.message}")
            }

        })
    }

    private fun checkReloadAppOpen(activity: Activity, adPlacement: String, isReloadWhenClose: Boolean) {
        if (isReloadWhenClose) {
            loadAppOpen(activity, adPlacement)
        }
    }

    fun cancelAllAds(context: Context, isPurchased: Boolean) {
        AppOpenAdsHelper.getInstance(AdPlacement.APP_OPEN_MAIN).cancelRequestAndShowAllAds(isPurchased)
    }
}
