package com.ads.admob.listener

import com.ads.admob.data.ContentAd


interface AppOpenAdCallBack : AdCallback<ContentAd> {
    fun onAppOpenAdShow()
    fun onAppOpenAdClose()
}