package com.example.afica.lovetest

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import com.example.afica.lovetest.utils.SharedPreferenceUtils

@HiltAndroidApp
class LoveTestApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SharedPreferenceUtils.getInstance(this)
    }
}
