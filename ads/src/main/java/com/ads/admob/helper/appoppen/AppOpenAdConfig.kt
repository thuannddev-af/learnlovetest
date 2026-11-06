package com.ads.admob.helper.appoppen

import com.ads.admob.helper.IAdsConfig


class AppOpenAdConfig(
    override val idAds: String,
    val idAdsPriority: String? = null,
    override val canShowAds: Boolean = false,
    override val canReloadAds: Boolean = false,
    override val adPlacement: String,
    override val reloadIfFirstFail: Boolean = false
) : IAdsConfig