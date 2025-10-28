package com.npsdk.module;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
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
import vn.ninepay.sdk.encryptservice.EncryptService;
import com.npsdk.jetpack_sdk.base.api.NPayEncryptDataProvider;
import com.npsdk.jetpack_sdk.repository.BaseCallback;
import com.npsdk.jetpack_sdk.repository.CallbackCreateOrderPaymentMethod;
import com.npsdk.jetpack_sdk.repository.CallbackListPaymentMethod;
import com.npsdk.jetpack_sdk.repository.CreatePaymentOrderRepo;
import com.npsdk.jetpack_sdk.repository.GetInfoMerchant;
import com.npsdk.jetpack_sdk.repository.CuponRepo;
import com.npsdk.jetpack_sdk.repository.RefreshTokenCallback;
import com.npsdk.jetpack_sdk.repository.model.CouponInfo;
import com.npsdk.jetpack_sdk.repository.model.CreateOrderParamWalletMethod;
import com.npsdk.jetpack_sdk.repository.model.DataCreateOrderPaymentMethod;
import com.npsdk.jetpack_sdk.repository.model.GetListOfCouponsParams;
import com.npsdk.jetpack_sdk.repository.model.ValidateCouponParams;
import com.npsdk.module.api.GetInfoTask;
import com.npsdk.jetpack_sdk.repository.GetListPaymentMethodRepo;
import com.npsdk.module.api.GetPublickeyTask;
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
    private static final int MAX_RETRY_COUNT = 2;
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

        NPayEncryptDataProvider dataProvider = new NPayEncryptDataProvider();

        // Khởi tạo EncryptService của AAR
        EncryptService.INSTANCE.initialize(dataProvider);
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
        _getUserInfoSendToPayment(afterSuccess, 0);
    }

    private void _getUserInfoSendToPayment(@Nullable Runnable afterSuccess, int retryCount) {
        DataOrder.Companion.setUserInfo(null);
        String token = Preference.getString(activity, Flavor.prefKey + Constants.ACCESS_TOKEN, "");
        String publicKey = Preference.getString(activity, Flavor.prefKey + Constants.PUBLIC_KEY, "");
        if (token.isEmpty() || publicKey.isEmpty()) return;
        // Get user info
        GetInfoTask getInfoTask = new GetInfoTask(activity, "Bearer " + token, new GetInfoTask.OnGetInfoListener() {
            @Override
            public void onGetInfoSuccess(UserInfo userInfo) {
                Preference.save(activity, NPayLibrary.getInstance().sdkConfig.getEnv() + Constants.PHONE, userInfo.getPhone());
                DataOrder.Companion.setUserInfo(userInfo);
                listener.getInfoSuccess(buildUserInfoJson(userInfo, false));
                if (afterSuccess != null) {
                    afterSuccess.run();
                }
            }

            @Override
            public void onError(int errorCode, String message) {
                if (shouldRefreshToken(errorCode, message, retryCount)) {
                    refreshToken(new RefreshTokenCallback() {
                        @Override
                        public void onSuccess() {
                            _getUserInfoSendToPayment(afterSuccess, retryCount + 1);
                        }

                        @Override
                        public void onError(int errorCode, String message) {

                        }
                    });
                }
            }
        });
        getInfoTask.execute();
    }

    public void getUserInfo() {
        _getUserInfo(0);
    }

    private void _getUserInfo(int retryCount) {
        String token = Preference.getString(activity, Flavor.prefKey + Constants.ACCESS_TOKEN, "");

        if (token.isEmpty()) {
            listener.onError(Constants.NOT_LOGIN, "Tài khoản chưa được đăng nhập!");
            return;
        }
        GetInfoTask getInfoTask = new GetInfoTask(activity, "Bearer " + token, new GetInfoTask.OnGetInfoListener() {
            @Override
            public void onGetInfoSuccess(UserInfo userInfo) {
                DataOrder.Companion.setUserInfo(userInfo);
                listener.getInfoSuccess(buildUserInfoJson(userInfo, true));
            }

            @Override
            public void onError(int errorCode, String message) {
                if (shouldRefreshToken(errorCode, message, retryCount)) {
                    refreshToken(new RefreshTokenCallback() {
                        @Override
                        public void onSuccess() {
                            _getUserInfo(retryCount + 1);
                        }

                        @Override
                        public void onError(int errorCode, String message) {
                            listener.onError(errorCode, message);
                        }
                    });
                    return;
                }
                listener.onError(errorCode, message);
            }
        });
        getInfoTask.execute();
    }

    private void refreshToken(@Nullable RefreshTokenCallback refreshTokenCallback) {
        String deviceId = DeviceUtils.getDeviceID(activity);
        String UID = DeviceUtils.getUniqueID(activity);
        TokenManager.refreshTokenIfNeeded(activity, deviceId, UID, refreshTokenCallback);
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

    public void callBackToMerchant(String name, Boolean status, @Nullable Object params) {
        listener.sdkDidComplete(name, status, params);
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

    public void getListPaymentMethods(CallbackListPaymentMethod callback) {
        _getListPaymentMethods(callback, 0);
    }

    private void _getListPaymentMethods(CallbackListPaymentMethod callback, int retryCount) {
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
        getListPaymentMethodTask.check(activity, response -> {
            int errorCode = response.has("error_code") ? response.get("error_code").getAsInt() : 0;
            String message = response.has("message") ? response.get("message").getAsString() : "";

            if (shouldRefreshToken(errorCode, message, retryCount)) {
                refreshToken(new RefreshTokenCallback() {
                    @Override
                    public void onSuccess() {
                        _getListPaymentMethods(callback, retryCount + 1);
                    }

                    @Override
                    public void onError(int errorCode, String message) {
                        callback.onSuccess(response);
                    }
                });
                return;
            }

            callback.onSuccess(response);
        });
    }

    public void createOrder(
        @Nullable String requestId,
        String amount,
        String productName,
        String bType,
        String bInfo,
        @Nullable Integer couponId,
        @Nullable String coupon,
        @Nullable Map<String, Object> metaData,
        CallbackCreateOrderPaymentMethod callback
    ) {
        _createOrder(requestId, amount, productName, bType, bInfo, couponId, coupon, metaData, callback, 0);
    }

    private void _createOrder(
        @Nullable String requestId,
        String amount,
        String productName,
        String bType,
        String bInfo,
        @Nullable Integer couponId,
        @Nullable String coupon,
        @Nullable Map<String, Object> metaData,
        CallbackCreateOrderPaymentMethod callback,
        int retryCount
    ) {
        String _requestId = requestId;
        CallbackCreateOrderPaymentMethod wrappedCallback = new CallbackCreateOrderPaymentMethod() {
            @Override
            public void onSuccess(DataCreateOrderPaymentMethod result) {
                callback.onSuccess(result);
                CouponInfo info = result.getCouponInfo();
                payOrder(result.getOrderCode(), bType, bInfo, info != null ? info.getId() : null, info != null ? info.getCode() : null);
            }

            @Override
            public void onError(JsonObject error) {
                int errorCode = error.has("error_code") ? error.get("error_code").getAsInt() : 0;
                String message = error.has("message") ? error.get("message").getAsString() : "";

                if (shouldRefreshToken(errorCode, message, retryCount)) {
                    refreshToken(new RefreshTokenCallback() {
                        @Override
                        public void onSuccess() {
                            _createOrder(_requestId, amount, productName, bType, bInfo, couponId, coupon, metaData, callback, retryCount + 1);
                        }

                        @Override
                        public void onError(int errorCode, String message) {
                            callback.onError(error);
                        }
                    });
                    return;
                }
                callback.onError(error);
            }
        };
        CreatePaymentOrderRepo createPaymentOrderRepo = new CreatePaymentOrderRepo();

        // Tạo requestId nếu null
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
        }

        CreateOrderParamWalletMethod param = new CreateOrderParamWalletMethod(
            amount,
            productName,
            requestId,
            sdkConfig.getMerchantCode(),
            couponId,
            coupon,
            metaData
        );
        createPaymentOrderRepo.check(activity, param, wrappedCallback);
    }

    public void payOrder(
        String orderId,
        String bType,
        String bInfo,
        @Nullable Integer couponId,
        @Nullable String coupon
    ) {
        Intent intent = new Intent(activity, NPayActivity.class);

        String endpoint = "payment";
        Map<String, String> params = new HashMap<>(Map.of(
            "order_id", orderId,
            "b_type", bType,
            "b_info", bInfo
        ));

        if (couponId != null) {
            params.put("coupon_id", String.valueOf(couponId));
        }
        if (coupon != null) {
            params.put("coupon", coupon);
        }

        String encodedUrl = encodeEndpoint(endpoint, params);
        String data = NPayLibrary.getInstance().walletData(encodedUrl);
        intent.putExtra("data", data);
        activity.startActivity(intent);
    }

    public void getListOfCoupons(String amount, @Nullable String eventId, BaseCallback callback) {
        _getListOfCoupons(amount, eventId, callback, 0);
    }

    private void _getListOfCoupons(String amount, @Nullable String eventId, BaseCallback callback, int retryCount) {
        CuponRepo cuponRepo = new CuponRepo();
        GetListOfCouponsParams getListOfCouponsParams = new GetListOfCouponsParams(amount, eventId);

        cuponRepo.getListOfCoupons(getListOfCouponsParams, new BaseCallback() {
            @Override
            public void onSuccess(JsonObject response) {
                int errorCode = response.has("error_code") ? response.get("error_code").getAsInt() : 0;
                if (isSuccess(errorCode)) {
                    callback.onSuccess(response);
                } else {
                    callback.onError(response);
                }
            }
            @Override
            public void onError(JsonObject error) {
                int errorCode = error.has("error_code") ? error.get("error_code").getAsInt() : 0;
                String message = error.has("message") ? error.get("message").getAsString() : "";

                if (shouldRefreshToken(errorCode, message, retryCount)) {
                    refreshToken(new RefreshTokenCallback() {
                        @Override
                        public void onSuccess() {
                            _getListOfCoupons(amount, eventId, callback, retryCount + 1);
                        }

                        @Override
                        public void onError(int errorCode, String message) {
                            callback.onError(error);
                        }
                    });
                    return;
                }
                callback.onError(error);
            }
        });
    }

    public void validateCoupon(String amount, @Nullable Integer couponId, @Nullable String coupon, @Nullable String eventId, BaseCallback callback) {
        _validateCoupon(amount, couponId, coupon, eventId, callback, 0);
    }

    private void _validateCoupon(String amount, @Nullable Integer couponId, @Nullable String coupon, @Nullable String eventId, BaseCallback callback, int retryCount) {
        CuponRepo cuponRepo = new CuponRepo();
        ValidateCouponParams params = new ValidateCouponParams(amount, couponId, coupon, eventId);

        cuponRepo.validateCoupon(params, new BaseCallback() {
            @Override
            public void onSuccess(JsonObject response) {
                int errorCode = response.has("error_code") ? response.get("error_code").getAsInt() : 0;
                String message = response.has("message") ? response.get("message").getAsString() : "";

                if (shouldRefreshToken(errorCode, message, retryCount)) {
                    refreshToken(new RefreshTokenCallback() {
                        @Override
                        public void onSuccess() {
                            _validateCoupon(amount, couponId, coupon, eventId, callback, retryCount + 1);
                        }

                        @Override
                        public void onError(int errorCode, String message) {
                            callback.onError(JsonUtils.wrapWithDefault(
                                message,
                                errorCode
                            ));
                        }
                    });
                    return;
                }

                if (isSuccess(errorCode)) {
                    callback.onSuccess(response);
                } else {
                    callback.onError(response);
                }
            }

            @Override
            public void onError(JsonObject error) {
                int errorCode = error.has("error_code") ? error.get("error_code").getAsInt() : 0;
                String message = error.has("message") ? error.get("message").getAsString() : "";

                if (shouldRefreshToken(errorCode, message, retryCount)) {
                    refreshToken(new RefreshTokenCallback() {
                        @Override
                        public void onSuccess() {
                            _validateCoupon(amount, couponId, coupon, eventId, callback, retryCount + 1);
                        }

                        @Override
                        public void onError(int errorCode, String message) {
                            callback.onError(error);
                        }
                    });
                    return;
                }
                callback.onError(error);
            }
        });
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

    private String buildUserInfoJson(UserInfo userInfo, boolean withBanks) {
        Gson gson = new Gson();
        DataOrder.Companion.setUserInfo(userInfo);
        Map<String, Object> userInfoMap = new HashMap<>();
        userInfoMap.put("phone", userInfo.getPhone());
        userInfoMap.put("balance", userInfo.getBalance());
        userInfoMap.put("kycStatus", userInfo.getStatus());
        userInfoMap.put("name", userInfo.getName());
        if (withBanks) {
            userInfoMap.put("banks", userInfo.getBanks());
        }
        return gson.toJson(userInfoMap);
    }

    private boolean shouldRefreshToken(int errorCode, String message, int retryCount) {
        return (errorCode == Constants.NOT_LOGIN || message.contains("đã hết hạn") || message.toLowerCase().contains("không tìm thấy")) && retryCount <= MAX_RETRY_COUNT;
    }

    private boolean isSuccess(int errorCode) {
        return errorCode == 0;
    }
}
