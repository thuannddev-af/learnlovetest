package com.example.afica.lovetest.utils

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {
    fun onAttach(context: Context, defaultLanguage: String): Context =
        setLocale(context, defaultLanguage)

    fun setLocale(context: Context, language: String): Context {
        val locale = Locale.forLanguageTag(language.replace('_', '-'))
        Locale.setDefault(locale)
        val configuration = Configuration(context.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        return context.createConfigurationContext(configuration)
    }
}
