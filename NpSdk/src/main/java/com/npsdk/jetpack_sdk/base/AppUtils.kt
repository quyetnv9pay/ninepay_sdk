package com.npsdk.jetpack_sdk.base

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.npsdk.module.NPayLibrary
import com.npsdk.module.utils.Constants
import com.npsdk.module.utils.Preference
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


object AppUtils {

    fun getMonthCurrent(): Int {
        val calendar = Calendar.getInstance()
        return calendar.get(Calendar.MONTH) + 1
    }

    fun getYearCurrent(): Int {
        val calendar = Calendar.getInstance()
        return calendar.get(Calendar.YEAR)
    }

    fun getMonthYearCurrent(): Pair<Int, Int> {
        val calendar = Calendar.getInstance()
        val month = calendar.get(Calendar.MONTH) + 1
        val year = calendar.get(Calendar.YEAR)
        return month to year
    }

    fun formatMoney(amount: Any): String {
        val decimalFormatSymbols = DecimalFormatSymbols(Locale("vi", "VN"))
        decimalFormatSymbols.groupingSeparator = '.'
        decimalFormatSymbols.decimalSeparator = '.'

        val decimalFormat = DecimalFormat("#,###", decimalFormatSymbols)
        return decimalFormat.format(amount) + "đ"
    }

    fun timeIntToDateString(time: Int): String {
        try {
            val simpleDateFormat = SimpleDateFormat(" HH:mm:ss, dd MMMM yyyy", Locale.ENGLISH)
            return simpleDateFormat.format(time * 1000L)

        } catch (e: Exception) {
            return ""
        }
    }

    fun isLogged(): Boolean {
        val publicKey = Preference.getString(
            NPayLibrary.getInstance().activity, NPayLibrary.getInstance().sdkConfig.env + Constants.PUBLIC_KEY, ""
        )

        val accessToken = Preference.getString(
            NPayLibrary.getInstance().activity, NPayLibrary.getInstance().sdkConfig.env + Constants.ACCESS_TOKEN, ""
        )

        return publicKey.isNotBlank() && accessToken.isNotBlank()
    }

    private fun getVersionCode(context: Context, packageName: String): Int {
        return try {
            val packageManager = context.packageManager
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            packageInfo.versionCode
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            -1
        }
    }

    fun openBrowser(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        // Set the package name for Chrome explicitly to ensure it opens in Chrome.
        intent.`package` = "com.android.chrome"
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // If Chrome is not installed or the intent fails, you can handle the exception here.
            e.printStackTrace()
        }
    }

    fun getCurrentTimeFormatted(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }
}