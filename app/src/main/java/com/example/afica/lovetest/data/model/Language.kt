package com.example.afica.lovetest.data.model

data class Language(
    val languageCode: String,
    val iconRes: Int,
    val nameRes: Int,
    var selected: Boolean = false
)
