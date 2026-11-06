package com.ads.admob.helper.reward.params

import com.ads.admob.data.ContentAd
import com.ads.admob.helper.params.IAdsParam
import com.google.android.gms.ads.rewarded.RewardedAd


sealed class RewardAdParam : IAdsParam {
    data class Show(val rewardedAd: ContentAd) : RewardAdParam()
    object ShowAd : RewardAdParam()
    object Request : RewardAdParam() {
        @JvmStatic
        fun create(): Request {
            return this
        }
    }

    data class Clickable(
        val minimumTimeKeepAdsDisplay: Long
    ) : RewardAdParam()
}