package com.ads.admob.event

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.android.gms.ads.AdValue
import com.google.firebase.analytics.FirebaseAnalytics

class FirebaseTrackingManager private constructor(context: Context) {
    private var firebaseAnalytics: FirebaseAnalytics? = null

    companion object {
        @Volatile
        private var INSTANCE: FirebaseTrackingManager? = null

        fun initialize(context: Context): FirebaseTrackingManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirebaseTrackingManager(context).also { INSTANCE = it }
            }
        }

        fun getInstance(): FirebaseTrackingManager {
            return INSTANCE
                ?: throw IllegalStateException("FirebaseTrackingManager is not initialized. Call initialize() first.")
        }
    }

    init {
        firebaseAnalytics = FirebaseAnalytics.getInstance(context)
    }

    fun logScreenView(screenName: String, screenClass: String? = null) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            screenClass?.let { putString(FirebaseAnalytics.Param.SCREEN_CLASS, it) }
        }
        firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }

    fun logEvent(eventName: String, data: Bundle? = null) {
        firebaseAnalytics?.logEvent(eventName, data)
    }

    fun logRevenue(adValue: AdValue, adPlacement:String?,adSource:String?, adUnitId:String?, adFormat:String){
        val microsValue = adValue.valueMicros
        val currencyCode = adValue.currencyCode
        val revenue = microsValue / 1_000_000.0

        val bundle = Bundle().apply {
            putDouble("ad_revenue", revenue)
            putString("ad_currency", currencyCode)
            putString("ad_platform", "admob")
            putString("ad_placement", adPlacement)
            putString("ad_source", adSource)
            putString("ad_unit_id", adUnitId)
            putString("ad_format", adFormat)
        }

        firebaseAnalytics?.logEvent("admob_revenue", bundle)
    }
}