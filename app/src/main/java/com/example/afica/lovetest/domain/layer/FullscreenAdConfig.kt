package com.example.afica.lovetest.domain.layer

import com.ads.admob.helper.adnative.NativeAdHelper

data class FullscreenAdConfig(
    val tag: String,
    val afterNonFullscreenCount: Int,
    val adHelper: NativeAdHelper?
)
