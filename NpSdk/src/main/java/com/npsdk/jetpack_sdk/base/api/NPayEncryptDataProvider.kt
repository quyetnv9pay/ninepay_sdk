package com.npsdk.jetpack_sdk.base.api

import com.npsdk.module.NPayLibrary
import com.npsdk.module.utils.Constants
import com.npsdk.module.utils.DeviceUtils
import com.npsdk.module.utils.Preference
import vn.ninepay.sdk.encryptservice.EncryptDataProvider

class NPayEncryptDataProvider : EncryptDataProvider {

    // Triển khai hàm lấy ID
    override fun getUniqueID(): String {
        return DeviceUtils.getUniqueID(NPayLibrary.getInstance().activity)
    }

    // Triển khai hàm lấy Public Key
    override fun getPublicKeySaved(): String? {
        return Preference.getString(
            NPayLibrary.getInstance().activity,
            NPayLibrary.getInstance().sdkConfig.env + Constants.PUBLIC_KEY
        )
    }
}