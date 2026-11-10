package com.example.afica.lovetest.domain.layer

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class AdConfig(
    @SerializedName("inter_splash") @Expose var enableInterSplash: Boolean = true,
    @SerializedName("native_splash") @Expose var enableNativeSplash: Boolean = true,
    @SerializedName("native_language") @Expose var enableNativeLanguage: Boolean = true,
    @SerializedName("native_onboarding") @Expose var enableNativeOnBoarding: Boolean = true,
    @SerializedName("native_onboarding_fullscreen") @Expose var enableNativeFullscreen: Boolean = true,
    @SerializedName("native_main") @Expose var enableNativeMain: Boolean = true,
    @SerializedName("appopen_resume") @Expose var enableOpenApp: Boolean = true,
    @SerializedName("inter_main") @Expose var enableInterMain: Boolean = true,
    @SerializedName("app_open_main") @Expose var enableAppOpenMain: Boolean = true,
    @SerializedName("reward_unlock") @Expose var enableRewardUnlock: Boolean = true,
    @SerializedName("banner_main") @Expose var enableBannerMain: Boolean = true,

)
