/*
 * Copyright (C) 2023 AfterLife Project
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

package declan.prjct.settings.widget;

import android.content.*;
import android.database.*;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.os.*;
import android.provider.*;
import android.util.*;
import android.view.*;
import android.widget.*;

import com.android.settingslib.widget.R;
import com.declan.prjct.utils.DeclanUtils;

public class DeclanCategoryLayout extends LinearLayout {
	
	private boolean iconEnabled, allCapsEnabled, boldEnabled, strokeEnabled, dividerEnabled;
	private int cornerTopLeft, cornerTopRight, cornerBotRight, cornerBotLeft, paddingTop, paddingBot, paddingLeft, paddingRight,
	iconStyle, strokeColorStyle, strokeCustomColor, strokeWidth;
	
	private ImageView iconView;
	private View iconView1, dividerView;
	private TextView titleView;
	
	public DeclanCategoryLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		iconView = findViewById(R.id.icon);
		iconView1 = findViewById(R.id.icon_group);
		titleView = findViewById(android.R.id.title);
		dividerView = findViewById(R.id.divider);
		new SettingsObserver(new Handler(Looper.getMainLooper())).observe();
		updateSettings();
	}
	
	private void updateSettings() {
		ContentResolver cr = mContext.getContentResolver();
		iconEnabled = Settings.System.getInt(cr, "declan_category_icon_enabled", 0) == 1;
		iconStyle = Settings.System.getInt(cr, "declan_category_icon_style", 0);
		allCapsEnabled = Settings.System.getInt(cr, "declan_category_allcaps_enabled", 0) == 1;
		boldEnabled = Settings.System.getInt(cr, "declan_category_bold_enabled", 0) == 1;
		strokeEnabled = Settings.System.getInt(cr, "declan_category_stroke_enabled", 0) == 1;
		cornerTopLeft = DeclanUtils.getValueInDp(Settings.System.getInt(cr, "declan_category_corner_topL", 8));
		cornerTopRight = DeclanUtils.getValueInDp(Settings.System.getInt(cr, "declan_category_corner_topR", 8));
		cornerBotLeft = DeclanUtils.getValueInDp(Settings.System.getInt(cr, "declan_category_corner_botL", 8));
		cornerBotRight = DeclanUtils.getValueInDp(Settings.System.getInt(cr, "declan_category_corner_botR", 8));
		paddingLeft = DeclanUtils.getValueInDp(Settings.System.getInt(cr, "declan_category_padding_left", 4));
		paddingRight = DeclanUtils.getValueInDp(Settings.System.getInt(cr, "declan_category_padding_right", 4));
		paddingTop = DeclanUtils.getValueInDp(Settings.System.getInt(cr, "declan_category_padding_top", 4));
		paddingBot = DeclanUtils.getValueInDp(Settings.System.getInt(cr, "declan_category_padding_bot", 4));
		strokeColorStyle = Settings.System.getInt(cr, "declan_category_stroke_style", 0);
		strokeCustomColor = Settings.System.getInt(cr, "declan_category_stroke_color", 0);
		strokeWidth = DeclanUtils.getValueInDp(Settings.System.getInt(cr, "declan_category_stroke_width", 2));
		dividerEnabled = Settings.System.getInt(cr, "declan_category_divider_enabled", 0) == 1;
		updateTextTitle();
		updateIconStyle();
		updateBackgroundTitle();
		updateDividerView();
	}
	
	private void updateTextTitle() {
		if (allCapsEnabled) {
			titleView.setAllCaps(true);
		} else {
			titleView.setAllCaps(false);
		}
		if (boldEnabled) {
			titleView.setTypeface(Typeface.DEFAULT_BOLD);
		} else {
			titleView.setTypeface(Typeface.DEFAULT);
		}
	}
	
	private void updateIconStyle() {
		if (iconEnabled) {
			if (iconStyle == 1) {
				iconView.setVisibility(View.VISIBLE);
				iconView1.setVisibility(View.GONE);
			} else {
				iconView.setVisibility(View.GONE);
				iconView1.setVisibility(View.VISIBLE);
			}
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			params.setMargins(10, 0, 0, 0);
			titleView.setLayoutParams(params);
		} else {
			iconView.setVisibility(View.GONE);
			iconView1.setVisibility(View.GONE);
		}
	}
	
	private void updateBackgroundTitle() {
		GradientDrawable backgroundDrawable = new GradientDrawable();
		backgroundDrawable.setColor(android.R.color.transparent);
		backgroundDrawable.setCornerRadii(new float[]{cornerTopLeft, cornerTopLeft, cornerTopRight, cornerTopRight, cornerBotRight, cornerBotRight, cornerBotLeft, cornerBotLeft});
		backgroundDrawable.setStroke(strokeWidth, getStrokeColor());
		if (strokeEnabled) {
			titleView.setBackground(backgroundDrawable);
			titleView.setPadding(paddingLeft, paddingTop, paddingRight, paddingBot);
		} else {
			titleView.setBackgroundResource(android.R.color.transparent);
			titleView.setPadding(0, 0, 0, 0);
		}
	}
	
	private void updateDividerView() {
		if (dividerEnabled) {
			dividerView.setVisibility(View.VISIBLE);
			dividerView.setBackgroundColor(getStrokeColor());
		} else {
			dividerView.setVisibility(View.GONE);
			dividerView.setBackgroundResource(android.R.color.transparent);
		}
	}
	
	private int getStrokeColor() {
		if (strokeColorStyle == 1) {
			return strokeCustomColor;
		} else {
			return DeclanUtils.getColorAttr(mContext, android.R.attr.colorAccent);
		}
	}
	
	class SettingsObserver extends ContentObserver {
		public SettingsObserver(Handler handler) {
			super(handler);
		}
		
		public void observe() {
			ContentResolver cr = mContext.getContentResolver();
			cr.registerContentObserver(Settings.System.getUriFor("declan_category_icon_enabled"), false, this);
			cr.registerContentObserver(Settings.System.getUriFor("declan_category_icon_style"), false, this);
			cr.registerContentObserver(Settings.System.getUriFor("declan_category_allcaps_enabled"), false, this);
			cr.registerContentObserver(Settings.System.getUriFor("declan_category_bold_enabled"), false, this);
			cr.registerContentObserver(Settings.System.getUriFor("declan_category_stroke_enabled"), false, this);
			cr.registerContentObserver(Settings.System.getUriFor("declan_category_corner_topL"), false, this);
			cr.registerContentObserver(Settings.System.getUriFor("declan_category_corner_topR"), false, this);
			cr.registerContentObserver(Settings.System.getUriFor("declan_category_corner_botL"), false, this);
			cr.registerContentObserver(Settings.System.getUriFor("declan_category_corner_botR"), false, this);
			cr.registerContentObserver(Settings.System.getUriFor("declan_category_padding_left"), false, this);
			cr.registerContentObserver(Settings.System.getUriFor("declan_category_padding_right"), false, this);
			cr.registerContentObserver(Settings.System.getUriFor("declan_category_padding_top"), false, this);
			cr.registerContentObserver(Settings.System.getUriFor("declan_category_padding_bot"), false, this);
			cr.registerContentObserver(Settings.System.getUriFor("declan_category_stroke_style"), false, this);
			cr.registerContentObserver(Settings.System.getUriFor("declan_category_stroke_color"), false, this);
			cr.registerContentObserver(Settings.System.getUriFor("declan_category_stroke_width"), false, this);
			cr.registerContentObserver(Settings.System.getUriFor("declan_category_divider_enabled"), false, this);
		}
		
		@Override
		public void onChange(boolean selfChange) {
			updateSettings();
		}
    }
}