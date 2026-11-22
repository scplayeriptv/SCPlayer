package com.sc.player.data

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams

/**
 * الدفع عن طريق Google Play (نسخة Play بس): اشتراك سنوي أو شراء مرة واحدة (Lifetime).
 * المنتجات لازم تتعمل في Play Console بنفس المعرّفات: premium_yearly (اشتراك) و premium_lifetime (منتج داخل التطبيق).
 */
object PlayBilling {
    const val SKU_YEARLY = "premium_yearly"
    const val SKU_LIFETIME = "premium_lifetime"

    var yearlyPrice by mutableStateOf<String?>(null)
        private set
    var lifetimePrice by mutableStateOf<String?>(null)
        private set
    var busy by mutableStateOf(false)
        private set
    /** رسالة خطأ للعرض (null = مفيش). */
    var error by mutableStateOf<String?>(null)
        private set

    private var client: BillingClient? = null
    private var yearly: ProductDetails? = null
    private var lifetime: ProductDetails? = null
    private val main = Handler(Looper.getMainLooper())

    fun Context.findActivity(): Activity? {
        var c: Context? = this
        while (c is ContextWrapper) {
            if (c is Activity) return c
            c = c.baseContext
        }
        return null
    }

    private fun ensure(app: Context, then: () -> Unit) {
        val c = client ?: BillingClient.newBuilder(app.applicationContext)
            .setListener { r, list -> onPurchasesUpdated(app, r, list) }
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .build().also { client = it }
        if (c.isReady) { then(); return }
        c.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(r: BillingResult) {
                main.post {
                    if (r.responseCode == BillingClient.BillingResponseCode.OK) then()
                    else { error = r.debugMessage.ifBlank { "Billing unavailable (${r.responseCode})" }; busy = false }
                }
            }
            override fun onBillingServiceDisconnected() {}
        })
    }

    /** بيجيب الأسعار ويتأكد من المشتريات الحالية. */
    fun start(ctx: Context) {
        val app = ctx.applicationContext
        ensure(app) {
            queryDetails(BillingClient.ProductType.SUBS, SKU_YEARLY) { pd ->
                yearly = pd
                yearlyPrice = pd?.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.lastOrNull()?.formattedPrice
            }
            queryDetails(BillingClient.ProductType.INAPP, SKU_LIFETIME) { pd ->
                lifetime = pd
                lifetimePrice = pd?.oneTimePurchaseOfferDetails?.formattedPrice
            }
            refresh(app)
        }
    }

    private fun queryDetails(type: String, sku: String, done: (ProductDetails?) -> Unit) {
        val c = client ?: return
        val params = QueryProductDetailsParams.newBuilder().setProductList(
            listOf(QueryProductDetailsParams.Product.newBuilder().setProductId(sku).setProductType(type).build()),
        ).build()
        c.queryProductDetailsAsync(params) { r, list ->
            main.post { done(if (r.responseCode == BillingClient.BillingResponseCode.OK) list.firstOrNull() else null) }
        }
    }

    /** يقرا مشتريات المستخدم من Google (بيشتغل كمان كـ Restore). */
    fun refresh(ctx: Context) {
        val app = ctx.applicationContext
        ensure(app) {
            val c = client ?: return@ensure
            var pending = 2
            var ok = true
            var entitled = false
            fun finish() {
                if (--pending > 0) return
                busy = false
                if (ok) Premium.applyPlay(app, entitled)
            }
            for (type in listOf(BillingClient.ProductType.SUBS, BillingClient.ProductType.INAPP)) {
                c.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(type).build()) { r, list ->
                    main.post {
                        if (r.responseCode != BillingClient.BillingResponseCode.OK) ok = false
                        else if (handle(app, list)) entitled = true
                        finish()
                    }
                }
            }
        }
    }

    /** true لو في شراء صالح. بيعمل acknowledge للجديد. */
    private fun handle(app: Context, list: List<Purchase>): Boolean {
        var has = false
        for (p in list) {
            if (p.purchaseState != Purchase.PurchaseState.PURCHASED) continue
            if (p.products.none { it == SKU_YEARLY || it == SKU_LIFETIME }) continue
            has = true
            if (!p.isAcknowledged) {
                client?.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build()) { }
            }
        }
        return has
    }

    private fun onPurchasesUpdated(app: Context, r: BillingResult, list: List<Purchase>?) {
        main.post {
            busy = false
            when (r.responseCode) {
                BillingClient.BillingResponseCode.OK -> if (list != null && handle(app, list)) Premium.applyPlay(app, true)
                BillingClient.BillingResponseCode.USER_CANCELED -> {}
                BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> refresh(app)
                else -> error = r.debugMessage.ifBlank { "Purchase failed (${r.responseCode})" }
            }
        }
    }

    fun buy(activity: Activity, sku: String) {
        error = null
        val pd = if (sku == SKU_YEARLY) yearly else lifetime
        val c = client
        if (pd == null || c == null) {
            error = "Product not available yet. Check your internet and try again."
            start(activity)
            return
        }
        val b = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(pd)
        if (sku == SKU_YEARLY) {
            val token = pd.subscriptionOfferDetails?.firstOrNull()?.offerToken
            if (token == null) { error = "Subscription offer not available."; return }
            b.setOfferToken(token)
        }
        busy = true
        val res = c.launchBillingFlow(activity, BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(b.build())).build())
        if (res.responseCode != BillingClient.BillingResponseCode.OK) {
            busy = false
            error = res.debugMessage.ifBlank { "Couldn't open payment (${res.responseCode})" }
        }
    }
}
