package com.example.afica.lovetest.ads

import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Lifecycle
import com.example.afica.lovetest.R
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
        ONBOARDING(AdsConfig.NATIVE_ONBOARDING_ID, RemoteConfigManager.NAITVE_ONBOARDING)
    }

    private val natives = mutableMapOf<FrameLayout, () -> Unit>()
    private val remoteConfig by lazy { RemoteConfigManager() }

    fun showNative(
        activity: FragmentActivity,
        container: FrameLayout,
        placement: Placement = Placement.LANGUAGE,
        lifecycleOwner: LifecycleOwner = activity
    ) {
        destroy(container)
        container.removeAllViews()
        container.visibility = View.GONE
        if (activity.isFinishing || activity.isDestroyed ||
            lifecycleOwner.lifecycle.currentState == Lifecycle.State.DESTROYED ||
            !remoteConfig.canShowAd(placement.configKey)) return

        var disposed = false
        var nativeAd: NativeAd? = null
        var nativeView: NativeAdView? = null
        val observer = object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) = destroy(container)
        }
        natives[container] = {
            disposed = true
            lifecycleOwner.lifecycle.removeObserver(observer)
            nativeAd?.destroy()
            nativeView?.destroy()
            container.removeAllViews()
            container.visibility = View.GONE
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        AdLoader.Builder(activity.applicationContext, placement.adId)
            .forNativeAd { ad ->
                if (disposed) {
                    ad.destroy()
                    return@forNativeAd
                }
                nativeAd = ad
                val view = activity.layoutInflater.inflate(R.layout.layout_native_ad, container, false) as NativeAdView
                nativeView = view
                view.headlineView = view.findViewById<TextView>(R.id.nativeHeadline).apply { text = ad.headline }
                view.bodyView = view.findViewById<TextView>(R.id.nativeBody).apply {
                    text = ad.body
                    visibility = if (ad.body == null) View.GONE else View.VISIBLE
                }
                view.callToActionView = view.findViewById<Button>(R.id.nativeAction).apply {
                    text = ad.callToAction
                    visibility = if (ad.callToAction == null) View.GONE else View.VISIBLE
                }
                view.iconView = view.findViewById<ImageView>(R.id.nativeIcon).apply {
                    setImageDrawable(ad.icon?.drawable)
                    visibility = if (ad.icon == null) View.GONE else View.VISIBLE
                }
                view.mediaView = view.findViewById<MediaView>(R.id.nativeMedia)
                view.setNativeAd(ad)
                container.addView(view)
                container.visibility = View.VISIBLE
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    if (!disposed) destroy(container)
                }
            })
            .build()
            .loadAd(AdRequest.Builder().build())
    }

    fun destroy(container: FrameLayout) { natives.remove(container)?.invoke() }
}
