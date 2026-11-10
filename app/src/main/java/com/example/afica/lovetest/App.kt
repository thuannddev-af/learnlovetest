package com.example.afica.lovetest

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.lifecycle.ProcessLifecycleOwner
import com.ads.admob.admob.AdmobFactory
import com.ads.admob.cmp.ConsentManager
import com.ads.admob.cmp.interfaces.OnConsentResponse
import com.ads.admob.config.AfAdConfig
import com.ads.admob.config.AfAdjustConfig
import com.ads.admob.config.NetworkProvider
import com.ads.admob.data.ContentAd
import com.ads.admob.helper.appoppen.AppResumeAdConfig
import com.ads.admob.helper.appoppen.AppResumeAdHelper
import com.ads.admob.listener.AdmobCallBack
import com.ads.admob.listener.AppOpenAdCallBack
import com.example.afica.lovetest.BuildConfig
import com.example.afica.lovetest.ads.AdPlacement
import com.example.afica.lovetest.component.main.MainActivity
import com.example.afica.lovetest.component.splash.SplashActivity
import com.example.afica.lovetest.utils.Constant
import com.example.afica.lovetest.utils.FirebaseConfigManager
import com.example.afica.lovetest.utils.LocaleHelper
import com.example.afica.lovetest.utils.Logger
import com.example.afica.lovetest.utils.SharedPreferenceUtils
import com.example.afica.lovetest.utils.SpManager
import com.example.afica.lovetest.utils.getDeviceLanguage
import com.google.android.gms.ads.AdActivity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@HiltAndroidApp
@Singleton
class App : Application(), Application.ActivityLifecycleCallbacks {
    @Inject
    lateinit var spManager: SpManager

    private var isColdStart = true
    private var firstActivityCreated = false

    companion object {
        var context: Context? = null
        private var mInstance: App? = null
        val instance get() = mInstance
        var appResumeAdHelper: AppResumeAdHelper? = null
            private set
    }

    private fun initAppOpenAd(): AppResumeAdHelper {
        val listClassInValid = mutableListOf<Class<*>>()
        listClassInValid.add(AdActivity::class.java)
        listClassInValid.add(SplashActivity::class.java)
        val config = AppResumeAdConfig(
            idAds = BuildConfig.appopen_resume,
            listClassInValid = listClassInValid,
            canShowAds = true,
            networkProvider = NetworkProvider.ADMOB,
            adPlacement = AdPlacement.OPEN_RESUME,
            reloadIfFirstFail = true
        )
        return AppResumeAdHelper(
            application = this,
            lifecycleOwner = ProcessLifecycleOwner.get(),
            config = config
        )
    }

    override fun onCreate() {
        super<Application>.onCreate()
        mInstance = this
        context = applicationContext
        initAdmob()
//        initBilling()
        FirebaseApp.initializeApp(this)
        FirebaseConfigManager.instance().fetch()
        registerActivityLifecycleCallbacks(this)

    }

    private fun handleColdStart(activity: Activity) {
        if (firstActivityCreated) return
        firstActivityCreated = true

        if (activity !is SplashActivity) {
            initAdmob()
        }
    }

    fun initConsentManager(activity: Activity, action: () -> Unit) {
        val consentManager = ConsentManager.getInstance(activity)
        consentManager.initReleaseConsent(onConsentResponse = object : OnConsentResponse {
            override fun onResponse(errorMessage: String?) {
                Logger.e("onResponse: $errorMessage -- ${consentManager.canRequestAds} ")
                initOpenResume()
                action.invoke()
            }

            override fun onPolicyRequired(isRequired: Boolean) {
                Logger.e("onPolicyRequired: $isRequired")
                spManager.putBoolean(Constant.KEY_SP_IS_SHOW_UMP_SETTING, isRequired)
            }
        })
    }

    private fun initAdmob() {
        Logger.e("initAdmob")
        val afAdjustConfig = AfAdjustConfig.Build(
            adjustToken = "kthfy8dfgkqo",
            adRevenueKey = "",
            environmentProduct = true
        ).build()
        val afAdConfig = AfAdConfig.Builder(afAdjustConfig = afAdjustConfig)
            .intervalBetweenInterstitial(1000)
            .buildVariantProduce(false)
            .mediationProvider(NetworkProvider.ADMOB)
//            .eventConfig(EventConfig(25_900, "VND")) // config exchangeRate  ?current = 1 usd
            .listTestDevices(arrayListOf("45EFA44EA19BAA15F2E090E74382177F"))
            .build()
        AdmobFactory.INSTANCE.initAdmob(this, afAdConfig, adCallback = object : AdmobCallBack {
            override fun initialized() {
                Logger.e("Admob initialized")
            }
        })
    }

