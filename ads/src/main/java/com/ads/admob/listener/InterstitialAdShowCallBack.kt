package com.ads.admob.listener

import com.ads.admob.data.ContentAd
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError

interface InterstitialAdShowCallBack {
    fun onNextAction()
    fun onAdClose()
    fun onInterstitialShow()
    fun onAdClicked()
    fun onAdImpression()
    fun onAdFailedToShow(adError: AdError)
}