package com.example.afica.lovetest.component.onboarding

import android.view.LayoutInflater
import android.view.ViewGroup
import com.example.afica.lovetest.R
import com.example.afica.lovetest.base.adapter.BaseAdapter
import com.example.afica.lovetest.data.model.OnBoarding
import com.example.afica.lovetest.databinding.ItemOnBoardingBinding

class OnBoardingAdapter : BaseAdapter<OnBoarding, ItemOnBoardingBinding>() {
    var onNextClick: ((Int) -> Unit)? = null

    override fun binData(viewBinding: ItemOnBoardingBinding, item: OnBoarding, position: Int) {
        with(viewBinding) {
            imgBoarding.setImageResource(item.imageId)
            tvTitle.setText(item.title)
            tvDescription.setText(item.description)
            val dotSize = root.resources.getDimensionPixelSize(R.dimen.onboarding_dot_size)
            val selectedWidth = root.resources.getDimensionPixelSize(R.dimen.onboarding_dot_selected_width)
            for (index in 0 until onboardingDots.childCount) {
                val dot = onboardingDots.getChildAt(index)
                val selected = index == position
                dot.setBackgroundResource(if (selected) R.drawable.onboarding_dot_selected else R.drawable.dot_normal)
                dot.layoutParams = dot.layoutParams.apply {
                    width = if (selected) selectedWidth else dotSize
                    height = dotSize
                }
            }
            btnNext.setOnClickListener { onNextClick?.invoke(position) }
        }
    }

    override fun provideViewBinding(parent: ViewGroup): ItemOnBoardingBinding =
        ItemOnBoardingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
}
