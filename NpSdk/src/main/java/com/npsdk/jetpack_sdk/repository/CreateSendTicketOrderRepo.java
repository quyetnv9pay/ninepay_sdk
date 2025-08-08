package com.npsdk.jetpack_sdk.repository;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.npsdk.jetpack_sdk.base.api.BaseApiClient;
import com.npsdk.jetpack_sdk.repository.model.CreateOrderPaymentMethodModel;
import com.npsdk.jetpack_sdk.repository.model.CreateSendTicketOrderBody;
import com.npsdk.jetpack_sdk.repository.model.CreateSendTicketOrderModel;
import com.npsdk.module.NPayLibrary;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateSendTicketOrderRepo extends BaseApiClient {
    Handler mainThread = new Handler(Looper.getMainLooper());

    public void sendTicket(CreateSendTicketOrderBody body, NPayLibrary.CreateSendTicketOrderCallback callback) {
        // Call API
        Call<String> call = apiService.createSendTicketOrder(
            NPayLibrary.getInstance().sdkConfig.getMerchantCode(),
            NPayLibrary.getInstance().sdkConfig.getSecretKey(),
            body
        );
        enqueue(call, new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                Log.d("SEND_TICKET", String.valueOf(response.code()));
                if (response.code() == 200 && response.body() != null) {
                    Log.d("SEND_TICKET", response.body());
                    JsonObject result = JsonParser.parseString(response.body()).getAsJsonObject();
                    mainThread.post(() -> {
                        callback.onSuccess(result);
                    });
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
                                Log.d("SEND_TICKET_ERROR", errorMessage);
                            }
                        } catch (Exception e) {
                            // Ignore parsing error, fallback to response.message()
                        }
                    }
                    String finalErrorMessage = errorMessage;
                    Log.d("SEND_TICKET_ERROR", finalErrorMessage);
                    mainThread.post(() -> {
                        JsonObject error = new JsonObject();
                        error.addProperty("message", finalErrorMessage);
                        callback.onFailed(error);
                    });
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                mainThread.post(() -> {
                    JsonObject error = new JsonObject();
                    error.addProperty("message", "Có lỗi xảy ra");
                    Log.d("SEND_TICKET_ERROR", "Có lỗi xảy ra");
                    callback.onFailed(error);
                });
            }
        });
    }
}
