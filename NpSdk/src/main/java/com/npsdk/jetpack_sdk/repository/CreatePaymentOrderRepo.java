package com.npsdk.jetpack_sdk.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.npsdk.jetpack_sdk.base.api.BaseApiClient;
import com.npsdk.jetpack_sdk.base.api.EncryptServiceHelper;
import com.npsdk.jetpack_sdk.repository.model.CreateOrderParamWalletMethod;
import com.npsdk.jetpack_sdk.repository.model.CreateOrderPaymentMethodModel;
import com.npsdk.module.utils.Flavor;

import org.bouncycastle.util.encoders.DecoderException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreatePaymentOrderRepo extends BaseApiClient {
    ExecutorService executor = Executors.newSingleThreadExecutor();
    Handler mainThread = new Handler(Looper.getMainLooper());

    public void check(Context context, CreateOrderParamWalletMethod param, CallbackCreateOrderPaymentMethod callbackCreateOrder) {
        String key = EncryptServiceHelper.INSTANCE.getRandomkeyRaw();

        Map<String, String> mapEncrypt = new HashMap<>();
        mapEncrypt.put("amount", param.getAmount());
        mapEncrypt.put("product_name", param.getProductName());
        mapEncrypt.put("request_id", param.getRequestId());
        mapEncrypt.put("merchant_code", param.getMerchantCode());

        String jsonRaw = new Gson().toJson(mapEncrypt);
        String jsonString = EncryptServiceHelper.INSTANCE.encryptKeyAesBase64(jsonRaw, key);

        // Call API
        Call<String> call = apiService.createOrder(
                key,
                jsonString
        );
        enqueue(call, new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.code() == 200 && response.body() != null) {
                    try {
                        String objectDecrypt = EncryptServiceHelper.INSTANCE.decryptAesBase64(
                                response.body(),
                                EncryptServiceHelper.INSTANCE.getRandomkeyRaw());
                        Gson gson = new Gson();
                        CreateOrderPaymentMethodModel createOrderCardModel = gson.fromJson(objectDecrypt, CreateOrderPaymentMethodModel.class);
                        mainThread.post(() -> {
                            callbackCreateOrder.onSuccess(createOrderCardModel.getData());
                        });
                    } catch (JsonSyntaxException | DecoderException e) {
                        mainThread.post(() -> {
                            JsonObject error = new JsonObject();
                            error.addProperty("message", "Có lỗi xảy ra");
                            callbackCreateOrder.onError(error);
                        });
                    }
                } else {
                    String errorMessage = response.message();
                    if (response.errorBody() != null) {
                        try {
                            String errorBodyString = response.errorBody().string();
                            // Attempt to parse error body as JSON and extract message if available
                            Gson gson = new Gson();
                            JsonObject errorJson = gson.fromJson(errorBodyString, JsonObject.class);
                            if (errorJson != null && errorJson.has("message")) {
                                errorMessage = errorJson.get("message").getAsString();
                            }
                        } catch (Exception e) {
                            // Ignore parsing error, fallback to response.message()
                        }
                    }
                    String finalErrorMessage = errorMessage;
                    mainThread.post(() -> {
                        JsonObject error = new JsonObject();
                        error.addProperty("message", finalErrorMessage);
                        callbackCreateOrder.onError(error);
                    });
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                mainThread.post(() -> {
                    JsonObject error = new JsonObject();
                    error.addProperty("message", "Có lỗi xảy ra");
                    callbackCreateOrder.onError(error);
                });
            }
        });

    }
}
