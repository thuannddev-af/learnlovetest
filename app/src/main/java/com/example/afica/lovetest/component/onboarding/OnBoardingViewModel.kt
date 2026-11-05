package com.example.afica.lovetest.component.onboarding

import androidx.lifecycle.viewModelScope
import com.example.afica.lovetest.base.viewmodel.BaseViewModel
import com.example.afica.lovetest.data.model.OnBoarding
import com.example.afica.lovetest.domain.usecase.GetListOnBoardingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnBoardingViewModel @Inject constructor(private val getListOnBoardingUseCase: GetListOnBoardingUseCase) : BaseViewModel() {

    private val listOnBoardingFlow = MutableStateFlow<List<OnBoarding>>(emptyList())
    var listOnBoarding = listOnBoardingFlow.asStateFlow()
    var currentPosition = 0

    fun getListOnBoarding() {
        viewModelScope.launch {
            listOnBoardingFlow.value = getListOnBoardingUseCase.execute(GetListOnBoardingUseCase.Param())
        }
    }
}