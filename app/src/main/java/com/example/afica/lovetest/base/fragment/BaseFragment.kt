package com.example.afica.lovetest.base.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.example.afica.lovetest.base.activity.BaseActivity

abstract class BaseFragment<V : ViewBinding> : Fragment() {
    private var binding: V? = null
    protected val viewBinding: V
        get() = checkNotNull(binding) { "ViewBinding is only available between onCreateView and onDestroyView" }
    protected var restoredState: Bundle? = null
        private set
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = provideViewBinding(container)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        restoredState = savedInstanceState
        viewBinding.root.isClickable = true
        initViews()
        initData()
        initObserver()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
        restoredState = null
    }

    fun addFragment(id: Int, fragment: Fragment) {
        (activity as? BaseActivity<*>)?.addFragment(id, fragment)
    }

    fun replaceFragment(id: Int, fragment: Fragment) {
        (activity as? BaseActivity<*>)?.replaceFragment(id, fragment)
    }

//    fun showLoading() {
//        (activity as? BaseActivity<*>)?.showLoading()
//    }

//    fun hideLoading() {
//        (activity as? BaseActivity<*>)?.hideLoading()
//    }

//    fun showToast(mes: String, duration: Int = Toast.LENGTH_SHORT) {
//        (activity as? BaseActivity<*>)?.showToast(mes, duration)
//    }

//    fun showInterAd(action: () -> Unit) {
//        (activity as? BaseActivity<*>)?.showInterAd(action) ?: action.invoke()
//    }

//    fun showRewardAd(action: () -> Unit) {
//        (activity as? BaseActivity<*>)?.showRewardAd(action) ?: action.invoke()
//    }

    abstract fun provideViewBinding(container: ViewGroup?): V
    open fun initViews() {}
    open fun initData() {}

    open fun initObserver() {}

}
