package com.npsdk.jetpack_sdk.repository.model

data class CreateOrderParamWalletMethod (
    var amount: String? = "",
    var productName: String? = "",
    var requestId: String? = "",
    var merchantCode: String? = "",
    var couponId: Int?,
    var coupon: String?,
    var metaData: Map<String, Any>? = emptyMap()
)