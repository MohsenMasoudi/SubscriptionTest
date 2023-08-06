package com.ad8.presentation.fragments.main_page

import android.net.Uri
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import androidx.fragment.app.viewModels
import com.ad8.presentation.R
import com.ad8.presentation.base.BaseFragment
import com.ad8.presentation.databinding.FragmentMainPageBinding
import com.ad8.presentation.util.UserHelper
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.bumptech.glide.RequestManager
import com.google.firebase.crashlytics.internal.model.ImmutableList
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class MainPageFragment : BaseFragment<FragmentMainPageBinding>(R.layout.fragment_main_page),PurchasesUpdatedListener{
    private val clickHandler = ClickHandler(this)


    @Inject
    lateinit var glide: RequestManager

    @Inject
    lateinit var userHelper: UserHelper



    private lateinit var billingClient:BillingClient


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


    }

    private fun setupObservers() {

    }




    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
    }

    private fun setupUI() {
        binding.clickHandler = clickHandler
        billingClient = BillingClient.newBuilder(requireContext())
            .setListener(this)
            .enablePendingPurchases()
            .build()

    }



    override fun expireToken() {
    }

    inner class ClickHandler(fragment: MainPageFragment) {



        fun onPayClicked(view: View) {
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode ==  BillingClient.BillingResponseCode.OK) {
                        // The BillingClient is ready. You can query purchases here.
                        getProducts()
                        Log.d("checkBilling","BillingResponseCode.OK")
                    }
                }
                override fun onBillingServiceDisconnected() {
                    Log.d("checkBilling","onBillingServiceDisconnected")
                    // Try to restart the connection on the next request to
                    // Google Play by calling the startConnection() method.
                }
            })
            Log.d("checkBilling","is Feature supported?=${billingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS)}")

        }

        fun onTakePictureClicked(view: View) {
        }
    }

    override fun onPurchasesUpdated(p0: BillingResult, p1: MutableList<Purchase>?) {

    }

    fun getProducts(){
        Log.d("checkBilling","getProducts")
        val queryProductDetailsParams =
            QueryProductDetailsParams.newBuilder()
                .setProductList(
                    ImmutableList.from(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId("product_id_example")
                            .setProductType(BillingClient.ProductType.SUBS)
                            .build()))
                .build()

        billingClient.queryProductDetailsAsync(queryProductDetailsParams) {
                billingResult,
                productDetailsList ->
            // check billingResult
            // process returned productDetailsList
            Log.d("checkBilling","onGettingResult")
            productDetailsList.forEach {productDetail->
                Log.d("checkDetail",productDetail.productId)
                Log.d("checkDetail",productDetail.productType)
//x                Log.d("checkDetail",productDetail.toString())
            }
        }

    }
    fun requestForBilling(productDetails: ProductDetails){
//        val offerToken = productDetails.subscriptionOfferDetails?.get(selectedOfferIndex)?.offerToken

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                // retrieve a value for "productDetails" by calling queryProductDetailsAsync()
                .setProductDetails(productDetails)
                // to get an offer token, call ProductDetails.subscriptionOfferDetails()
                // for a list of offers that are available to the user
//                .setOfferToken(selectedOfferToken)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

// Launch the billing flow
//        val billingResult = billingClient.launchBillingFlow(activity, billingFlowParams)
    }


}