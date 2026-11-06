package com.ads.admob.helper.appoppen.params


open class AdAppOpenState {
     object None : AdAppOpenState()
     object Fail : AdAppOpenState()
     object Loading : AdAppOpenState()
     object Loaded : AdAppOpenState()
     object ShowFail : AdAppOpenState()
     object Showed : AdAppOpenState()
     object Cancel : AdAppOpenState()
}