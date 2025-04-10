package com.npsdk.jetpack_sdk.repository;

import com.google.gson.JsonObject;
import com.npsdk.jetpack_sdk.repository.model.DataCreateOrderPaymentMethod;

public interface CallbackCreateOrderPaymentMethod {
    void onSuccess(DataCreateOrderPaymentMethod data);

    void onError(JsonObject error);
}
