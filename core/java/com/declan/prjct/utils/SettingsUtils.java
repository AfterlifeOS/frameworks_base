/*
 * Copyright (C) 2023-2024 AfterlifeOS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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