package com.ads.admob.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.ads.admob.R
import com.ads.admob.billing.factory.IapType
import com.ads.admob.databinding.ViewBillingTestBinding
import com.ads.admob.listener.PurchaseListener
import com.ads.admob.widget.DataWrappers
import com.android.billingclient.api.ProductDetails



internal class IapDialog(
    context: Context,
    private val typeIap: String,
    private val productDetails: ProductDetails,
    private val purchaseListener: PurchaseListener
) : Dialog(context, R.style.Dialog_FullScreen_Light) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.e("IapDialog", "onCreate: ")
        window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        val wlp: WindowManager.LayoutParams? = window?.attributes

        wlp?.gravity = Gravity.BOTTOM
        window?.attributes = wlp
        val binding = ViewBillingTestBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.txtTitle.text = productDetails.title
        binding.txtDescription.text = productDetails.description
        binding.txtId.text = productDetails.productId
        if (typeIap === IapType.PURCHASE)
            binding.txtPrice.text =
                productDetails.oneTimePurchaseOfferDetails!!.formattedPrice
        else
            binding.txtPrice.text =
                productDetails.subscriptionOfferDetails!![0].pricingPhases.pricingPhaseList[0].formattedPrice

        binding.txtContinuePurchase.setOnClickListener { v: View? ->
            Log.e("IapDialog", "onCreate: 2e321")

            dismiss()
            purchaseListener.onProductPurchased(
                DataWrappers.PurchaseInfo(
                    purchaseState = 0,
                    purchaseTime = 2L,
                    purchaseToken = "dsa",
                    packageName = "dsad",
                    developerPayload = "das",
                    isAcknowledged = false,
                    isAutoRenewing = false,
                    orderId = "dsa",
                    originalJson ="",
                    signature ="",
                    sku =  "",
                    accountIdentifiers = null
                )
            )
        }
    }
}