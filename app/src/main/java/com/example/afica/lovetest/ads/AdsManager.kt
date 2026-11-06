package com.example.afica.lovetest.ads

import android.app.Activity
import android.view.View
import android.widget.FrameLayout
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import android.util.Log
class AdsManager(
    private val remoteConfigManager: RemoteConfigManager
) {

    private var bannerHomeAdView: AdView? = null
    private var interstitialSplashAd: InterstitialAd? = null
    private var destroyed = false

    fun loadBannerHome(
        activity: Activity,
        container: FrameLayout
    ) {
        destroyBannerHome()
        container.removeAllViews()
        container.visibility = View.GONE

        // Kiểm tra Firebase Remote Config
        if (!remoteConfigManager.canShowAd(
                RemoteConfigManager.BANNER_HOME_ENABLE
            )
        ) {
            return
        }

        // Tạo Banner Ads
        val adView = AdView(activity).apply {
            adUnitId = AdsConfig.BANNER_HOME_ID
            setAdSize(AdSize.BANNER)
        }
        bannerHomeAdView = adView

        // Lắng nghe trạng thái quảng cáo
        adView.adListener = object : AdListener() {

            override fun onAdLoaded() {
                if (bannerHomeAdView === adView) {
                    container.visibility = View.VISIBLE
                }
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                if (bannerHomeAdView === adView) {
                    container.visibility = View.GONE
                }
            }
        }
        container.addView(adView)
        adView.loadAd(
            AdRequest.Builder().build()
        )
    }

    fun loadInter(
        activity: Activity,
        onComplete: (Boolean) -> Unit
    ) {
        if (destroyed) return
        interstitialSplashAd = null
        if (!remoteConfigManager.canShowAd(
                RemoteConfigManager.INTERSTITIAL_SPLASH_ENABLE
        )) {
            onComplete(false)
            return
        }
        InterstitialAd.load(
            activity,
            AdsConfig.INTERSTITIAL_SPLASH_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    if (destroyed) return
                    Log.d("SplashAds", "Interstitial tải thành công")

                    interstitialSplashAd = ad
                    onComplete(true)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    if (destroyed) return
                    Log.e(
                        "SplashAds",
                        "Interstitial tải thất bại: ${error.code} - ${error.message}"
                    )
                    interstitialSplashAd = null
                    onComplete(false)
                }
            }
        )
    }

    fun showInter(
        activity: Activity,
        onComplete: () -> Unit
    ) {
        if (!remoteConfigManager.canShowAd(
                RemoteConfigManager.INTERSTITIAL_SPLASH_ENABLE
        )){
            onComplete()
            return
        }
        val ad = interstitialSplashAd

        if (ad == null) {
            onComplete()
            return
        }

        interstitialSplashAd = null

        ad.fullScreenContentCallback =
            object : FullScreenContentCallback() {

                override fun onAdDismissedFullScreenContent() {
                    onComplete()
                }

                override fun onAdFailedToShowFullScreenContent(
                    error: AdError
                ) {
                    Log.e("SplashAds", "Interstitial không thể hiển thị: ${error.code} - ${error.message}")
                    onComplete()
                }
            }

        ad.show(activity)
    }


    fun destroyBannerHome() {
        bannerHomeAdView?.destroy()
        bannerHomeAdView = null
    }

    fun destroy() {
        destroyed = true
        interstitialSplashAd?.fullScreenContentCallback = null
        interstitialSplashAd = null
        destroyBannerHome()
    }
}
