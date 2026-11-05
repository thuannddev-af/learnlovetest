package com.example.afica.lovetest.domain.usecase

import com.example.afica.lovetest.data.repositores.LocalRespository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GetListBackgroundUseCase @Inject constructor(private val localRepository: LocalRespository) :
    UseCase<GetListBackgroundUseCase.Param, List<Int>>() {
    class Param : UseCase.Param()

    override suspend fun execute(param: Param): List<Int> = withContext(Dispatchers.IO) {
        localRepository.getListBackground()
    }
}
