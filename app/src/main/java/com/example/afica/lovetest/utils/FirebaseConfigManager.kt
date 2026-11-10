package com.example.afica.lovetest.utils

import com.example.afica.lovetest.domain.layer.AdConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.gson.Gson


class FirebaseConfigManager {

    companion object {
        const val KEY_ADS_STATUS_ENABLE = "ad_config"
        const val KEY_VERSION_FORCE = "version_force"
        const val KEY_IS_FORCE_UPDATE = "is_force_update"

        private var mInstance: FirebaseConfigManager = FirebaseConfigManager()
        fun instance(): FirebaseConfigManager {
            return mInstance
        }
    }

    private val mMaxTryTime = 5
    private var mTryTime = 0

    private var isFetchDone = false
    var versionForce = "1.0.0"
    var isForceUpdate = false
    private val pendingActions: MutableList<() -> Unit> = mutableListOf()

    var adConfig: AdConfig = AdConfig()

    fun doAfterFetch(action: () -> Unit) {
        if (isFetchDone) {
            action()
        } else {
            pendingActions.add(action)
        }
    }

    fun fetch(onFetchComplete: (() -> Unit)? = null) {
        val longCache = 15 * 60L
        FirebaseRemoteConfig.getInstance().fetch(longCache).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Logger.e("load config success")
                FirebaseRemoteConfig.getInstance().activate()
                versionForce = FirebaseRemoteConfig.getInstance().getString(KEY_VERSION_FORCE)
                isForceUpdate = FirebaseRemoteConfig.getInstance().getBoolean(KEY_IS_FORCE_UPDATE)
                val json = FirebaseRemoteConfig.getInstance().getString(KEY_ADS_STATUS_ENABLE)
                Logger.e("FirebaseConfigManager : $json")
                if (json.isEmpty()) {
                    tryFetchAgain(onFetchComplete)
                } else {
                    try {
                        adConfig = Gson().fromJson(json, AdConfig::class.java)
                        onFetchFinished(onFetchComplete)
                    } catch (e: Exception) {
                        tryFetchAgain(onFetchComplete)
                        Logger.e(e.message)
                    }
                }

            } else {
                tryFetchAgain(onFetchComplete)
                Logger.e("load config fail")
            }
        }
    }

    private fun onFetchFinished(onFetchComplete: (() -> Unit)?) {
        isFetchDone = true
        onFetchComplete?.invoke()
        pendingActions.forEach { it() }
        pendingActions.clear()
    }

    private fun tryFetchAgain(onFetchComplete: (() -> Unit)? = null) {
        ++mTryTime
        if (mTryTime < mMaxTryTime) {
            fetch(onFetchComplete)
        } else {
            mTryTime = 0
            onFetchFinished(onFetchComplete)
        }
    }
}