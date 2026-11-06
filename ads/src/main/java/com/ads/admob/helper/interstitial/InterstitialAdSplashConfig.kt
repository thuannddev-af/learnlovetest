package com.ads.admob.helper.interstitial

import com.ads.admob.helper.IAdsConfig

class InterstitialAdSplashConfig(
    override val idAds: String,
    val idAdsPriority: String? = null,
    val timeOut: Long,
    val timeDelay: Long,
    val showReady: Boolean = false,
    override val canShowAds: Boolean,
    override val canReloadAds: Boolean,
    override val adPlacement: String,
    override val reloadIfFirstFail: Boolean = false
) : IAdsConfig