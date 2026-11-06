package com.ads.admob.helper.adnative

import android.util.Log
import androidx.annotation.LayoutRes
import com.ads.admob.data.ContentAd
import com.ads.admob.helper.IAdsConfig
import com.ads.admob.helper.adnative.params.AdNativeMediation
import com.ads.admob.helper.adnative.params.NativeLayoutMediation
import com.google.android.gms.ads.nativead.NativeAd


class NativeAdConfig(
    override val idAds: String,
    val idAdsPriority: String? = null,
    override val canShowAds: Boolean,
    override val canReloadAds: Boolean,
    @LayoutRes val layoutId: Int,
    override val adPlacement: String,
    override val reloadIfFirstFail: Boolean = false,
    val refreshIntervalSeconds: Int = 0
) : IAdsConfig {
    /**
     * List of layouts associated with different ad mediation types.
     */
    var listLayoutByMediation: List<NativeLayoutMediation> = emptyList()
        private set

    /**
     * Set the layout mediation configurations.
     *
     * @param layoutMediation One or more [NativeLayoutMediation] instances representing layouts for different ad mediations.
     * @return Reference to this [NativeAdConfig] for method chaining.
     */
    fun setLayoutMediation(vararg layoutMediation: NativeLayoutMediation) = apply {
        this.listLayoutByMediation = layoutMediation.toList()
    }

    /**
     * Set the layout mediation configurations with a list.
     *
     * @param listLayoutMediation List of [NativeLayoutMediation] instances representing layouts for different ad mediations.
     * @return Reference to this [NativeAdConfig] for method chaining.
     */
    fun setLayoutMediation(listLayoutMediation: List<NativeLayoutMediation>) = apply {
        this.listLayoutByMediation = listLayoutMediation
    }

    /**
     * Get the layout ID based on the current mediation type of the native ad.
     *
     * If no specific layout is defined for the current mediation, it falls back to the default layout ID.
     *
     * @param nativeAd The [NativeAd] instance for which the layout ID is requested.
     * @return The layout ID associated with the current mediation type or the default layout ID.
     */
    @LayoutRes
    fun getLayoutIdByMediationNativeAd(nativeAd: ContentAd?): Int {
        if (nativeAd is ContentAd.AdmobAd.ApNativeAd){
            val listLayout = listLayoutByMediation

            return if (listLayout.isEmpty()) {
                layoutId
            } else {
                val currentMediation = AdNativeMediation.get(nativeAd.nativeAd)
                listLayout.find { currentMediation == it.mediationType }?.also {
                    Log.d("NativeAdHelper", "show with mediation ${it.mediationType.name}")
                }?.layoutId ?: layoutId
            }
        } else {
          return  layoutId
        }
    }
}
