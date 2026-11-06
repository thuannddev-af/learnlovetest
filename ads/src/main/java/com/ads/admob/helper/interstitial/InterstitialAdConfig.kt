package com.ads.admob.helper.interstitial

import com.ads.admob.helper.IAdsConfig

class InterstitialAdConfig(
    override val idAds: String,
    val idAdsPriority: String? = null,
    val showByTime: Int = 1,
    override val canShowAds: Boolean,
    override val canReloadAds: Boolean,
    val currentTime: Int = 0,
    override val adPlacement: String,
    override val reloadIfFirstFail: Boolean = false
) : IAdsConfig