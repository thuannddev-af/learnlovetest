package com.ads.admob.helper.appoppen.params

import com.ads.admob.helper.params.IAdsParam


open class AppOpenAdParam : IAdsParam {
     object Show : AppOpenAdParam()
     object Request : AppOpenAdParam() {
        @JvmStatic
        fun create(): Request {
            return this
        }
    }

    data class Clickable(
        val minimumTimeKeepAdsDisplay: Long
    ) : AppOpenAdParam()
}