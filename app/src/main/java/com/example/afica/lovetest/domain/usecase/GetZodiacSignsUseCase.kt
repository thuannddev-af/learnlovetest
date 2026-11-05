package com.example.afica.lovetest.domain.usecase

import com.example.afica.lovetest.data.model.ZodiacSign
import com.example.afica.lovetest.data.repositores.LocalRespository
import javax.inject.Inject

class GetZodiacSignsUseCase @Inject constructor(private val repository: LocalRespository) :
    UseCase<GetZodiacSignsUseCase.Param, List<ZodiacSign>>() {
    class Param : UseCase.Param()
    override suspend fun execute(param: Param): List<ZodiacSign> = repository.getZodiacSigns()
}
