package com.example.afica.lovetest.component.main.lovetestfragment

import com.example.afica.lovetest.utils.setOnClickWithPressEffect

import android.content.Intent
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import com.example.afica.lovetest.R
import com.example.afica.lovetest.ads.InterAdsUtils
import com.example.afica.lovetest.base.fragment.BaseFragment
import com.example.afica.lovetest.component.lovetest.NameTestActivity
import com.example.afica.lovetest.component.lovetest.HoroscopeTestActivity
import com.example.afica.lovetest.databinding.FragmentLoveTestBinding
import com.example.afica.lovetest.utils.setOnSingleClickListener

class LoveTestFragment : BaseFragment<FragmentLoveTestBinding>() {
    private var waitingForInter = false
    override fun provideViewBinding(container: ViewGroup?): FragmentLoveTestBinding =
        FragmentLoveTestBinding.inflate(layoutInflater, container, false)

    override fun initViews() {
        val bottomPadding = viewBinding.root.paddingBottom
//        ViewCompat.setOnApplyWindowInsetsListener(viewBinding.root) { view, insets ->
//            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            view.setPadding(0, bars.top, 0, bottomPadding)
//            insets
//        }
        viewBinding.btnNameTest.setOnSingleClickListener(3000L) {
            showInter {
                startActivity(Intent(requireContext(), NameTestActivity::class.java))
            }
        }
        viewBinding.btnHoroscopeTest.setOnSingleClickListener(3000L) {
            InterAdsUtils.showInter(requireActivity(), InterAdsUtils.Placement.INTER_LOVE) {
                startActivity(Intent(requireContext(), HoroscopeTestActivity::class.java))
            }
        }
        val title = getString(R.string.love_test_title)
        viewBinding.tvTitle.text = HtmlCompat.fromHtml(
            getString(R.string.love_test_title),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
    }

    private fun showInter(action: () -> Unit) {
        if (waitingForInter) return
        val owner = viewLifecycleOwner
        waitingForInter = true
        InterAdsUtils.showInter(requireActivity(), InterAdsUtils.Placement.INTER_LOVE) {
            if (!isAdded || owner.lifecycle.currentState == Lifecycle.State.DESTROYED) {
                return@showInter
            }
            waitingForInter = false
            action()
        }
    }

    override fun onDestroyView() {
        waitingForInter = false
        super.onDestroyView()
    }
}
