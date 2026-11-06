package com.ads.admob.listener

import com.ads.admob.data.ContentAd
import com.google.android.gms.ads.rewarded.RewardItem

interface RewardAdCallBack : AdCallback<ContentAd> {
    fun onAdClose()
    fun onUserEarnedReward(rewardItem: RewardItem?)
    fun onRewardShow()
}