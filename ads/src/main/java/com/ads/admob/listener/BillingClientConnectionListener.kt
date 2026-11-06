package com.ads.admob.listener

interface BillingClientConnectionListener {
    fun onConnected(status: Boolean, billingResponseCode: Int)
}