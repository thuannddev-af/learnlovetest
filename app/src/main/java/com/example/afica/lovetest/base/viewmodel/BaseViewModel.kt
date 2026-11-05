package com.example.afica.lovetest.base.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

open class BaseViewModel: ViewModel() {
    private val _showLoadingEvent = MutableLiveData<Unit>()
    val showLoadingEvent: LiveData<Unit>
        get() = _showLoadingEvent

    private val _hideLoadingEvent = MutableLiveData<Unit>()
    val hideLoadingEvent: LiveData<Unit>
        get() = _hideLoadingEvent

    fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            _showLoadingEvent.postValue(Unit)
        } else {
            _hideLoadingEvent.postValue(Unit)
        }
    }

    protected fun <T : Any> processData(
        response: Result<T>,
        onSuccess: ((T?) -> Unit)?,
        onError: (() -> Unit)? = null
    ) {
        if (response.isSuccess) {
            onSuccess?.invoke(response.getOrNull())
        } else {
            onError?.invoke()
        }
    }

}