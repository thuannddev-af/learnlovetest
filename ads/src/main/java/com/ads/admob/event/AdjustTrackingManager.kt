package com.ads.admob.event

import android.util.Log
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustAdRevenue
import com.adjust.sdk.AdjustEvent
import com.applovin.mediation.MaxAd
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.AdapterResponseInfo

object AdjustTrackingManager {
    // === CONSTANTS FOR ADJUST ENVIRONMENTS ===
    /** Môi trường Sandbox - dùng cho testing và development */
    const val ENVIRONMENT_SANDBOX = "sandbox"

    /** Môi trường Production - dùng cho app release */
    const val ENVIRONMENT_PRODUCTION = "production"

    // === CONSTANTS FOR URL STRATEGIES ===
    /** Chiến lược URL cho thị trường Ấn Độ */
    const val URL_STRATEGY_INDIA = "url_strategy_india"

    /** Chiến lược URL cho thị trường Trung Quốc */
    const val URL_STRATEGY_CHINA = "url_strategy_china"

    /** Chiến lược URL cho Trung Quốc (viết tắt) */
    const val URL_STRATEGY_CN = "url_strategy_cn"

    /** Chiến lược URL chỉ dành cho Trung Quốc */
    const val URL_STRATEGY_CN_ONLY = "url_strategy_cn_only"

    // === CONSTANTS FOR DATA RESIDENCY ===
    /** Lưu trữ dữ liệu tại châu Âu */
    const val DATA_RESIDENCY_EU = "data_residency_eu"

    /** Lưu trữ dữ liệu tại Thổ Nhĩ Kỳ */
    const val DATA_RESIDENCY_TR = "data_residency_tr"

    /** Lưu trữ dữ liệu tại Hoa Kỳ */
    const val DATA_RESIDENCY_US = "data_residency_us"

    // === CONSTANTS FOR AD REVENUE SOURCES ===
    /** Source identifier cho AppLovin MAX */
    const val AD_REVENUE_APPLOVIN_MAX = "applovin_max_sdk"

    /** Source identifier cho MoPub (deprecated) */
    const val AD_REVENUE_MOPUB = "mopub"

    /** Source identifier cho Google AdMob */
    const val AD_REVENUE_ADMOB = "admob_sdk"

    /** Source identifier cho IronSource */
    const val AD_REVENUE_IRONSOURCE = "ironsource_sdk"

    /** Source identifier cho AdMost */
    const val AD_REVENUE_ADMOST = "admost_sdk"

    /** Source identifier cho Unity Ads */
    const val AD_REVENUE_UNITY = "unity_sdk"

    /** Source identifier cho Helium Chartboost */
    const val AD_REVENUE_HELIUM_CHARTBOOST = "helium_chartboost_sdk"

    /** Source identifier cho Publisher SDK */
    const val AD_REVENUE_SOURCE_PUBLISHER = "publisher_sdk"

    /** Source identifier cho TopOn */
    const val AD_REVENUE_TOPON = "topon_sdk"

    /** Source identifier cho Google AdX */
    const val AD_REVENUE_ADX = "adx_sdk"

    /** Source identifier cho TradPlus */
    const val AD_REVENUE_TRADPLUS = "tradplus_sdk"

    /** Event name cho tracking purchase events */
    private var eventNamePurchase: String? = ""

    /**
     * Thiết lập tên event cho việc tracking purchase.
     * @param eventNamePurchase Tên event purchase được định nghĩa trong Adjust dashboard.
     */
    fun setEventNamePurchase(eventNamePurchase: String?) {
        AdjustTrackingManager.eventNamePurchase = eventNamePurchase
    }

    /**
     * Track ad revenue với một source ID cụ thể.
     * @param id Source identifier của ad network (sử dụng các constants AD_REVENUE_*).
     */
    fun trackAdRevenue(id: String?) {
        val adjustAdRevenue = AdjustAdRevenue(id)
        Adjust.trackAdRevenue(adjustAdRevenue)
    }

    /**
     * Track một event đơn giản với tên event.
     * @param eventName Tên event được định nghĩa trong Adjust dashboard.
     */
    fun onTrackEvent(eventName: String?) {
        val event = AdjustEvent(eventName)
        Adjust.trackEvent(event)
    }

    /**
     * Track một event với callback ID để theo dõi success/failure.
     * @param eventName Tên event được định nghĩa trong Adjust dashboard.
     * @param id Callback ID để nhận thông báo về trạng thái gửi event.
     */
    fun onTrackEvent(eventName: String?, id: String?) {
        val event = AdjustEvent(eventName)
        // Gán custom identifier cho event để nhận callback success/failure.
        event.setCallbackId(id)
        Adjust.trackEvent(event)
    }

    /**
     * Track event với thông tin revenue.
     * @param eventName Tên event được định nghĩa trong Adjust dashboard.
     * @param revenue Giá trị revenue (ví dụ: 0.01 cho 1 cent).
     * @param currency Mã tiền tệ (ví dụ: "USD", "EUR").
     */
    fun onTrackRevenue(eventName: String?, revenue: Float, currency: String?) {
        val event = AdjustEvent(eventName)
        // Thêm revenue với đơn vị tiền tệ.
        event.setRevenue(revenue.toDouble(), currency)
        Adjust.trackEvent(event)
    }

