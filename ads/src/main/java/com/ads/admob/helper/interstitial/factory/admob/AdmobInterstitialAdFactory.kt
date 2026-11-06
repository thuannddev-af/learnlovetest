package com.ads.admob.helper.interstitial.factory.admob

import android.content.Context
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.ads.admob.listener.InterstitialAdCallback

interface AdmobInterstitialAdFactory {
    fun requestInterstitialAd(context: Context, adId: String,  adPlacement: String, adCallback: InterstitialAdCallback)
    fun showInterstitial(
        context: Context,
        interstitialAd: InterstitialAd?,
        adCallback: InterstitialAdCallback
    )

    companion object {
        @JvmStatic
        fun getInstance(): AdmobInterstitialAdFactory = AdmobInterstitialAdFactoryImpl()
    }
}