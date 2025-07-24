package com.npsdk.jetpack_sdk.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.npsdk.jetpack_sdk.base.api.BaseApiClient;
import com.npsdk.jetpack_sdk.base.api.EncryptServiceHelper;
import com.npsdk.module.NPayLibrary;
import com.npsdk.module.utils.JsonUtils;

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
            Call<String> call = apiService.getListPaymentMethod();
            enqueue(call, new Callback<String>() {
                @Override
                public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                    if (response.code() == 200 && response.body() != null) {
                        String objectDecrypt = EncryptServiceHelper.INSTANCE.decryptAesBase64(
                                response.body(),
                                EncryptServiceHelper.INSTANCE.getRandomkeyRaw()
                        );
                        try {
                            JsonObject result = JsonParser.parseString(objectDecrypt).getAsJsonObject();
                            updateUI(() -> {
                                callback.onSuccess(result);
                            });
                        } catch (JsonSyntaxException e) {
                            callback.onSuccess(JsonUtils.wrapWithDefault(
                                    "Lỗi không xác định",
                                    2005
                            ));
                        }
                    } else {
                        callback.onSuccess(JsonUtils.wrapWithDefault(
                                response.message(),
                                response.code())
                        );
                    }
                }

                @Override
                public void onFailure(Call<String> call, Throwable t) {
                    callback.onSuccess(
                            JsonUtils.wrapWithDefault(t.getMessage(), 2005));
                }
            });
        });

    }

    private void updateUI(Runnable runnable) {
        mainThread.post(runnable);
    }
}

