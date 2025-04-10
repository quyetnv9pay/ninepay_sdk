package com.npsdk.module;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.hardware.Camera;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.*;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.webkit.*;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.window.OnBackInvokedDispatcher;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.gson.Gson;
import com.npsdk.NotificationCancelReceiver;
import com.npsdk.R;
import com.npsdk.module.model.HyperKycParams;
import com.npsdk.module.utils.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

//import co.hyperverge.hyperkyc.HyperKyc;
//import co.hyperverge.hyperkyc.data.models.HyperKycConfig;

public class NPayActivity extends AppCompatActivity {
    public static final String TAG = NPayActivity.class.getName();
    private static final int FILE_CHOOSER_REQUEST_CODE = 10000;
    @SuppressLint("StaticFieldLeak")
    public static WebView webView, webView2;
    Map<String, String> headerWebView = NPayLibrary.getInstance().getHeader();
    boolean isProgressDeposit = false;
    private View btnClose;
    private Toolbar toolbar;
    private LinearProgressIndicator progressBar;
    private BroadcastReceiver changeUrlBR;
    private JsHandler jsHandler;
    private ValueCallback<Uri[]> fileUploadCallback;
    private FileObserver fileObserver;
    private LinearLayout linearLayout;
    private boolean isRedirected;

    private static String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type;
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_npay);

        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().build());

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {

            }
        };
        this.getOnBackPressedDispatcher().addCallback(this, callback);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, () -> {
            });
        }


        findView();
        // Set color progress bar webview loading
        closeButtonWebview();
        jsHandler = new JsHandler(this);
        String data = getIntent().getStringExtra("data");
        Log.d(TAG, "onCreate: data ==   " + data);

        IntentFilter filter = new IntentFilter();
        filter.addAction("webViewBroadcast");
        filter.addAction("nativeBroadcast");
        filter.addAction("closeLoading");
        filter.addAction("callEkyc");
        listentChangeUrlBroadcast();

        LocalBroadcastManager.getInstance(this).registerReceiver(changeUrlBR, filter);
        linearLayout.setVisibility(View.VISIBLE);

        settingWebview(webView);
        settingWebview(webView2);

        setUpweb1Client();
        setUpWeb2Client();
        setCookieRefreshToken();

