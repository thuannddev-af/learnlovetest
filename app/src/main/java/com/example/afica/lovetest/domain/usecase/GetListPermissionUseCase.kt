package com.example.afica.lovetest.domain.usecase

import com.example.afica.lovetest.data.model.PermissionModel
import com.example.afica.lovetest.data.repositores.LocalRespository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GetListPermissionUseCase @Inject constructor(private val localRepository: LocalRespository) :
    UseCase<GetListPermissionUseCase.Param, List<PermissionModel>>() {
    open class Param : UseCase.Param()

    override suspend fun execute(param: Param): List<PermissionModel> =
        withContext(Dispatchers.IO) {
            localRepository.getListPermission()
        }
}
