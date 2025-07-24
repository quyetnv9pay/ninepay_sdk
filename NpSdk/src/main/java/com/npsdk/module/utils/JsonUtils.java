package com.npsdk.module.utils;

import com.google.gson.JsonObject;

public class JsonUtils {
    public static JsonObject wrapWithDefault(String message, int errorCode) {
        JsonObject obj = new JsonObject();
        obj.addProperty("message", message);
        obj.addProperty("error_code", errorCode);
        obj.add("data", getDefaultPaymentMethod());
        return obj;
    }

    private static JsonObject getDefaultPaymentMethod() {
        JsonObject walletInfo = new JsonObject();
        walletInfo.addProperty("name", "Ví điện tử 9Pay");
        walletInfo.addProperty("short_name", "9Pay");
        walletInfo.addProperty("logo", "https://storage.googleapis.com/npay/images/vi-dien-tu-9pay-1599266043.png");
        walletInfo.addProperty("type", 1);
        walletInfo.addProperty("secure_payload", "");

        JsonObject data = new JsonObject();
        data.add("wallet_info", walletInfo);

        return data;
    }
}
