package com.ads.admob.helper.adnative

import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class NativeAdapterConfig private constructor(
    val nativeAdId: String,
    val adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>,
    @LayoutRes val itemNativeAd: Int,
    @LayoutRes val nativeContentView: Int,
    val firstPositionNativeApp: Int,
    val gridLayoutManager: GridLayoutManager,
    val adItemInterval: Int,
    val isRepeat: Boolean = false,
    val forceReloadAdOnBind: Boolean = false,
    val adPlacement: String,
    val reloadIfFirstFail: Boolean = false
) {
    class Builder(
        val nativeAdId: String,
        private val adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>,
        @LayoutRes val itemNativeAd: Int,
        @LayoutRes val nativeContentView: Int,
        private val firstPositionNativeApp: Int,
        private val gridLayoutManager: GridLayoutManager,
        private val adItemInterval: Int,
        private val isRepeat: Boolean = false,
        private val forceReloadAdOnBind: Boolean = false,
        private val adPlacement: String ,
        private val reloadIfFirstFail: Boolean = false
    ) {
        fun build() = NativeAdapterConfig(
            nativeAdId,
            adapter,
            itemNativeAd,
            nativeContentView,
            firstPositionNativeApp,
            gridLayoutManager,
            adItemInterval,
            isRepeat,
            forceReloadAdOnBind,
            adPlacement,
            reloadIfFirstFail
        )
    }
}