    private fun initOpenResume() {
        appResumeAdHelper = initAppOpenAd()
        appResumeAdHelper?.registerAdListener(object : AppOpenAdCallBack {
            override fun onAppOpenAdShow() {
                Logger.e("onAppOpenAdShow: ")
            }

            override fun onAppOpenAdClose() {
                Logger.e("onAppOpenAdClose: ")
            }

            override fun onAdLoaded(data: ContentAd) {
                Logger.e("onAdLoaded: ")
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                Logger.e("onAdFailedToLoad: ")
            }

            override fun onAdClicked() {
                Logger.e("onAdClicked: ")
            }

            override fun onAdImpression() {
                Logger.e("onAdImpression: ")
            }

            override fun onAdFailedToShow(adError: AdError) {
                Logger.e("onAdFailedToShow: ")
            }
        })
    }

//    private fun initBilling() {
//        val listPurchaseItem: MutableList<IapItem> = ArrayList()
//        listPurchaseItem.add(IapItem(Constant.IAP_PREMIUM_UPGRADE, IapType.PURCHASE))
//        listPurchaseItem.add(IapItem(Constant.IAP_WEEKLY, IapType.SUBSCRIPTION))
//        IapFactory.initialize(this, listPurchaseItem, false)
//        IapFactory.getInstance()
//            .registerBillingClientConnectionListener(object : BillingClientConnectionListener {
//                override fun onConnected(status: Boolean, billingResponseCode: Int) {
//                    Logger.e("onConnected: $status $billingResponseCode")
//                }
//            })
//
//        IapFactory.getInstance().registerPurchaseServiceListener(object : PurchaseServiceListener {
//            override fun onPricesUpdated(iapKeyPrices: Map<String, List<DataWrappers.ProductDetails>>) {
//                Logger.e("onPricesUpdated: ${IapFactory.getInstance().isProductPurchased()}")
//            }
//
//            override fun onProductPurchased(purchaseInfo: DataWrappers.PurchaseInfo?) {
//                Logger.e("onProductPurchased: ")
//                initPurchased(IapFactory.getInstance().isProductPurchased())
//                if (IapFactory.getInstance().isProductPurchased() && purchaseInfo?.sku == Constant.IAP_PREMIUM_UPGRADE) {
//                    spManager.setPremiumUpgraded(true)
//                }
//            }
//
//            override fun onProductRestored(purchaseInfo: DataWrappers.PurchaseInfo?) {
//                Logger.e("onProductRestored: ")
//                initPurchased(IapFactory.getInstance().isProductPurchased())
//            }
//
//            override fun onPurchaseFailed(
//                purchaseInfo: DataWrappers.PurchaseInfo?,
//                billingResponseCode: Int?
//            ) {
//                Logger.e("onPurchaseFailed: $billingResponseCode")
//            }
//        })
//    }
//
//    private fun initPurchased(isPurchased: Boolean) {
//        CoroutineScope(Dispatchers.Main).launch {
//            if (isPurchased) {
//                AdmobFactory.INSTANCE.cancelRequestAndShowAllAds()
//            }
//            NativeAdsUtils.cancelAllAds(isPurchased)
//            context?.let { RewardAdsUtils.cancelAllAds(it, isPurchased) }
//            context?.let { InterAdsUtils.cancelAllAds(it, isPurchased) }
//            BannerAdsUtils.cancelAllAds(isPurchased)
//            appResumeAdHelper?.cancelRequestAndShowAllAds(isPurchased)
//            spManager.setPurchased(isPurchased)
//            Logger.e("initPurchased: $isPurchased")
//        }
//    }

    fun destroyAppOpenAd() {
        appResumeAdHelper?.unregisterAllAdListener()
        appResumeAdHelper?.destroyAds()
        appResumeAdHelper = null
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        val locale = getDeviceLanguage()
        val language = spManager.getLanguage()
        LocaleHelper.onAttach(this, language.languageCode)
        super.onConfigurationChanged(newConfig)
    }

    override fun attachBaseContext(base: Context?) {
        val context = LocaleHelper.onAttach(base, Locale.getDefault().language)
        super.attachBaseContext(context)
    }
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (isColdStart) {
            isColdStart = false
            handleColdStart(activity)
        }
    }


    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {
        if (activity is MainActivity) {
            destroyAppOpenAd()
        }
    }
}