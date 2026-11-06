package com.ads.admob.helper.appoppen.factory

import android.content.Context
import com.ads.admob.listener.AppOpenAdCallBack
import com.google.android.gms.ads.appopen.AppOpenAd

interface AdmobAppOpenAdFactory {
    fun requestAppOpenAd(context: Context, adId: String, adPlacement: String, adCallback: AppOpenAdCallBack)
    fun showAppOpen(
        context: Context,
        appOpenAd: AppOpenAd?,
        adCallback: AppOpenAdCallBack
    )

    companion object {
        @JvmStatic
        fun getInstance(): AdmobAppOpenAdFactory = AdmobAppOpenAdFactoryImpl()
    }
}