package com.ads.admob.helper

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.ads.admob.billing.factory.IapFactory
import com.ads.admob.cmp.ConsentManager
import com.ads.admob.helper.params.IAdsParam
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean


abstract class AdsHelper<C : IAdsConfig, P : IAdsParam>(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val config: C
) {

    internal var isCancelRequestAndShowAllAds = false
    fun cancelRequestAndShowAllAds(isPurchased: Boolean){
        isCancelRequestAndShowAllAds = isPurchased
    }
    private var tag: String = context::class.java.simpleName
    internal val flagActive: AtomicBoolean = AtomicBoolean(false)
    internal val lifecycleEventState = MutableStateFlow(Lifecycle.Event.ON_ANY)
    var flagUserEnableReload = true
        set(value) {
            field = value
            logZ("setFlagUserEnableReload($field)")
        }

    init {
        CoroutineScope(Dispatchers.Main).launch {
            lifecycleOwner.lifecycle.addObserver(object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    lifecycleEventState.update { event }
                    when (event) {
                        Lifecycle.Event.ON_DESTROY -> {
                            lifecycleOwner.lifecycle.removeObserver(this)
                        }

                        else -> Unit
                    }
                }
            })
        }
    }

    open fun canShowAds(): Boolean {
        val isPurchased = try {
            IapFactory.getInstance().isProductPurchased()
        } catch (ex: Exception) {
            false
        }
        Log.e("TAG", "canShowAds: ${config.canShowAds && !isCancelRequestAndShowAllAds && !isPurchased}", )
        return config.canShowAds && !isCancelRequestAndShowAllAds && !isPurchased
    }

    open fun canRequestAds(): Boolean {
        val isPurchased = try {
            IapFactory.getInstance().isProductPurchased()
        } catch (ex: Exception) {
            false
        }
        Log.e("TAG", "canRequestAds: ${canShowAds() && isOnline() && ConsentManager.getInstance(context as Activity).getConsentResult(context) && !isCancelRequestAndShowAllAds && !isPurchased}", )
        return canShowAds()
                && isOnline()
                && ConsentManager.getInstance(context as Activity).getConsentResult(context)
                && !isCancelRequestAndShowAllAds
                && !isPurchased
    }

    abstract fun requestAds(param: P)

    abstract fun cancel()

    fun setTagForDebug(tag: String) {
        this.tag = tag
    }

    fun isActiveState(): Boolean {
        return flagActive.get()
    }

    fun canReloadAd(): Boolean {
        return config.canReloadAds && flagUserEnableReload
    }

    internal fun logZ(message: String) {
        Log.d(this::class.java.simpleName, "${tag}: $message")
    }

    internal fun logInterruptExecute(message: String) {
        logZ("$message not execute because has called cancel()")
    }

    internal fun isOnline(): Boolean {
        val netInfo = kotlin.runCatching {
            (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).activeNetworkInfo
        }.getOrNull()
        return netInfo != null && netInfo.isConnected
    }
}

interface IAdsConfig {
    val idAds: String
    val adPlacement : String
    val canShowAds: Boolean
    val canReloadAds: Boolean
    val reloadIfFirstFail: Boolean
}

/**
 * Enum representing the visibility options for advertising elements.
 */
enum class AdOptionVisibility {
    /**
     * The advertising element is not visible and does not occupy any space in the layout.
     */
    GONE,

    /**
     * The advertising element is invisible but still occupies space in the layout.
     */
    INVISIBLE
}