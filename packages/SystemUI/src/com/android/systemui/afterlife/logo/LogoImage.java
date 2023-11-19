/*
 * Copyright (C) 2018-2022 crDroid Android Project
 * Copyright (C) 2018-2019 AICP
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

package com.android.systemui.afterlife.logo;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import com.android.systemui.R;
import com.android.systemui.Dependency;
import com.android.systemui.plugins.DarkIconDispatcher;
import com.android.systemui.plugins.DarkIconDispatcher.DarkReceiver;

import java.util.ArrayList;

public abstract class LogoImage extends ImageView {

    private Context mContext;

    private boolean mAttached;

    private boolean mShowLogo;
    public int mLogoPosition;
    private int mTintColor = Color.WHITE;

    private static final String STATUS_BAR_LOGO = "status_bar_logo";
    private static final String STATUS_BAR_LOGO_POSITION = "status_bar_logo_position";

    class SettingsObserver extends ContentObserver {

        public SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(STATUS_BAR_LOGO), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(STATUS_BAR_LOGO_POSITION), false, this);
        }

        @Override
        public void onChange(boolean selfChange) {
            updateSettings();
        }
    }

    public LogoImage(Context context) {
        this(context, null);
    }

    public LogoImage(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LogoImage(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
    }

    protected abstract boolean isLogoVisible();

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mAttached)
            return;

        mAttached = true;

        SettingsObserver observer = new SettingsObserver(new Handler());
        observer.observe();
        updateSettings();

        Dependency.get(DarkIconDispatcher.class).addDarkReceiver(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (!mAttached)
            return;

        mAttached = false;
        Dependency.get(DarkIconDispatcher.class).removeDarkReceiver(this);
    }

    public void onDarkChanged(ArrayList<Rect> areas, float darkIntensity, int tint) {
        mTintColor = DarkIconDispatcher.getTint(areas, this, tint);
        if (mShowLogo && isLogoVisible()) {
            updateLogo();
        }
    }

    public void updateLogo() {
        Drawable drawable = mContext.getResources().getDrawable(R.drawable.ic_afterlife_logo);
        drawable.setTint(mTintColor);
        setImageDrawable(drawable);
    }

    public void updateSettings() {
        mShowLogo = Settings.System.getInt(mContext.getContentResolver(), STATUS_BAR_LOGO, 0) != 0;
        mLogoPosition = Settings.System.getInt(mContext.getContentResolver(), STATUS_BAR_LOGO_POSITION, 0);
        if (!mShowLogo || !isLogoVisible()) {
            setImageDrawable(null);
            setVisibility(View.GONE);
            return;
        }
        updateLogo();
        setVisibility(View.VISIBLE);
    }

    public static int getLogoPosition(Context context) {
        return Settings.System.getInt(context.getContentResolver(), STATUS_BAR_LOGO_POSITION, 0);
    }
}