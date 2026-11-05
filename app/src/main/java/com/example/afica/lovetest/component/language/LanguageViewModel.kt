package com.example.afica.lovetest.component.language

import androidx.lifecycle.viewModelScope
import com.example.afica.lovetest.base.viewmodel.BaseViewModel
import com.example.afica.lovetest.data.model.Language
import com.example.afica.lovetest.domain.usecase.GetListLanguageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LanguageViewModel @Inject constructor(
    private val getListLanguageUseCase: GetListLanguageUseCase
) : BaseViewModel() {
    private val listLanguageFlow = MutableStateFlow<List<Language>>(emptyList())
    val listLanguage = listLanguageFlow.asStateFlow()

    fun loadListLanguage() {
        viewModelScope.launch {
            listLanguageFlow.value = getListLanguageUseCase.execute(GetListLanguageUseCase.Param())
        }
    }
}
