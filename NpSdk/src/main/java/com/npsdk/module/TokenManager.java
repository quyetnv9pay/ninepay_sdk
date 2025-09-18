package com.npsdk.module;

import android.content.Context;

import com.npsdk.jetpack_sdk.repository.RefreshTokenCallback;
import com.npsdk.module.api.RefreshTokenTask;
import com.npsdk.module.utils.Constants;
import com.npsdk.module.utils.Preference;

import java.util.ArrayList;
import java.util.List;

public class TokenManager {
    private static boolean isRefreshing = false;
    private static final List<RefreshTokenCallback> pendingTasks = new ArrayList<>();

    public synchronized static void refreshTokenIfNeeded(
            Context context, String deviceId, String UID, RefreshTokenCallback refreshTokenCallback) {

        if (isRefreshing) {
            // Đang refresh → add vào queue, chờ refresh xong sẽ gọi lại
            pendingTasks.add(refreshTokenCallback);
            return;
        }

        isRefreshing = true;
        pendingTasks.add(refreshTokenCallback);

        RefreshTokenTask refreshTokenTask = new RefreshTokenTask(context, deviceId, UID, new RefreshTokenTask.OnRefreshListener() {
            @Override
            public void onRefreshSuccess() {
                synchronized (TokenManager.class) {
                    isRefreshing = false;
                    // chạy toàn bộ task chờ
                    for (RefreshTokenCallback task : pendingTasks) {
                        task.onSuccess();
                    }
                    pendingTasks.clear();
                }
            }

            @Override
            public void onError(int errorCode, String message) {
                synchronized (TokenManager.class) {
                    isRefreshing = false;
                    for (RefreshTokenCallback task : pendingTasks) {
                        task.onError(errorCode, message);
                    }
                    pendingTasks.clear();
                }
            }
        }, Preference.getString(context, NPayLibrary.getInstance().sdkConfig.getEnv() + Constants.REFRESH_TOKEN));

        refreshTokenTask.execute();
    }
}
