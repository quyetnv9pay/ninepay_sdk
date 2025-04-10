package com.npsdk.module.utils;
import android.content.Context;
import android.nfc.NfcAdapter;

public class NfcUtil {
    public static int checkNfcStatus(Context context) {
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(context);
        if (nfcAdapter == null) {
            return Constants.NFC_NOT_FOUND;
        } else if (!nfcAdapter.isEnabled()) {
            return Constants.NFC_NOT_ENABLE;
        } else {
            return Constants.NFC_ENABLE;
        }
    }
}
