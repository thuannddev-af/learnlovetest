package com.ads.admob.listener

import com.ads.admob.data.ContentAd


interface NativeAdCallback : AdCallback<ContentAd> {
    fun populateNativeAd()
}
