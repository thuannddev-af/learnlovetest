package com.example.afica.lovetest.component.main.memory

import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.afica.lovetest.BuildConfig
import com.example.afica.lovetest.R
import com.example.afica.lovetest.ads.AdPlacement
import com.example.afica.lovetest.ads.InterAdsUtils
import com.example.afica.lovetest.base.fragment.BaseFragment
import com.example.afica.lovetest.component.lovetest.NameTestActivity
import com.example.afica.lovetest.data.repositores.LocalRespository
import com.example.afica.lovetest.databinding.FragmentMemoryBinding
import com.example.afica.lovetest.utils.FirebaseConfigManager
import com.example.afica.lovetest.utils.setOnClickWithPressEffect
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MemoryFragment : BaseFragment<FragmentMemoryBinding>() {
    @Inject
    lateinit var repository: LocalRespository

    private var interdelay = false
    private val memoryAdapter = MemoryAdapter()

    override fun provideViewBinding(container: ViewGroup?): FragmentMemoryBinding =
        FragmentMemoryBinding.inflate(layoutInflater, container, false)

    override fun initViews() {
        val views = viewBinding

        val bottomPadding = views.root.paddingBottom
//        ViewCompat.setOnApplyWindowInsetsListener(views.root) { target, insets ->
//            target.setPadding(0, insets.getInsets(WindowInsetsCompat.Type.systemBars()).top, 0, bottomPadding)
//            insets
//        }
        views.rclMemories.adapter = memoryAdapter
        views.btnAddFirst.setOnClickWithPressEffect {
            showInter {
                startActivity(Intent(requireContext(), MemoryActivity::class.java))
            }
        }

    }

    private fun showInter(action:() -> Unit) {
        if(interdelay) return
        val owner = viewLifecycleOwner
        interdelay = true
//        InterAdsUtils.showInter(requireActivity(), InterAdsUtils.Placement.INTER_ADD) {
//            if(!isAdded && owner.lifecycle.currentState == Lifecycle.State.DESTROYED) {
//                return@showInter
//            }
//            interdelay = false
//            action()
//        }
        InterAdsUtils.loadandShowInterAds(
            requireActivity(), viewLifecycleOwner, BuildConfig.inter_add, AdPlacement.INTER_ADD, isEnable = FirebaseConfigManager.instance().adConfig.enableInterMain
        ) {
            startActivity(
                Intent(requireActivity(), NameTestActivity::class.java)
            )
        }
    }
    override fun onDestroyView() {
        viewBinding.rclMemories.adapter = null

        super.onDestroyView()
    }

    override fun initObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                try {
                    repository.observeMemories().collect { memories ->
                        val current = viewBinding
                        memoryAdapter.setData(ArrayList(memories))
                        current.emptyState.visibility =
                            if (memories.isEmpty()) View.VISIBLE else View.GONE
                        current.rclMemories.visibility =
                            if (memories.isEmpty()) View.GONE else View.VISIBLE
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    Toast.makeText(requireContext(), R.string.memory_load_error, Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

}

