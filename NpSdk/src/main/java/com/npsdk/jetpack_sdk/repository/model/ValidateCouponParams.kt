package com.npsdk.jetpack_sdk.repository.model

import kotlin.properties.Delegates

data class ValidateCouponParams (
    var amount: String?,
    var couponId: Int?,
    var coupon: String?,
    var eventId: String?,
)