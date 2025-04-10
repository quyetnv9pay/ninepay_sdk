package com.npsdk.jetpack_sdk.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.npsdk.jetpack_sdk.base.api.BaseApiClient;
import com.npsdk.jetpack_sdk.base.api.EncryptServiceHelper;
import com.npsdk.module.NPayLibrary;
import com.npsdk.module.model.ListPaymentMethodResponse;
import com.npsdk.module.utils.Flavor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GetListPaymentMethodRepo extends BaseApiClient {
    ExecutorService executor = Executors.newSingleThreadExecutor();
    Handler mainThread = new Handler(Looper.getMainLooper());

    public void check(Context context, NPayLibrary.ListPaymentMethodCallback callback) {
        executor.execute(() -> {
            Call<String> call = apiService.getListPaymentMethod(
            );
            enqueue(call, new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {
                    if (response.code() == 200 && response.body() != null) {
                        String objectDecrypt = EncryptServiceHelper.INSTANCE.decryptAesBase64(
                                response.body(),
                                EncryptServiceHelper.INSTANCE.getRandomkeyRaw()
                        );
                        Gson gson = new GsonBuilder().setPrettyPrinting().create();
                        try {
                            ListPaymentMethodResponse listPaymentMethodResponse = gson.fromJson(objectDecrypt, ListPaymentMethodResponse.class);
                            updateUI(() -> {
                                callback.onSuccess(listPaymentMethodResponse.getData());
                            });
                        } catch (JsonSyntaxException e) {
                            JsonObject errorObject = new JsonObject();
                            errorObject.addProperty("code", 2005);
                            errorObject.addProperty("message", "Lỗi không xác định");
                            callback.onError(errorObject);
                        }
                    } else {
                        JsonObject errorObject = new JsonObject();
                        errorObject.addProperty("code", 2005);
                        errorObject.addProperty("message", "Lỗi không xác định");
                        callback.onError(errorObject);
                    }
                }

                @Override
                public void onFailure(Call<String> call, Throwable t) {
                    JsonObject errorObject = new JsonObject();
                    errorObject.addProperty("code", 2005);
                    errorObject.addProperty("message", "Lỗi không xác định");
                    callback.onError(errorObject);
                }
            });
        });

    }

    private void updateUI(Runnable runnable) {
        mainThread.post(runnable);
    }
}
