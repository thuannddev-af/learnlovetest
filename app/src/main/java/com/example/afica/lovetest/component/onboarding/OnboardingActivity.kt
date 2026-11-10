package com.example.afica.lovetest.component.onboarding

import android.content.Intent
import android.os.Bundle

import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import com.example.afica.lovetest.component.permission.PermissionActivity
import com.example.afica.lovetest.base.activity.BaseActivity
import com.example.afica.lovetest.databinding.ActivityOnboardingBinding
import com.example.afica.lovetest.ads.NativeAdsUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OnboardingActivity : BaseActivity<ActivityOnboardingBinding>() {
    private val viewModel: OnBoardingViewModel by viewModels()
    private val onboardingAdapter by lazy { OnBoardingAdapter(this) }
    private var restoredPosition = 0
    private var initialPageRestored = false
    private var swipeDirection = 1
    private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            if (position != viewModel.currentPosition) {
                swipeDirection = if (position > viewModel.currentPosition) 1 else -1
            }
            viewModel.currentPosition = position
            if (onboardingAdapter.isFailedAd(position)) skipFailedAd(position)
        }
    }

    override fun provideViewBinding(): ActivityOnboardingBinding = ActivityOnboardingBinding.inflate(layoutInflater)

    override fun initViews() {
        restoredPosition = restoredState?.getInt(CURRENT_PAGE) ?: viewModel.currentPosition
        viewBaseActivity.viewPager.adapter = onboardingAdapter
        viewBaseActivity.viewPager.isUserInputEnabled = true
        viewBaseActivity.viewPager.registerOnPageChangeCallback(pageChangeCallback)
        onboardingAdapter.onAdFailed = { position -> skipFailedAd(position) }
        onboardingAdapter.onNextClick = { position ->
            val pager = viewBaseActivity.viewPager
            if (position == pager.currentItem && pager.scrollState == ViewPager2.SCROLL_STATE_IDLE) {
                if (position < onboardingAdapter.itemCount - 1) {
                    pager.setCurrentItem(position + 1, true)
                } else {
                    startActivity(Intent(this, PermissionActivity::class.java))
                    finish()
                }
            }
        }
    }

    private fun skipFailedAd(position: Int) {
        viewBaseActivity.viewPager.post {
            val pager = viewBaseActivity.viewPager
            val target = position + swipeDirection
            if (!isFinishing && !isDestroyed && pager.currentItem == position &&
                target in 0 until onboardingAdapter.itemCount) {
                pager.setCurrentItem(target, false)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(CURRENT_PAGE, viewBaseActivity.viewPager.currentItem)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        viewBaseActivity.viewPager.unregisterOnPageChangeCallback(pageChangeCallback)
        viewBaseActivity.viewPager.adapter = null
        super.onDestroy()
    }

    companion object {
        private const val CURRENT_PAGE = "onboarding_current_page"
    }

    override fun initObserver() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.listOnBoarding.collect { pages ->
                    if (pages.isNotEmpty()) {
                        val position = if (initialPageRestored) viewModel.currentPosition else restoredPosition
                        onboardingAdapter.setData(ArrayList(pages))
                        viewBaseActivity.viewPager.setCurrentItem(
                            position.coerceIn(0, onboardingAdapter.itemCount - 1), false)
                        initialPageRestored = true
                    }
                }
            }
        }
    }

    override fun initData() {
        NativeAdsUtils.preloadNative(applicationContext, NativeAdsUtils.Placement.ONBOARDING)
        NativeAdsUtils.preloadNative(applicationContext, NativeAdsUtils.Placement.ONBOARDING_SECOND)
        NativeAdsUtils.preloadNative(applicationContext, NativeAdsUtils.Placement.ONBOARDING_INLINE_2)
        NativeAdsUtils.preloadNative(applicationContext, NativeAdsUtils.Placement.ONBOARDING_INLINE_3)
        viewModel.getListOnBoarding()
    }

}

