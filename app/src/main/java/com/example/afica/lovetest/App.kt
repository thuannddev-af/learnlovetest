package com.example.afica.lovetest

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.afica.lovetest.utils.SharedPreferenceUtils
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        SharedPreferenceUtils.getInstance(this)
        initFullscreen()
    }

    private fun initFullscreen() {
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                hideSystemBars(activity)
                activity.window.decorView.viewTreeObserver.addOnWindowFocusChangeListener { hasFocus ->
                    if (hasFocus) hideSystemBars(activity)
                }
            }

            override fun onActivityResumed(activity: Activity) = hideSystemBars(activity)
            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }

    fun hideSystemBars(activity: Activity) {
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
        WindowInsetsControllerCompat(activity.window, activity.window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}
