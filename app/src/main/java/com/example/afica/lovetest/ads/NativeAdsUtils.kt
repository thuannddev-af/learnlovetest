package com.example.afica.lovetest.ads

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.example.afica.lovetest.R
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

object NativeAdsUtils {
    enum class Placement(val adId: String, val configKey: String) {
        LANGUAGE(AdsConfig.NATIVE_LANGUAGE_ID, RemoteConfigManager.NATIVE_LANGUAGE),
        ONBOARDING(AdsConfig.NATIVE_ONBOARDING_ID, RemoteConfigManager.NATIVE_ONBOARDING),
        ONBOARDING_SECOND(AdsConfig.NATIVE_ONBOARDING_ID, RemoteConfigManager.NATIVE_ONBOARDING),
        ONBOARDING_INLINE_2(AdsConfig.NATIVE_ONBOARDING_INLINE_2_ID, RemoteConfigManager.NATIVE_ONBOARDING),
        ONBOARDING_INLINE_3(AdsConfig.NATIVE_ONBOARDING_INLINE_3_ID, RemoteConfigManager.NATIVE_ONBOARDING)
    }

    private val natives = mutableMapOf<FrameLayout, () -> Unit>()
    private val remoteConfig by lazy { RemoteConfigManager() }

    private class Preload {
        var ad: NativeAd? = null
        val listeners = mutableListOf<(NativeAd?) -> Unit>()
    }

    private val preloads = mutableMapOf<Placement, Preload>()
    private val cacheHandler = Handler(Looper.getMainLooper())

