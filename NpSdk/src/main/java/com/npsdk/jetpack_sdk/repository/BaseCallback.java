package com.npsdk.jetpack_sdk.repository;

import com.google.gson.JsonObject;

public interface BaseCallback {
    void onSuccess(JsonObject response);
    void onError(JsonObject response);
}