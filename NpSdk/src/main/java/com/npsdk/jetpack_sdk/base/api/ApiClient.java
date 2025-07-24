package com.npsdk.jetpack_sdk.base.api;

import androidx.annotation.Nullable;
import com.npsdk.module.NPayLibrary;
import com.npsdk.module.utils.Constants;
import com.npsdk.module.utils.Flavor;
import com.npsdk.module.utils.Preference;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.concurrent.TimeUnit;

public class ApiClient {

    private static Retrofit retrofit;

    public static Retrofit getClient() {
        OkHttpClient httpClient = getCommonClient();
        retrofit =
                new Retrofit.Builder().baseUrl(Flavor.baseApi).addConverterFactory(ScalarsConverterFactory.create()).addConverterFactory(GsonConverterFactory.create()).client(httpClient).build();
        return retrofit;
    }

    @Nullable
    private static String getToken() {
        try {
            String token = Preference.getString(NPayLibrary.getInstance().activity,
                    NPayLibrary.getInstance().sdkConfig.getEnv() + Constants.ACCESS_TOKEN, null);
            if (token != null) token = "Bearer " + token;
            return token;
        } catch (Exception e) {
            return null;
        }
    }

    private static OkHttpClient getCommonClient() {
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient httpClient = null;
        try {
            httpClient = new OkHttpClient.Builder().addInterceptor(chain -> {
                Request.Builder builder = chain.request().newBuilder()
                        .addHeader("Merchant-Code", NPayLibrary.getInstance().sdkConfig.getMerchantCode())
                        .addHeader("App-Type", "SDK")
                        .addHeader("platform", "android")
                        .addHeader("is-new-sdk", "true")
                        .addHeader("Secret-Key", NPayLibrary.getInstance().sdkConfig.getSecretKey());
                String Rke = EncryptServiceHelper.INSTANCE.getRandomkeyEncrypt();
                if (getToken() != null && Rke != null) {
                    builder.addHeader("Authorization", getToken()).addHeader("Rke", Rke);
                } else if (Rke != null) {
                    builder.addHeader("Rke", Rke);
                }

                Request request = builder.build();
                return chain.proceed(request);
            }).connectTimeout(30, TimeUnit.SECONDS).writeTimeout(30, TimeUnit.SECONDS).readTimeout(30,
                    TimeUnit.SECONDS).addInterceptor(logging).addInterceptor(new CustomInterceptor()).build();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return httpClient;
    }
}