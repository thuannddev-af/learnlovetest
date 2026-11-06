package com.ads.admob.helper.adnative.params

import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout

class NativeAdAdapterParam {
    var admobNativeId: String = ""
    lateinit var adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>
    var adItemInterval = 0
    var forceReloadAdOnBind = false
    var layout = 0
    var adPosition: Int? = null

    @LayoutRes
    var itemContainerLayoutRes = 0

    @IdRes
    var itemContainerId = 0
    var gridLayoutManager: GridLayoutManager? = null
    @LayoutRes
    var nativeContentView = 0
}