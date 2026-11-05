package com.example.afica.lovetest.domain.usecase

import com.example.afica.lovetest.data.model.OnBoarding
import com.example.afica.lovetest.data.repositores.LocalRespository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GetListOnBoardingUseCase @Inject constructor(private val localRepository: LocalRespository) :
    UseCase<GetListOnBoardingUseCase.Param, List<OnBoarding>>() {

    open class Param : UseCase.Param()

    override suspend fun execute(param: Param): List<OnBoarding> = withContext(Dispatchers.IO) {
        localRepository.getListOnBoarding()
    }
}