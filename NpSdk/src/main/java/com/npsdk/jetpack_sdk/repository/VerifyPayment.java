package com.npsdk.jetpack_sdk.repository;


import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.npsdk.jetpack_sdk.base.api.BaseApiClient;
import vn.ninepay.sdk.encryptservice.EncryptService;
import com.npsdk.jetpack_sdk.repository.model.VerifyPaymentModel;
import com.npsdk.module.NPayLibrary;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VerifyPayment extends BaseApiClient {

    ExecutorService executor = Executors.newSingleThreadExecutor();
    Handler mainThread = new Handler(Looper.getMainLooper());

    public void create(Context context, String paymentId, String otp, CallbackVerifyPayment callback) {
        executor.execute(() -> {
            Call<String> call = apiService.verifyPayment(paymentId, otp);
            enqueue(call, new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {
                    if (response.code() == 200 && response.body() != null) {
                        String objectDecrypt = EncryptService.INSTANCE.decryptAesBase64(
                                response.body(),
                                EncryptService.INSTANCE.getRandomkeyRaw()
                        );
                        Gson gson = new GsonBuilder().setPrettyPrinting().create();
                        try {
                            VerifyPaymentModel verifyPaymentModel = gson.fromJson(objectDecrypt, VerifyPaymentModel.class);
                            updateUI(() -> {
                                if (verifyPaymentModel.getErrorCode() == 1) {
                                    callback.onSuccess(verifyPaymentModel.getMessage());
                                } else {
                                    callback.onSuccess(null);
                                }
                            });
                        } catch (JsonSyntaxException e) {
                            NPayLibrary.getInstance().callbackError(2004, "Không thể giải mã dữ liệu.");
                        }
                    } else {
                        NPayLibrary.getInstance().callbackError(2005, "Lỗi không xác định");
                    }
                }

                @Override
                public void onFailure(Call<String> call, Throwable t) {
                    NPayLibrary.getInstance().callbackError(2005, "Lỗi không xác định");
                }
            });
        });

    }

    private void updateUI(Runnable runnable) {
        mainThread.post(runnable);
    }
}