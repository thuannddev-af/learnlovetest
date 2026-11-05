package com.example.afica.lovetest.domain.usecase

import com.example.afica.lovetest.data.model.Language
import com.example.afica.lovetest.data.repositores.LocalRespository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GetListLanguageUseCase @Inject constructor(private val localResposity : LocalRespository) :
    UseCase<GetListLanguageUseCase.Param, List<Language>>()
{
    open class Param : UseCase.Param()

    override suspend fun execute(param: Param): List<Language> = withContext(Dispatchers.IO) {
        localResposity.getListLanguage()
    }
}