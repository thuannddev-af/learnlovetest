package com.example.afica.lovetest.component.main.memory

import android.content.Intent
import android.view.ViewGroup
import android.view.View
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.afica.lovetest.base.fragment.BaseFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.afica.lovetest.R
import com.example.afica.lovetest.data.repositores.LocalRespository
import com.example.afica.lovetest.databinding.FragmentMemoryBinding
import com.example.afica.lovetest.utils.setOnClickWithPressEffect
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MemoryFragment : BaseFragment<FragmentMemoryBinding>() {
    @Inject lateinit var repository: LocalRespository

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
        val addMemory = View.OnClickListener { startActivity(Intent(requireContext(), MemoryActivity::class.java)) }
        views.btnAddFirst.setOnClickWithPressEffect { addMemory.onClick(it) }

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
                        current.emptyState.visibility = if (memories.isEmpty()) View.VISIBLE else View.GONE
                        current.rclMemories.visibility = if (memories.isEmpty()) View.GONE else View.VISIBLE
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    Toast.makeText(requireContext(), R.string.memory_load_error, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

}

