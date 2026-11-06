package com.ads.admob.listener

import com.ads.admob.widget.DataWrappers.PurchaseInfo

interface PurchaseListener {
    fun onProductPurchased(purchaseInfo: PurchaseInfo)
    fun displayErrorMessage(errorMsg: String?)
    fun onUserCancelBilling()
}
