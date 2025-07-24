package com.npsdk.module.utils;

import android.util.Log;

public class Utils {
    public static String convertUrlToOrderId(String url) {
        String orderId = last(url.split("/")).trim();
        Log.d("TAG", "orderId: " + orderId);
        return orderId;
    }

    private static <T> T last(T[] array) {
        return array[array.length - 1];
    }

    public static String getUrlActionShop(String action) {
        String path = null;
        switch (action) {
            case Actions.SHOP:
            case Actions.BILLING:
                path = "/sdk/billing";
                break;
            case Actions.BILLING_DIEN:
                path = "/hoa-don/hoa-don-tien-dien";
                break;
            case Actions.BILLING_TRUYEN_HINH:
                path = "/hoa-don/hoa-don-truyen-hinh";
                break;
            case Actions.BILLING_DIEN_THOAI:
                path = "/hoa-don/hoa-don-dien-thoai-co-dinh";
                break;
            case Actions.BILLING_INTERNET:
                path = "/hoa-don/hoa-don-internet";
                break;
            case Actions.BILLING_NUOC:
                path = "/hoa-don/hoa-don-nuoc";
                break;
            case Actions.BILLING_BAO_HIEM:
                path = "/hoa-don/hoa-don-bao-hiem";
                break;
            case Actions.BILLING_TAI_CHINH:
                path = "/hoa-don/hoa-don-tai-chinh";
                break;
            case Actions.BILLING_TRA_SAU:
                path = "/hoa-don/hoa-don-tra-sau";
                break;
            case Actions.BILLING_TIN_DUNG:
                path = "/hoa-don/hoa-don-the-tin-dung";
                break;
            case Actions.BILLING_HOC_PHI:
                path = "/hoa-don/hoa-don-hoc-phi";
                break;
            case Actions.BILLING_TRA_GOP:
                path = "/hoa-don/hoa-don-tra-gop";
                break;
            case Actions.BILLING_VE_TAU_XE:
                path = "/hoa-don/hoa-don-ve-xe";
                break;
            case Actions.BILLING_VETC:
                path = "/hoa-don/hoa-don-duong-bo";
                break;
            case Actions.TOPUP:
                path = "/sdk/topup-phone";
                break;
            case Actions.PHONE_CARD:
                path = "/sdk/phone-card";
                break;
            case Actions.DATA_CARD:
                path = "/sdk/data-card";
                break;
            case Actions.GAME_CARD:
                path = "/sdk/game";
                break;
            case Actions.SERVICE_CARD:
                path = "/sdk/service-card";
                break;
        }

        return Flavor.baseShop + path;
    }

    public static String getUrlActionSdk(String action) {
        String path = null;
        switch (action) {
            case Actions.OPEN_WALLET:
                path = "/v1/home";
                break;
            case Actions.LOGIN:
                path = "/v1/dang-nhap";
                break;
            case Actions.HISTORY:
                path = "/v1/lich-su";
                break;
            case Actions.TRANSFER:
                path = "/v1/chuyen-tien";
                break;
            case Actions.DEPOSIT:
                path = "/v1/nap-tien";
                break;
            case Actions.QR:
                path = "/v1/scan-qr-code";
                break;
            case Actions.WITHDRAW:
                path = "/v1/rut-tien";
                break;
            case Actions.LINK_BANK:
                path = "/v1/napas-link/danh-sach";
                break;
            case Actions.EKYC:
                path = "/v1/kyc";
                break;
        }
        return Flavor.baseUrl + path;
    }
}
