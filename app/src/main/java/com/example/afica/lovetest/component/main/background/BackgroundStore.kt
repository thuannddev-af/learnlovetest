package com.example.afica.lovetest.component.main.background

import android.content.Context
import android.net.Uri
import com.example.afica.lovetest.utils.SharedPreferenceUtils

class BackgroundStore(context: Context) {
    private val preferences = SharedPreferenceUtils(context, "home")
    fun selected(): Uri? = preferences.getString(SELECTED_URI)?.let(Uri::parse)

    fun select(uri: Uri) {
        preferences.putString(SELECTED_URI, uri.toString())
    }

    companion object {
        private const val SELECTED_URI = "background_uri"
    }
}
