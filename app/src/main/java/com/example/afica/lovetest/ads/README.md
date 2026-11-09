# Calling ads

Call these functions on the main thread after MobileAds initialization and Remote Config initialization (currently done by SplashActivity). Each object checks the global ads flag and its placement flag. The IDs are in AdsConfig.

```kotlin
// An ordinary interstitial: load, show, then continue.
InterAdsUtils.showInter(this) {
    openNextScreen()
}

// Splash: preload while dots animate; show only after animation completes.
InterAdsUtils.loadInter(this, InterAdsUtils.Placement.SPLASH) { loaded ->
    // Store readiness and wait for the splash animation.
}
InterAdsUtils.showInter(this, InterAdsUtils.Placement.SPLASH) {
    openNextScreen()
}

// Activity: adContainer is a dedicated FrameLayout in the screen layout.
NativeAdsUtils.showNative(this, binding.adContainer, NativeAdsUtils.Placement.LANGUAGE)
NativeAdsUtils.showNative(this, binding.adContainer, NativeAdsUtils.Placement.ONBOARDING)
BannerAdsUtils.showBanner(this, binding.bannerContainer)

// Fragment: release the ad when the fragment view is destroyed.
NativeAdsUtils.showNative(requireActivity(), binding.adContainer,
    NativeAdsUtils.Placement.LANGUAGE, viewLifecycleOwner)
BannerAdsUtils.showBanner(requireActivity(), binding.bannerContainer, viewLifecycleOwner)
```

Native and banner ads load and display with one call, hide their container when disabled or failed, and are automatically destroyed with their lifecycle owner. To remove them earlier, call `NativeAdsUtils.destroy(container)` or `BannerAdsUtils.destroy(container)`.

Interstitial completion runs after dismissal, failure, or disabled ads. Splash retains its existing timeout and navigation guard. Native and banner utilities do not add placements to screens automatically; call them in the screen that needs the ad.
