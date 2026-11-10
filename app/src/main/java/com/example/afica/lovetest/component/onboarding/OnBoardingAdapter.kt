package com.example.afica.lovetest.component.onboarding

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.afica.lovetest.R
import com.example.afica.lovetest.ads.NativeAdsUtils
import com.example.afica.lovetest.ads.RemoteConfigManager
import com.example.afica.lovetest.data.model.OnBoarding
import com.example.afica.lovetest.databinding.ItemOnBoardingBinding
import com.example.afica.lovetest.databinding.ItemOnboardingNativeBinding
import com.example.afica.lovetest.utils.setOnClickWithPressEffect

class OnBoardingAdapter(private val activity: FragmentActivity) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private data class Page(
        val content: OnBoarding? = null, val contentIndex: Int = 0,
        val placement: NativeAdsUtils.Placement? = null
    )

    private var contents = emptyList<OnBoarding>()
    private val pages = mutableListOf<Page>()
    private val failedAds = mutableSetOf<NativeAdsUtils.Placement>()
    var onNextClick: ((Int) -> Unit)? = null
    var onAdFailed: ((Int) -> Unit)? = null

    fun setData(items: ArrayList<OnBoarding>) {
        if (contents == items) return
        contents = items.toList()
        pages.clear()
        val showAds = RemoteConfigManager().canShowAd(RemoteConfigManager.NATIVE_ONBOARDING)
        items.forEachIndexed { index, content ->
            pages.add(Page(content, index))
            if (showAds && index < items.lastIndex && index < 2) {
                val placement = if (index == 0) NativeAdsUtils.Placement.ONBOARDING
                else NativeAdsUtils.Placement.ONBOARDING_SECOND
                pages.add(Page(placement = placement))
            }
        }
        notifyDataSetChanged()
    }

    fun isFailedAd(position: Int): Boolean = pages.getOrNull(position)?.placement in failedAds
    override fun getItemCount() = pages.size
    override fun getItemViewType(position: Int) = if (pages[position].placement == null) 0 else 1

    private class ContentHolder(val binding: ItemOnBoardingBinding) :
        RecyclerView.ViewHolder(binding.root)

    private class AdHolder(val binding: ItemOnboardingNativeBinding) :
        RecyclerView.ViewHolder(binding.root) {
        var placement: NativeAdsUtils.Placement? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == 0) ContentHolder(
            ItemOnBoardingBinding.inflate(
                inflater,
                parent,
                false
            )
        )
        else AdHolder(ItemOnboardingNativeBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val page = pages[position]
        if (holder is ContentHolder) {
            val item = requireNotNull(page.content)
            with(holder.binding) {
                imgBoarding.setImageResource(item.imageId)
                tvTitle.setText(item.title)
                tvDescription.setText(item.description)
                val dotSize = root.resources.getDimensionPixelSize(R.dimen.onboarding_dot_size)
                val selectedWidth =
                    root.resources.getDimensionPixelSize(R.dimen.onboarding_dot_selected_width)
                for (index in 0 until onboardingDots.childCount) {
                    val dot = onboardingDots.getChildAt(index)
                    val selected = index == page.contentIndex
                    dot.setBackgroundResource(if (selected) R.drawable.onboarding_dot_selected else R.drawable.dot_normal)
                    dot.layoutParams = dot.layoutParams.apply {
                        width = if (selected) selectedWidth else dotSize
                        height = dotSize
                    }
                }
                btnNext.setOnClickWithPressEffect {
                    holder.bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }
                        ?.let { onNextClick?.invoke(it) }
                }
            }
        } else if (holder is AdHolder) {
            val placement = requireNotNull(page.placement)
            holder.binding.btnNext.setOnClickWithPressEffect {
                holder.bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }
                    ?.let { onNextClick?.invoke(it) }
            }
            if (holder.placement == placement) return
            holder.placement = placement


            NativeAdsUtils.showNativeOnboardingFullscreen(
                activity, holder.binding.nativeContainer,
                slot = if (placement == NativeAdsUtils.Placement.ONBOARDING) 1 else 2,
                onFailed = {
                    failedAds.add(placement)
                    val index = pages.indexOfFirst { it.placement == placement }
                    if (index >= 0) onAdFailed?.invoke(index)
                })
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        if (holder is AdHolder) {
            NativeAdsUtils.destroy(holder.binding.nativeContainer)
            holder.placement = null
        }
        super.onViewRecycled(holder)
    }
}
