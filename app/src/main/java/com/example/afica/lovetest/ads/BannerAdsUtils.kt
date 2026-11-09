package com.example.afica.lovetest.ads

import android.view.View
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
    private val banners = mutableMapOf<FrameLayout, () -> Unit>()
    private val remoteConfig by lazy { RemoteConfigManager() }

    fun showBanner(
        activity: FragmentActivity,
        container: FrameLayout,
        lifecycleOwner: LifecycleOwner = activity
    ) {
        destroy(container)
        container.removeAllViews()
        container.visibility = View.GONE
        if (activity.isFinishing || activity.isDestroyed ||
            lifecycleOwner.lifecycle.currentState == Lifecycle.State.DESTROYED ||
            !remoteConfig.canShowAd(RemoteConfigManager.BANNER_HOME_ENABLE)) return

        val adView = AdView(activity).apply {
            adUnitId = AdsConfig.BANNER_HOME_ID
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
            override fun onAdLoaded() { container.visibility = View.VISIBLE }
            override fun onAdFailedToLoad(error: LoadAdError) { container.visibility = View.GONE }
        }
        container.addView(adView)
        lifecycleOwner.lifecycle.addObserver(observer)
        adView.loadAd(AdRequest.Builder().build())
    }

    fun destroy(container: FrameLayout) { banners.remove(container)?.invoke() }
}
