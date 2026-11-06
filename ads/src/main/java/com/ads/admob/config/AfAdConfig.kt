package com.ads.admob.config

import android.app.Application

class AfAdConfig private constructor(
    val application: Application? = null,
    val isVariantProduce: Boolean = false,
    val provider: Int = NetworkProvider.ADMOB,
    val listDevices: List<String> = arrayListOf(),
    val disableAdsResumeByAd: Boolean = true,
    val afAdjustConfig: AfAdjustConfig,
    val intervalBetweenInterstitial: Long = 10_000,
    val eventConfig: EventConfig? = null
) {

    class Builder(
        private var application: Application? = null,
        private var isBuildVariantProduce: Boolean = false,
        private var mediationProvider: Int = NetworkProvider.ADMOB,
        private var listTestDevices: List<String> = arrayListOf(),
        private var disableAdsResumeByAd: Boolean = true,
        private var afAdjustConfig: AfAdjustConfig,
        private var intervalBetweenInterstitial: Long = 10_000,
        private var eventConfig: EventConfig? = null
    ) {
        fun application(application: Application) = apply { this.application = application }

        fun buildVariantProduce(variantProduce: Boolean) =
            apply { this.isBuildVariantProduce = variantProduce }

        fun mediationProvider(mediation: Int) = apply { this.mediationProvider = mediation }

        fun listTestDevices(listDevices: List<String>) =
            apply { this.listTestDevices = listDevices }

        fun disableAdsResumeByAd(isDisabled: Boolean) =
            apply { this.disableAdsResumeByAd = isDisabled }

        fun intervalBetweenInterstitial(interval: Long) = apply { this.intervalBetweenInterstitial = interval }

        fun eventConfig(eventConfig: EventConfig) = apply { this.eventConfig = eventConfig }

        fun build() =
            AfAdConfig(
                application,
                isBuildVariantProduce,
                mediationProvider,
                listTestDevices,
                disableAdsResumeByAd,
                afAdjustConfig,
                intervalBetweenInterstitial,
                eventConfig
            )
    }
}

/**
 * Lớp cấu hình cho việc tracking events và analytics.
 * Chứa các thông số cần thiết để theo dõi và phân tích hành vi người dùng.
 *
 * @param exchangeRate Tỷ giá quy đổi cho việc tính toán revenue (ví dụ: từ USD sang VND).
 *                     Giá trị này được sử dụng để chuyển đổi revenue từ ads sang đơn vị tiền tệ mong muốn.
 */
class EventConfig(val exchangeRate: Long, val exchangeCurrency: String)