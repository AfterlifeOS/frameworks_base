/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.systemui.afterlife;

import android.content.*;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.net.*;
import android.net.wifi.*;
import android.os.*;
import android.provider.Settings;
import android.telephony.*;
import android.text.*;
import android.text.format.Formatter;
import android.text.format.Formatter.BytesResult;
import android.util.*;
import android.view.*;
import android.widget.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.android.systemui.R;
import com.declan.prjct.utils.DeclanUtils;
import com.android.systemui.afterlife.ImageUtils;
import java.util.List;

public class UsernameLayout extends LinearLayout {
	
	public static final String TAG = "DeclanUsernameLayout";
	
    private TextView mUserName;
	private String mUserText;
    private boolean mUserEnabled;

    public UsernameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
		mUserName = findViewById(R.id.username);
		mUserName.setSelected(true);
		new SettingsObserver(new Handler()).observe();
		updateProfileView();
	}
	
	private void updateProfileView() {
		ContentResolver resolver = mContext.getContentResolver();
		mUserEnabled = Settings.System.getIntForUser(resolver, "qs_footer_text_show", 0, UserHandle.USER_CURRENT) == 1;
		mUserText = Settings.System.getStringForUser(resolver, "qs_footer_text_string", UserHandle.USER_CURRENT);
		
		if (mUserEnabled) {
			mUserName.setText(mUserText);
		} else {
			mUserName.setText("#KeepAfterLife");
		}
	}

    public final class SettingsObserver extends ContentObserver {
		public SettingsObserver(Handler handler) {
			super(handler);
		}
		
		public final void observe() {
			ContentResolver contentResolver = mContext.getContentResolver();
			contentResolver.registerContentObserver(Settings.System.getUriFor("qs_footer_text_show"), false, this);
			contentResolver.registerContentObserver(Settings.System.getUriFor("qs_footer_text_string"), false, this);
		}
		
		@Override
		public void onChange(boolean selfChange) {
			updateProfileView();
		}
    }
}
