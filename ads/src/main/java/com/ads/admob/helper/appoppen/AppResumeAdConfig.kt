package com.ads.admob.helper.appoppen

import com.ads.admob.config.NetworkProvider
import com.ads.admob.helper.IAdsConfig


class AppResumeAdConfig(
    override val idAds: String,
    val networkProvider: Int = NetworkProvider.ADMOB,
    val listClassInValid: MutableList<Class<*>> = arrayListOf(),
    override val canShowAds: Boolean = false,
    override val canReloadAds: Boolean = false,
    override val adPlacement: String,
    override val reloadIfFirstFail: Boolean = false,
    /**
     * Khi true: không pre-load lúc pause mà sẽ load + show ngay khi user mở lại app.
     */
    val loadOnResume: Boolean = false,
) : IAdsConfig