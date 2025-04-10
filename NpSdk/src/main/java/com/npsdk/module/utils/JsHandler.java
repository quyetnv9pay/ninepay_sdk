package com.npsdk.module.utils;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ShareCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.npsdk.jetpack_sdk.DataOrder;
import com.npsdk.jetpack_sdk.OrderActivity;
import com.npsdk.jetpack_sdk.ResultPayment;
import com.npsdk.jetpack_sdk.base.AppUtils;
import com.npsdk.module.NPayActivity;
import com.npsdk.module.NPayLibrary;
import com.npsdk.module.PaymentMethod;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import static android.content.Context.CLIPBOARD_SERVICE;

public class JsHandler {

    public static final int PERMISSION_CAMERA_REQUEST_CODE = 999;
    public static final int PERMISSION_STORAGE_REQUEST_CODE = 888;
    private static final String TAG = JsHandler.class.getSimpleName();

    private final Activity activity;

    public JsHandler(Activity activity) {
        this.activity = activity;
    }

    public static void sendStatusCamera(boolean status) {
        String jsExcute = "javascript: window.sendStatusCamera(" + status + ")";
        Handler mainHandler = new Handler(Looper.getMainLooper());
        Runnable myRunnable = () -> {
            NPayActivity.webView.loadUrl(jsExcute);
        };
        mainHandler.post(myRunnable);
    }

    public static void sendStatusStorage(boolean status) {
        String jsExcute = "javascript: window.getRequestGallery(" + status + ")";
        Handler mainHandler = new Handler(Looper.getMainLooper());
        Runnable myRunnable = () -> {
            NPayActivity.webView.loadUrl(jsExcute);
        };
        mainHandler.post(myRunnable);
    }

