package com.ads.admob

import android.app.Activity
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import androidx.annotation.IntDef
import androidx.annotation.StringDef
import com.ads.admob.config.NetworkProvider
import com.applovin.mediation.MaxError
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.LoadAdError

const val MAX_SMALL_INLINE_BANNER_HEIGHT = 50
fun getAdRequest(): AdRequest {
    val builder = AdRequest.Builder()
    return builder.build()
}

fun getCollapsibleAdRequest(type: String): AdRequest {
    try {
        val builder = AdRequest.Builder()
        builder.addNetworkExtrasBundle(AdMobAdapter::class.java, Bundle().apply {
            putString("collapsible", type)
        })
        return builder.build()
    } catch (ex: Exception) {
        return getAdRequest()
    }
}

fun getAdSize(
    mActivity: Activity,
    useInlineAdaptive: Boolean,
    inlineStyle: Int
): AdSize {

    // Step 2 - Determine the screen width (less decorations) to use for the ad width.
    val display = mActivity.windowManager.defaultDisplay
    val outMetrics = DisplayMetrics()
    display.getMetrics(outMetrics)
    val widthPixels = outMetrics.widthPixels.toFloat()
    val density = outMetrics.density
    val adWidth = (widthPixels / density).toInt()

    // Step 3 - Get adaptive ad size and return for setting on the ad view.
    return if (useInlineAdaptive) {
        if (inlineStyle == BannerInlineStyle.LARGE_STYLE) {
            Log.e("TAG", "getAdSize: 2121")
            AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(
                mActivity,
                adWidth
            )
        } else {
            Log.e("TAG", "getAdSize: ")
            AdSize.getInlineAdaptiveBannerAdSize(
                adWidth,
                MAX_SMALL_INLINE_BANNER_HEIGHT
            )
        }
    } else AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
        mActivity,
        adWidth
    )
}

@IntDef(BannerInlineStyle.SMALL_STYLE, BannerInlineStyle.LARGE_STYLE)
annotation class BannerInlineStyle {
    companion object {
        const val SMALL_STYLE = 0
        const val LARGE_STYLE = 1
    }
}

@StringDef(BannerCollapsibleGravity.BOTTOM, BannerCollapsibleGravity.TOP)
annotation class BannerCollapsibleGravity {
    companion object {
        const val BOTTOM = "bottom"
        const val TOP = "top"
    }
}

@IntDef(RewardType.NORMAL, RewardType.INTERSTITIAL)
annotation class RewardType {
    companion object {
        const val NORMAL = 0
        const val INTERSTITIAL = 1
    }
}

fun MaxError.toLoadAdError() = run { LoadAdError(this.code, this.message, "", null, null) }
fun MaxError.toAdError() = run { AdError(this.code, this.message, "") }

fun String.idToNetworkProvider() : Int = run {
    val admobPattern = Regex("""^ca-app-pub-\d{16}/\d{10}$""")
    val applovinPattern = Regex("""^[a-zA-Z0-9]{16}$""")
    when {
        admobPattern.matches(this) -> NetworkProvider.ADMOB
        applovinPattern.matches(this) -> NetworkProvider.MAX
        else -> 0
    }
}