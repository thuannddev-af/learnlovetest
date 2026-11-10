package com.example.afica.lovetest.ads

import android.view.View
import android.view.Gravity
import android.view.ViewGroup
import android.util.Log
import android.widget.FrameLayout
import androidx.fragment.app.FragmentActivity
import androidx.core.view.doOnLayout
import com.example.afica.lovetest.R
import com.facebook.shimmer.ShimmerFrameLayout
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Lifecycle
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

object BannerAdsUtils {
    enum class Placement(val adId: String, val configKey: String) {
        BANNER_HOME(AdsConfig.BANNER_HOME_ID, RemoteConfigManager.BANNER_HOME_ENABLE),
        BANNER_MEMORY(AdsConfig.BANNER_MEMORY_ID, RemoteConfigManager.BANNER_MEMORY_ENABLE)
    }

    private val banners = mutableMapOf<FrameLayout, () -> Unit>()
    private val remoteConfig by lazy { RemoteConfigManager() }

    fun showBanner(
        activity: FragmentActivity,
        container: FrameLayout,
        placement: Placement,
        lifecycleOwner: LifecycleOwner = activity
    ) {
        destroy(container)
        container.removeAllViews()
        container.visibility = View.GONE
        if (activity.isFinishing || activity.isDestroyed ||
            lifecycleOwner.lifecycle.currentState == Lifecycle.State.DESTROYED) return
        if (!remoteConfig.canShowAd(placement.configKey)) {
            Log.d("BannerAds", "$placement disabled: ads_enable=${remoteConfig.isAdsEnable()}, " +
                "${placement.configKey}=${remoteConfig.getBoolean(placement.configKey)}")
            return
        }
        Log.d("BannerAds", "$placement loading")

        val adView = AdView(activity).apply {
            adUnitId = placement.adId
            visibility = View.GONE
        }
        val shimmer = activity.layoutInflater.inflate(
            R.layout.layout_banner_control, container, false
        ) as ShimmerFrameLayout
        var disposed = false
        val observer = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) { shimmer.startShimmer() }
            override fun onStop(owner: LifecycleOwner) { shimmer.stopShimmer() }
            override fun onResume(owner: LifecycleOwner) = adView.resume()
            override fun onPause(owner: LifecycleOwner) = adView.pause()
            override fun onDestroy(owner: LifecycleOwner) = destroy(container)
        }
        banners[container] = {
            disposed = true
            shimmer.stopShimmer()
            lifecycleOwner.lifecycle.removeObserver(observer)
            adView.adListener = object : AdListener() {}
            container.removeView(adView)
            container.removeView(shimmer)
            adView.destroy()
            container.visibility = View.GONE
        }
        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                if (disposed) return
                Log.d("BannerAds", "$placement loaded")
                shimmer.stopShimmer()
                container.removeView(shimmer)
                adView.visibility = View.VISIBLE
                container.visibility = View.VISIBLE
            }
            override fun onAdFailedToLoad(error: LoadAdError) {
                if (disposed) return
                Log.e("BannerAds", "$placement failed: ${error.code} - ${error.message}")
                shimmer.stopShimmer()
                container.removeView(shimmer)
                container.visibility = View.GONE
            }
        }
        container.addView(shimmer)
        container.addView(adView, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER
        ))
        container.visibility = View.VISIBLE
        lifecycleOwner.lifecycle.addObserver(observer)

        //set ads banner ngang màn hình
        container.doOnLayout {
            if (disposed) return@doOnLayout
            val widthPixels = container.width - container.paddingLeft - container.paddingRight
            val widthDp = (widthPixels / container.resources.displayMetrics.density).toInt()
            if (widthDp <= 0) {
                destroy(container)
                return@doOnLayout
            }
            adView.setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, widthDp))
            adView.loadAd(AdRequest.Builder().build())
        }
    }

    fun destroy(container: FrameLayout) { banners.remove(container)?.invoke() }
}
