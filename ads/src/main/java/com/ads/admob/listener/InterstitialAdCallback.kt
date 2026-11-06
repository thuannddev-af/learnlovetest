package com.ads.admob.listener

import com.ads.admob.data.ContentAd


interface InterstitialAdCallback : AdCallback<ContentAd> {
    fun onNextAction()
    fun onAdClose()
    fun onInterstitialShow()
}