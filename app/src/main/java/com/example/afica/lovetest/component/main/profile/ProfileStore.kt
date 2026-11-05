package com.example.afica.lovetest.component.main.profile

import com.example.afica.lovetest.utils.Constant

import android.content.Context
import com.example.afica.lovetest.R
import com.example.afica.lovetest.data.model.ProfileInformation
import com.example.afica.lovetest.utils.SharedPreferenceUtils
import java.util.Calendar

class ProfileStore(context: Context) {
    private val context = context.applicationContext
    private val preferences = SharedPreferenceUtils(context, "profiles")

    fun load(id: String): ProfileInformation {
        val birthday = Calendar.getInstance().apply { add(Calendar.YEAR, -26) }.timeInMillis
        return ProfileInformation(
            preferences.getString(id + "_name", context.getString(
                if (id == Constant.PROFILE_BOY) R.string.home_boy_name else R.string.home_girl_name
            )).orEmpty(),
            preferences.getBoolean(id + "_male", id == Constant.PROFILE_BOY),
            preferences.getLong(id + "_birthday", birthday),
            preferences.getString(id + "_photo", null)
        )
    }

    fun save(id: String, profile: ProfileInformation) {
        preferences.edit {
            putString(id + "_name", profile.name)
            putBoolean(id + "_male", profile.isMale)
            putLong(id + "_birthday", profile.birthday)
            putString(id + "_photo", profile.photoPath)
        }
    }

    
}
