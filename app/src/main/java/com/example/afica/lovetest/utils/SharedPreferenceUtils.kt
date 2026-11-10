package com.example.afica.lovetest.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.afica.lovetest.R
import com.example.afica.lovetest.data.model.Language
import org.json.JSONException
import org.json.JSONObject

class SharedPreferenceUtils(context: Context, fileName: String = "app_preferences") {
    private val preferences = context.applicationContext.getSharedPreferences(fileName, Context.MODE_PRIVATE)

    fun getString(key: String, defaultValue: String? = null): String? =
        preferences.getString(key, defaultValue)

    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean =
        preferences.getBoolean(key, defaultValue)

    fun getLong(key: String, defaultValue: Long = 0L): Long =
        preferences.getLong(key, defaultValue)

    fun getInt(key: String, defaultValue: Int = 0): Int =
        preferences.getInt(key, defaultValue)

    fun contains(key: String): Boolean = preferences.contains(key)

    fun putString(key: String, value: String?) = edit { putString(key, value) }

    fun putBoolean(key: String, value: Boolean) = edit { putBoolean(key, value) }

    fun putLong(key: String, value: Long) = edit { putLong(key, value) }

    fun putInt(key: String, value: Int) = edit { putInt(key, value) }

    fun remove(key: String) = edit { remove(key) }

    fun saveLanguage(language: Language) {
        val json = JSONObject().apply {
            put("languageCode", language.languageCode)
            put("iconRes", language.iconRes)
            put("nameRes", language.nameRes)
            put("selected", language.selected)
        }
        putString(Constant.KEY_SP_CURRENT_LANGUAGE, json.toString())
    }

    fun getLanguage(): Language {
        val fallback = Language("en", R.drawable.ic_english, R.string.english)
        val json = getString(Constant.KEY_SP_CURRENT_LANGUAGE)?.takeIf { it.isNotBlank() }
            ?: return fallback
        return try {
            val value = JSONObject(json)
            val code = value.getString("languageCode").takeIf { it.isNotBlank() }
                ?: return fallback
            Language(
                code,
                value.getInt("iconRes"),
                value.getInt("nameRes"),
                value.optBoolean("selected", false)
            )
        } catch (_: JSONException) {
            fallback
        }
    }

    fun setLanguageChosen() = putBoolean(Constant.KEY_SP_LANGUAGE_CHOSEN, true)

    fun isLanguageChosen(): Boolean = getBoolean(Constant.KEY_SP_LANGUAGE_CHOSEN)

    fun saveApplicationStop(isStop: Boolean) = putBoolean(Constant.KEY_SP_IS_APPLICATION_STOP, isStop)

    fun isApplicationStopped(): Boolean = getBoolean(Constant.KEY_SP_IS_APPLICATION_STOP)

    fun edit(block: SharedPreferences.Editor.() -> Unit) {
        val editor = preferences.edit()
        editor.block()
        editor.apply()
    }

    companion object {
        @Volatile
        private var instance: SharedPreferenceUtils? = null

        fun getInstance(): SharedPreferenceUtils =
            checkNotNull(instance) { "Initialize SharedPreferenceUtils in Application.onCreate first." }
    }
}