    /**
     * Track revenue cho purchase event đã được thiết lập trước.
     * @param revenue Giá trị revenue của purchase.
     * @param currency Mã tiền tệ.
     */
    fun onTrackRevenuePurchase(revenue: Float, currency: String?) {
        onTrackRevenue(eventNamePurchase, revenue, currency)
    }

//    fun pushTrackEventAdmob(adValue: AdValue) {
//        val adRevenue = AdjustAdRevenue(AdjustConfig.AD_REVENUE_ADMOB)
//        adRevenue.setRevenue(adValue.getValueMicros() / 1000000.0, adValue.getCurrencyCode())
//        Adjust.trackAdRevenue(adRevenue)
//    }


    /**
     * Track ad revenue từ Google AdMob với thông tin chi tiết.
     * @param adValue Đối tượng chứa thông tin revenue từ AdMob.
     * @param loadedAdapterResponseInfo Thông tin về adapter đã load quảng cáo.
     * @param placementName Tên placement của quảng cáo trong app.
     */
    fun pushTrackEventAdmob(
        adValue: AdValue,
        loadedAdapterResponseInfo: AdapterResponseInfo?,
        placementName: String?
    ) {
        val adRevenue = AdjustAdRevenue(AD_REVENUE_ADMOB)
        // Chuyển đổi từ micros (1/1,000,000) sang đơn vị chuẩn.
        adRevenue.setRevenue(adValue.valueMicros / 1000000.0, adValue.currencyCode)
        adRevenue.adRevenuePlacement = placementName
        if (loadedAdapterResponseInfo != null) {
            // Thêm thông tin về ad network thực tế đã serve quảng cáo.
            adRevenue.adRevenueNetwork = loadedAdapterResponseInfo.adSourceName
        }
        Adjust.trackAdRevenue(adRevenue)
    }


    /**
     * Track AdMob revenue như một custom event thay vì ad revenue.
     * Sử dụng khi muốn track revenue dưới dạng event thay vì ad revenue.
     * @param adValue Đối tượng chứa thông tin revenue từ AdMob.
     * @param adjustRevKey Event key được định nghĩa trong Adjust dashboard.
     */
    fun pushTrackEvenAdjustRevenueAdMod(
        adValue: AdValue,
        adjustRevKey: String?
    ) {
        val adjustEvent = AdjustEvent(adjustRevKey)
        // Chuyển đổi từ micros sang đơn vị chuẩn và gán revenue cho event.
        adjustEvent.setRevenue(adValue.valueMicros / 1000000.0, adValue.currencyCode)
        Adjust.trackEvent(adjustEvent)
    }

    fun pushTrackEvenAdjustRevenueMintegral(
        adValue: AdValue,
        adjustRevMinKey: String?
    ) {
        val adjustEvent = AdjustEvent(adjustRevMinKey)
        val revenue = adValue.valueMicros / 1000000.0 * 0.6 //60%
        adjustEvent.setRevenue(revenue, adValue.currencyCode)
        adjustEvent.addCallbackParameter("ad_network", "mintegral")
        Adjust.trackEvent(adjustEvent)
    }


    /**
     * Track ad revenue từ AppLovin MAX.
     * @param ad Đối tượng MaxAd chứa thông tin về quảng cáo AppLovin.
     * @param placementName Tên placement của quảng cáo trong app.
     */
    fun pushTrackEventApplovin(ad: MaxAd, placementName: String?) {
        val adjustAdRevenue = AdjustAdRevenue(AdjustTrackingManager.AD_REVENUE_APPLOVIN_MAX)
        // AppLovin trả về revenue đã được format sẵn.
        adjustAdRevenue.setRevenue(ad.revenue, "USD")
        adjustAdRevenue.setAdRevenueNetwork(ad.networkName)
        adjustAdRevenue.setAdRevenueUnit(ad.adUnitId)
        adjustAdRevenue.setAdRevenuePlacement(placementName)
        Log.d("!!!!!!", "pushTrackEventApplovin: " + placementName + "_____" + ad.revenue)
        Adjust.trackAdRevenue(adjustAdRevenue)
    }

    /**
     * Track AppLovin revenue như một custom event thay vì ad revenue.
     * @param ad Đối tượng MaxAd chứa thông tin về quảng cáo AppLovin.
     * @param adjustRevenueKey Event key được định nghĩa trong Adjust dashboard.
     */
    fun pushTrackEventAdjustRevenueApplovin(
        ad: MaxAd,
        adjustRevenueKey: String?
    ) {
        val adjustAdRevenue: AdjustEvent = AdjustEvent(adjustRevenueKey);
        // Gán revenue cho custom event.
        adjustAdRevenue.setRevenue(ad.revenue, "USD")
        Log.d(
            "!!!!!!",
            "pushTrackEventAdjustRevenueApplovin: " + ad.placement + "_____" + ad.revenue
        )
        Adjust.trackEvent(adjustAdRevenue)
    }
}

/*
Hướng dẫn sử dụng AdjustEventTracking:

1. Thiết lập event purchase:
   AdjustEventTracking.setEventNamePurchase("abc123")

2. Track simple event:
   AdjustEventTracking.onTrackEvent("button_click_event")

3. Track event với callback:
   AdjustEventTracking.onTrackEvent("level_complete", "callback_id_123")

4. Track revenue event:
   AdjustEventTracking.onTrackRevenue("purchase_event", 9.99f, "USD")

5. Track AdMob revenue:
   AdjustEventTracking.pushTrackEventAdmob(adValue, responseInfo, "banner_home")

6. Track AppLovin revenue:
   AdjustEventTracking.pushTrackEventApplovin(maxAd, "interstitial_level_complete")

Lưu ý:
- Tất cả event names và keys phải được setup trước trong Adjust dashboard
- Revenue được tính theo đơn vị tiền tệ thực (không phải micros)
- Placement names nên có naming convention nhất quán để dễ phân tích
*/
