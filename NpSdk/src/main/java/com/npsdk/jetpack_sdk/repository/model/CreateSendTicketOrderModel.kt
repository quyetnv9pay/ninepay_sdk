package com.npsdk.jetpack_sdk.repository.model

import com.google.gson.annotations.SerializedName


data class CreateSendTicketOrderModel(
    @SerializedName("success") var success: Boolean? = null,
    @SerializedName("server_time") var serverTime: Int? = null,
    @SerializedName("status") var status: Int? = null,
    @SerializedName("message") var message: String? = null,
    @SerializedName("error_code") var errorCode: Int? = null,
    @SerializedName("data") var data: SendTicketOrderData? = SendTicketOrderData()
)

data class SendTicketOrderData(
    @SerializedName("order_code") var orderCode: String? = null,
    @SerializedName("request_id") var requestId: String? = null,
    @SerializedName("title") var title: String? = null,
    @SerializedName("description") var description: String? = null,
    @SerializedName("amount") var amount: Int? = null,
)