//        screenshotDetecter();
        downloadWebview(webView);
        downloadWebview(webView2);

        try {
            Uri.Builder builder = new Uri.Builder();
            JSONObject jsonObject = new JSONObject(data);
            String route = jsonObject.getString("route");
            String orderId = "";
            if (jsonObject.has("order_id")) {
                orderId = jsonObject.getString("order_id");
            }

            // Chống load lặp màn cũ.
            webView.loadUrl("javascript:document.open();document.close();");

            // Các route thuộc danh mục hóa đơn.
            if (Actions.listAllServices().contains(route)) {
                webView2.setVisibility(View.GONE);
                webView.setVisibility(View.VISIBLE);
                webView.loadUrl(Utils.getUrlActionShop(route), headerWebView);
                System.out.println("Webview 1 load url " + Utils.getUrlActionShop(route));
                showOrHideToolbar();
            } else if (isValidEndpoint(route)) {
                webView2.setVisibility(View.GONE);
                webView.setVisibility(View.VISIBLE);
                webView.loadUrl(Flavor.baseUrl + "/v1/" + route, headerWebView);
                showOrHideToolbar();
            } else {
                if (Actions.listActionSdk().contains(route)) {
                    webView2.setVisibility(View.GONE);
                    webView.setVisibility(View.VISIBLE);
                    webView.loadUrl(Utils.getUrlActionSdk(route), headerWebView);
                    System.out.println("Webview 1 load url " + Utils.getUrlActionSdk(route));
                    return;
                }
                if (route.startsWith("http")) {
                    webView.loadUrl(route, headerWebView);
                    System.out.println("Webview 1 load url " + route);
                    return;
                }
                builder.scheme("https").encodedAuthority(Flavor.baseUrl.replaceAll("https://", "")).appendPath("v1").appendQueryParameter("route", route).appendQueryParameter("Merchant-Code", jsonObject.getString("Merchant-Code")).appendQueryParameter("Merchant-Uid", jsonObject.getString("Merchant-Uid")).appendQueryParameter("brand_color", NPayLibrary.getInstance().sdkConfig.getBrandColor()).appendQueryParameter("platform", "android").appendQueryParameter("device-name", DeviceUtils.getDeviceName());
                if (jsonObject.has("order_id")) {
                    builder.appendQueryParameter("order_id", Utils.convertUrlToOrderId(orderId));
                }
                Log.d(TAG, "onCreate: Flavor.baseUrl ==   " + builder);
                clearWebview2NonToolbar();
                webView.loadUrl(builder.toString(), headerWebView);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public static boolean isValidEndpoint(String url) {
        String expectedEndpoint = "payment";
        Set<String> requiredParams = Set.of("order_id", "b_type", "b_info");

        Uri uri = Uri.parse(url);

        if (!expectedEndpoint.equals(uri.getPath())) {
            return false;
        }

        return uri.getQueryParameterNames().containsAll(requiredParams);
    }

    private void screenshotDetecter() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        if (Build.VERSION.SDK_INT >= 29) {
            final List<File> files = new ArrayList<>();
            final List<String> paths = new ArrayList<>();
            for (Path path : Path.values()) {
                files.add(new File(path.getPath()));
                paths.add(path.getPath());
            }
            fileObserver = new FileObserver(files) {
                @Override
                public void onEvent(int event, final String filename) {
                    if (event == FileObserver.CREATE) {
                        for (String fullPath : paths) {
                            File file = new File(fullPath + filename);
                            if (file.exists()) {
                                String mime = getMimeType(file.getPath());
                                if (mime != null && mime.contains("image")) {
                                    sendStatusScreenshot();
                                }
                            }
                        }
                    }
                }
            };

        } else {
            for (final Path path : Path.values()) {
                fileObserver = new FileObserver(path.getPath()) {
                    @Override
                    public void onEvent(int event, final String filename) {
                        File file = new File(path.getPath() + filename);
                        if (event == FileObserver.CREATE) {
                            if (file.exists()) {
                                String mime = getMimeType(file.getPath());
                                if (mime != null && mime.contains("image")) {
                                    sendStatusScreenshot();
                                }
                            }
                        }
                    }
                };
            }
        }
        fileObserver.startWatching();
    }

    private void sendStatusScreenshot() {
        String jsExecute = "javascript: window.sendStatusTakeScreenshot()";
        Handler mainHandler = new Handler(Looper.getMainLooper());
        Runnable myRunnable = () -> {
            webView.loadUrl(jsExecute);
        };
        mainHandler.post(myRunnable);
    }

    private void setCookieRefreshToken() {
        String accessToken = Preference.getString(this, Flavor.prefKey + Constants.ACCESS_TOKEN, "");
        String refreshToken = Preference.getString(this, Flavor.prefKey + Constants.REFRESH_TOKEN, "");
        String publicKey = Preference.getString(this, Flavor.prefKey + Constants.PUBLIC_KEY, "");
        publicKey = (URLEncoder.encode(publicKey)).replace("++++", "");

        CookieManager cookieManager = CookieManager.getInstance();

        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);
        cookieManager.setAcceptThirdPartyCookies(webView2, true);

        if (!accessToken.isEmpty() && !refreshToken.isEmpty() && !publicKey.isEmpty()) {
            String cookieAcccessToken = "actk=" + accessToken + "; path=/v1";
            cookieManager.setCookie(Flavor.baseUrl.replaceAll("https://", ""), cookieAcccessToken);

            String cookieRefreshToken = "rtk=" + refreshToken + "; path=/v1";
            cookieManager.setCookie(Flavor.baseUrl.replaceAll("https://", ""), cookieRefreshToken);

            String publicKeyString = "pk=" + publicKey + "; path=/v1";
            cookieManager.setCookie(Flavor.baseUrl.replaceAll("https://", ""), publicKeyString);
        }
    }

    private void setUpweb1Client() {
        webView.setWebViewClient(new WebViewClient() {

//            @Override
//            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
//                handler.proceed();
//            }


            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request,
                                            WebResourceResponse errorResponse) {
                if (Arrays.asList(500).contains(errorResponse.getStatusCode())) {
                    Toast.makeText(view.getContext(), "Đã có lỗi " + errorResponse.getStatusCode() + " xảy ra!",
                            Toast.LENGTH_LONG).show();
                }
                super.onReceivedHttpError(view, request, errorResponse);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (!isRedirected) {
                    linearLayout.setVisibility(View.GONE);
                }
                webView.setVisibility(View.VISIBLE);
                CookieManager.getInstance().flush();
                super.onPageFinished(view, url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {

                String url = request.getUrl().toString();
                isRedirected = true;

                System.out.println("shouldOverrideUrlLoading 1: " + url);

                if (!url.contains(Flavor.baseUrl) && !url.contains(Flavor.baseShop)) {
                    webView.setVisibility(View.GONE);
                    clearWebview2NonToolbar();
                    webView2.setVisibility(View.VISIBLE);
                    webView2.loadUrl(url, headerWebView);
                    return false;
                }
                if (url.contains("/merchant/payment/") || url.contains("/thanh-toan-qr/")) {
                    try {
                        Uri.Builder builder = new Uri.Builder();

                        builder.scheme("https").encodedAuthority(Flavor.baseUrl.replaceAll("https://", "")).appendPath("v1").appendPath("payment").appendQueryParameter("route", Constants.VERIFY_PAYMENT_ROUTE).appendQueryParameter("Merchant-Code", NPayLibrary.getInstance().sdkConfig.getMerchantCode()).appendQueryParameter("Merchant-Uid", NPayLibrary.getInstance().sdkConfig.getUid()).appendQueryParameter("brand_color", NPayLibrary.getInstance().sdkConfig.getBrandColor()).appendQueryParameter("platform", "android").appendQueryParameter("order_id", Utils.convertUrlToOrderId(url)).appendQueryParameter("device", DeviceUtils.getDeviceName());
                        clearWebview2NonToolbar();
                        webView2.setVisibility(View.GONE);
                        webView.setVisibility(View.VISIBLE);
                        webView.loadUrl(builder.toString(), headerWebView);

                    } catch (Exception ignored) {
                        System.out.println("Error webiew " + ignored);
                    }
                } else {
                    view.loadUrl(request.getUrl().toString(), headerWebView);
                }
                return false;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                showOrHideToolbar();
                if(!isRedirected) {
                    linearLayout.setVisibility(View.VISIBLE);
                }

                isRedirected = false;
                super.onPageStarted(view, url, favicon);
            }

        });
    }

    private void downloadWebview(WebView webView) {
        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            if (url.startsWith("data:")) {  //when url is base64 encoded data
                String path = createAndSaveFileFromBase64Url(url);
                Toast.makeText(this, "Download image saved at " + path, Toast.LENGTH_LONG).show();
                return;
            }
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setMimeType(mimetype);
            request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimetype));
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url,
                    contentDisposition, mimetype));
            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            dm.enqueue(request);
            Toast.makeText(this, "Đang tải xuống...", Toast.LENGTH_LONG).show();
        });
    }

    private String createAndSaveFileFromBase64Url(String url) {
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        String filetype = url.substring(url.indexOf("/") + 1, url.indexOf(";"));
        String filename = System.currentTimeMillis() + "." + filetype;
        File file = new File(path, filename);
        try {
            if (!path.exists()) path.mkdirs();
            if (!file.exists()) file.createNewFile();

            String base64EncodedString = url.substring(url.indexOf(",") + 1);
            byte[] decodedBytes = Base64.decode(base64EncodedString, Base64.DEFAULT);
            OutputStream os = new FileOutputStream(file);
            os.write(decodedBytes);
            os.close();

            //Tell the media scanner about the new file so that it is immediately available to the user.
            MediaScannerConnection.scanFile(this, new String[]{file.toString()}, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                public void onScanCompleted(String path, Uri uri) {
                    Log.i("ExternalStorage", "Scanned " + path + ":");
                    Log.i("ExternalStorage", "-> uri=" + uri);
                }
            });

            //Set notification after download complete and add "click to view" action to that
            String mimetype = url.substring(url.indexOf(":") + 1, url.indexOf("/"));
            Intent intent = new Intent();
            intent.setAction(android.content.Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(file), (mimetype + "/*"));
            int noti_id = 12345;
            Intent cancel = new Intent(this, NotificationCancelReceiver.class);
            cancel.putExtra("noti_id", noti_id);
            cancel.putExtra("path", file.toString());
            PendingIntent cancelPending = PendingIntent.getBroadcast(this, 0, cancel,
                    PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            @SuppressLint("NotificationTrampoline") NotificationCompat.Builder builderNoti =
                    new NotificationCompat.Builder(this, "CHANNEL_ID").setSmallIcon(android.R.drawable.stat_sys_download_done).setContentTitle("Download image").setContentText("Download success").setPriority(NotificationCompat.PRIORITY_DEFAULT).setContentIntent(cancelPending).setAutoCancel(true);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                int importance = NotificationManager.IMPORTANCE_DEFAULT;
                NotificationChannel channel = new NotificationChannel("CHANNEL_ID", "Notification", importance);
                channel.setDescription("Notification download images");
                // Register the channel with the system; you can't change the importance
                // or other notification behaviors after this
                NotificationManager notificationManager = getSystemService(NotificationManager.class);
                notificationManager.createNotificationChannel(channel);
            }

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                notificationManager.notify(noti_id, builderNoti.build());
            }


        } catch (IOException e) {
            Log.w("ExternalStorage", "Error writing " + file, e);
        }

        return file.toString();
    }

    private void setUpWeb2Client() {
        webView2.setWebViewClient(new WebViewClient() {

//            @Override
//            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
//                handler.proceed();
//            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request,
                                            WebResourceResponse errorResponse) {
                if (Arrays.asList(500).contains(errorResponse.getStatusCode())) {
                    Toast.makeText(view.getContext(), "Đã có lỗi " + errorResponse.getStatusCode() + " xảy ra!",
                            Toast.LENGTH_LONG).show();
                }
                super.onReceivedHttpError(view, request, errorResponse);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                Log.d(TAG, "shouldOverrideUrlLoading 2: url ==   " + url);

                if (url.endsWith("close-webview")) {
                    clearWebview2WithToolbar();
                    return false;
                }

                if (url.startsWith(Flavor.baseUrl)) {
                    clearWebview2WithToolbar();
                    return false;
                }
                webView2.loadUrl(url, headerWebView);
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                CookieManager.getInstance().flush();
                super.onPageFinished(view, url);
            }
        });
    }

    private void listentChangeUrlBroadcast() {
        changeUrlBR = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d("onReceive", "onReceive:  ==   " + intent.getAction());
                if (intent.getAction().equals("webViewBroadcast")) {
                    if (webView2 != null) {
                        clearWebview2NonToolbar();
                    }
                    String getURL = intent.getStringExtra("url");
                    String nameAction = intent.getStringExtra("name");
                    if (nameAction != null) {
                        isProgressDeposit = nameAction.equals("napas-deposit");
                    }

                    /// Remove validate by URL
                    webView.setVisibility(View.GONE);
                    webView2.setVisibility(View.VISIBLE);
                    webView2.loadUrl(getURL, headerWebView);

                    showOrHideToolbar();

                }
                if (intent.getAction().equals("nativeBroadcast")) {
                    if (intent.getStringExtra("action").equals("close")) {
                        finish();
                    }
                }
                if (intent.getAction().equals("closeLoading")) {
                    linearLayout.setVisibility(View.GONE);
                }

                if (intent.getAction().equals("callEkyc")) {
//                    String jsonString = intent.getStringExtra("JSON_OBJECT");
//                    HyperKycParams hvParam = KycUtil.createHyperKycParams(jsonString);
//                    if (hvParam == null) {
//                        System.out.println("Param null");
//                        return;
//                    }
//                    KycUtil.startWorkflow( hyperKycLauncher,hvParam);
                }
            }
        };
    }

