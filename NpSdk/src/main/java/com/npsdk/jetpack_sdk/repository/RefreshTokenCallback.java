package com.npsdk.jetpack_sdk.repository;

public interface RefreshTokenCallback {
    void onSuccess();
    void onError(int errorCode, String message);
}
