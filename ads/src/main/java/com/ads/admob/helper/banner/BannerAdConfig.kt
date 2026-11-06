package com.ads.admob.helper.banner

import com.ads.admob.BannerInlineStyle
import com.ads.admob.helper.IAdsConfig



data class BannerAdConfig(
    override val idAds: String,
    val idAdsPriority: String? = null,
    override val canShowAds: Boolean,
    override val canReloadAds: Boolean,
    val bannerInlineStyle: Int = BannerInlineStyle.SMALL_STYLE,
    val useInlineAdaptive: Boolean = false,
    override val adPlacement: String,
    override val reloadIfFirstFail: Boolean = false,
) : IAdsConfig {
    var collapsibleGravity: String? = null
}