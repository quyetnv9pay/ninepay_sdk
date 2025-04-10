package com.npsdk.module.utils;

public class Constants {
    public static final String PHONE = "phone";
    public static final String ACCESS_TOKEN = "access_token";
    public static final String REFRESH_TOKEN = "refresh_token";
    public static final String PUBLIC_KEY = "public_key";
    public static final String LAST_TIME_PUBLIC_KEY = "last_time_public_key";
    public static final String MERCHANT_CODE = "merchant_code";
    public static final String INIT_ENVIRONMENT = "init_environment";

    public static final String PROD_URL = "https://sdk.9pay.vn";
    public static final String SANDBOX_URL = "https://sand-sdk.9pay.vn";
    public static final String STAGING_URL = "https://stg-sdk.9pay.mobi";

    // API
    public static final String STAGING_API = "https://stg-api.9pay.mobi";
    public static final String SANDBOX_API = "https://sand-api.9pay.vn";
    public static final String PROD_API = "https://api.9pay.vn";

    // SHOP
    public static final String PROD_SHOP = "https://shop.9pay.vn";
    public static final String SANDBOX_SHOP = "https://sand-shop.9pay.vn";
    public static final String STAGING_SHOP = "https://stg-shop.9pay.mobi";

    // Route to payment in SDK Flutter.
    public static final String VERIFY_PAYMENT_ROUTE = "payment_merchant_verify";
    public static final String SUCCESS = "SUCCESS";
    public static final String FAIL = "FAIL";
    public static final int NOT_LOGIN = 403;
    public static final int NOT_VERIFY = 401;

    // NFC status
    public static final int NFC_NOT_FOUND = 0;
    public static final int NFC_NOT_ENABLE = 1;
    public static final int NFC_ENABLE = 2;
}