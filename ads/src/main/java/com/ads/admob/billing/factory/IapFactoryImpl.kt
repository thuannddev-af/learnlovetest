package com.ads.admob.billing.factory

import android.app.Activity
import android.app.Application
import android.util.Log
import com.ads.admob.data.IapItem
import com.ads.admob.dialog.IapDialog
import com.ads.admob.listener.BillingClientConnectionListener
import com.ads.admob.listener.PurchaseListener
import com.ads.admob.listener.PurchaseServiceListener
import com.ads.admob.widget.DataWrappers
import com.ads.admob.widget.Security
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.AcknowledgePurchaseResponseListener
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesResult
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

internal class IapFactoryImpl(
    application: Application,
    iapList: List<IapItem>,
    private val debugMode: Boolean = false
) : IapFactory, PurchasesUpdatedListener, AcknowledgePurchaseResponseListener {

    private val billingClient: BillingClient
    private val billingConnectionListeners: CopyOnWriteArrayList<BillingClientConnectionListener> = CopyOnWriteArrayList()
    private val purchaseServiceListeners: CopyOnWriteArrayList<PurchaseServiceListener> = CopyOnWriteArrayList()
    private val productDetailsCache = mutableMapOf<String, ProductDetails?>()
    private val listIapOwned = mutableListOf<DataWrappers.PurchaseInfo>()

    // Tracks which product IDs are consumable, set per buyIap() call
    private val consumableProductIds = mutableSetOf<String>()

    private var decodedKey: String? = null

    private fun BillingResult.isOk() = responseCode == BillingClient.BillingResponseCode.OK

    companion object {
        private val TAG = IapFactoryImpl::class.simpleName
    }

    init {
        val context = application.applicationContext ?: application
        val pendingPurchasesParam = PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases(pendingPurchasesParam)
            .build()
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() {
                Log.d(TAG, "onBillingServiceDisconnected")
            }

            override fun onBillingSetupFinished(billingResult: BillingResult) {
                Log.d(TAG, "onBillingSetupFinished: $billingResult")
                if (billingResult.isOk()) {
                    invokeConnectionListeners { it.onConnected(true, billingResult.responseCode) }
                    // Query INAPP then SUBS sequentially, then restore purchases
                    iapList.filter { it.type == BillingClient.ProductType.INAPP }
                        .syncProductDetails {
                            iapList.filter { it.type == BillingClient.ProductType.SUBS }
                                .syncProductDetails {
                                    Log.d(TAG, "Product details loaded: ${productDetailsCache.size}")
                                    CoroutineScope(Dispatchers.IO).launch { queryAndRestorePurchases() }
                                }
                        }
                } else {
                    Log.e(TAG, "onBillingSetupFinished failed: ${billingResult.debugMessage}")
                    invokeConnectionListeners { it.onConnected(false, billingResult.responseCode) }
                }
            }
        })
    }


    private fun List<IapItem>.syncProductDetails(onDone: () -> Unit) {
        if (!billingClient.isReady || isEmpty()) {
            onDone()
            return
        }
        val productList = map {
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(it.itemId)
                .setProductType(it.type)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()
        billingClient.queryProductDetailsAsync(params) { billingResult, result ->
            if (billingResult.isOk()) {
                result.productDetailsList.forEach { productDetailsCache[it.productId] = it }
                invokeConnectionListeners { it.onConnected(true, billingResult.responseCode) }
            }
            onDone()
        }
    }

    private fun getProductDetails(
        productId: String,
        type: String,
        onResult: (ProductDetails?) -> Unit
    ) {
        if (!billingClient.isReady) {
            Log.w(TAG, "getProductDetails: BillingClient not ready")
            onResult(null)
            return
        }
        val cached = productDetailsCache[productId]
        if (cached != null) {
            onResult(cached)
            return
        }
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(type)
                        .build()
                )
            ).build()
        billingClient.queryProductDetailsAsync(params) { billingResult, result ->
            if (billingResult.isOk()) {
                val details = result.productDetailsList.find { it.productId == productId }
                productDetailsCache[productId] = details
                onResult(details)
            } else {
                Log.e(TAG, "getProductDetails failed for $productId: ${billingResult.debugMessage}")
                onResult(null)
            }
        }
    }

    private fun launchBillingFlow(
        activity: Activity,
        productId: String,
        type: String,
        obfuscatedAccountId: String? = null,
        obfuscatedProfileId: String? = null
    ) {
        getProductDetails(productId, type) { details ->
            if (details == null) {
                Log.e(TAG, "launchBillingFlow: ProductDetails not found for $productId")
                return@getProductDetails
            }
            if (debugMode) {
                activity.runOnUiThread {
                    IapDialog(activity, type, details, object : PurchaseListener {
                        override fun onProductPurchased(purchaseInfo: DataWrappers.PurchaseInfo) {
                            notifyOwned(purchaseInfo, isRestore = false)
                        }

                        override fun displayErrorMessage(errorMsg: String?) {
                            invokePurchaseListeners { it.onPurchaseFailed(null, null) }
                        }

                        override fun onUserCancelBilling() {
                            invokePurchaseListeners { it.onPurchaseFailed(null, null) }
                        }
                    }).show()
                }
            } else {
                val paramsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(details)
                if (type == BillingClient.ProductType.SUBS) {
                    details.subscriptionOfferDetails?.getOrNull(0)?.let {
                        paramsBuilder.setOfferToken(it.offerToken)
                    }
                }
                val flowParamsBuilder = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(listOf(paramsBuilder.build()))
                if (obfuscatedAccountId != null) flowParamsBuilder.setObfuscatedAccountId(obfuscatedAccountId)
                if (obfuscatedProfileId != null) flowParamsBuilder.setObfuscatedProfileId(obfuscatedProfileId)

                val billingResult = billingClient.launchBillingFlow(activity, flowParamsBuilder.build())
                Log.d(TAG, "launchBillingFlow result: $billingResult")
            }
        }
    }

    override fun buyIap(activity: Activity, iapId: String, isConsumable: Boolean) {
        if (isConsumable) consumableProductIds.add(iapId) else consumableProductIds.remove(iapId)
        if (!iapId.isProductReady()) {
            Log.w(TAG, "buyIap: product $iapId is not ready")
            return
        }
        launchBillingFlow(activity, iapId, BillingClient.ProductType.INAPP)
    }

    override fun buySubscription(activity: Activity, subId: String) {
        if (!subId.isProductReady()) {
            Log.w(TAG, "buySubscription: product $subId is not ready")
            return
        }
        launchBillingFlow(activity, subId, BillingClient.ProductType.SUBS)
    }


    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        Log.d(TAG, "onPurchasesUpdated: ${billingResult.responseCode} ${billingResult.debugMessage}")
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                processPurchases(purchases, isRestore = false)
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "onPurchasesUpdated: user canceled")
                invokePurchaseListeners { it.onPurchaseFailed(null, billingResult.responseCode) }
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                Log.d(TAG, "onPurchasesUpdated: item already owned, querying purchases")
                CoroutineScope(Dispatchers.IO).launch { queryAndRestorePurchases() }
            }
            else -> {
                Log.e(TAG, "onPurchasesUpdated: failed ${billingResult.debugMessage}")
                purchases?.map { getPurchaseInfo(it) }?.forEach { info ->
                    invokePurchaseListeners { it.onPurchaseFailed(info, billingResult.responseCode) }
                }
            }
        }
    }

    private fun processPurchases(purchasesList: List<Purchase>?, isRestore: Boolean) {
        if (purchasesList.isNullOrEmpty()) {
            Log.d(TAG, "processPurchases: empty list")
            return
        }
        Log.d(TAG, "processPurchases: ${purchasesList.size} purchase(s), isRestore=$isRestore")
        for (purchase in purchasesList) {
            if (!isSignatureValid(purchase)) {
                Log.w(TAG, "processPurchases: invalid signature for ${purchase.products}")
                invokePurchaseListeners { it.onPurchaseFailed(getPurchaseInfo(purchase), null) }
                continue
            }
            when (purchase.purchaseState) {
                Purchase.PurchaseState.PURCHASED -> handlePurchased(purchase, isRestore)
                Purchase.PurchaseState.PENDING -> Log.d(TAG, "processPurchases: purchase pending ${purchase.products}")
                else -> Log.d(TAG, "processPurchases: unknown state ${purchase.purchaseState}")
            }
        }
    }

    private fun handlePurchased(purchase: Purchase, isRestore: Boolean) {
        val productId = purchase.products.firstOrNull() ?: return
        val type = productDetailsCache[productId]?.productType

        when (type) {
            BillingClient.ProductType.INAPP -> {
                if (consumableProductIds.contains(productId)) {
                    // Consumable: consume trước rồi mới notify (tránh exploit nếu consume fail)
                    consumeAndNotify(purchase, isRestore)
                } else {
                    // Non-consumable: notify ngay, acknowledge async — không chờ round-trip mạng
                    notifyOwned(getPurchaseInfo(purchase), isRestore)
                    acknowledgeIfNeeded(purchase)
                }
            }
            BillingClient.ProductType.SUBS -> {
                // Subscription: notify ngay, acknowledge async
                notifyOwned(getPurchaseInfo(purchase), isRestore)
                acknowledgeIfNeeded(purchase)
            }
            else -> {
                // productType chưa có trong cache (ví dụ restore trước khi query xong)
                // Fallback: notify ngay + acknowledge
                Log.w(TAG, "handlePurchased: productType unknown for $productId, notifying anyway")
                notifyOwned(getPurchaseInfo(purchase), isRestore)
                acknowledgeIfNeeded(purchase)
            }
        }
    }

    private fun consumeAndNotify(purchase: Purchase, isRestore: Boolean) {
        val params = ConsumeParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
        billingClient.consumeAsync(params) { billingResult, _ ->
            if (billingResult.isOk()) {
                notifyOwned(getPurchaseInfo(purchase), isRestore)
            } else {
                Log.e(TAG, "consumeAndNotify failed: ${billingResult.debugMessage}")
                invokePurchaseListeners { it.onPurchaseFailed(getPurchaseInfo(purchase), billingResult.responseCode) }
            }
        }
    }

    private fun acknowledgeIfNeeded(purchase: Purchase) {
        if (purchase.isAcknowledged) return
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken).build()
        billingClient.acknowledgePurchase(params, this)
    }

    override fun onAcknowledgePurchaseResponse(billingResult: BillingResult) {
        Log.d(TAG, "onAcknowledgePurchaseResponse: ${billingResult.responseCode}")
    }

    private fun notifyOwned(purchaseInfo: DataWrappers.PurchaseInfo, isRestore: Boolean) {
        if (!listIapOwned.any { it.purchaseToken == purchaseInfo.purchaseToken }) {
            listIapOwned.add(purchaseInfo)
        }
        if (isRestore) {
            invokePurchaseListeners { it.onProductRestored(purchaseInfo) }
        } else {
            invokePurchaseListeners { it.onProductPurchased(purchaseInfo) }
        }
    }

    // endregion

    // region Restore Purchases

    private suspend fun queryAndRestorePurchases() {
        val inApp: PurchasesResult = billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        )
        processPurchases(inApp.purchasesList, isRestore = true)

        val subs: PurchasesResult = billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        )
        processPurchases(subs.purchasesList, isRestore = true)
    }

    // endregion

    // region Price Helpers

    override fun getPriceById(skuId: String, type: String): String? {
        var price: String? = null
        getProductDetails(skuId, type) {
            price = if (type == BillingClient.ProductType.INAPP) {
                it?.oneTimePurchaseOfferDetails?.formattedPrice
            } else {
                it?.subscriptionOfferDetails?.lastOrNull()
                    ?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
            }
        }
        return price
    }

    override fun getOfferById(skuId: String): String? {
        var offerPrice: String? = null
        getProductDetails(skuId, BillingClient.ProductType.SUBS) { details ->
            details?.subscriptionOfferDetails?.forEach { offer ->
                val trialPhase = offer.pricingPhases.pricingPhaseList.find { it.recurrenceMode == 2 }
                if (trialPhase != null) offerPrice = trialPhase.formattedPrice
            }
        }
        return offerPrice
    }

    override fun getPriceByNumOfWeek(skuId: String, type: String, numWeek: Int): String {
        var priceAmountMicros = 0L
        var currency = "USD"
        getProductDetails(skuId, type) {
            if (type == BillingClient.ProductType.INAPP) {
                priceAmountMicros = it?.oneTimePurchaseOfferDetails?.priceAmountMicros ?: 0L
                currency = it?.oneTimePurchaseOfferDetails?.priceCurrencyCode ?: "USD"
            } else {
                val phase = it?.subscriptionOfferDetails?.lastOrNull()
                    ?.pricingPhases?.pricingPhaseList?.firstOrNull()
                priceAmountMicros = phase?.priceAmountMicros ?: 0L
                currency = phase?.priceCurrencyCode ?: "USD"
            }
        }
        return formatPrice(priceAmountMicros, currency, numWeek)
    }

    fun formatPrice(priceAmountMicros: Long, priceCurrencyCode: String, numWeek: Int): String {
        val price = (priceAmountMicros / 1_000_000.0) / numWeek
        val format = NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
            currency = Currency.getInstance(priceCurrencyCode)
            maximumFractionDigits = 2
            minimumFractionDigits = 0
        }
        var formatted = format.format(price)
        formatted = if (price >= 10) {
            formatted.replace(Regex("([.,]\\d{1,2})(?!\\d)"), "")
        } else {
            formatted.replace(Regex("([.,]00)(?!\\d)"), "")
        }
        return formatted
    }

    // endregion

    // region Query Helpers

    override fun isProductPurchased(): Boolean = listIapOwned.isNotEmpty()

    override fun getProductPurchaseList(): List<DataWrappers.PurchaseInfo> = listIapOwned

    private fun String.isProductReady(): Boolean {
        return productDetailsCache.containsKey(this) && productDetailsCache[this] != null
    }

    private fun isSignatureValid(purchase: Purchase): Boolean {
        val key = decodedKey ?: return true
        return Security.verifyPurchase(key, purchase.originalJson, purchase.signature)
    }

    private fun getPurchaseInfo(purchase: Purchase) = DataWrappers.PurchaseInfo(
        purchaseState = purchase.purchaseState,
        developerPayload = purchase.developerPayload,
        isAcknowledged = purchase.isAcknowledged,
        isAutoRenewing = purchase.isAutoRenewing,
        orderId = purchase.orderId,
        originalJson = purchase.originalJson,
        packageName = purchase.packageName,
        purchaseTime = purchase.purchaseTime,
        purchaseToken = purchase.purchaseToken,
        signature = purchase.signature,
        sku = purchase.products.firstOrNull() ?: "",
        accountIdentifiers = purchase.accountIdentifiers
    )

    // endregion

    // region Listener Management

    override fun registerBillingClientConnectionListener(adCallback: BillingClientConnectionListener) {
        billingConnectionListeners.add(adCallback)
    }

    override fun unregisterBillingClientConnectionListener(adCallback: BillingClientConnectionListener) {
        billingConnectionListeners.remove(adCallback)
    }

    override fun unregisterAllBillingClientConnectionListener() {
        billingConnectionListeners.clear()
    }

    override fun registerPurchaseServiceListener(adCallback: PurchaseServiceListener) {
        purchaseServiceListeners.add(adCallback)
    }

    override fun unregisterPurchaseServiceListener(adCallback: PurchaseServiceListener) {
        purchaseServiceListeners.remove(adCallback)
    }

    override fun unregisterAllPurchaseServiceListener() {
        purchaseServiceListeners.clear()
    }

    private fun invokeConnectionListeners(action: (BillingClientConnectionListener) -> Unit) {
        billingConnectionListeners.forEach(action)
    }

    private fun invokePurchaseListeners(action: (PurchaseServiceListener) -> Unit) {
        purchaseServiceListeners.forEach(action)
    }

}