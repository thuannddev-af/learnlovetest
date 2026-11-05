package com.example.afica.lovetest.domain.usecase

import com.example.afica.lovetest.data.model.LoveTestResult
import com.example.afica.lovetest.data.repositores.LocalRespository
import javax.inject.Inject

class GetLoveTestResultUseCase @Inject constructor(private val repository: LocalRespository) :
    UseCase<GetLoveTestResultUseCase.Param, LoveTestResult>() {
    class Param(val percentage: Int, val isHoroscope: Boolean = false) : UseCase.Param()

    override suspend fun execute(param: Param): LoveTestResult {
        require(param.percentage in 0..100)
        val messages = if (param.isHoroscope) repository.getHoroscopeMessages() else repository.getLoveTestMessages()
        val message = messages.single {
            param.percentage in it.minPercentage..it.maxPercentage
        }
        return LoveTestResult(param.percentage, message.message)
    }
}