//    private final ActivityResultLauncher<HyperKycConfig> hyperKycLauncher =
//            registerForActivityResult(new HyperKyc.Contract(), result -> {
//                Gson gson = new Gson();
//                String jsonStr = gson.toJson(result);
//                String jsExecute = "javascript: window.pasteDataNFC('" + jsonStr + "')";
//                Handler mainHandler = new Handler(Looper.getMainLooper());
//                Runnable myRunnable = () -> {
//                    if (NPayActivity.webView == null) return;
//                    NPayActivity.webView.loadUrl(jsExecute);
//                };
//                mainHandler.post(myRunnable);
//            });

    private void findView() {
        webView = findViewById(R.id.webView);
        webView2 = findViewById(R.id.webView2);
        toolbar = findViewById(R.id.toolbar);
        btnClose = findViewById(R.id.btnClose);
        progressBar = findViewById(R.id.progressBar);
        linearLayout = findViewById(R.id.progressLayout);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void settingWebview(WebView webView) {
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        webView.addJavascriptInterface(jsHandler, "JsHandler");
        WebSettings webSettings = webView.getSettings();
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setSupportMultipleWindows(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setPluginState(WebSettings.PluginState.ON);

        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback,
                                             FileChooserParams fileChooserParams) {
                fileUploadCallback = filePathCallback;
                openFileChooser();
                return true;
            }

            @Override
            public void onPermissionRequest(PermissionRequest request) {
                request.grant(request.getResources());
            }

            @Override
            public void onProgressChanged(WebView view, int newProgress) {

                    progressBar.setProgress(newProgress);

                super.onProgressChanged(view, newProgress);
            }
        });
    }

    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Chọn ảnh"), FILE_CHOOSER_REQUEST_CODE);
    }

    public void clearWebview2WithToolbar() {
        if (webView2.getVisibility() == View.VISIBLE) {
            clearWebview2NonToolbar();
            webView2.setVisibility(View.GONE);
            webView.setVisibility(View.VISIBLE);
        }
        showOrHideToolbar();
    }

    public void clearWebview2NonToolbar() {
        webView2.clearHistory();
        webView2.clearCache(true);
        webView2.clearFormData();
        webView2.loadUrl("javascript:document.open();document.close();");
    }

    void closeButtonWebview() {
        btnClose.setOnClickListener(view -> {
            if (isProgressDeposit) {
                webView.loadUrl("javascript: window.sendEventDismissScreen()");
                isProgressDeposit = false;
            }
            clearWebview2WithToolbar();
        });
    }

    void showOrHideToolbar() {
        if (webView2.getVisibility() == View.VISIBLE) {
            toolbar.setVisibility(View.VISIBLE);
            return;
        }
        if (toolbar.getVisibility() == View.VISIBLE) {
            toolbar.setVisibility(View.GONE);
            closeCamera();
        }

        if (webView.getUrl() == null) {
            finish();
        }
    }

    void closeCamera() {
        try {
            Camera.open().release();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == JsHandler.PERMISSION_CAMERA_REQUEST_CODE) {
            JsHandler.sendStatusCamera(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED);
        } else if (requestCode == JsHandler.PERMISSION_STORAGE_REQUEST_CODE) {
            JsHandler.sendStatusStorage(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED);
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    @Nullable @org.jetbrains.annotations.Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQUEST_CODE) {
            if (fileUploadCallback == null) return;
            Uri[] results = null;

            if (resultCode == Activity.RESULT_OK && data != null) {
                String dataString = data.getDataString();
                if (dataString != null) {
                    results = new Uri[]{Uri.parse(dataString)};
                }
            }

            fileUploadCallback.onReceiveValue(results);
            fileUploadCallback = null;
        }
    }

    @Override
    protected void onDestroy() {
        if (changeUrlBR != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(changeUrlBR);
            changeUrlBR = null;
        }
        clearWebview2NonToolbar();
        closeCamera();
        if (fileObserver != null) fileObserver.stopWatching();
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private enum Path {
        DCIM(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + File.separator +
                "Screenshots" + File.separator),
        PICTURES(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + File.separator +
                "Screenshots" + File.separator);

        final private String path;

        Path(String path) {
            this.path = path;
        }

        public String getPath() {
            return path;
        }
    }
}