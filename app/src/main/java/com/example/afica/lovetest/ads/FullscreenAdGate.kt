package com.example.afica.lovetest.ads

/** Shared ownership for all SDK fullscreen ads. Call on the main thread. */
internal object FullscreenAdGate {
    private var owner: Any? = null
    val isBusy: Boolean get() = owner != null
    fun acquire(token: Any): Boolean {
        if (owner != null) return false
        owner = token
        AppOpenAdsUtils.skipNextResume()
        return true
    }
    fun release(token: Any) {
        if (owner === token) owner = null
    }
}