    fun preloadNative(context: Context, placement: Placement = Placement.LANGUAGE) {
        if (!remoteConfig.canShowAd(placement.configKey) || preloads.containsKey(placement)) return
        val request = Preload()
        preloads[placement] = request
        AdLoader.Builder(context.applicationContext, placement.adId)
            .forNativeAd { ad ->
                if (preloads[placement] !== request) {
                    ad.destroy()
                    return@forNativeAd
                }
                val listeners = request.listeners.toList()
                request.listeners.clear()
                if (listeners.isEmpty()) {
                    request.ad = ad
                    cacheHandler.postDelayed({
                        if (preloads[placement] === request) {
                            preloads.remove(placement)
                            request.ad?.destroy()
                        }
                    }, 55 * 60 * 1000L)
                } else {
                    preloads.remove(placement)
                    listeners.first()(ad)
                    listeners.drop(1).forEach { it(null) }
                }
            }
            .withAdListener(object : AdListener() {
                override fun onAdClicked() { AppOpenAdsUtils.skipNextResume() }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    if (preloads[placement] !== request) return
                    preloads.remove(placement)
                    val listeners = request.listeners.toList()
                    request.listeners.clear()
                    listeners.forEach { it(null) }
                }
            })
            .build().loadAd(AdRequest.Builder().build())
    }

    fun showNativeLanguage(
        activity: FragmentActivity,
        container: FrameLayout,
        lifecycleOwner: LifecycleOwner = activity,
        onFailed: () -> Unit = {}
    ) = showNative(activity, container, Placement.LANGUAGE, lifecycleOwner, onFailed = onFailed)

    fun showNativeOnboarding(
        activity: FragmentActivity,
        container: FrameLayout,
        page: Int = 2,
        lifecycleOwner: LifecycleOwner = activity,
        onFailed: () -> Unit = {}
    ) {
        require(page in 2..3) { "Inline native is available on onboarding page 2 or 3" }
        val placement = if (page == 2) Placement.ONBOARDING_INLINE_2 else Placement.ONBOARDING_INLINE_3
        showNative(activity, container, placement, lifecycleOwner,
            nativeLayout = if (page == 2) R.layout.layout_native_ad_view_type4 else R.layout.layout_native_ad,
            onFailed = onFailed)
    }

    fun showNativeOnboardingFullscreen(
        activity: FragmentActivity,
        container: FrameLayout,
        slot: Int = 1,
        lifecycleOwner: LifecycleOwner = activity,
        onFailed: () -> Unit = {}
    ) {
        val placement = if (slot == 1) Placement.ONBOARDING else Placement.ONBOARDING_SECOND
        showNative(
            activity,
            container,
            placement,
            lifecycleOwner,
            fullscreen = true,
            onFailed = onFailed
        )
    }

    private fun showNative(
        activity: FragmentActivity,
        container: FrameLayout,
        placement: Placement = Placement.LANGUAGE,
        lifecycleOwner: LifecycleOwner = activity,
        fullscreen: Boolean = false,
        nativeLayout: Int? = null,
        onFailed: () -> Unit = {}
    ) {
        destroy(container)
        container.removeAllViews()
        container.visibility = View.GONE
        if (activity.isFinishing || activity.isDestroyed ||
            lifecycleOwner.lifecycle.currentState == Lifecycle.State.DESTROYED ||
            !remoteConfig.canShowAd(placement.configKey)
        ) {
            onFailed()
            return
        }

        var disposed = false
        var nativeAd: NativeAd? = null
        var nativeView: NativeAdView? = null
        var loadingView: ShimmerFrameLayout? = null
        var pendingRequest: Preload? = null
        var pendingListener: ((NativeAd?) -> Unit)? = null
        val observer = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                loadingView?.startShimmer()
            }

            override fun onStop(owner: LifecycleOwner) {
                loadingView?.stopShimmer()
            }

            override fun onDestroy(owner: LifecycleOwner) = destroy(container)
        }
        natives[container] = {
            disposed = true
            pendingListener?.let { pendingRequest?.listeners?.remove(it) }
            lifecycleOwner.lifecycle.removeObserver(observer)
            loadingView?.stopShimmer()
            loadingView = null
            nativeAd?.destroy()
            nativeView?.destroy()
            container.removeAllViews()
            container.visibility = View.GONE
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        val displayAd: (NativeAd?) -> Unit = display@{ ad ->
            if (ad == null) {
                if (!disposed) {
                    destroy(container)
                    onFailed()
                }
                return@display
            }
            if (disposed) {
                ad.destroy()
                return@display
            }
            nativeAd = ad
            val layout =
                nativeLayout ?: if (fullscreen) R.layout.layout_native_ad_fullscreen else R.layout.layout_native_ad
            val type4 = layout == R.layout.layout_native_ad_view_type4
            val view = activity.layoutInflater.inflate(layout, container, false) as NativeAdView
            nativeView = view
            view.headlineView =
                view.findViewById<TextView>(if (type4) R.id.ad_headline else R.id.nativeHeadline).apply { text = ad.headline }
            view.bodyView = view.findViewById<TextView>(if (type4) R.id.ad_body else R.id.nativeBody).apply {
                text = ad.body
                visibility = if (ad.body == null) View.GONE else View.VISIBLE
            }
            view.callToActionView = view.findViewById<TextView>(if (type4) R.id.ad_call_to_action else R.id.nativeAction).apply {
                text = ad.callToAction
                visibility = if (ad.callToAction == null) View.GONE else View.VISIBLE
            }
            view.iconView = view.findViewById<ImageView>(if (type4) R.id.ad_app_icon else R.id.nativeIcon).apply {
                setImageDrawable(ad.icon?.drawable)
                visibility = if (ad.icon == null) View.GONE else View.VISIBLE
            }
            view.mediaView = view.findViewById<MediaView>(if (type4) R.id.ad_media else R.id.nativeMedia)
            view.setNativeAd(ad)
            loadingView?.stopShimmer()
            loadingView = null
            container.removeAllViews()
            container.addView(view)
            container.visibility = View.VISIBLE
        }
        preloadNative(activity.applicationContext, placement)
        val request = preloads[placement] ?: run {
            destroy(container)
            onFailed()
            return
        }
        val cachedAd = request.ad
        if (cachedAd != null) {
            preloads.remove(placement)
            request.ad = null
            displayAd(cachedAd)
        } else {
            val loadingLayout = when {
                nativeLayout == R.layout.layout_native_ad_view_type4 -> R.layout.loading_native_ad_view_type4
                fullscreen -> R.layout.loading_native_fullscreen_view
                else -> R.layout.layout_native_ads_type1
            }
            val shimmer = activity.layoutInflater.inflate(
                loadingLayout, container, false
            ) as ShimmerFrameLayout
            loadingView = shimmer
            container.addView(shimmer)
            container.visibility = View.VISIBLE
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                shimmer.startShimmer()
            }
            pendingRequest = request
            pendingListener = displayAd
            request.listeners.add(displayAd)
        }
    }

    fun destroy(container: FrameLayout) {
        natives.remove(container)?.invoke()
    }
}
