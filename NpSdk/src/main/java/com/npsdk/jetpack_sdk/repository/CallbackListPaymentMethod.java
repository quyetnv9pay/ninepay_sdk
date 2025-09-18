package com.npsdk.jetpack_sdk.repository;

import com.google.gson.JsonObject;

public interface CallbackListPaymentMethod {
    void onSuccess(JsonObject response);
}
