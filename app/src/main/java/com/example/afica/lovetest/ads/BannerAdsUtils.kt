package com.example.afica.lovetest.ads

import android.view.View
import android.util.Log
import android.widget.FrameLayout
import androidx.fragment.app.FragmentActivity
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
            setAdSize(AdSize.BANNER)
        }
        val observer = object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) = adView.resume()
            override fun onPause(owner: LifecycleOwner) = adView.pause()
            override fun onDestroy(owner: LifecycleOwner) = destroy(container)
        }
        banners[container] = {
            lifecycleOwner.lifecycle.removeObserver(observer)
            adView.adListener = object : AdListener() {}
            container.removeView(adView)
            adView.destroy()
            container.visibility = View.GONE
        }
        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                Log.d("BannerAds", "$placement loaded")
                container.visibility = View.VISIBLE
            }
            override fun onAdFailedToLoad(error: LoadAdError) {
                Log.e("BannerAds", "$placement failed: ${error.code} - ${error.message}")
                container.visibility = View.GONE
            }
        }
        container.addView(adView)
        lifecycleOwner.lifecycle.addObserver(observer)
        adView.loadAd(AdRequest.Builder().build())
    }

    fun destroy(container: FrameLayout) { banners.remove(container)?.invoke() }
}
