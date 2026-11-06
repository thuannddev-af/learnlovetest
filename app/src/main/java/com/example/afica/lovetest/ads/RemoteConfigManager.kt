package com.example.afica.lovetest.ads

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

class RemoteConfigManager {
    private val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

    companion object {
        //bật tắt ads
        const val ADS_ENABLE = "ads_enable"

        const val BANNER_HOME_ENABLE = "banner_home_enable"
        const val NATIVE_LANGUAGE = "native_language"
        const val NAITVE_ONBOARDING = "native_onboarding"
        const val INTER_ADS = "inter_ads"

        const val INTERSTITIAL_SPLASH_ENABLE =
            "interstitial_splash_enable"
    }

    fun init(onComplete: (Boolean) -> Unit) {
        val settings = FirebaseRemoteConfigSettings.Builder().setMinimumFetchIntervalInSeconds(0).build()

        val defaultValues = mapOf(
            ADS_ENABLE to false,
            BANNER_HOME_ENABLE to false,
            NATIVE_LANGUAGE to false,
            NAITVE_ONBOARDING to false,
            INTERSTITIAL_SPLASH_ENABLE to false,
            INTER_ADS to false
        )

        remoteConfig.setConfigSettingsAsync(settings)
            .continueWithTask {
            remoteConfig.setDefaultsAsync(defaultValues)
        }
            .continueWithTask { remoteConfig.fetchAndActivate() }
            .addOnCompleteListener { task ->
                onComplete(task.isSuccessful)
            }
    }

    fun isAdsEnable() : Boolean {
        return remoteConfig.getBoolean(ADS_ENABLE)
    }

    fun canShowAd(adKEy: String): Boolean {
        return isAdsEnable() && remoteConfig.getBoolean(adKEy)
    }

    fun getBoolean(key: String): Boolean {
        return remoteConfig.getBoolean(key)
    }
}