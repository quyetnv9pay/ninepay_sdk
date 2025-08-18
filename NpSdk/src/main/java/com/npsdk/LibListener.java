package com.npsdk;

import androidx.annotation.Nullable;

import com.npsdk.module.model.UserInfo;

public interface LibListener {
	public void getInfoSuccess(String jsonData);

	public void onError(int errorCode, String message);

	public void onLogoutSuccessful();

	public void onCloseSDK();
	public void sdkDidComplete(String name, Boolean status, @Nullable Object params);

	public void backToAppFrom(String screen);
}
