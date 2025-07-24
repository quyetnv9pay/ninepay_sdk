package com.npsdk.module;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.WebStorage;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.npsdk.LibListener;
import com.npsdk.jetpack_sdk.DataOrder;
import com.npsdk.jetpack_sdk.InputCardActivity;
import com.npsdk.jetpack_sdk.OrderActivity;
import com.npsdk.jetpack_sdk.base.AppUtils;
import com.npsdk.jetpack_sdk.repository.CallbackCreateOrderPaymentMethod;
import com.npsdk.jetpack_sdk.repository.CreatePaymentOrderRepo;
import com.npsdk.jetpack_sdk.repository.GetInfoMerchant;
import com.npsdk.jetpack_sdk.repository.model.CreateOrderParamWalletMethod;
import com.npsdk.jetpack_sdk.repository.model.DataCreateOrderPaymentMethod;
import com.npsdk.module.api.GetInfoTask;
import com.npsdk.jetpack_sdk.repository.GetListPaymentMethodRepo;
import com.npsdk.module.api.GetPublickeyTask;
import com.npsdk.module.api.RefreshTokenTask;
import com.npsdk.module.model.SdkConfig;
import com.npsdk.module.model.UserInfo;
import com.npsdk.module.utils.*;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@SuppressLint("StaticFieldLeak")
public class NPayLibrary {
    private static final String TAG = NPayLibrary.class.getSimpleName();
    private static NPayLibrary INSTANCE;
    public SdkConfig sdkConfig;
    public Activity activity;
    public LibListener listener;

