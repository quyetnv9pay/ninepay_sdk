package com.npsdk.jetpack_sdk.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.npsdk.jetpack_sdk.base.api.BaseApiClient;
import com.npsdk.jetpack_sdk.repository.model.GetListOfCouponsParams;
import com.npsdk.jetpack_sdk.repository.model.ValidateCouponParams;
import com.npsdk.module.utils.JsonUtils;

import org.json.JSONObject;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.ninepay.sdk.encryptservice.EncryptService;

public class CuponRepo extends BaseApiClient {
    ExecutorService executor = Executors.newSingleThreadExecutor();
    Handler mainThread = new Handler(Looper.getMainLooper());

    public void getListOfCoupons(GetListOfCouponsParams listOfCouponParams, BaseCallback callback) {
        executor.execute(() -> {
            Call<String> call = apiService.getListOfCoupons(listOfCouponParams.getAmount(), listOfCouponParams.getEventId());
            enqueue(call, new Callback<String>() {
                @Override
                public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        String objectDecrypt = EncryptService.INSTANCE.decryptAesBase64(
                            response.body(),
                            EncryptService.INSTANCE.getRandomkeyRaw()
                        );
                        try {
                            JsonObject result = JsonParser.parseString(objectDecrypt).getAsJsonObject();
                            updateUI(() -> {
                                callback.onSuccess(result);
                            });
                        } catch (JsonSyntaxException e) {
                            handleParserError(e, callback);
                        }
                    } else {
                        handleResponseError(response, callback);
                    }
                }

                @Override
                public void onFailure(Call<String> call, Throwable t) {
                    handleApiFailure(t, callback);
                }
            });
        });
    }

    public void validateCoupon(ValidateCouponParams params, BaseCallback callback) {
        executor.execute(() -> {
            Call<String> call = apiService.validateCoupon(params.getAmount(), params.getCouponId(), params.getCoupon(), params.getEventId());
            enqueue(call, new Callback<String>() {
                @Override
                public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        String objectDecrypt = EncryptService.INSTANCE.decryptAesBase64(
                            response.body(),
                            EncryptService.INSTANCE.getRandomkeyRaw()
                        );
                        try {
                            JsonObject result = JsonParser.parseString(objectDecrypt).getAsJsonObject();
                            updateUI(() -> {
                                callback.onSuccess(result);
                            });
                        } catch (JsonSyntaxException e) {
                            handleParserError(e, callback);
                        }
                    } else {
                        handleResponseError(response, callback);
                    }
                }

                @Override
                public void onFailure(Call<String> call, Throwable t) {
                    handleApiFailure(t, callback);
                }
            });
        });
    }

    private void handleResponseError(Response<String> response, BaseCallback callback) {
        try {
            if (response.errorBody() == null) {
                throw new IOException("Response body is null");
            }
            String errorJson = response.errorBody().string();
            JSONObject obj = new JSONObject(errorJson);
            String message = obj.optString("message", "Unknown error");
            JsonObject error = new JsonObject();
            error.addProperty("message", message);
            error.addProperty("error_code", response.code());
            updateUI(() -> {
                callback.onError(error);
            });
        } catch (Exception e) {
            updateUI(() -> {
                callback.onError(JsonUtils.wrapWithDefault(
                    "WRONG WITH JSON DECODE",
                    2004
                ));
            });
        }
    }

    private void handleParserError(JsonSyntaxException e, BaseCallback callback) {
        JsonObject error = new JsonObject();
        error.addProperty("message", "WRONG WITH JSON DECODE");
        error.addProperty("error_code", 2004);
        updateUI(() -> {
            callback.onError(error);
        });
    }

    private void handleApiFailure(Throwable t, BaseCallback callback) {
        JsonObject error = new JsonObject();
        error.addProperty("message", t.getMessage());
        error.addProperty("error_code", 2005);
        updateUI(() -> {
            callback.onError(error);
        });
    }

    private void updateUI(Runnable runnable) {
        mainThread.post(runnable);
    }
}
