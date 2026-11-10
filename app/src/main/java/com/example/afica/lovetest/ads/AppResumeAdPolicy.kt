package com.example.afica.lovetest.ads

/** Models activity transitions independently of Android callbacks. */
internal class AppResumeAdPolicy {
    private var started = 0
    private var backgrounded = false
    private var allowed = false
    private var pending = false
    private var skip = false

    fun skipNextResume() { skip = true; pending = false }

    fun onStarted(fullscreenBusy: Boolean) {
        if (started == 0 && backgrounded) {
            pending = allowed && !skip && !fullscreenBusy
            backgrounded = false
        }
        started++
    }

    fun onResumed(content: Boolean, fullscreenBusy: Boolean): Boolean {
        val show = pending && content && !skip && !fullscreenBusy
        pending = false
        skip = false
        return show
    }

    fun onStopped(content: Boolean, finishing: Boolean, changingConfig: Boolean, fullscreenBusy: Boolean) {
        started = (started - 1).coerceAtLeast(0)
        if (started == 0 && !changingConfig) {
            backgrounded = true
            allowed = content && !finishing && !skip && !fullscreenBusy
        }
    }
}
