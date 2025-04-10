package com.npsdk.module.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import com.npsdk.module.NPayLibrary;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * SharedPreferences Utilities
 * 
 */
public final class Preference {

	/**
	 * The name of SharedPreferences
	 */
	private static final String PREFERENCE_NAME = "sp_library";

	/**
	 * Get the instance of SharedPreferences
	 * 
	 * @param context
	 * @return
	 */
	public static SharedPreferences getSharedPreferences(Context context) {
		if (context != null) {
            try {
                String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
                return EncryptedSharedPreferences.create(
                        PREFERENCE_NAME,
                        masterKeyAlias,
                        context,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                );
            } catch (GeneralSecurityException | IOException e) {
                e.printStackTrace();
                return null;
            }
        } else {
			return null;
		}
	}

	/**
	 * Save Single String preference
	 *
	 * @param context
	 * @param key
	 * @param value
	 * @return
	 */
	public static boolean save(Context context, String key, String value) {
		try {
			SharedPreferences sharedPreferences = getSharedPreferences(context);
			Editor editor = sharedPreferences.edit();

			String trueKey = _generateKey(key, context);
			if (trueKey.isEmpty()){
				return false;
			}

			editor.putString(trueKey, value);
			return editor.commit();
		}catch (Exception e){
			return false;
		}
	}

	/**
	 * Get string value through key
	 * 
	 * @param context
	 * @param key
	 * @return
	 */
	public static String getString(Context context, String key) {
		try {
			SharedPreferences sharedPreferences = getSharedPreferences(context);

			String trueKey = _generateKey(key, context);
			if(trueKey.isEmpty()){
				return "";
			}

			return sharedPreferences.getString(trueKey, "");
		}catch (Exception e){
			e.printStackTrace();
			return "";
		}
	}

