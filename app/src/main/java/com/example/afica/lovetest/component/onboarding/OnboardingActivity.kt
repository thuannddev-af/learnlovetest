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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OnboardingActivity : BaseActivity<ActivityOnboardingBinding>() {
    private val viewModel: OnBoardingViewModel by viewModels()
    private val onboardingAdapter = OnBoardingAdapter()
    private var restoredPosition = 0
    private var initialPageRestored = false
    private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            viewModel.currentPosition = position
        }
    }

    override fun provideViewBinding(): ActivityOnboardingBinding = ActivityOnboardingBinding.inflate(layoutInflater)

    override fun initViews() {
        restoredPosition = restoredState?.getInt(CURRENT_PAGE) ?: viewModel.currentPosition
        viewBaseActivity.viewPager.adapter = onboardingAdapter
        viewBaseActivity.viewPager.isUserInputEnabled = true
        viewBaseActivity.viewPager.registerOnPageChangeCallback(pageChangeCallback)
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

//        ViewCompat.setOnApplyWindowInsetsListener(viewBaseActivity.root) { view, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
//            insets
//        }
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
                        viewBaseActivity.viewPager.setCurrentItem(position.coerceIn(pages.indices), false)
                        initialPageRestored = true
                    }
                }
            }
        }
    }

    override fun initData() {
        viewModel.getListOnBoarding()
    }

}

