package com.ads.admob.helper.interstitial.params

import com.google.android.gms.ads.interstitial.InterstitialAd


sealed class AdInterstitialState {
    object None : AdInterstitialState()
    object Fail : AdInterstitialState()
    object Loading : AdInterstitialState()
    object Loaded : AdInterstitialState()
    object ShowFail : AdInterstitialState()
    object Showed : AdInterstitialState()
    object Cancel : AdInterstitialState()
    class Show(val interstitialAd: InterstitialAd) : AdInterstitialState()
}