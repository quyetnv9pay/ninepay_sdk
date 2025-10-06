package com.npsdk.jetpack_sdk.repository.model

import com.google.gson.annotations.SerializedName

data class CreateOrderPaymentMethodModel (
    @SerializedName("server_time") var serverTime: Int? = null,
    @SerializedName("status") var status: Int? = null,
    @SerializedName("message") var message: String? = null,
    @SerializedName("error_code") var errorCode: Int? = null,
    @SerializedName("data") var data: DataCreateOrderPaymentMethod? = DataCreateOrderPaymentMethod()
)

class DataCreateOrderPaymentMethod (
    @SerializedName("order_id") var orderId: String? = null,
    @SerializedName("order_code") var orderCode: String? = null,
    @SerializedName("title") var title: String? = null,
    @SerializedName("desc") var desc: String? = null,
    @SerializedName("amount") var amount: String? = null,
    @SerializedName("coupon_info") var couponInfo: CouponInfo? = CouponInfo()
)

class CouponInfo (
    @SerializedName("id") var id: Int? = null,
    @SerializedName("code") var code: String? = null,
    @SerializedName("name") var name: String? = null,
    @SerializedName("banner") var banner: String? = null,
    @SerializedName("description") var description: String? = null,
)