package com.example.afica.lovetest.component.lovetest

import com.example.afica.lovetest.utils.Constant

import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.afica.lovetest.base.viewmodel.BaseViewModel
import com.example.afica.lovetest.data.model.LoveTestResult
import com.example.afica.lovetest.domain.usecase.GetLoveTestResultUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class NameTestResultViewModel @Inject constructor(
    private val getResult: GetLoveTestResultUseCase,
    private val savedState: SavedStateHandle
) : BaseViewModel() {
    private val resultFlow = MutableStateFlow(
        savedState.get<Int>(PERCENTAGE)?.let { score ->
            savedState.get<String>(MESSAGE)?.let { LoveTestResult(score, it) }
        }
    )
    val result = resultFlow.asStateFlow()
    private var calculation: Job? = null
    private val isHoroscope = savedState.get<Boolean>(Constant.EXTRA_IS_HOROSCOPE) == true

    init {
        if (resultFlow.value == null) startTest(false)
    }

    fun retry() {
        if (calculation?.isActive != true) startTest(true)
    }

    fun remainingLoadingMillis(): Long =
        ((savedState.get<Long>(DEADLINE) ?: SystemClock.elapsedRealtime()) -
            SystemClock.elapsedRealtime()).coerceIn(0L, DURATION_MS)

    val loadingDurationMillis: Long get() = DURATION_MS

    private fun startTest(retry: Boolean) {
        val score = if (retry) Random.nextInt(101)
            else savedState.get<Int>(TARGET_SCORE) ?: Random.nextInt(101)
        val deadline = if (retry) SystemClock.elapsedRealtime() + DURATION_MS
            else savedState.get<Long>(DEADLINE) ?: (SystemClock.elapsedRealtime() + DURATION_MS)
        savedState[TARGET_SCORE] = score
        savedState[DEADLINE] = deadline
        savedState.remove<Int>(PERCENTAGE)
        savedState.remove<String>(MESSAGE)
        resultFlow.value = null
        calculation = viewModelScope.launch {
            delay((deadline - SystemClock.elapsedRealtime()).coerceIn(0L, DURATION_MS))
            val completed = getResult.execute(GetLoveTestResultUseCase.Param(score, isHoroscope))
            savedState[PERCENTAGE] = completed.percentage
            savedState[MESSAGE] = completed.message
            resultFlow.value = completed
        }
    }

    companion object {
        private const val DURATION_MS = 5_000L
        private const val TARGET_SCORE = "target_score"
        private const val DEADLINE = "test_deadline"
        private const val PERCENTAGE = "result_percentage"
        private const val MESSAGE = "result_message"
    }
}
