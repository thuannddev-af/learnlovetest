package com.ads.admob.helper.appoppen.factory

import android.app.Activity
import android.content.Context
import android.util.Log
import com.ads.admob.data.ContentAd
import com.ads.admob.event.FirebaseTrackingManager
import com.ads.admob.getAdRequest
import com.ads.admob.helper.appoppen.AppOpenAdManager
import com.ads.admob.helper.appoppen.AppOpenAdManager.Companion.TAG
import com.ads.admob.listener.AppOpenAdCallBack
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd


class AdmobAppOpenAdFactoryImpl : AdmobAppOpenAdFactory {
    companion object {
        val TAG = AdmobAppOpenAdFactoryImpl::class.simpleName
    }

    override fun requestAppOpenAd(
        context: Context,
        adId: String,
        adPlacement: String,
        adCallback: AppOpenAdCallBack
    ) {
        AppOpenAd.load(
            context,
            adId,
            getAdRequest(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    adCallback.onAdFailedToLoad(adError)
                }

                override fun onAdLoaded(ad: AppOpenAd) {
                    adCallback.onAdLoaded(ContentAd.AdmobAd.ApAppOpenAd(ad))
                }
            }
        )
    }

    override fun showAppOpen(
        context: Context,
        appOpenAd: AppOpenAd?,
        adCallback: AppOpenAdCallBack
    ) {
        if (appOpenAd == null) {
            adCallback.onAppOpenAdClose()
            return
        }
        appOpenAd.fullScreenContentCallback =
            object : FullScreenContentCallback() {
                /** Called when full screen content is dismissed. */
                override fun onAdDismissedFullScreenContent() {
                    // Set the reference to null so isAdAvailable() returns false.
                    FirebaseTrackingManager.getInstance().logEvent("ad_open_closed")
                    adCallback.onAppOpenAdClose()
                    Log.d(TAG, "onAdDismissedFullScreenContent.")
                }

                /** Called when fullscreen content failed to show. */
                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    FirebaseTrackingManager.getInstance().logEvent("ad_open_show_failed")
                    adCallback.onAdFailedToShow(adError)
                    Log.d(TAG, "onAdFailedToShowFullScreenContent: " + adError.message)
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
        appOpenAd.show(context as Activity)
    }
}