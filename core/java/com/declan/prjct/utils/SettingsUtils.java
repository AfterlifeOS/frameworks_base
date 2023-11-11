package com.declan.prjct.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

public class SettingsUtils {
	
	public static boolean isPackageEnabled(Context context, String packageName) {
		try {
			PackageInfo pi = context.getPackageManager().getPackageInfo(packageName, 0);
			return pi.applicationInfo.enabled;
		} catch (PackageManager.NameNotFoundException notFound) {
			return false;
		}
	}
	
	public static boolean isPackageAktif(Context context, String packageName) {
		return isPackageEnabled(context, packageName);
	}
}