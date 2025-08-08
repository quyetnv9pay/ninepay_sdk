package com.npsdk.jetpack_sdk.repository.model

data class CreateSendTicketOrderBody (
    var amount: Int,
    var fee: Int,
    var content: String,
    var request_id: String,
    var request_time: String,
    var request_action: String,
)