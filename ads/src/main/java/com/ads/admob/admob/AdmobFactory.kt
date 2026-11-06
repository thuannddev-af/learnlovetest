package com.ads.admob.admob

import android.app.Activity
import android.app.Application
import android.content.Context
import android.widget.FrameLayout
import androidx.annotation.LayoutRes
import com.ads.admob.BannerInlineStyle
import com.ads.admob.config.AfAdConfig
import com.ads.admob.config.EventConfig
import com.ads.admob.data.ContentAd
import com.ads.admob.listener.AdmobCallBack
import com.ads.admob.listener.AppOpenAdCallBack
import com.ads.admob.listener.BannerAdCallBack
import com.ads.admob.listener.InterstitialAdCallback
import com.ads.admob.listener.NativeAdCallback
import com.ads.admob.listener.RewardAdCallBack
import com.facebook.shimmer.ShimmerFrameLayout


interface AdmobFactory {
    fun initAdmob(context: Application, afAdConfig: AfAdConfig, adCallback: AdmobCallBack)
    fun cancelRequestAndShowAllAds()

    fun requestBannerAd(
        context: Context,
        adId: String,
        adPlacement: String,
        collapsibleGravity: String? = null,
        bannerInlineStyle: Int = BannerInlineStyle.SMALL_STYLE,
        useInlineAdaptive: Boolean = false,
        reloadIfFirstFail: Boolean = false,
        adCallback: BannerAdCallBack
    )

    fun requestNativeAd(context: Context, adId: String, adPlacement: String, reloadIfFirstFail: Boolean = false, adCallback: NativeAdCallback)

    fun populateNativeAdView(
        activity: Context,
        nativeAd: ContentAd,
        @LayoutRes nativeAdViewId: Int,
        adPlaceHolder: FrameLayout,
        containerShimmerLoading: ShimmerFrameLayout?,
        adCallback: NativeAdCallback
    )

    fun requestInterstitialAds(context: Context, adId: String, adPlacement: String,reloadIfFirstFail: Boolean = false, adCallback: InterstitialAdCallback)

    fun showInterstitial(
        context: Context,
        interstitialAd: ContentAd?,
        adCallback: InterstitialAdCallback
    )

    fun requestAppOpenAds(context: Context, adId: String, adPlacement: String,reloadIfFirstFail: Boolean = false, adCallback: AppOpenAdCallBack)

    fun showAppOpen(
        context: Context,
        appOpenAd: ContentAd?,
        adCallback: AppOpenAdCallBack
    )

    fun requestRewardAd(context: Context, adId: String, adPlacement: String,reloadIfFirstFail: Boolean = false, adCallback: RewardAdCallBack)
    fun showRewardAd(
        activity: Activity,
        rewardedAd: ContentAd,
        adCallback: RewardAdCallBack
    )

    fun isShowAdsIntervalValid(): Boolean

    fun getConfig(): AfAdConfig
    fun setEventConfig(eventConfig: EventConfig)
    fun initMediation(context: Context)

    companion object {
        val INSTANCE: AdmobFactory by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { AdmobFactoryImpl() }
    }
}