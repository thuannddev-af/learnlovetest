package com.ads.admob.billing.factory

import android.app.Activity
import android.app.Application
import com.ads.admob.data.IapItem
import com.ads.admob.listener.BillingClientConnectionListener
import com.ads.admob.listener.PurchaseServiceListener
import com.ads.admob.widget.DataWrappers
import com.android.billingclient.api.BillingClient

interface IapFactory {

    fun buySubscription(activity: Activity, subId: String)
    fun buyIap(activity: Activity, iapId: String, isConsumable: Boolean = false)
    fun getPriceById(skuId: String, type: String): String?
    fun getOfferById(skuId: String): String?
    fun getPriceByNumOfWeek(skuId: String, type: String, numWeek:Int): String

    fun registerBillingClientConnectionListener(adCallback: BillingClientConnectionListener)

    fun unregisterBillingClientConnectionListener(adCallback: BillingClientConnectionListener)

    fun unregisterAllBillingClientConnectionListener()
    fun registerPurchaseServiceListener(adCallback: PurchaseServiceListener)
    fun unregisterPurchaseServiceListener(adCallback: PurchaseServiceListener)

    fun unregisterAllPurchaseServiceListener()
    fun isProductPurchased(): Boolean
    fun getProductPurchaseList(): List<DataWrappers.PurchaseInfo>

    companion object {
        @Volatile
        private var INSTANCE: IapFactory? = null

        fun initialize(application: Application, iapList: List<IapItem>, debugMode: Boolean = false): IapFactory {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: IapFactoryImpl(application, iapList, debugMode).also {
                    INSTANCE = it
                }
            }
        }

        fun getInstance(): IapFactory {
            return INSTANCE
                ?: throw IllegalStateException("SamsungRemoteFactory is not initialized. Call initialize() first.")
        }
    }
}

annotation class IapType {
    companion object {
        const val PURCHASE = BillingClient.ProductType.INAPP
        const val SUBSCRIPTION = BillingClient.ProductType.SUBS
    }
}