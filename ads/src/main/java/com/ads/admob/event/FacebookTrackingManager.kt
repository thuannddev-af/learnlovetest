package com.ads.admob.event

import android.app.Application
import android.os.Bundle
import android.util.Log
import com.ads.admob.config.EventConfig
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsConstants
import com.facebook.appevents.AppEventsConstants.EVENT_PARAM_CURRENCY
import com.facebook.appevents.AppEventsLogger
import java.math.BigDecimal
import java.util.Currency


class FacebookTrackingManager private constructor(application: Application) {
    private var logger: AppEventsLogger? = null
    private var eventConfig: EventConfig? = null

    companion object {
        @Volatile
        private var INSTANCE: FacebookTrackingManager? = null

        fun initialize(application: Application): FacebookTrackingManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FacebookTrackingManager(application).also { INSTANCE = it }
            }
        }

        fun getInstance(): FacebookTrackingManager {
            return INSTANCE
                ?: throw IllegalStateException("FacebookTrackingManager is not initialized. Call initialize() first.")
        }
    }

    init {
        FacebookSdk.sdkInitialize(application);
        FacebookSdk.fullyInitialize()
        AppEventsLogger.activateApp(application);
        logger = AppEventsLogger.newLogger(application)

    }

    fun setEventConfig(eventConfig: EventConfig) {
        this.eventConfig = eventConfig
        Log.e("TAG", "setEventConfig: ${eventConfig.exchangeRate} ")
    }

    fun logPurchase(amount: Double, currency: String?) {
        eventConfig?.let { config ->
            logger?.logPurchase(
                BigDecimal.valueOf(amount * config.exchangeRate),
                Currency.getInstance(config.exchangeCurrency)
            )
        } ?: run {
            logger?.logPurchase(BigDecimal.valueOf(amount), Currency.getInstance(currency))
        }
    }

    fun logAdImpression(amount: Double, currency: String?) {
        val params = Bundle()
        eventConfig?.let { config ->
            params.putString(EVENT_PARAM_CURRENCY, config.exchangeCurrency)
            logger?.logEvent(AppEventsConstants.EVENT_NAME_AD_IMPRESSION, amount * config.exchangeRate, params)
        } ?: run {
            params.putString(EVENT_PARAM_CURRENCY, currency)
            logger?.logEvent(AppEventsConstants.EVENT_NAME_AD_IMPRESSION, amount, params)
        }
    }
}