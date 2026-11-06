package com.example.afica.lovetest.data.source

import android.os.Build
import com.example.afica.lovetest.R
import com.example.afica.lovetest.data.database.AppDatabase
import com.example.afica.lovetest.data.model.Language
import com.example.afica.lovetest.data.model.OnBoarding
import com.example.afica.lovetest.data.model.PermissionModel
import com.example.afica.lovetest.data.model.Memory
import com.example.afica.lovetest.data.model.LoveTestMessage
import com.example.afica.lovetest.data.model.ZodiacSign
import javax.inject.Inject

class LocalDataSource @Inject constructor(private val appDatabase: AppDatabase)
{
    fun getListLanguage() : List<Language> {
        return listOf(
            Language("en", R.drawable.ic_english, R.string.english),
            Language("hi", R.drawable.ic_hindi, R.string.hindi),
            Language("es", R.drawable.ic_spanish, R.string.spanish),
            Language("fr", R.drawable.ic_french, R.string.french),
            Language("pt", R.drawable.ic_portugal, R.string.portuguese),
            Language("in", R.drawable.ic_indo, R.string.indo),
            Language("de", R.drawable.ic_german, R.string.german)
        )
    }

    fun getListOnBoarding(): List<OnBoarding> {
        return listOf(
            OnBoarding(R.drawable.onboarding1, R.string.onboarding_relationship_title, R.string.onboarding_relationship_description),
            OnBoarding(R.drawable.onboarding2, R.string.onboarding_counter_title, R.string.onboarding_counter_description),
            OnBoarding(R.drawable.onboarding3, R.string.onboarding_moments_title, R.string.onboarding_moments_description)
        )
    }
    //fun getList() = appDatabase.tableTestDao().getList()
    fun getZodiacSigns(): List<ZodiacSign> = listOf(
        ZodiacSign("aries", R.string.zodiac_aries, R.drawable.love_aries),
        ZodiacSign("taurus", R.string.zodiac_taurus, R.drawable.love_taurus),
        ZodiacSign("gemini", R.string.zodiac_gemini, R.drawable.love_gemini),
        ZodiacSign("cancer", R.string.zodiac_cancer, R.drawable.love_cancer),
        ZodiacSign("leo", R.string.zodiac_leo, R.drawable.love_leo),
        ZodiacSign("virgo", R.string.zodiac_virgo, R.drawable.love_virgo),
        ZodiacSign("libra", R.string.zodiac_libra, R.drawable.love_libra),
        ZodiacSign("scorpio", R.string.zodiac_scorpio, R.drawable.love_scorpio),
        ZodiacSign("sagittarius", R.string.zodiac_sagittarius, R.drawable.love_sagit),
        ZodiacSign("capricorn", R.string.zodiac_capricorn, R.drawable.love_capri),
        ZodiacSign("aquarius", R.string.zodiac_aquarius, R.drawable.love_aqua),
        ZodiacSign("pisces", R.string.zodiac_pisces, R.drawable.love_pisces)
    )

    fun getHoroscopeMessages(): List<LoveTestMessage> = listOf(
        LoveTestMessage(0, 5,
            "Everything can become chaotic because it is difficult for them to trust each other and cannot sympathize with the other person’s behavior. If there is a misunderstanding in a relationship, the emotions of both people often tend to go out of control, easily causing arguments to become fierce and lead to not very good results"),
        LoveTestMessage(6, 20,
            "In a relationship, both partners may try to change for each other, but eventually they may feel exhausted and unable to maintain a long-term relationship"),
        LoveTestMessage(21, 35,
            "Overall, both are stubborn but in different ways, lacking patience and understanding. Feelings are hurt and distance appears in the relationship."),
        LoveTestMessage(36, 50,
            "The couple is not too intense, nor too sweet and mellow; they are together, cultivating love with sympathy, understanding and connection."),
        LoveTestMessage(51, 70,
            "A beautiful, peaceful and harmonious love affair like a calm sea. They stay together, silently trying to achieve their goals. They stay together, silently trying to achieve their goals."),
        LoveTestMessage(71, 90,
            "Love each other in our own way, protect each other from the smallest things. Even when far away, both halves silently watch, waiting to support the other."),
        LoveTestMessage(91, 100,
            "They have many things in common, attracted to each other by harmony and passion. The couple is always full of bold ideas and willing to spend time implementing them together.")
    )
    fun getLoveTestMessages(): List<LoveTestMessage> = listOf(
        LoveTestMessage(0, 20,
            "You should be friends not fall in love! It will either be the best relationship of your life, or you will lose that person forever. Sometimes, the greatest love stories are the ones that remain unspoken between best friends"),
        LoveTestMessage(21, 50,
            "More than friends, we should be our loves! Love story should be started writing now! Love is like the wind, you can't see it but you can feel it."),
        LoveTestMessage(51, 80,
            "I want to hold you close to me and feel our hearts beat as one forever! You call it madness, but I call it love. We can only learn to love by loving"),
        LoveTestMessage(81, 100,
            "I was born to be your lover! You are the apple of my eyes! We must be our own before we can be another’s. My soul and your soul are forever tangled...")
    )
    fun observeMemories() = appDatabase.memoryDao().observeMemories()
    suspend fun saveMemory(memory: Memory) = appDatabase.memoryDao().save(memory)
    fun getListBackground(): List<Int> = listOf(
        R.drawable.default1,
        R.drawable.default2
    )
    fun getListPermission(): List<PermissionModel> = listOf(
        PermissionModel(R.string.permission_allow_access)
    )
}
