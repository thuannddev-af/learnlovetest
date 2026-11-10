package com.example.afica.lovetest.base.activity

import android.content.Context
import android.os.Bundle
import android.os.Build
import android.view.ViewTreeObserver
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.viewbinding.ViewBinding
import com.example.afica.lovetest.data.model.Language
import com.example.afica.lovetest.utils.LocaleHelper
import com.example.afica.lovetest.utils.SharedPreferenceUtils
import com.example.afica.lovetest.utils.SpManager

abstract class BaseActivity <V : ViewBinding> : AppCompatActivity() {
    lateinit var viewBaseActivity: V
        private set
    protected var restoredState: Bundle? = null
        private set

    lateinit var language: Language
    private var localeDrawBlocker: ViewTreeObserver.OnPreDrawListener? = null
    open fun onBack() {
        finish()
    }

    override fun attachBaseContext(newBase: Context) {
        language = SpManager.getInstance(newBase).getLanguage()
        val context = LocaleHelper.setLocale(newBase, language.languageCode)
        super.attachBaseContext(context)
    }

    override fun onStart() {
        super.onStart()
        if (language.languageCode != SpManager.getInstance().getLanguage().languageCode) {
            localeDrawBlocker = ViewTreeObserver.OnPreDrawListener { false }.also {
                viewBaseActivity.root.viewTreeObserver.addOnPreDrawListener(it)
            }
            intent.putExtra(EXTRA_LOCALE_REFRESH, true)
            window.setWindowAnimations(0)
            disableTransitionAnimations()
            recreate()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.getBooleanExtra(EXTRA_LOCALE_REFRESH, false)) {
            intent.removeExtra(EXTRA_LOCALE_REFRESH)
            window.setWindowAnimations(0)
            disableTransitionAnimations()
        }
        restoredState = savedInstanceState
        enableEdgeToEdge()
        viewBaseActivity = provideViewBinding()
        setContentView(viewBaseActivity.root)
        initViews()
        initData()
        initObserver()
        onBackPressedDispatcher.addCallback(this, backPressedCallback)
    }

    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            onBack()
        }
    }

    fun replaceFragment(id: Int, fragment: Fragment) {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            addToBackStack(fragment::class.java.simpleName)
            replace(id, fragment, fragment::class.java.simpleName)
        }
    }

    fun addFragment(id: Int, fragment: Fragment) {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            addToBackStack(fragment::class.java.simpleName)
            add(id, fragment, fragment::class.java.simpleName)
        }
    }
    abstract fun provideViewBinding(): V

    open fun initViews() {}
    open fun initData() {}
    open fun initObserver() {}

    protected fun disableTransitionAnimations() {
        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    override fun onDestroy() {
        localeDrawBlocker?.let {
            val observer = viewBaseActivity.root.viewTreeObserver
            if (observer.isAlive) observer.removeOnPreDrawListener(it)
        }
        localeDrawBlocker = null
        super.onDestroy()
    }

    private companion object {
        const val EXTRA_LOCALE_REFRESH = "com.example.afica.lovetest.LOCALE_REFRESH"
    }
}
