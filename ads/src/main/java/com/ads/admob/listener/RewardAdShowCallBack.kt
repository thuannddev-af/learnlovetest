package com.ads.admob.listener

import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.rewarded.RewardItem

interface RewardAdShowCallBack {
    fun onAdClose()
    fun onAdClicked() {}
    fun onAdImpression() {}
    fun onAdFailedToShow(adError: AdError)
    fun onUserEarnedReward(rewardItem: RewardItem?) {}
    fun onRewardShow() {}
}