    public static NPayLibrary getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new NPayLibrary();
        }
        return INSTANCE;
    }

    public void init(Activity activity, SdkConfig sdkConfig, LibListener listener) {
        this.activity = activity;
        this.sdkConfig = sdkConfig;
        this.listener = listener;
        Flavor.configFlavor(sdkConfig.getEnv());

        if (sdkConfig.getSecretKey() == null || sdkConfig.getSecretKey().isEmpty()) {
            Toast.makeText(activity, "Secret key not found!", Toast.LENGTH_SHORT).show();
            activity.finish();
            return;
        }
        if(isLogOut(sdkConfig)){
            logout();
            DataOrder.clearData();
        }
        saveSdkConfig(sdkConfig);
        new GetInfoMerchant().get();
        if (!AppUtils.INSTANCE.isLogged()) {
            GetPublickeyTask getPublickeyTask = new GetPublickeyTask(activity);
            getPublickeyTask.execute();
        }
    }

    public boolean isLogOut(SdkConfig sdkConfig) {
        String phoneCache = Preference.getString(activity, sdkConfig.getEnv() + Constants.PHONE, "");
        boolean isSamePhone = phoneCache.equals(sdkConfig.getPhoneNumber());

        String merchantCodeCache = Preference.getString(activity, sdkConfig.getEnv() + Constants.MERCHANT_CODE, "");
        boolean isSameMerchantCode = merchantCodeCache.equals(sdkConfig.getMerchantCode());

        String environment = Preference.getString(activity, Constants.INIT_ENVIRONMENT, "");
        boolean isSameEnvironment = environment.equals(sdkConfig.getEnv());

        return !isSamePhone || !isSameMerchantCode || !isSameEnvironment;
    }

    public void saveSdkConfig(SdkConfig sdkConfig) {
        Preference.save(activity, sdkConfig.getEnv() + Constants.MERCHANT_CODE, sdkConfig.getMerchantCode());
        Preference.save(activity,sdkConfig.getEnv() + Constants.PHONE, sdkConfig.getPhoneNumber());
    }

    public void openSDKWithAction(String actions) {
        Intent intent = new Intent(activity, NPayActivity.class);
        intent.putExtra("data", NPayLibrary.getInstance().walletData(actions));
        activity.startActivity(intent);
    }

    public void openPaymentOnSDK(String url, @Nullable String type, Boolean isShowResultScreen) {
        if (url == null || url.trim().isEmpty()) {
            Toast.makeText(activity, "Vui lòng nhập URL thanh toán!", Toast.LENGTH_SHORT).show();
            return;
        }
        long currentTime = System.currentTimeMillis();
        long lastTimeGetPublicKey = Preference.getLong(activity, Flavor.prefKey + Constants.LAST_TIME_PUBLIC_KEY, 0);
        boolean isNeedGetPublicKey = (currentTime - lastTimeGetPublicKey) > 36000; // if last get more than 10 hours.

        if (!AppUtils.INSTANCE.isLogged() && isNeedGetPublicKey) {
            GetPublickeyTask getPublickeyTask = new GetPublickeyTask(activity);
            getPublickeyTask.execute();
        }
        DataOrder.Companion.setUrlData(url);
        DataOrder.Companion.setShowResultScreen(isShowResultScreen);

        if (type == null || type.equals(PaymentMethod.DEFAULT) || type.equals(PaymentMethod.WALLET)) {
            if (type != null && type.equals(PaymentMethod.WALLET)) {
                String pubKey = Preference.getString(activity, Flavor.prefKey + Constants.PUBLIC_KEY, "");
                String token = Preference.getString(activity, Flavor.prefKey + Constants.ACCESS_TOKEN, "");

                if (pubKey.isEmpty() || token.isEmpty()) {
                    DataOrder.Companion.setProgressing(true);
                    DataOrder.Companion.setStartScreen(true);
                    // Gọi sang webview login
                    NPayLibrary.getInstance().openSDKWithAction(Actions.LOGIN);
                    return;
                }
            }
            Intent intent = new Intent(activity, OrderActivity.class);
            if (type != null) intent.putExtra("method", type);
            intent.putExtra("url", url);
            activity.startActivity(intent);
            return;
        }

        // Method other
        Intent intent = new Intent(activity, InputCardActivity.class);
        intent.putExtra("method", type);
        activity.startActivity(intent);
    }

    public void getUserInfoSendToPayment(@Nullable Runnable afterSuccess) {
        DataOrder.Companion.setUserInfo(null);
        String token = Preference.getString(activity, Flavor.prefKey + Constants.ACCESS_TOKEN, "");
        String publicKey = Preference.getString(activity, Flavor.prefKey + Constants.PUBLIC_KEY, "");
        String deviceId = DeviceUtils.getDeviceID(activity);
        String UID = DeviceUtils.getUniqueID(activity);
        if (token.isEmpty() || publicKey.isEmpty()) return;
        // Get user info
        GetInfoTask getInfoTask = new GetInfoTask(activity, "Bearer " + token, new GetInfoTask.OnGetInfoListener() {
            @Override
            public void onGetInfoSuccess(UserInfo userInfo) {
                Gson gson = new Gson();
                Preference.save(activity, NPayLibrary.getInstance().sdkConfig.getEnv() + Constants.PHONE, userInfo.getPhone());
                DataOrder.Companion.setUserInfo(userInfo);
                Map<String, Object> userInfoMap = new HashMap<>();
                userInfoMap.put("phone", userInfo.getPhone());
                userInfoMap.put("balance", userInfo.getBalance().toString());
                userInfoMap.put("statusKyc", userInfo.getStatus().toString());
                userInfoMap.put("name", userInfo.getName());
                String json = gson.toJson(userInfoMap);
                listener.getInfoSuccess(json);
                if (afterSuccess != null) {
                    afterSuccess.run();
                }
            }

            @Override
            public void onError(int errorCode, String message) {
                if (errorCode == Constants.NOT_LOGIN || message.contains("đã hết hạn") || message.toLowerCase().contains("không tìm thấy")) {
                    refreshToken(deviceId, UID, new Runnable() { // Refresh success
                        @Override
                        public void run() {
                            // Đệ quy
                            getUserInfoSendToPayment(null);
                        }
                    });
                }

            }
        });
        getInfoTask.execute();
    }

    public void getUserInfo() {
        String token = Preference.getString(activity, Flavor.prefKey + Constants.ACCESS_TOKEN, "");

        if (token.isEmpty()) {
            listener.onError(Constants.NOT_LOGIN, "Tài khoản chưa được đăng nhập!");
            return;
        }
        String deviceId = DeviceUtils.getDeviceID(activity);
        String UID = DeviceUtils.getUniqueID(activity);
        Log.d(TAG, "device id : " + deviceId + " , UID : " + UID);
        GetInfoTask getInfoTask = new GetInfoTask(activity, "Bearer " + token, new GetInfoTask.OnGetInfoListener() {
            @Override
            public void onGetInfoSuccess(UserInfo userInfo) {
                Gson gson = new Gson();
                DataOrder.Companion.setUserInfo(userInfo);
                Map<String, Object> userInfoMap = new HashMap<>();
                userInfoMap.put("phone", userInfo.getPhone());
                userInfoMap.put("balance", userInfo.getBalance().toString());
                userInfoMap.put("statusKyc", userInfo.getStatus().toString());
                userInfoMap.put("name", userInfo.getName());
                userInfoMap.put("banks", userInfo.getBanks());
                String json = gson.toJson(userInfoMap);
                listener.getInfoSuccess(json);
            }

            @Override
            public void onError(int errorCode, String message) {
                if (errorCode == Constants.NOT_LOGIN || message.contains("đã hết hạn") || message.toLowerCase().contains("không tìm thấy")) {
                    refreshToken(deviceId, UID, null);
                    return;
                }
                listener.onError(errorCode, message);
            }
        });
        getInfoTask.execute();
    }

    private void refreshToken(String deviceId, String UID, @Nullable Runnable runnable) {
        RefreshTokenTask refreshTokenTask = new RefreshTokenTask(activity, deviceId, UID, new RefreshTokenTask.OnRefreshListener() {
            @Override
            public void onRefreshSuccess() {

                if (runnable != null) {
                    runnable.run();
                } else {
                    getUserInfo();
                }
            }

            @Override
            public void onError(int errorCode, String message) {
                listener.onError(errorCode, message);

            }
        }, Preference.getString(activity, NPayLibrary.getInstance().sdkConfig.getEnv() + Constants.REFRESH_TOKEN));
        refreshTokenTask.execute();
    }

    // Remove cookie, session, phone number and merchant code
    // If you want to delete the password, call the removeToken function.
    public void logout() {
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookies(null);
        cookieManager.removeSessionCookies(null);
        WebStorage.getInstance().deleteAllData();
        cookieManager.flush();
        Preference.remove(activity, NPayLibrary.getInstance().sdkConfig.getEnv() + Constants.PHONE);
        Preference.remove(activity, NPayLibrary.getInstance().sdkConfig.getEnv() + Constants.LAST_TIME_PUBLIC_KEY);
        Preference.remove(activity, NPayLibrary.getInstance().sdkConfig.getEnv() + Constants.MERCHANT_CODE);
    }

    public void removeToken() {
        Preference.removeEncrypted(activity, NPayLibrary.getInstance().sdkConfig.getEnv() + Constants.ACCESS_TOKEN);
        Preference.removeEncrypted(activity, NPayLibrary.getInstance().sdkConfig.getEnv() + Constants.REFRESH_TOKEN);
        Preference.removeEncrypted(activity, NPayLibrary.getInstance().sdkConfig.getEnv() + Constants.PUBLIC_KEY);
        listener.onLogoutSuccessful();
    }

    public void close() {
        Intent intentClose = new Intent();
        intentClose.setAction("nativeBroadcast");
        intentClose.putExtra("action", "close");
        LocalBroadcastManager.getInstance(activity).sendBroadcast(intentClose);
        listener.onCloseSDK();
    }

    public Map<String, String> getHeader() {
        Map<String, String> header = new HashMap<>();
        header.put("Merchant-Code", sdkConfig.getMerchantCode());
        header.put("Secret-Key", sdkConfig.getSecretKey());
        header.put("Merchant-Uid", sdkConfig.getUid());
        header.put("env", sdkConfig.getEnv());
        header.put("App-Type", "SDK");
        header.put("is-new-sdk", "true");
        header.put("Access-Control-Allow-Origin", "*");
        header.put("brand_color", sdkConfig.getBrandColor());
        header.put("platform", "android");
        header.put("device-name", DeviceUtils.getDeviceName());
        header.put("User-Agent", DeviceUtils.getDeviceName());
        header.put("phone-number", sdkConfig.getPhoneNumber());
        return header;
    }

    private String walletData(String route) {
        Map<String, String> data = getHeader();
        data.put("route", route);
        JSONObject obj = new JSONObject(data);
        return obj.toString();
    }

    public void callBackToMerchant(String name, Object status, @Nullable Object param) {
        listener.sdkDidComplete(name, status, param);
    }

    public void callbackBackToAppfrom(String screen) {
        listener.backToAppFrom(screen);
    }

    public void callbackError(int errorCode, String message) {
        listener.onError(errorCode, message);
    }

    public boolean isLogin(){
        return !Preference.getString(activity, sdkConfig.getEnv() + Constants.ACCESS_TOKEN, "").isEmpty();
    }

    public void getListPaymentMethods(ListPaymentMethodCallback callback) {
        String token = Preference.getString(activity, Flavor.prefKey + Constants.ACCESS_TOKEN, "");
        String phone = Preference.getString(activity, sdkConfig.getEnv() + Constants.PHONE, "");
        if (token.isEmpty() || phone.isEmpty()) {
            callback.onSuccess(JsonUtils.wrapWithDefault(
                    "Tài khoản chưa được đăng nhập!",
                    Constants.NOT_LOGIN
            ));
            return;
        }

        GetListPaymentMethodRepo getListPaymentMethodTask = new GetListPaymentMethodRepo();
        getListPaymentMethodTask.check(activity, callback);

    }

    public void createOrder(
            String amount,
            String productName,
            String bType,
            String bInfo,
            FailureCallback onFail
    ) {
        CallbackCreateOrderPaymentMethod callback = new CallbackCreateOrderPaymentMethod() {
            @Override
            public void onSuccess(DataCreateOrderPaymentMethod result) {
                Intent intent = new Intent(activity, NPayActivity.class);

                String endpoint = "payment";
                Map<String, String> params = Map.of(
                        "order_id", result.getOrderCode(),
                        "b_type", bType,
                        "b_info", bInfo
                );

                String encodedUrl = encodeEndpoint(endpoint, params);
                String data = NPayLibrary.getInstance().walletData(encodedUrl);
                intent.putExtra("data", data);
                activity.startActivity(intent);
            }

            @Override
            public void onError(JsonObject error) {
                onFail.onFailed(error);
            }
        };
        CreatePaymentOrderRepo createPaymentOrderRepo = new CreatePaymentOrderRepo();

        String requestId = UUID.randomUUID().toString();

        CreateOrderParamWalletMethod param = new CreateOrderParamWalletMethod(
                amount,
                productName,
                requestId,
                sdkConfig.getMerchantCode()
        );
        createPaymentOrderRepo.check(activity, param, callback);
    }

    public void testOrder(
            String amount,
            String productName,
            String bType,
            String bInfo,
            FailureCallback onFail
    ) {
        Intent intent = new Intent(activity, NPayActivity.class);

        String endpoint = "payment";
        Map<String, String> params = Map.of(
                "order_id", productName,
                "b_type", bType,
                "b_info", bInfo
        );

        String encodedUrl = encodeEndpoint(endpoint, params);
        String data = NPayLibrary.getInstance().walletData(encodedUrl);
        intent.putExtra("data", data);
        activity.startActivity(intent);
    }

    public static String encodeEndpoint(String endpoint, Map<String, String> params) {
        StringBuilder encodedUrl = new StringBuilder(endpoint);

        if (!params.isEmpty()) {
            encodedUrl.append("?");
            for (Map.Entry<String, String> entry : params.entrySet()) {
                try {
                    String encodedKey = URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8.toString());
                    String encodedValue = URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8.toString());
                    encodedUrl.append(encodedKey).append("=").append(encodedValue).append("&");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
            // Xóa ký tự `&` cuối cùng
            encodedUrl.setLength(encodedUrl.length() - 1);
        }

        return encodedUrl.toString();
    }

    public interface ListPaymentMethodCallback {
        void onSuccess(JsonObject response);
    }

    public interface FailureCallback {
        void onFailed(JsonObject error);
    }
}