    @JavascriptInterface
    public void executeFunction(String command, String params) {
        try {
            Log.i(TAG, "executeFunctionP: command = " + command + "; params = " + params);
            JSONObject paramJson = new JSONObject(params);
            switch (switchCommandJS.valueOf(command)) {
                case open9PayApp:
                    String appPackageName = "vn.ninepay.ewallet";
                    openSchemaApp(appPackageName);
                    break;
                case close:
                    activity.finish();
                    NPayLibrary.getInstance().listener.onCloseSDK();
                    break;
                case backToApp:
                    activity.finish();
                    try {
                        if (paramJson.has("name")) {
                            String screenName = paramJson.getString("name");
                            NPayLibrary.getInstance().callbackBackToAppfrom(screenName);
                        }
                    } catch (Exception e) {
                        System.out.println("ERROR backToApp " + e.getMessage());
                    }
                    break;
                case getDeviceID:
                    break;
                case share:
                    ShareCompat.IntentBuilder.from(activity).setType("text/plain").setChooserTitle("9Pay").setText(paramJson.getString("text")).startChooser();
                    break;
                case copy:
                    ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("", paramJson.getString("text"));
                    clipboard.setPrimaryClip(clip);
                    break;
                case openOtherUrl:
                    Intent intentOpenWebView = new Intent();
                    intentOpenWebView.setAction("webViewBroadcast");
                    intentOpenWebView.putExtra("url", paramJson.getString("url"));
                    intentOpenWebView.putExtra("token", paramJson.getString("token"));
                    intentOpenWebView.putExtra("name", paramJson.getString("name"));
                    LocalBroadcastManager.getInstance(activity).sendBroadcast(intentOpenWebView);
                    break;
                case call:
                    Intent callIntent = new Intent(Intent.ACTION_DIAL);
                    callIntent.setData(Uri.parse(paramJson.getString("text")));
                    activity.startActivity(callIntent);
                    break;
                case message:
                    Intent messageIntent = new Intent(Intent.ACTION_VIEW);
                    messageIntent.setData(Uri.parse(paramJson.getString("text")));
                    activity.startActivity(messageIntent);
                    break;
                case onLoggedInSuccess:
                    NPayLibrary.getInstance().callBackToMerchant(NameCallback.LOGIN, "onSuccess", null);
                    break;
                case onPaymentSuccess:
                    NPayLibrary.getInstance().callBackToMerchant(NameCallback.SDK_PAYMENT, true, null);
                    break;
                case onError:
                    int errorCode = paramJson.getInt("error_code");
                    String message = paramJson.getString("message");
                    NPayLibrary.getInstance().listener.onError(errorCode, message);
                    break;
                case getAllToken:
                    Preference.save(activity, NPayLibrary.getInstance().sdkConfig.getEnv() + Constants.ACCESS_TOKEN,
                            paramJson.getString("access_token"));
                    Preference.save(activity, NPayLibrary.getInstance().sdkConfig.getEnv() + Constants.REFRESH_TOKEN,
                            paramJson.getString("refresh_token"));
                    Preference.save(activity, NPayLibrary.getInstance().sdkConfig.getEnv() + Constants.PUBLIC_KEY,
                            paramJson.getString("public_key"));
                    break;
                case requestCamera:
                    requestCamera(activity);
                    break;
                case openSchemaApp:
                    openSchemaApp(paramJson.getString("schema"));
                    break;
                case logout:
                case clearToken:
                    NPayLibrary.getInstance().logout();
                    break;
                case requestGallery:
                    requestStorage();
                    break;
                case checkPermissionStorage:
                    sendStatusStorage(isHavePermissionStorage());
                    break;
                case callbackToApp:
                    getCallbackFromJs(paramJson);
                    break;
                case send_email:
                    try {
                        String email = paramJson.getString("email");
                        Intent intentEmail = new Intent(Intent.ACTION_SENDTO);
                        intentEmail.setData(Uri.parse("mailto:" + email));
                        activity.startActivity(intentEmail);
                    } catch (Exception e) {
                    }
                    break;
//                case result_payment_token:
                case result_payment_js:
                    handleCallbackPaymentJs(paramJson);
                    break;
                case openAppSettings:
                    openAppSettings();
                    break;
                case shareImage:
                    if (paramJson.has("data")) {
                        String dataImageBase64 = paramJson.getString("data");
                        shareImage(dataImageBase64);
                    }
                    break;
                case openBrowser:
                    if (paramJson.has("url")) {
                        AppUtils.INSTANCE.openBrowser(activity, paramJson.getString("url"));
                    }
                    break;
                case openGoogleAuthen:
                    openSchemaApp("com.google.android.apps.authenticator2");
                    break;
                case paste:
                    getClipboardData();
                    break;
                case webviewLoaded:
                    Intent intentCloseLoading = new Intent();
                    intentCloseLoading.setAction("closeLoading");
                    LocalBroadcastManager.getInstance(activity).sendBroadcast(intentCloseLoading);
                    break;
                case call_ekyc:
                    Intent intentEkyc = new Intent();
                    intentEkyc.putExtra("JSON_OBJECT", paramJson.toString());
                    intentEkyc.setAction("callEkyc");
                    LocalBroadcastManager.getInstance(activity).sendBroadcast(intentEkyc);
                    break;
                case request_nfc:
                    parseNFCStatus();
                    break;
                default:
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void parseNFCStatus() {
        int data = NfcUtil.checkNfcStatus(activity.getBaseContext());
        String jsExecute = "javascript: window.parseNFCStatus('" + data + "')";
        Handler mainHandler = new Handler(Looper.getMainLooper());
        Runnable myRunnable = () -> {
            if (NPayActivity.webView == null) return;
            NPayActivity.webView.loadUrl(jsExecute);
        };
        mainHandler.post(myRunnable);
    }

    private void getClipboardData() {
        try {
            ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(CLIPBOARD_SERVICE);
            String data = clipboard.getPrimaryClip().getItemAt(0).getText().toString();
            String jsExecute = "javascript: window.pasteData('" + data + "')";
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = () -> {
                if (NPayActivity.webView == null) return;
                NPayActivity.webView.loadUrl(jsExecute);
            };
            mainHandler.post(myRunnable);
        } catch (Exception e) {
            Toast.makeText(activity, "Error when paste", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareImage(String base64Image) {
        try {
            // Decode the Base64 string into a bitmap
            byte[] decodedBytes = android.util.Base64.decode(base64Image, android.util.Base64.DEFAULT);
            Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

            // Save the bitmap to a temporary file
            File cachePath = new File(activity.getCacheDir(), "images");
            cachePath.mkdirs();
            File imageFile = new File(cachePath, "shared_image.png");
            FileOutputStream fos = new FileOutputStream(imageFile);
            decodedBitmap.compress(Bitmap.CompressFormat.PNG, 30, fos);
            fos.flush();
            fos.close();

            // Create an Intent to share the image
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/*");
            Uri uriShare = FileProvider.getUriForFile(activity, activity.getPackageName() + ".provider", imageFile);
            shareIntent.putExtra(Intent.EXTRA_STREAM, uriShare);

            // Start an activity to allow the user to choose the app to share with
            Intent chooser = Intent.createChooser(shareIntent, "Share Image");
            List<ResolveInfo> resInfoList = activity.getPackageManager().queryIntentActivities(chooser,
                    PackageManager.MATCH_DEFAULT_ONLY);

            for (ResolveInfo resolveInfo : resInfoList) {
                String packageName = resolveInfo.activityInfo.packageName;
                activity.grantUriPermission(packageName, uriShare,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            activity.startActivity(chooser);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(activity, "Error sharing image", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleCallbackPaymentJs(JSONObject jsonObject) {
        activity.finish();

        Intent intentResult = new Intent(activity, ResultPayment.class);
        String status = Constants.SUCCESS;
        try {
            if (jsonObject != null) {
                status = jsonObject.getString("order_status");
                if (status.contains(Constants.FAIL)) {
                    intentResult.putExtra("message", jsonObject.getString("message"));
                }
            }
        } catch (JSONException exception) {
            status = Constants.FAIL;
        }

        intentResult.putExtra("status", status);
        activity.startActivity(intentResult); // Luôn hiển thị kết quả.
//        if (DataOrder.Companion.isShowResultScreen()) {
//            activity.startActivity(intentResult);
//        } else {
//            Boolean isSuccess = status.contains(Constants.SUCCESS);
//            NPayLibrary.getInstance().callBackToMerchant(NameCallback.SDK_PAYMENT, isSuccess, null);
//            NPayLibrary.getInstance().listener.onCloseSDK();
//            NPayLibrary.getInstance().callbackError(2002, "Lỗi khi thanh toán");
//        }

    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
        intent.setData(uri);
        activity.startActivity(intent);
    }

    private void getCallbackFromJs(JSONObject jsonObject) {
        try {
            System.out.println("params " + jsonObject);
            String nameCallback = jsonObject.getString("name");
            Object statusCallback = jsonObject.getString("status");
            String params = null;
            if (jsonObject.has("params")) {
                params = jsonObject.getString("params");
            }

            // Send callback
            NPayLibrary.getInstance().callBackToMerchant(nameCallback, statusCallback, params);


            // Back to SDK
            Boolean isResetPassword = nameCallback.equals("RESET_PASSWORD");
            Boolean isDeposit = nameCallback.equals("DEPOSIT");
            Boolean isLoginSuccess =
                    nameCallback.equals("LOGIN") && (jsonObject.has("status") && jsonObject.getString("status").equals("onSuccess"));


            if (isResetPassword || isDeposit || isLoginSuccess) {

                // Kiem tra co click tu native mobile payment hay khong.
                if (DataOrder.Companion.isProgressing()) {
                    DataOrder.Companion.setProgressing(false);
                    if (isDeposit || !DataOrder.Companion.isStartScreen()) {
                        NPayLibrary.getInstance().getUserInfoSendToPayment(new Runnable() {
                            @Override
                            public void run() {
                                activity.finish();
                            }
                        });
                    } else if (isLoginSuccess) {
                        if (DataOrder.Companion.isStartScreen()) {
                            // Co the la login den tu viec nap tien
                            Intent intent = new Intent(activity, OrderActivity.class);
                            intent.putExtra("method", PaymentMethod.WALLET);
                            intent.putExtra("url", DataOrder.Companion.getUrlData());
                            activity.startActivity(intent);
                        }
                    }
                }
            }
        } catch (JSONException jsonException) {
            System.out.println(jsonException);
        }
    }

    private void requestCamera(Activity activity) {
        if (isHavePermissionCamera()) {
            sendStatusCamera(true);
            return;
        }
        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.CAMERA},
                PERMISSION_CAMERA_REQUEST_CODE);
    }

    private void requestStorage() {
        if (isHavePermissionStorage()) {
            sendStatusStorage(true);
            return;
        }

        String[] typePermission = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13
            typePermission = new String[]{Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO};
        }

        ActivityCompat.requestPermissions(activity, typePermission, PERMISSION_STORAGE_REQUEST_CODE);
    }

    private boolean isHavePermissionStorage() {
        int checked = PackageManager.PERMISSION_DENIED;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13
            checked = ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_IMAGES);
        } else {
            checked = ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        return checked == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isHavePermissionCamera() {
        int result = ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    private void openSchemaApp(String schema) {
        if (schema == null || schema.isEmpty()) return;
        try {
            Intent intent = activity.getPackageManager().getLaunchIntentForPackage(schema);
            if (intent == null) {
                // Bring user to the market or let them choose an app?
                intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("market://details?id=" + schema));
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            Log.d("OPEN APP", ex.getMessage());
        }
    }

    private enum switchCommandJS {
        open9PayApp, close, logout, openOtherUrl, share, copy, call, message, clearToken, onLoggedInSuccess,
        onPaymentSuccess, onError, getAllToken, getDeviceID, requestCamera, openSchemaApp, requestGallery,
        checkPermissionStorage, backToApp, callbackToApp, send_email, result_payment_js, openAppSettings,
        shareImage, openBrowser, openGoogleAuthen, paste, webviewLoaded, call_ekyc, request_nfc,
    }
}