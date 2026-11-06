package com.ads.admob.listener

import com.google.android.gms.ads.AdError

interface AppOpenAdShowCallBack {
    fun onNextAction()
    fun onAdClose()
    fun onAppOpenShow()
    fun onAdClicked()
    fun onAdImpression()
    fun onAdFailedToShow(adError: AdError)
}