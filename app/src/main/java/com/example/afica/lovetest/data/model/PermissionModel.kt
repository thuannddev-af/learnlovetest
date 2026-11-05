package com.example.afica.lovetest.data.model

import androidx.annotation.StringRes

data class PermissionModel(
    @StringRes
    val content: Int,
    val isAllowed: Boolean = false,
)
