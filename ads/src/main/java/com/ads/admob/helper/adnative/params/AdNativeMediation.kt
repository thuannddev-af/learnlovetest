package com.ads.admob.helper.adnative.params

import com.google.ads.mediation.admob.AdMobAdapter
import com.google.ads.mediation.applovin.AppLovinMediationAdapter
import com.google.ads.mediation.facebook.FacebookMediationAdapter
import com.google.android.gms.ads.nativead.NativeAd

/**
 * Created by KO Huyn on 26/12/2023.
 */
/**
 * Enum class representing different mediation platforms for native ads.
 *
 * @property clazz The corresponding mediation adapter class for each platform.
 */
enum class AdNativeMediation(val clazz: Class<*>) {
    ADMOB(AdMobAdapter::class.java),
    FACEBOOK(FacebookMediationAdapter::class.java),
    APPLOVIN(AppLovinMediationAdapter::class.java);

    companion object {
        /**
         * Gets the [AdNativeMediation] based on the provided [NativeAd].
         *
         * @param nativeAd The native ad from which to extract mediation information.
         * @return The corresponding [AdNativeMediation] or null if not found.
         */
        fun get(nativeAd: NativeAd): AdNativeMediation? {
            val adapterClassName = nativeAd.responseInfo?.mediationAdapterClassName ?: return null
            return values().find { adapterClassName.contains(it.clazz.simpleName) }
        }
    }
}
