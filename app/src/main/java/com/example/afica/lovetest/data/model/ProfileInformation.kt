package com.example.afica.lovetest.data.model

import java.util.Calendar

data class ProfileInformation(
    val name: String,
    val isMale: Boolean,
    val birthday: Long,
    val photoPath: String?
) {
    fun age(today: Calendar = Calendar.getInstance()): Int {
        val birth = Calendar.getInstance().apply { timeInMillis = birthday }
        var years = today.get(Calendar.YEAR) - birth.get(Calendar.YEAR)
        if (today.get(Calendar.MONTH) < birth.get(Calendar.MONTH) ||
            (today.get(Calendar.MONTH) == birth.get(Calendar.MONTH) &&
                today.get(Calendar.DAY_OF_MONTH) < birth.get(Calendar.DAY_OF_MONTH))) years--
        return years.coerceAtLeast(0)
    }
}
