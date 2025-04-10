package com.npsdk.module.api;

import android.content.Context;
import android.os.AsyncTask;

import android.widget.Toast;
import androidx.annotation.NonNull;

import com.npsdk.module.NPayLibrary;
import com.npsdk.module.model.RefreshTokenModel;
import com.npsdk.module.model.RefreshTokenResponse;
import com.npsdk.module.utils.Constants;
import com.npsdk.module.utils.Preference;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RefreshTokenTask extends AsyncTask<Void, Void, Void> {
    private final OnRefreshListener callback;
    private final Context context;
    private final String refreshToken;
    private String deviceId;
    private String UID;

    public RefreshTokenTask(Context ct, String deviceId , String UID, OnRefreshListener callback, String refreshToken) {
        context = ct;
        this.callback = callback;
        this.refreshToken = refreshToken;
        this.deviceId = deviceId;
        this.UID = UID;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        try {
            RestfulApi restfulApi = RestfulApi.getInstance(context);
            Call<RefreshTokenResponse> call = restfulApi.RefreshTokenTask(refreshToken, deviceId, UID);
            call.enqueue(new Callback<RefreshTokenResponse>() {
                @Override
                public void onResponse(@NonNull Call<RefreshTokenResponse> call, @NonNull Response<RefreshTokenResponse> response) {
                    try {
                        if (response.body() != null) {
                            RefreshTokenModel model = response.body().getData();
                            if (model != null) {
                                Preference.save(context,
                                        NPayLibrary.getInstance().sdkConfig.getEnv() + Constants.ACCESS_TOKEN,
                                        model.getAccessToken());
                                Preference.save(context,
                                        NPayLibrary.getInstance().sdkConfig.getEnv() + Constants.REFRESH_TOKEN,
                                        model.getRefreshToken());
                                Preference.save(context,
                                        NPayLibrary.getInstance().sdkConfig.getEnv() + Constants.PUBLIC_KEY,
                                        model.getPublicKey());
                                callback.onRefreshSuccess();
                            } else {
                                NPayLibrary.getInstance().removeToken();
                                NPayLibrary.getInstance().logout();
                                NPayLibrary.getInstance().close();
                            }

                        } else {
                            callback.onError(500, "Có lỗi xảy ra");
                        }
                    } catch (IOException e) {
                        callback.onError(500, "Có lỗi xảy ra");
                    }
                }

                @Override
                public void onFailure(Call<RefreshTokenResponse> call, Throwable t) {
                    callback.onError(500, "Có lỗi xảy ra");
                }
            });
        } catch (Exception e) {
            callback.onError(500, "Có lỗi xảy ra");
            e.printStackTrace();
        }
        return null;
    }

    public interface OnRefreshListener {
        void onRefreshSuccess() throws IOException;

        void onError(int errorCode, String message);
    }
}
