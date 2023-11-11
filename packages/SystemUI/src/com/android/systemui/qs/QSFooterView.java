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

package com.android.systemui.qs;

import static android.app.StatusBarManager.DISABLE2_QUICK_SETTINGS;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Outline;
import android.graphics.drawable.BitmapDrawable;
import android.net.*;
import android.net.wifi.*;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.*;
import android.text.BidiFormatter;
import android.text.format.Formatter;
import android.text.format.Formatter.BytesResult;
import android.util.AttributeSet;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.android.settingslib.net.DataUsageController;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.android.settingslib.development.DevelopmentSettingsEnabler;
import com.android.settingslib.net.DataUsageController;
import com.android.systemui.R;


import com.declan.prjct.utils.DeclanUtils;
import com.declan.prjct.utils.ImageUtils;

import java.util.List;

/**
 * Footer of expanded Quick Settings, tiles page indicator, (optionally) build number and
 * {@link FooterActionsView}
 */
public class QSFooterView extends FrameLayout {
	
	public static final String TAG = "QSFooterView";
	public static final String AVATAR_FILE_NAME = "custom_file_avatar_image";
	
    private PageIndicator mPageIndicator;
    private TextView mUserName, mUsageText;
    private View mAftShortcutButton, mSettingButton, mEditButton, mRunningServiceButton, mInterfaceButton, mUserButton, mDataUsageButton;
    
    private DataUsageController mDataController;
	private ConnectivityManager mConnectivityManager;
	private WifiManager mWifiManager;
	private SubscriptionManager mSubManager;
	
	private ImageView mUserAvatar;
	private BitmapDrawable mAvatarBitmap; 
	private String mAvatarUri, mUserText;

    @Nullable
    protected TouchAnimator mFooterAnimator;

    private boolean mQsDisabled, mExpanded, mCustomAvatarEnabled, mUserEnabled;
    private float mExpansionAmount;

    @Nullable
    private OnClickListener mExpandClickListener;

    private final ContentObserver mSettingsObserver = new ContentObserver(new Handler(mContext.getMainLooper())) {
		@Override
		public void onChange(boolean selfChange, Uri uri) {
			super.onChange(selfChange, uri);
			updateProfileView();
		}
    };

