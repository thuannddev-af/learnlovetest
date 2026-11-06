package com.ads.admob.helper.reward.factory.admob

import android.app.Activity
import android.content.Context
import com.ads.admob.listener.RewardAdCallBack
import com.ads.admob.listener.RewardInterAdCallBack
import com.google.android.gms.ads.rewarded.RewardedAd

interface AdmobRewardAdFactory {
    fun requestRewardAd(context: Context, adId: String, adCallback: RewardAdCallBack)
    fun requestRewardInterAd(context: Context, adId: String, adCallback: RewardInterAdCallBack)
    fun showRewardAd(
        activity: Activity,
        rewardedAd: RewardedAd,
        adCallback: RewardAdCallBack
    )

    companion object {
        @JvmStatic
        fun getInstance(): AdmobRewardAdFactoryImpl = AdmobRewardAdFactoryImpl()
    }
}