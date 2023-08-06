package com.ad8.presentation.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.ad8.presentation.databinding.ActivityPaymentBinding;
import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.google.firebase.crashlytics.internal.model.ImmutableList;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PaymentActivity extends AppCompatActivity {
    private BillingClient billingClient;
    String subName, phases, des, dur;
    boolean isSuccess = false;
    ActivityPaymentBinding binding;

    private String productId="test_product_1";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPaymentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        billingClient = BillingClient.newBuilder(this)
                .setListener(purchasesUpdatedListener)
                .enablePendingPurchases()
                .build();
        getPrice();
    }

    private PurchasesUpdatedListener purchasesUpdatedListener = new PurchasesUpdatedListener() {
        @Override
        public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> purchases) {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
                for (Purchase purchase : purchases) {
                    handlePurchase(purchase);
                }
            } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
                binding.tvSubsts.setText("Already Subscribed");
                isSuccess = true;
                ConnectionClass.premium = true;
                ConnectionClass.locked = false;
                binding.btnSubscribe.setVisibility(View.GONE);
            } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED) {
                binding.tvSubsts.setText("Feature Not Supported");

            } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.BILLING_UNAVAILABLE) {
                binding.tvSubsts.setText("Billing Unavailable");

            } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
                binding.tvSubsts.setText("User Cancelled");

            } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.DEVELOPER_ERROR) {
                binding.tvSubsts.setText("Developer Error");

            } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.ITEM_UNAVAILABLE) {
                binding.tvSubsts.setText("Item Unavailable");

            } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.NETWORK_ERROR) {
                binding.tvSubsts.setText("Network Error");

            } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.SERVICE_DISCONNECTED) {
                binding.tvSubsts.setText("Service Disconnected");

            }
        }

        ;
    };

    void handlePurchase(final Purchase purchase) {
        ConsumeParams consumeParams =
                ConsumeParams.newBuilder()
                        .setPurchaseToken(purchase.getPurchaseToken())
                        .build();
        ConsumeResponseListener listener = (billingResult, purchaseToken) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                // Handle the success of the consume operation.
            }
        };
        billingClient.consumeAsync(consumeParams, listener);
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            if (!verifyValidSignature(purchase.getOriginalJson(), purchase.getSignature())) {
                // Invalid purchase
                // show error to user
                Toast.makeText(this, "Error : Invalid Purchase", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!purchase.isAcknowledged()) {
                AcknowledgePurchaseParams acknowledgePurchaseParams =
                        AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchase.getPurchaseToken())
                                .build();
                billingClient.acknowledgePurchase(acknowledgePurchaseParams, acknowledgePurchaseResponseListener);
                binding.tvSubsts.setText("Subscribed");
            } else {
                binding.tvSubsts.setText("Already Subscribed");
                isSuccess = true;
                ConnectionClass.premium = true;
                ConnectionClass.locked = false;
                binding.btnSubscribe.setVisibility(View.GONE);
            }
        } else if (purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) {
            binding.tvSubsts.setText("Subscription Pending");
        } else if (purchase.getPurchaseState() == Purchase.PurchaseState.UNSPECIFIED_STATE) {
            binding.tvSubsts.setText("Subscription Unspecified");

        }
    }

    AcknowledgePurchaseResponseListener acknowledgePurchaseResponseListener = new AcknowledgePurchaseResponseListener() {
        @Override
        public void onAcknowledgePurchaseResponse(@NonNull BillingResult billingResult) {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                binding.tvSubsts.setText("Subscribed");
                isSuccess = true;
                ConnectionClass.premium = true;
                ConnectionClass.locked = false;
            }
        }
    };

    private boolean verifyValidSignature(String signedData, String signature) {
        try {
            String base64Key = "";
            return Security.verifyPurchase(base64Key, signedData, signature);
        } catch (IOException e) {
            return false;
        }
    }

    private void getPrice() {
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    ExecutorService executorService = Executors.newSingleThreadExecutor();
                    executorService.execute(new Runnable() {
                        @Override
                        public void run() {
                            QueryProductDetailsParams queryProductDetailsParams =
                                    QueryProductDetailsParams.newBuilder()
                                            .setProductList(
                                                    ImmutableList.from(
                                                            QueryProductDetailsParams.Product.newBuilder()
                                                                    .setProductId(productId)
                                                                    .setProductType(BillingClient.ProductType.SUBS)
                                                                    .build()))
                                            .build();

                            billingClient.queryProductDetailsAsync(
                                    queryProductDetailsParams,
                                    new ProductDetailsResponseListener() {
                                        public void onProductDetailsResponse(BillingResult billingResult,
                                                                             List<ProductDetails> productDetailsList) {

                                            for (ProductDetails productDetails : productDetailsList) {
                                                String offerToken = productDetails.getSubscriptionOfferDetails().get(0).getOfferToken();
                                                ImmutableList productDetailsParamsList =
                                                        ImmutableList.from(
                                                                BillingFlowParams.ProductDetailsParams.newBuilder()
                                                                        .setProductDetails(productDetails)
                                                                        .setOfferToken(offerToken)
                                                                        .build()
                                                        );
                                                subName = productDetails.getName();
                                                des = productDetails.getDescription();
                                                String formattedPrice = productDetails.getSubscriptionOfferDetails().get(0).getPricingPhases()
                                                        .getPricingPhaseList().get(0).getFormattedPrice();
                                                String billingPeriod = productDetails.getSubscriptionOfferDetails().get(0).getPricingPhases()
                                                        .getPricingPhaseList().get(0).getBillingPeriod();
                                                int recurrenceMode = productDetails.getSubscriptionOfferDetails().get(0).getPricingPhases()
                                                        .getPricingPhaseList().get(0).getRecurrenceMode();
                                                String n, duration, bp;
                                                bp = billingPeriod;
                                                n = billingPeriod.substring(1, 2);
                                                duration = billingPeriod.substring(2, 3);

                                                if (recurrenceMode == 2) {
                                                    if (duration.equals("M")) {
                                                        dur = "For " + n + " Month";
                                                    } else if (duration.equals("Y")) {
                                                        dur = "For " + n + " Year";
                                                    } else if (duration.equals("W")) {
                                                        dur = "For " + n + " Week";
                                                    } else if (duration.equals("D")) {
                                                        dur = "For " + n + " Days";
                                                    }

                                                } else {
                                                    if (bp.equals("P1M")) {
                                                        dur = "/Monthly";
                                                    } else if (bp.equals("P6M")) {
                                                        dur = "/Every 6 Month";
                                                    } else if (bp.equals("P1Y")) {
                                                        dur = "/Yearly";
                                                    } else if (bp.equals("P1W")) {
                                                        dur = "/Weekly";
                                                    } else if (bp.equals("P3W")) {
                                                        dur = "Every /3 Week";
                                                    }
                                                }
                                                phases = formattedPrice + " " + dur;
                                                for (int i = 0; i <= (productDetails.getSubscriptionOfferDetails().get(0).getPricingPhases().getPricingPhaseList().size()); i++) {
                                                    if (i > 0) {
                                                        String period = productDetails.getSubscriptionOfferDetails().get(0).getPricingPhases().getPricingPhaseList().get(i).getBillingPeriod();
                                                        String price = productDetails.getSubscriptionOfferDetails().get(0).getPricingPhases().getPricingPhaseList().get(i).getFormattedPrice();
                                                        if (period.equals("P1M")) {
                                                            dur = "/Monthly";
                                                        } else if (period.equals("P6M")) {
                                                            dur = "/Every 6 Month";
                                                        } else if (period.equals("P1Y")) {
                                                            dur = "/Yearly";
                                                        } else if (period.equals("P1W")) {
                                                            dur = "/Weekly";
                                                        } else if (period.equals("P3W")) {
                                                            dur = "Every /3 Week";
                                                        }
                                                        phases += "\n" + price + dur;
                                                    }
                                                }


                                            }

                                        }
                                    }
                            );
                        }
                    });
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            binding.tvSubid.setText(subName);
                            binding.txtPrice.setText("Price: " + phases);
                            binding.tvBenefit.setText(des);
                        }
                    });
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
            }
        });
    }

    public void getPriceClicked(View view) {
        getPrice();
    }

    public void btnSubClicked(View view) {
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingServiceDisconnected() {

            }

            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                QueryProductDetailsParams queryProductDetailsParams = QueryProductDetailsParams.newBuilder().setProductList(
                        ImmutableList.from(QueryProductDetailsParams.Product.newBuilder()
                                .setProductId(productId)
                                .setProductType(BillingClient.ProductType.SUBS)
                                .build()
                        )
                ).build();
                billingClient.queryProductDetailsAsync(
                        queryProductDetailsParams, new ProductDetailsResponseListener() {
                            @Override
                            public void onProductDetailsResponse(@NonNull BillingResult billingResult, @NonNull List<ProductDetails> list) {
                                for (ProductDetails productDetails : list) {
                                    String offerToken = productDetails.getSubscriptionOfferDetails().get(0).getOfferToken();
                                    ImmutableList productDetailsParamsList = ImmutableList.from(
                                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                                    .setProductDetails(productDetails)
                                                    .setOfferToken(offerToken)
                                                    .build()
                                    );
                                    BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                                            .setProductDetailsParamsList(productDetailsParamsList)
                                            .build();
                                    billingClient.launchBillingFlow(PaymentActivity.this, billingFlowParams);
                                }
                            }
                        }
                );
            }
        });

    }

    public void btnQuitClicked(View view) {
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (billingClient != null) {
            billingClient.endConnection();
        }
    }
}