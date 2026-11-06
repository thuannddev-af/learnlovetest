package com.ads.admob.helper.banner

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.ads.admob.R
import com.ads.admob.admob.AdmobFactory
import com.ads.admob.admob.BannerInlineStyle
import com.ads.admob.data.ContentAd
import com.ads.admob.helper.AdsHelper
import com.ads.admob.helper.banner.params.AdBannerState
import com.ads.admob.helper.banner.params.BannerAdParam
import com.ads.admob.listener.BannerAdCallBack
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger


class BannerAdHelper(
    private val activity: Activity,
    private val lifecycleOwner: LifecycleOwner,
    private val config: BannerAdConfig,
) : AdsHelper<BannerAdConfig, BannerAdParam>(activity, lifecycleOwner, config) {
    private val TAG = BannerAdHelper::class.simpleName
    private val adBannerState: MutableStateFlow<AdBannerState> =
        MutableStateFlow(if (canRequestAds()) AdBannerState.None else AdBannerState.Fail)
    private var timeShowAdImpression: Long = 0
    private val listAdCallback: CopyOnWriteArrayList<BannerAdCallBack> = CopyOnWriteArrayList()
    private val resumeCount: AtomicInteger = AtomicInteger(0)
    private var shimmerLayoutView: ShimmerFrameLayout? = null
    private var bannerContentView: FrameLayout? = null
    private var isRequestValid = true

    var bannerAdView: ContentAd? = null
        private set

    init {
        registerAdListener(getDefaultCallback())
        lifecycleEventState.onEach {
            if (it == Lifecycle.Event.ON_CREATE) {
                if (!canRequestAds()) {
                    bannerContentView?.isVisible = false
                    shimmerLayoutView?.isVisible = false
                }
            }

            if (it == Lifecycle.Event.ON_RESUME) {
                if (!canShowAds() && isActiveState()) {
                    cancel()
                }
            }
        }.launchIn(lifecycleOwner.lifecycleScope)
        //Request when resume
        lifecycleEventState.debounce(300).onEach { event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                resumeCount.incrementAndGet()
                logZ("Resume repeat ${resumeCount.get()} times")
                if (!isActiveState()) {
                    logInterruptExecute("Request when resume")
                }
            }
            if (event == Lifecycle.Event.ON_RESUME && resumeCount.get() > 1 && bannerAdView != null && canRequestAds() && canReloadAd() && isActiveState()) {
                if (!isRequestValid) {
                    isRequestValid = true
                    return@onEach
                }
                logZ("requestAds on resume")
                requestAds(BannerAdParam.Request)
            }
        }.launchIn(lifecycleOwner.lifecycleScope)
        //for action resume or init
        adBannerState.onEach { adsParam ->
            logZ("dsadsadr24")
            handleShowAds(adsParam)
        }.launchIn(lifecycleOwner.lifecycleScope)
    }

    fun getBannerState(): Flow<AdBannerState> {
        return adBannerState.asStateFlow()
    }

    private fun handleShowAds(adsParam: AdBannerState) {
        bannerContentView?.isGone = adsParam is AdBannerState.Cancel || !canShowAds()
        shimmerLayoutView?.isVisible = adsParam is AdBannerState.Loading
        when (adsParam) {
            is AdBannerState.Loaded -> {
                val bannerContentView = bannerContentView
                val shimmerLayoutView = shimmerLayoutView
                if (bannerContentView != null && shimmerLayoutView != null) {
                    bannerContentView.setBackgroundColor(Color.WHITE)
                    val view = View(bannerContentView.context)
                    val oldHeight = bannerContentView.height
                    bannerContentView.let {
                        it.removeAllViews()
                        when (adsParam.adBanner) {
                            is ContentAd.AdmobAd.ApBannerAd -> {
                                if (!config.useInlineAdaptive && config.bannerInlineStyle == BannerInlineStyle.SMALL_STYLE) {
                                    it.addView(view, 0, oldHeight)
                                }
                                it.addView(adsParam.adBanner.adView)
                            }

                            else -> {
                                invokeAdListener {
                                    it.onAdFailedToShow(AdError(1999, "Ad not support", ""))
                                }
                            }
                        }
                    }
                }
            }

            else -> Unit
        }
    }

    override fun requestAds(param: BannerAdParam) {
        logZ("requestAds with param:${param::class.java.simpleName}")
        if (canRequestAds()) {
            lifecycleOwner.lifecycleScope.launch {
                when (param) {
                    is BannerAdParam.Request -> {
                        flagActive.compareAndSet(false, true)
                        if (bannerAdView == null) {
                            adBannerState.emit(AdBannerState.Loading)
                        }
                        loadBannerAd()
                    }

                    is BannerAdParam.Ready -> {
                        flagActive.compareAndSet(false, true)
                        adBannerState.emit(AdBannerState.Loaded(param.bannerAds))
                    }

                    is BannerAdParam.Clickable -> {
                        if (isActiveState()) {
                            if (timeShowAdImpression + param.minimumTimeKeepAdsDisplay < System.currentTimeMillis()) {
                                loadBannerAd()
                            }
                        } else {
                            logInterruptExecute("requestAds Clickable")
                        }
                    }
                }
            }
        } else {
            if (!isOnline() && bannerAdView == null) {
                cancel()
            }
        }
    }

    override fun cancel() {
        logZ("cancel() called")
        flagActive.compareAndSet(true, false)
        bannerAdView = null
        bannerContentView?.isVisible = false
        lifecycleOwner.lifecycleScope.launch { adBannerState.emit(AdBannerState.Cancel) }
    }

    private fun loadBannerAd() {
        if (canRequestAds()) {
            if (config.idAdsPriority != null) {
                requestAdsAlternate(activity, config.idAdsPriority, config.idAds, invokeListenerAdCallback())
            } else {
                AdmobFactory.INSTANCE
                    .requestBannerAd(
                        activity,
                        config.idAds,
                        config.adPlacement,
                        config.collapsibleGravity,
                        config.bannerInlineStyle,
                        config.useInlineAdaptive,
                        config.reloadIfFirstFail,
                        invokeListenerAdCallback()
                    )
            }
        }
    }

    private fun requestAdsAlternate(
        activity: Context,
        idAdPriority: String,
        idAdNormal: String,
        bannerAdCallBack: BannerAdCallBack
    ) {
        Log.e(TAG, "requestAdsAlternate: ")
        AdmobFactory.INSTANCE
            .requestBannerAd(
                activity,
                idAdPriority,
                config.adPlacement,
                config.collapsibleGravity,
                config.bannerInlineStyle,
                config.useInlineAdaptive,
                false,
                object : BannerAdCallBack {
                    override fun onAdLoaded(data: ContentAd) {
                        Log.e(TAG, " requestAdsAlternate onAdLoaded: Priority ")
                        bannerAdCallBack.onAdLoaded(data)
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        Log.e(TAG, " requestAdsAlternate onAdFailedToLoad: Priority ")
                        AdmobFactory.INSTANCE
                            .requestBannerAd(
                                activity,
                                idAdNormal,
                                config.adPlacement,
                                config.collapsibleGravity,
                                config.bannerInlineStyle,
                                config.useInlineAdaptive,
                                config.reloadIfFirstFail,
                                object : BannerAdCallBack {
                                    override fun onAdLoaded(data: ContentAd) {
                                        Log.e(TAG, " requestAdsAlternate onAdLoaded: normal ")
                                        bannerAdCallBack.onAdLoaded(data)
                                    }

                                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                                        Log.e(TAG, " requestAdsAlternate onAdFailedToLoad: normal ")
                                        bannerAdCallBack.onAdFailedToLoad(loadAdError)
                                    }

                                    override fun onAdClicked() {
                                        bannerAdCallBack.onAdClicked()
                                    }

                                    override fun onAdImpression() {
                                        bannerAdCallBack.onAdImpression()
                                    }

                                    override fun onAdFailedToShow(adError: AdError) {
                                        bannerAdCallBack.onAdFailedToShow(adError)
                                    }

                                }
                            )
                    }

                    override fun onAdClicked() {
                        bannerAdCallBack.onAdClicked()
                    }

                    override fun onAdImpression() {
                        bannerAdCallBack.onAdImpression()
                    }

                    override fun onAdFailedToShow(adError: AdError) {
                        bannerAdCallBack.onAdFailedToShow(adError)
                    }

                }
            )
    }

    fun setShimmerLayoutView(shimmerLayoutView: ShimmerFrameLayout) = apply {
        kotlin.runCatching {
            this.shimmerLayoutView = shimmerLayoutView
            if (lifecycleOwner.lifecycle.currentState in Lifecycle.State.CREATED..Lifecycle.State.RESUMED) {
                if (!canRequestAds()) {
                    shimmerLayoutView.isVisible = false
                }
            }
        }
    }

    fun setBannerContentView(nativeContentView: FrameLayout) = apply {
        kotlin.runCatching {
            this.bannerContentView = nativeContentView
            this.shimmerLayoutView =
                nativeContentView.findViewById(R.id.shimmer_container_banner)
            if (lifecycleOwner.lifecycle.currentState in Lifecycle.State.CREATED..Lifecycle.State.RESUMED) {
                if (!canRequestAds()) {
                    nativeContentView.isVisible = false
                    shimmerLayoutView?.isVisible = false
                }
            }
        }
    }

    private fun getDefaultCallback(): BannerAdCallBack {
        return object : BannerAdCallBack {
            override fun onAdLoaded(data: ContentAd) {
                if (isActiveState()) {
                    lifecycleOwner.lifecycleScope.launch {
                        bannerAdView = data
                        adBannerState.emit(AdBannerState.Loaded(data))
                    }
                    logZ("onBannerLoaded()")
                } else {
                    logInterruptExecute("onBannerLoaded")
                }
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                if (isActiveState()) {
                    lifecycleOwner.lifecycleScope.launch {
                        adBannerState.emit(AdBannerState.Fail)
                    }
                    logZ("onAdFailedToLoad()")
                } else {
                    logInterruptExecute("onAdFailedToLoad")
                }
            }

            override fun onAdClicked() {
                invokeAdListener { it.onAdClicked() }
            }

            override fun onAdImpression() {
                isRequestValid = lifecycleOwner.lifecycle.currentState == Lifecycle.State.RESUMED
                invokeAdListener { it.onAdImpression() }
            }

            override fun onAdFailedToShow(adError: AdError) {
                invokeAdListener { it.onAdFailedToShow(adError) }
            }

        }
    }

    fun registerAdListener(adCallback: BannerAdCallBack) {
        this.listAdCallback.add(adCallback)
    }

    fun unregisterAdListener(adCallback: BannerAdCallBack) {
        this.listAdCallback.remove(adCallback)
    }

    fun unregisterAllAdListener() {
        this.listAdCallback.clear()
    }

    private fun invokeListenerAdCallback(): BannerAdCallBack {
        return object : BannerAdCallBack {
            override fun onAdLoaded(data: ContentAd) {
                invokeAdListener { it.onAdLoaded(data) }
                logZ("onAdLoaded")
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                invokeAdListener { it.onAdFailedToLoad(loadAdError) }
                logZ("onAdFailedToLoad: ${loadAdError.message}")
            }

            override fun onAdClicked() {
                invokeAdListener { it.onAdClicked() }
                logZ("onAdClicked")
            }

            override fun onAdImpression() {
                invokeAdListener { it.onAdImpression() }
                logZ("onAdImpression")
            }

            override fun onAdFailedToShow(adError: AdError) {
                invokeAdListener { it.onAdFailedToShow(adError) }
                logZ("onAdFailedToShow: ${adError.message}")
            }
        }
    }

    private fun invokeAdListener(action: (adCallback: BannerAdCallBack) -> Unit) {
        listAdCallback.forEach(action)
    }
}