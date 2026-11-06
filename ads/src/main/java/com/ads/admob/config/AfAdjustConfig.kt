package com.ads.admob.config

class AfAdjustConfig private constructor(
    val adjustToken: String,
    val environmentProduct: Boolean,
    val adRevenueKey: String,
    val adRevenueMintegralKey:String
) {
    class Build(
        private var adjustToken: String,
        private var environmentProduct: Boolean = true,
        private var adRevenueKey: String = "",
        private var adRevenueMintegralKey: String = "",
    ) {
        fun adRevenueKey(key: String) = apply {
            adRevenueKey = key
        }
        fun environmentProduct(product: Boolean) = apply { environmentProduct = product }
        fun build() = AfAdjustConfig(adjustToken, environmentProduct, adRevenueKey,adRevenueMintegralKey)
    }

}