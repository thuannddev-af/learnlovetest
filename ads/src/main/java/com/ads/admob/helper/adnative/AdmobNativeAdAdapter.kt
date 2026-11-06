package com.ads.admob.helper.adnative

import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ads.admob.R
import com.ads.admob.admob.AdmobFactory
import com.ads.admob.data.ContentAd
import com.ads.admob.listener.NativeAdCallback
import com.ads.admob.widget.RecyclerViewAdapterWrapper
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError
import kotlin.math.roundToInt


class AdmobNativeAdAdapter(private val nativeAdapterConfig: NativeAdapterConfig) :
    RecyclerViewAdapterWrapper(nativeAdapterConfig.adapter) {
    private var nativeData: ContentAd? = null
    init {
        setSpanAds()
    }

    private fun convertAdPosition2OrgPosition(position: Int): Int {
        return if (nativeAdapterConfig.isRepeat) {
            position - (position + (nativeAdapterConfig.adItemInterval - nativeAdapterConfig.firstPositionNativeApp)) / (nativeAdapterConfig.adItemInterval + 1)
        } else {
            if (itemCount <= nativeAdapterConfig.firstPositionNativeApp) {
                0
            } else {
                val positionAds =
                    ((nativeAdapterConfig.firstPositionNativeApp + position + 1) / (nativeAdapterConfig.adapter.itemCount + 1).toFloat()).roundToInt()
                if (position > positionAds && positionAds < itemCount) {
                    (position) - positionAds
                } else {
                    (position) - ((nativeAdapterConfig.firstPositionNativeApp + position) / (nativeAdapterConfig.adapter.itemCount + 1).toFloat()).roundToInt()
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (isAdPosition(position)) {
            TYPE_FB_NATIVE_ADS
        } else super.getItemViewType(
            convertAdPosition2OrgPosition(position)
        )
    }

    private fun isAdPosition(position: Int): Boolean {
        return if (nativeAdapterConfig.isRepeat) {
            if (position <= nativeAdapterConfig.firstPositionNativeApp) {
                position == nativeAdapterConfig.firstPositionNativeApp
            } else {
                (position - nativeAdapterConfig.firstPositionNativeApp) % (nativeAdapterConfig.adItemInterval + 1) == 0
            }
        } else {
            if (itemCount < nativeAdapterConfig.firstPositionNativeApp) {
                false
            } else {
                position == nativeAdapterConfig.firstPositionNativeApp
            }
        }
    }

    override fun getItemCount(): Int {
        val realCount = super.getItemCount()
        val count = if (nativeAdapterConfig.isRepeat) {
            val interval = nativeAdapterConfig.adItemInterval
            if (interval <= 0) realCount
            else realCount + (realCount / interval.toFloat()).roundToInt()
        } else {
            if (realCount <= nativeAdapterConfig.firstPositionNativeApp) {
                realCount
            } else {
                realCount + 1
            }
        }
        return count
    }

    private fun onBindAdViewHolder(holder: RecyclerView.ViewHolder) {
        val adHolder = holder as AdViewHolder
        if (nativeAdapterConfig.forceReloadAdOnBind || !adHolder.loaded) {
            AdmobFactory.INSTANCE.requestNativeAd(
                holder.itemView.context,
                nativeAdapterConfig.nativeAdId,
                nativeAdapterConfig.adPlacement,
                nativeAdapterConfig.reloadIfFirstFail,
                object : NativeAdCallback {
                    override fun populateNativeAd() {

                    }

                    override fun onAdLoaded(data: ContentAd) {
                        nativeData = data
                        AdmobFactory.INSTANCE.populateNativeAdView(
                            holder.itemView.context,
                            data,
                            nativeAdapterConfig.nativeContentView,
                            holder.nativeContentView,
                            holder.shimmerLayoutView,
                            object : NativeAdCallback {
                                override fun populateNativeAd() {

                                }

                                override fun onAdLoaded(data: ContentAd) {
                                }

                                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                                }

                                override fun onAdClicked() {
                                }

                                override fun onAdImpression() {
                                }

                                override fun onAdFailedToShow(adError: AdError) {
                                }

                            }
                        )
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    }

                    override fun onAdClicked() {
                    }

                    override fun onAdImpression() {
                    }

                    override fun onAdFailedToShow(adError: AdError) {
                    }

                })
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == TYPE_FB_NATIVE_ADS) {
            onBindAdViewHolder(holder)
        } else {
            super.onBindViewHolder(holder, convertAdPosition2OrgPosition(position))
        }
    }

    private fun onCreateAdViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val adLayoutOutline = inflater
            .inflate(nativeAdapterConfig.itemNativeAd, parent, false)
        return AdViewHolder(adLayoutOutline)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_FB_NATIVE_ADS) {
            onCreateAdViewHolder(parent)
        } else super.onCreateViewHolder(parent, viewType)
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        if (holder.itemViewType != TYPE_FB_NATIVE_ADS) {
            super.onViewRecycled(holder)
        }
    }

    private fun setSpanAds() {
        val gridLayoutManager = nativeAdapterConfig.gridLayoutManager as? GridLayoutManager ?: return
        val spl = gridLayoutManager.spanSizeLookup
        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (isAdPosition(position)) spl.getSpanSize(position) else 1
            }
        }
    }

    private class AdViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var loaded = false
        var shimmerLayoutView: ShimmerFrameLayout = view.findViewById(R.id.shimmer_container_native)
        var nativeContentView: FrameLayout = view.findViewById(R.id.frAds)
    }

    companion object {
        const val TYPE_FB_NATIVE_ADS = 900
    }
}
