package com.npsdk.module.utils;

import android.content.Context;
import android.util.Log;

import com.npsdk.module.EnvironmentKey;

public class Flavor {
    public static String baseUrl;
    public static String prefKey;
    public static String baseApi;
    public static String baseShop;

    public static void configFlavor(String env) {
        switch (env) {
            case EnvironmentKey.STAGING:
                baseUrl = Constants.STAGING_URL;
                prefKey = EnvironmentKey.STAGING;
                baseApi = Constants.STAGING_API;
                baseShop = Constants.STAGING_SHOP;
                break;
            case EnvironmentKey.SANDBOX:
                baseUrl = Constants.SANDBOX_URL;
                prefKey = EnvironmentKey.SANDBOX;
                baseApi = Constants.SANDBOX_API;
                baseShop = Constants.SANDBOX_SHOP;
                break;
            case EnvironmentKey.PRODUCTION:
                baseUrl = Constants.PROD_URL;
                prefKey = EnvironmentKey.PRODUCTION;
                baseApi = Constants.PROD_API;
                baseShop = Constants.PROD_SHOP;
                break;
        }
    }

    public static String setEnvTest(Context context) {
//        String packageName = context.getPackageName();
////        if (packageName.contains("stg")) {
//            return EnvironmentKey.STAGING;
////        } else if (packageName.contains("sand")) {
////           return EnvironmentKey.SANDBOX;
////        } else {
          return EnvironmentKey.STAGING;
////        }
    }
}
