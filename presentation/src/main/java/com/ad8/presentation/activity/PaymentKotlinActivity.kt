package com.ad8.presentation.activity

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ad8.presentation.databinding.ActivityPaymentBinding
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.AcknowledgePurchaseResponseListener
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingFlowParams.ProductDetailsParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ConsumeResponseListener
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.google.firebase.crashlytics.internal.model.ImmutableList
import java.io.IOException
import java.util.concurrent.Executors

class PaymentKotlinActivity : AppCompatActivity() {
    private var billingClient: BillingClient? = null
    var subName: String? = null
    var phases: String? = null
    var des: String? = null
    var dur: String? = null
    var isSuccess = false
    var binding: ActivityPaymentBinding? = null
    private val productId = "test_product_1"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        billingClient = BillingClient.newBuilder(this)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
            .build()
        price
    }

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
            binding!!.tvSubsts.text = "Already Subscribed"
            isSuccess = true
            ConnectionClass.premium = true
            ConnectionClass.locked = false
            binding!!.btnSubscribe.visibility = View.GONE
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED) {
            binding!!.tvSubsts.text = "Feature Not Supported"
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.BILLING_UNAVAILABLE) {
            binding!!.tvSubsts.text = "Billing Unavailable"
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            binding!!.tvSubsts.text = "User Cancelled"
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.DEVELOPER_ERROR) {
            binding!!.tvSubsts.text = "Developer Error"
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.ITEM_UNAVAILABLE) {
            binding!!.tvSubsts.text = "Item Unavailable"
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.NETWORK_ERROR) {
            binding!!.tvSubsts.text = "Network Error"
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.SERVICE_DISCONNECTED) {
            binding!!.tvSubsts.text = "Service Disconnected"
        }
    }

    fun handlePurchase(purchase: Purchase) {
        val consumeParams = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        val listener =
            ConsumeResponseListener { billingResult: BillingResult, purchaseToken: String? ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    // Handle the success of the consume operation.
                }
            }
        billingClient!!.consumeAsync(consumeParams, listener)
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!verifyValidSignature(purchase.originalJson, purchase.signature)) {
                // Invalid purchase
                // show error to user
                Toast.makeText(this, "Error : Invalid Purchase", Toast.LENGTH_SHORT).show()
                return
            }
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient!!.acknowledgePurchase(
                    acknowledgePurchaseParams,
                    acknowledgePurchaseResponseListener
                )
                binding!!.tvSubsts.text = "Subscribed"
            } else {
                binding!!.tvSubsts.text = "Already Subscribed"
                isSuccess = true
                ConnectionClass.premium = true
                ConnectionClass.locked = false
                binding!!.btnSubscribe.visibility = View.GONE
            }
        } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            binding!!.tvSubsts.text = "Subscription Pending"
        } else if (purchase.purchaseState == Purchase.PurchaseState.UNSPECIFIED_STATE) {
            binding!!.tvSubsts.text = "Subscription Unspecified"
        }
    }

    var acknowledgePurchaseResponseListener = AcknowledgePurchaseResponseListener { billingResult ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            binding!!.tvSubsts.text = "Subscribed"
            isSuccess = true
            ConnectionClass.premium = true
            ConnectionClass.locked = false
        }
    }

    private fun verifyValidSignature(signedData: String, signature: String): Boolean {
        return try {
            val base64Key = ""
            Security.verifyPurchase(base64Key, signedData, signature)
        } catch (e: IOException) {
            false
        }
    }

    private val price: Unit
        private get() {
            billingClient!!.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        val executorService = Executors.newSingleThreadExecutor()
                        executorService.execute {
                            val queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
                                .setProductList(
                                    ImmutableList.from(
                                        QueryProductDetailsParams.Product.newBuilder()
                                            .setProductId(productId)
                                            .setProductType(BillingClient.ProductType.SUBS)
                                            .build()
                                    )
                                )
                                .build()
                            billingClient!!.queryProductDetailsAsync(
                                queryProductDetailsParams
                            ) { billingResult, productDetailsList ->
                                for (productDetails in productDetailsList) {
                                    val offerToken =
                                        productDetails.subscriptionOfferDetails!![0].offerToken
                                    val productDetailsParamsList: ImmutableList<*> =
                                        ImmutableList.from(
                                            ProductDetailsParams.newBuilder()
                                                .setProductDetails(productDetails)
                                                .setOfferToken(offerToken)
                                                .build()
                                        )
                                    subName = productDetails.name
                                    des = productDetails.description
                                    val formattedPrice =
                                        productDetails.subscriptionOfferDetails!![0].pricingPhases
                                            .pricingPhaseList[0].formattedPrice
                                    val billingPeriod =
                                        productDetails.subscriptionOfferDetails!![0].pricingPhases
                                            .pricingPhaseList[0].billingPeriod
                                    val recurrenceMode =
                                        productDetails.subscriptionOfferDetails!![0].pricingPhases
                                            .pricingPhaseList[0].recurrenceMode
                                    var n: String
                                    var duration: String
                                    var bp: String
                                    bp = billingPeriod
                                    n = billingPeriod.substring(1, 2)
                                    duration = billingPeriod.substring(2, 3)
                                    if (recurrenceMode == 2) {
                                        if (duration == "M") {
                                            dur = "For $n Month"
                                        } else if (duration == "Y") {
                                            dur = "For $n Year"
                                        } else if (duration == "W") {
                                            dur = "For $n Week"
                                        } else if (duration == "D") {
                                            dur = "For $n Days"
                                        }
                                    } else {
                                        if (bp == "P1M") {
                                            dur = "/Monthly"
                                        } else if (bp == "P6M") {
                                            dur = "/Every 6 Month"
                                        } else if (bp == "P1Y") {
                                            dur = "/Yearly"
                                        } else if (bp == "P1W") {
                                            dur = "/Weekly"
                                        } else if (bp == "P3W") {
                                            dur = "Every /3 Week"
                                        }
                                    }
                                    phases = "$formattedPrice $dur"
                                    for (i in 0..productDetails.subscriptionOfferDetails!![0].pricingPhases.pricingPhaseList.size) {
                                        if (i > 0) {
                                            val period =
                                                productDetails.subscriptionOfferDetails!![0].pricingPhases.pricingPhaseList[i].billingPeriod
                                            val price =
                                                productDetails.subscriptionOfferDetails!![0].pricingPhases.pricingPhaseList[i].formattedPrice
                                            if (period == "P1M") {
                                                dur = "/Monthly"
                                            } else if (period == "P6M") {
                                                dur = "/Every 6 Month"
                                            } else if (period == "P1Y") {
                                                dur = "/Yearly"
                                            } else if (period == "P1W") {
                                                dur = "/Weekly"
                                            } else if (period == "P3W") {
                                                dur = "Every /3 Week"
                                            }
                                            phases += """
                                            
                                            $price$dur
                                            """.trimIndent()
                                        }
                                    }
                                }
                            }
                        }
                        runOnUiThread {
                            try {
                                Thread.sleep(1000)
                            } catch (e: InterruptedException) {
                                e.printStackTrace()
                            }
                            binding!!.tvSubid.text = subName
                            binding!!.txtPrice.text = "Price: $phases"
                            binding!!.tvBenefit.text = des
                        }
                    }
                }

                override fun onBillingServiceDisconnected() {
                    // Try to restart the connection on the next request to
                    // Google Play by calling the startConnection() method.
                }
            })
        }

    fun getPriceClicked(view: View?) {
        price
    }

    fun btnSubClicked(view: View?) {
        billingClient!!.startConnection(object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() {}
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                val queryProductDetailsParams =
                    QueryProductDetailsParams.newBuilder().setProductList(
                        ImmutableList.from(
                            QueryProductDetailsParams.Product.newBuilder()
                                .setProductId(productId)
                                .setProductType(BillingClient.ProductType.SUBS)
                                .build()
                        )
                    ).build()
                billingClient!!.queryProductDetailsAsync(
                    queryProductDetailsParams
                ) { billingResult, list ->
                    for (productDetails in list) {
                        val offerToken = productDetails.subscriptionOfferDetails!![0].offerToken
                        val productDetailsParamsList: ImmutableList<BillingFlowParams.ProductDetailsParams> = ImmutableList.from(
                            ProductDetailsParams.newBuilder()
                                .setProductDetails(productDetails)
                                .setOfferToken(offerToken)
                                .build()
                        )
                        val billingFlowParams = BillingFlowParams.newBuilder()
                            .setProductDetailsParamsList(productDetailsParamsList)
                            .build()
                        billingClient!!.launchBillingFlow(
                            this@PaymentKotlinActivity,
                            billingFlowParams
                        )
                    }
                }
            }
        })
    }

    fun btnQuitClicked(view: View?) {
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (billingClient != null) {
            billingClient!!.endConnection()
        }
    }
}