package com.example.afica.lovetest.component.main

import android.graphics.Color

import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.afica.lovetest.R
import com.example.afica.lovetest.ads.BannerAdsUtils
import com.example.afica.lovetest.base.activity.BaseActivity
import com.example.afica.lovetest.component.main.homefragment.HomeFragment
import com.example.afica.lovetest.component.main.lovetestfragment.LoveTestFragment
import com.example.afica.lovetest.component.main.memory.MemoryFragment
import com.example.afica.lovetest.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding>() {
    private val pageCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) { updateNavigation(position) }
    }

    override fun provideViewBinding(): ActivityMainBinding = ActivityMainBinding.inflate(layoutInflater)

    override fun initViews() {
//        ViewCompat.setOnApplyWindowInsetsListener(viewBaseActivity.root) { view, insets ->
//            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            view.setPadding(bars.left, 0, bars.right, bars.bottom)
//            insets
//        }
        viewBaseActivity.viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = 3
            override fun createFragment(position: Int): Fragment = when (position) {
                0 -> HomeFragment()
                1 -> LoveTestFragment()
                else -> MemoryFragment()
            }
        }
        viewBaseActivity.viewPager.registerOnPageChangeCallback(pageCallback)
        listOf(viewBaseActivity.tabHome, viewBaseActivity.tabLoveTest, viewBaseActivity.tabMemory)
            .forEachIndexed { index, tab ->
                tab.isFocusable = true
                tab.setOnClickListener { viewBaseActivity.viewPager.setCurrentItem(index, false) }
            }
        updateNavigation(viewBaseActivity.viewPager.currentItem)
        showBanner()
    }

    private fun updateNavigation(position: Int) {
        listOf(viewBaseActivity.tabHome, viewBaseActivity.tabLoveTest, viewBaseActivity.tabMemory)
            .forEachIndexed { index, tab -> tab.isSelected = index == position }
        WindowInsetsControllerCompat(window, viewBaseActivity.root).apply {
            isAppearanceLightStatusBars = position != 0
            isAppearanceLightNavigationBars = true
        }
        if (position == 0) viewBaseActivity.root.setBackgroundColor(Color.parseColor("#FFCEE1"))
        else viewBaseActivity.root.setBackgroundResource(R.drawable.splash_background)
    }

    override fun onDestroy() {
        viewBaseActivity.viewPager.unregisterOnPageChangeCallback(pageCallback)
        super.onDestroy()
    }
    private fun showBanner() {
        BannerAdsUtils.showBanner(
            this,
            viewBaseActivity.frAdsBanner,
            BannerAdsUtils.Placement.BANNER_HOME
        )
    }
}