	/**
	 * Save map String preference
	 * 
	 * @param context
	 * @param valuesMap
	 * @return
	 */
	public static boolean save(Context context, HashMap<String, String> valuesMap) {
		try {
			SharedPreferences sharedPreferences = getSharedPreferences(context);
			Editor editor = sharedPreferences.edit();
			String value = "";
			for (String key : valuesMap.keySet()) {
				value = valuesMap.get(key);
				editor.putString(key, value);
			}
			return editor.commit();
		}catch (Exception e){
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Save single boolean preference
	 * 
	 * @param context
	 * @param key
	 * @param value
	 * @return
	 */
	public static boolean save(Context context, String key, boolean value) {
		try {
			SharedPreferences sharedPreferences = getSharedPreferences(context);
			Editor editor = sharedPreferences.edit();
			editor.putBoolean(key, value);
			return editor.commit();
		}catch (Exception e){
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Get boolean value through key
	 * 
	 * @param context
	 * @param key
	 * @return
	 */
	public static boolean getBoolean(Context context, String key, boolean defaultValue) {
		try {
			SharedPreferences sharedPreferences = getSharedPreferences(context);
			return sharedPreferences.getBoolean(key, defaultValue);
		}catch (Exception e){
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Clean the SharedPreferences
	 * 
	 * @param context
	 */
	public static void clear(Context context) {
		try {
			SharedPreferences sharedPreferences = getSharedPreferences(context);
			Editor editor = sharedPreferences.edit();
			editor.clear();
			editor.apply();
		}catch (Exception e){
			e.printStackTrace();
		}
	}

	/**
	 * remove the value by key
	 * 
	 * @param context
	 * @param key
	 */
	public static void remove(Context context, String key) {
		try {
			SharedPreferences sharedPreferences = getSharedPreferences(context);
			Editor editor = sharedPreferences.edit();
			editor.remove(key);
			editor.apply();
		}catch (Exception e){
			e.printStackTrace();
		}
	}

	/**
	 * remove the value by key with encrypted
	 *
	 * @param context
	 * @param key
	 */
	public static void removeEncrypted(Context context, String key) {
		try {
			SharedPreferences sharedPreferences = getSharedPreferences(context);
			String trueKey = _generateKey(key, context);
			if (trueKey.isEmpty()){
				return;
			}
			Editor editor = sharedPreferences.edit();
			editor.remove(trueKey);
			editor.apply();
		}catch (Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * Save int value
	 * 
	 * @param context
	 * @param key
	 * @param value
	 * @return
	 */
	public static boolean save(Context context, String key, int value) {
		try {
			SharedPreferences sharedPreferences = getSharedPreferences(context);
			Editor editor = sharedPreferences.edit();
			editor.putInt(key, value);
			return editor.commit();
		}catch (Exception e){
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Save long value
	 * 
	 * @param context
	 * @param key
	 * @param value
	 * @return
	 */
	public static boolean save(Context context, String key, long value) {
		try {
			SharedPreferences sharedPreferences = getSharedPreferences(context);
			Editor editor = sharedPreferences.edit();
			editor.putLong(key, value);
			return editor.commit();
		}catch (Exception e){
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Get int value
	 * 
	 * @param context
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public static int getInt(Context context, String key, int defaultValue) {
		try {
			SharedPreferences sharedPreferences = getSharedPreferences(context);
			return sharedPreferences.getInt(key, defaultValue);
		}catch (Exception e){
			e.printStackTrace();
			return 0;
		}
	}
	

	/**
	 * Get long value
	 * 
	 * @param context
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public static long getLong(Context context, String key, long defaultValue) {
		try {
			SharedPreferences sharedPreferences = getSharedPreferences(context);
			return sharedPreferences.getLong(key, defaultValue);
		}catch (Exception e){
			e.printStackTrace();
			return 0;
		}
	}

	/**
	 * Save float value
	 * 
	 * @param context
	 * @param key
	 * @param value
	 * @return
	 */
	public static boolean save(Context context, String key, float value) {
		try {
			SharedPreferences sharedPreferences = getSharedPreferences(context);
			Editor editor = sharedPreferences.edit();
			editor.putFloat(key, value);
			return editor.commit();
		}catch (Exception e){
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Get float value
	 * 
	 * @param context
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public static float getFloat(Context context, String key, float defaultValue) {
		try {
			SharedPreferences sharedPreferences = getSharedPreferences(context);
			return sharedPreferences.getFloat(key, defaultValue);
		}catch (Exception e){
			e.printStackTrace();
			return 0;
		}
	}
	
	public static boolean save(Context context, String key, Set<String> value) {
		try {
			SharedPreferences sharedPreferences = getSharedPreferences(context);
			Editor editor = sharedPreferences.edit();
			editor.putStringSet(key, value);
			return editor.commit();
		}catch (Exception e){
			e.printStackTrace();
			return false;
		}
	}
	public static String getString(Context context, String key , String strDefault) {
		try {
			SharedPreferences sharedPreferences = getSharedPreferences(context);
			String trueKey = _generateKey(key, context);

			if (trueKey.isEmpty()) {
				return strDefault;
			}

			return sharedPreferences.getString(trueKey, strDefault);
		}catch (Exception e){
			e.printStackTrace();
			return strDefault;
		}
	}

	public static Set<String> getStringSet(Context context, String key, Set<String> defaultValue) {
		try {
			SharedPreferences sharedPreferences = getSharedPreferences(context);
			return sharedPreferences.getStringSet(key, defaultValue);
		}catch (Exception e){
			e.printStackTrace();
			return new HashSet<String>();
		}
	}

	// Generate key with phone number and merchant code
	public static String _generateKey(String key, Context context) {
		if (key.contains(Constants.PHONE) ||
						key.contains(Constants.MERCHANT_CODE) ||
						key.contains(Constants.INIT_ENVIRONMENT)) {
			return key;
		}

		String phoneNumber = _getSavedDefault(context, Flavor.prefKey + Constants.PHONE);
		if (phoneNumber == null || phoneNumber.isEmpty()) {
			return "";
		}

		String merchantCode = _getSavedDefault(context, Flavor.prefKey + Constants.MERCHANT_CODE);
		if (merchantCode == null || merchantCode.isEmpty()) {
			return "";
		}

		return phoneNumber + "_" + merchantCode + "_" + key;
	}

	public static String _getSavedDefault(Context context, String key) {
		try {
			SharedPreferences sharedPreferences = getSharedPreferences(context);
			return sharedPreferences.getString(key, "");
		}catch (Exception e){
			e.printStackTrace();
			return "";
		}
	}
}
