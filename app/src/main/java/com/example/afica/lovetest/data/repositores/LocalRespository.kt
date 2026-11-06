package com.example.afica.lovetest.data.repositores

import com.example.afica.lovetest.data.model.Language
import com.example.afica.lovetest.data.model.Memory
import com.example.afica.lovetest.data.model.OnBoarding
import com.example.afica.lovetest.data.model.PermissionModel
import com.example.afica.lovetest.data.source.LocalDataSource
import javax.inject.Inject

class LocalRespository @Inject constructor(
    private val localDataSource: LocalDataSource,
) {
    fun getListLanguage(): List<Language> {
        return localDataSource.getListLanguage()
    }

    //    fun getList() = localDataSource.getList()
    fun getListOnBoarding(): List<OnBoarding> {
        return localDataSource.getListOnBoarding()
    }

    fun getLoveTestMessages() = localDataSource.getLoveTestMessages()
    fun getHoroscopeMessages() = localDataSource.getHoroscopeMessages()
    fun getZodiacSigns() = localDataSource.getZodiacSigns()
    fun observeMemories() = localDataSource.observeMemories()
    suspend fun saveMemory(memory: Memory) = localDataSource.saveMemory(memory)

    fun getListPermission(): List<PermissionModel> = localDataSource.getListPermission()
    fun getListBackground(): List<Int> = localDataSource.getListBackground()

}
