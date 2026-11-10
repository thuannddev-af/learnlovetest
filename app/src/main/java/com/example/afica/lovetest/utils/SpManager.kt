package com.example.afica.lovetest.utils

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.core.content.edit
import com.example.afica.lovetest.App
import com.example.afica.lovetest.R
import com.example.afica.lovetest.data.model.Language

class SpManager(private val preferences: SharedPreferences) {
    companion object {
        private var instance: SpManager? = null

        fun getInstance(context: Context): SpManager {
            if (instance == null) {
                instance = SpManager(PreferenceManager.getDefaultSharedPreferences(context))
            }
            return instance!!
        }

        fun getInstance(): SpManager {
            if (instance == null) {
                instance = App.context?.let {
                    PreferenceManager.getDefaultSharedPreferences(
                        it
                    )
                }?.let { SpManager(it) }
            }
            return instance!!
        }
    }

    fun putBoolean(key: String, value: Boolean) {
        preferences.edit {
            putBoolean(key, value)
        }
    }

    fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        preferences.getBoolean(key, defaultValue)

    fun getInt(key: String, defaultValue: Int): Int = preferences.getInt(key, defaultValue)

    fun putInt(key: String, value: Int) {
        preferences.edit {
            putInt(key, value)
        }
    }

    fun getLong(key: String, defaultValue: Long): Long =
        preferences.getLong(key, defaultValue)

    fun putLong(key: String, value: Long) {
        preferences.edit {
            putLong(key, value)
        }
    }

    fun getString(key: String, defaultValue: String): String? =
        preferences.getString(key, defaultValue)

    fun putString(key: String, value: String) {
        preferences.edit {
            putString(key, value)
        }
    }

    fun saveLanguage(language: Language) {
        preferences.edit {
            putString(Constant.KEY_SP_CURRENT_LANGUAGE, language.toJson())
        }
    }

    fun getLanguage(): Language {
        return preferences.getString(Constant.KEY_SP_CURRENT_LANGUAGE, "")?.toLanguageModel()
            ?: Language("en", R.drawable.ic_english, R.string.english)
    }

    fun setLanguageChosen() {
        preferences.edit { putBoolean(Constant.KEY_SP_LANGUAGE_CHOSEN, true) }
    }

    fun isLanguageChosen(): Boolean {
        return preferences.getBoolean(Constant.KEY_SP_LANGUAGE_CHOSEN, false)
    }

    fun saveApplicationStop(isStop: Boolean) {
        preferences.edit { putBoolean(Constant.KEY_SP_IS_APPLICATION_STOP, isStop) }
    }

    fun isApplicationStopped(): Boolean {
        return preferences.getBoolean(Constant.KEY_SP_IS_APPLICATION_STOP, false)
    }

}