    public QSFooterView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mDataController = new DataUsageController(context);
		mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		mSubManager = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mPageIndicator = findViewById(R.id.footer_page_indicator);
		mEditButton = findViewById(android.R.id.edit);
		mSettingButton = findViewById(R.id.settings_button);
		mRunningServiceButton = findViewById(R.id.running_services_button);
		mInterfaceButton = findViewById(R.id.interface_button);
		mAftShortcutButton = findViewById(R.id.aft_footer_shortcut);
		mUserButton = findViewById(R.id.user_button);
		mUserAvatar = findViewById(R.id.user_picture);
		mUserAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
		mUserAvatar.setClipToOutline(true);
		mUserAvatar.setOutlineProvider(new ViewOutlineProvider() {
			@Override
			public void getOutline(View view, Outline outline) {
				outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), DeclanUtils.dpToPx(100));
			}
		});
		mUserName = findViewById(R.id.username);
		mUserName.setSelected(true);
		mDataUsageButton = findViewById(R.id.data_usage_button);
		mUsageText = findViewById(R.id.data_usage_text);
		mUsageText.setSelected(true);
		
		setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
		setUsageText();
		updateProfileView();
	}
	
	private void updateProfileView() {
		ContentResolver resolver = mContext.getContentResolver();
		mCustomAvatarEnabled = Settings.System.getIntForUser(resolver, "qs_footer_avatar_show", 0, UserHandle.USER_CURRENT) == 1;
		mAvatarUri = Settings.System.getStringForUser(resolver, "declan_avatar_picker", UserHandle.USER_CURRENT);
		mUserEnabled = Settings.System.getIntForUser(resolver, "qs_footer_text_show", 0, UserHandle.USER_CURRENT) == 1;
		mUserText = Settings.System.getStringForUser(resolver, "qs_footer_text_string", UserHandle.USER_CURRENT);
			
		String avatarUri = mAvatarUri;
		if (avatarUri != null) {
			Uri parse = Uri.parse(avatarUri);
			Log.i(TAG, "Save avatar image" + avatarUri);
			try {
				final InputStream avatarStream = mContext.getContentResolver().openInputStream(parse);
				File file = new File(mContext.getFilesDir(), AVATAR_FILE_NAME);
				if (file.exists()) {
					file.delete();
				}
				FileOutputStream output = new FileOutputStream(file);
				byte[] buffer = new byte[8 * 1024];
				int read;
				while ((read = avatarStream.read(buffer)) != -1) {
					output.write(buffer, 0, read);
				}
				output.flush();
				Log.i(TAG, "Save avatar image " + " " + file.getAbsolutePath());
			} catch (IOException e) {
				Log.e(TAG, "Save avatar image failed " + " " + parse);
			}
		}
		mAvatarBitmap = null;
		File file = new File(mContext.getFilesDir(), AVATAR_FILE_NAME);
		if (file.exists()) {
			Log.i(TAG, "Load avatar image");
			mAvatarBitmap = new BitmapDrawable(mContext.getResources(), ImageUtils.resizeMaxDeviceSize(mContext, BitmapFactory.decodeFile(file.getAbsolutePath())));
		}
		if (mCustomAvatarEnabled) {
			mUserAvatar.setImageDrawable(mAvatarBitmap);
		} else {
			mUserAvatar.setImageResource(R.drawable.ic_avatar_user);
		}
		
		if (mUserEnabled) {
			mUserName.setText(mUserText);
		} else {
			mUserName.setText("#KeepAfterLife");
		}
	}
	
	public void setUsageText() {
		if (mUsageText == null) return;
		DataUsageController.DataUsageInfo info;
		String suffix;
		if (isWifiConnected()) {
			info = mDataController.getWifiDailyDataUsageInfo();
			suffix = getWifiSsid();
		} else {
			mDataController.setSubscriptionId(SubscriptionManager.getDefaultDataSubscriptionId());
			info = mDataController.getDailyDataUsageInfo();
			suffix = getSlotCarrierName();
		}
		mUsageText.setText(formatDataUsage(info.usageLevel) + " " + mContext.getResources().getString(R.string.usage_data) + " (" + suffix + ")");
    }

    private CharSequence formatDataUsage(long byteValue) {
        final BytesResult res = Formatter.formatBytes(mContext.getResources(), byteValue,
                Formatter.FLAG_IEC_UNITS);
        return BidiFormatter.getInstance().unicodeWrap(mContext.getString(
                com.android.internal.R.string.fileSizeSuffix, res.value, res.units));
    }

    private boolean isWifiConnected() {
		final Network network = mConnectivityManager.getActiveNetwork();
		if (network != null) {
			NetworkCapabilities capabilities = mConnectivityManager.getNetworkCapabilities(network);
			return capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
		} else {
			return false;
		}
    }
    
    private String getSlotCarrierName() {
		CharSequence result = mContext.getResources().getString(R.string.usage_data_default_suffix);
		int subId = mSubManager.getDefaultDataSubscriptionId();
        final List<SubscriptionInfo> subInfoList = mSubManager.getActiveSubscriptionInfoList(true);
		if (subInfoList != null) {
			for (SubscriptionInfo subInfo : subInfoList) {
				if (subId == subInfo.getSubscriptionId()) {
					result = subInfo.getDisplayName();
					break;
				}
			}
		}
		return result.toString();
    }
    
    private String getWifiSsid() {
		final WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
		if (wifiInfo.getHiddenSSID() || wifiInfo.getSSID() == WifiManager.UNKNOWN_SSID) {
			return mContext.getResources().getString(R.string.usage_wifi_default_suffix);
		} else {
			return wifiInfo.getSSID().replace("\"", "");
		}
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateResources();
    }

    private void updateResources() {
        updateFooterAnimator();
        MarginLayoutParams lp = (MarginLayoutParams) getLayoutParams();
        lp.bottomMargin = getResources().getDimensionPixelSize(R.dimen.qs_footers_margin_bottom);
        setLayoutParams(lp);
    }

    private void updateFooterAnimator() {
        mFooterAnimator = createFooterAnimator();
    }

    @Nullable
    private TouchAnimator createFooterAnimator() {
        TouchAnimator.Builder builder = new TouchAnimator.Builder()
                .addFloat(mPageIndicator, "alpha", 0, 1)
				.addFloat(mAftShortcutButton, "alpha", 0, 1)
				.addFloat(mUserButton, "alpha", 0, 1)
				.addFloat(mDataUsageButton, "alpha", 0, 1)
                .setStartDelay(0.9f);
        return builder.build();
    }

    /** */
    public void setKeyguardShowing() {
        setExpansion(mExpansionAmount);
    }

    public void setExpandClickListener(OnClickListener onClickListener) {
        mExpandClickListener = onClickListener;
    }

    void setExpanded(boolean expanded) {
        if (mExpanded == expanded) return;
        mExpanded = expanded;
        updateEverything();
    }

    /** */
    public void setExpansion(float headerExpansionFraction) {
        mExpansionAmount = headerExpansionFraction;
        if (mFooterAnimator != null) {
            mFooterAnimator.setPosition(headerExpansionFraction);
        }

        if (mUsageText == null) return;
		if (headerExpansionFraction == 1.0f) {
			mUsageText.postDelayed(new Runnable() {
				@Override
				public void run() {
					mUsageText.setSelected(true);
				}
			}, 1000);
		} else {
			mUsageText.setSelected(false);
		}
    }
    
    @Override
    protected void onAttachedToWindow() {
		super.onAttachedToWindow();
        mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("qs_footer_text_show"), false, mSettingsObserver, UserHandle.USER_ALL);
		mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("qs_footer_text_string"), false, mSettingsObserver, UserHandle.USER_ALL);
		mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("qs_footer_avatar_show"), false, mSettingsObserver, UserHandle.USER_ALL);
		mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("declan_avatar_picker"), false, mSettingsObserver, UserHandle.USER_ALL);
    }

    @Override
    public void onDetachedFromWindow() {
		mContext.getContentResolver().unregisterContentObserver(mSettingsObserver);
		super.onDetachedFromWindow();
    }

    void disable(int state2) {
        final boolean disabled = (state2 & DISABLE2_QUICK_SETTINGS) != 0;
        if (disabled == mQsDisabled) return;
        mQsDisabled = disabled;
        updateEverything();
    }

    void updateEverything() {
        post(() -> {
            updateVisibilities();
            updateClickabilities();
            setClickable(false);
        });
    }

    private void updateClickabilities() {
    }

    private void updateVisibilities() {
		mUsageText.setVisibility(mExpanded ? View.VISIBLE : View.INVISIBLE);
		if (mExpanded) setUsageText();
		mUserAvatar.setVisibility(mExpanded ? View.VISIBLE : View.INVISIBLE);
		mUserName.setVisibility(mExpanded ? View.VISIBLE : View.INVISIBLE);
		if (mExpanded) updateProfileView();
    }
}
