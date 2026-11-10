package com.example.afica.lovetest.ads

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

class RemoteConfigManager {
    private val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

    companion object {
        private val shared by lazy { RemoteConfigManager() }
        fun instance(): RemoteConfigManager = shared
        //bật tắt ads
        const val ADS_ENABLE = "ads_enable"
        const val APP_OPEN_ENABLE = "app_open_enable"

        const val BANNER_HOME_ENABLE = "banner_home_enable"
        const val BANNER_MEMORY_ENABLE = "banner_home_enable"
        const val NATIVE_LANGUAGE = "native_language"
        const val NATIVE_ONBOARDING = "native_onboarding"
        const val INTER_ADS = "inter_ads"
        const val REWARD_ADS = "reward_ads"

        const val INTER_SPLASH =
            "interstitial_splash_enable"
    }

    private var fetching = false
    private var fetched = false
    private var fetchSucceeded = false
    private val pendingActions = mutableListOf<(Boolean) -> Unit>()

    fun fetch() = init {}
    fun doAfterFetch(action: () -> Unit) = init { action() }

    fun init(onComplete: (Boolean) -> Unit) {
        if (this !== instance()) { instance().init(onComplete); return }
        if (fetched) { onComplete(fetchSucceeded); return }
        pendingActions.add(onComplete)
        if (fetching) return
        fetching = true
        val settings =
            FirebaseRemoteConfigSettings.Builder().setMinimumFetchIntervalInSeconds(0).build()

        val defaultValues = mapOf(
            ADS_ENABLE to false,
            APP_OPEN_ENABLE to false,
            BANNER_HOME_ENABLE to false,
            BANNER_MEMORY_ENABLE to false,
            NATIVE_LANGUAGE to false,
            NATIVE_ONBOARDING to false,
            INTER_SPLASH to false,
            INTER_ADS to false,
            REWARD_ADS to false
        )

        remoteConfig.setConfigSettingsAsync(settings)
            .continueWithTask {
                remoteConfig.setDefaultsAsync(defaultValues)
            }
            .continueWithTask { remoteConfig.fetchAndActivate() }
            .addOnCompleteListener { task ->
                fetching = false
                fetched = task.isSuccessful
                fetchSucceeded = task.isSuccessful
                val actions = pendingActions.toList()
                pendingActions.clear()
                actions.forEach { it(fetchSucceeded) }
            }
    }

    fun isAdsEnable(): Boolean {
        return remoteConfig.getBoolean(ADS_ENABLE)
    }

    fun canShowAd(adKEy: String): Boolean {
        return isAdsEnable() && remoteConfig.getBoolean(adKEy)
    }

    fun getBoolean(key: String): Boolean {
        return remoteConfig.getBoolean(key)
    }
}
