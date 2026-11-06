package com.ads.admob.helper.banner.params

import com.ads.admob.data.ContentAd


sealed class AdBannerState {
    object None : AdBannerState()
    object Fail : AdBannerState()
    object Loading : AdBannerState()
    object Cancel : AdBannerState()
    data class Loaded(val adBanner: ContentAd) : AdBannerState()
}