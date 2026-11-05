package com.example.afica.lovetest.component.permission

import dagger.hilt.android.lifecycle.HiltViewModel
import com.example.afica.lovetest.base.viewmodel.BaseViewModel
import com.example.afica.lovetest.domain.usecase.GetListPermissionUseCase
import javax.inject.Inject

@HiltViewModel
class PermissionViewModel
@Inject
constructor(
    private val getListPermissionUseCase: GetListPermissionUseCase
)
    : BaseViewModel() {
}
