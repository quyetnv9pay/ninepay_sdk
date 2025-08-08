package com.npsdk.jetpack_sdk.repository;

import com.google.gson.JsonObject;
import com.npsdk.jetpack_sdk.repository.model.CreateSendTicketOrderModel;

public interface CallbackCreateSendTicketOrder {
    void onSuccess(CreateSendTicketOrderModel data);
    void onError(JsonObject error);
}
