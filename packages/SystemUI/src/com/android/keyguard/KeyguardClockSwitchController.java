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

package com.android.keyguard;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import static com.android.keyguard.KeyguardClockSwitch.LARGE;
import static com.android.keyguard.KeyguardClockSwitch.SMALL;

import android.annotation.Nullable;
import android.database.ContentObserver;
import android.content.ContentResolver;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.view.Gravity;
import android.widget.*;

import androidx.annotation.NonNull;

import com.android.systemui.Dependency;
import com.android.systemui.Dumpable;
import com.android.systemui.R;
import com.android.systemui.afterlife.CurrentWeatherView;
import com.android.systemui.dagger.qualifiers.Main;
import com.android.systemui.dump.DumpManager;
import com.android.systemui.keyguard.KeyguardUnlockAnimationController;
import com.android.systemui.log.dagger.KeyguardClockLog;
import com.android.systemui.plugins.ClockAnimations;
import com.android.systemui.plugins.ClockController;
import com.android.systemui.plugins.log.LogBuffer;
import com.android.systemui.plugins.log.LogLevel;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.shared.clocks.ClockRegistry;
import com.android.systemui.shared.regionsampling.RegionSampler;
import com.android.systemui.statusbar.lockscreen.LockscreenSmartspaceController;
import com.android.systemui.statusbar.notification.AnimatableProperty;
import com.android.systemui.statusbar.notification.PropertyAnimator;
import com.android.systemui.statusbar.notification.stack.AnimationProperties;
import com.android.systemui.statusbar.phone.NotificationIconAreaController;
import com.android.systemui.statusbar.phone.NotificationIconContainer;
import com.android.systemui.tuner.TunerService;
import com.android.systemui.util.ViewController;
import com.android.systemui.util.settings.SecureSettings;

import java.io.PrintWriter;
import java.util.Locale;
import java.util.concurrent.Executor;

import javax.inject.Inject;

/**
 * Injectable controller for {@link KeyguardClockSwitch}.
 */
public class KeyguardClockSwitchController extends ViewController<KeyguardClockSwitch>
        implements Dumpable, TunerService.Tunable {
    private static final String TAG = "KeyguardClockSwitchController";

    private static final String LOCKSCREEN_WEATHER_ENABLED =
            "system:" + Settings.System.LOCKSCREEN_WEATHER_ENABLED;
    private static final String LOCKSCREEN_WEATHER_STYLE =
            "system:" + Settings.System.LOCKSCREEN_WEATHER_STYLE;

    private final StatusBarStateController mStatusBarStateController;
    private final ClockRegistry mClockRegistry;
    private final KeyguardSliceViewController mKeyguardSliceViewController;
    private final NotificationIconAreaController mNotificationIconAreaController;
    private final LockscreenSmartspaceController mSmartspaceController;
    private final SecureSettings mSecureSettings;
    private final DumpManager mDumpManager;
    private final ClockEventController mClockEventController;
    private final LogBuffer mLogBuffer;
    private final TunerService  mTunerService;
    private final ContentResolver mCR;

    private FrameLayout mSmallClockFrame; // top aligned clock
    private FrameLayout mLargeClockFrame; // centered clock
    private View mCustomClockFrame; // custom clock

    @KeyguardClockSwitch.ClockSize
    private int mCurrentClockSize = SMALL;

    private int mKeyguardSmallClockTopMargin = 0;
    private int mKeyguardLargeClockTopMargin = 0;
    private final ClockRegistry.ClockChangeListener mClockChangedListener;

    private ViewGroup mStatusArea;

    // If the SMARTSPACE flag is set, keyguard_slice_view is replaced by the following views.
    private ViewGroup mDateWeatherView;
    private View mWeatherView;
    private View mSmartspaceView;

    private final KeyguardUnlockAnimationController mKeyguardUnlockAnimationController;

    private CurrentWeatherView mCurrentWeatherView;
    private boolean mShowWeather;
    private boolean mPixelStyle;

    private boolean mOnlyClock = false;
    private final Executor mUiExecutor;
    private boolean mCanShowDoubleLineClock = true;
    private final ContentObserver mDoubleLineClockObserver = new ContentObserver(null) {
        @Override
        public void onChange(boolean change) {
            updateDoubleLineClock();
        }
    };
    private boolean mEnableCustomClock = true;
    private final ContentObserver mCustomClockObserver = new ContentObserver(null) {
        @Override
        public void onChange(boolean change) {
            updateCustomClock();
        }
    };
    private final ContentObserver mShowWeatherObserver = new ContentObserver(null) {
        @Override
        public void onChange(boolean change) {
            setWeatherVisibility();
        }
    };

    private final KeyguardUnlockAnimationController.KeyguardUnlockAnimationListener
            mKeyguardUnlockAnimationListener =
            new KeyguardUnlockAnimationController.KeyguardUnlockAnimationListener() {
                @Override
                public void onUnlockAnimationFinished() {
                    // For performance reasons, reset this once the unlock animation ends.
                    setClipChildrenForUnlock(true);
                }
            };

    @Inject
    public KeyguardClockSwitchController(
            KeyguardClockSwitch keyguardClockSwitch,
            StatusBarStateController statusBarStateController,
            ClockRegistry clockRegistry,
            KeyguardSliceViewController keyguardSliceViewController,
            NotificationIconAreaController notificationIconAreaController,
            LockscreenSmartspaceController smartspaceController,
            KeyguardUnlockAnimationController keyguardUnlockAnimationController,
            SecureSettings secureSettings,
            ContentResolver cr,
            @Main Executor uiExecutor,
            DumpManager dumpManager,
            ClockEventController clockEventController,
            @KeyguardClockLog LogBuffer logBuffer) {
        super(keyguardClockSwitch);
        mStatusBarStateController = statusBarStateController;
        mClockRegistry = clockRegistry;
        mKeyguardSliceViewController = keyguardSliceViewController;
        mNotificationIconAreaController = notificationIconAreaController;
        mSmartspaceController = smartspaceController;
        mSecureSettings = secureSettings;
        mCR = cr;
        mUiExecutor = uiExecutor;
        mKeyguardUnlockAnimationController = keyguardUnlockAnimationController;
        mDumpManager = dumpManager;
        mClockEventController = clockEventController;
        mLogBuffer = logBuffer;
        mView.setLogBuffer(mLogBuffer);

        mClockChangedListener = new ClockRegistry.ClockChangeListener() {
            @Override
            public void onCurrentClockChanged() {
                setClock(mClockRegistry.createCurrentClock());
            }
            @Override
            public void onAvailableClocksChanged() { }
        };

        mTunerService = Dependency.get(TunerService.class);
    }

    /**
     * Mostly used for alternate displays, limit the information shown
     */
    public void setOnlyClock(boolean onlyClock) {
        mOnlyClock = onlyClock;
    }

    /**
     * Attach the controller to the view it relates to.
     */
    @Override
    protected void onInit() {
        mKeyguardSliceViewController.init();

        mSmallClockFrame = mView.findViewById(R.id.lockscreen_clock_view);
        mLargeClockFrame = mView.findViewById(R.id.lockscreen_clock_view_large);
        mCurrentWeatherView = mView.findViewById(R.id.weather_container);
        mCustomClockFrame = mView.findViewById(R.id.clock_ls);

        mDumpManager.unregisterDumpable(getClass().toString()); // unregister previous clocks
        mDumpManager.registerDumpable(getClass().toString(), this);

        mTunerService.addTunable(this, LOCKSCREEN_WEATHER_ENABLED);
        mTunerService.addTunable(this, LOCKSCREEN_WEATHER_STYLE);
        
    }

    @Override
    protected void onViewAttached() {
        mClockRegistry.registerClockChangeListener(mClockChangedListener);
        setClock(mClockRegistry.createCurrentClock());
        mClockEventController.registerListeners(mView);
        mKeyguardSmallClockTopMargin =
                mView.getResources().getDimensionPixelSize(R.dimen.keyguard_clock_top_margin);
        mKeyguardLargeClockTopMargin =
                mView.getResources().getDimensionPixelSize(R.dimen.keyguard_large_clock_top_margin);

        updateWeatherView();

        if (mOnlyClock) {
            View ksv = mView.findViewById(R.id.keyguard_slice_view);
            ksv.setVisibility(View.GONE);

            View nic = mView.findViewById(
                    R.id.left_aligned_notification_icon_container);
            nic.setVisibility(View.GONE);
            return;
        }
        updateAodIcons();

        mStatusArea = mView.findViewById(R.id.keyguard_status_area);

        if (mSmartspaceController.isEnabled() && !mShowWeather) {
            View ksv = mView.findViewById(R.id.keyguard_slice_view);
            int viewIndex = mStatusArea.indexOfChild(ksv);
            ksv.setVisibility(View.GONE);

            // TODO(b/261757708): add content observer for the Settings toggle and add/remove
            //  weather according to the Settings.
            if (mSmartspaceController.isDateWeatherDecoupled()) {
                addDateWeatherView(viewIndex);
                viewIndex += 1;
            }

            addSmartspaceView(viewIndex);
        }

        mSecureSettings.registerContentObserverForUser(
                Settings.Secure.LOCKSCREEN_USE_DOUBLE_LINE_CLOCK,
                false, /* notifyForDescendants */
                mDoubleLineClockObserver,
                UserHandle.USER_ALL
        );
        
        mSecureSettings.registerContentObserverForUser(
                Settings.Secure.CLOCK_LS,
                false, /* notifyForDescendants */
                mCustomClockObserver,
                UserHandle.USER_ALL
        );

        mSecureSettings.registerContentObserverForUser(
                Settings.Secure.LOCK_SCREEN_WEATHER_ENABLED,
                false, /* notifyForDescendants */
                mShowWeatherObserver,
                UserHandle.USER_ALL
        );
        
        mCR.registerContentObserver(
                Settings.System.getUriFor("clock_style"),
                false,
                mCustomClockObserver
        );

        updateDoubleLineClock();
        setWeatherVisibility();
        updateCustomClock();

        mKeyguardUnlockAnimationController.addKeyguardUnlockAnimationListener(
                mKeyguardUnlockAnimationListener);
    }

    int getNotificationIconAreaHeight() {
        return mNotificationIconAreaController.getHeight();
    }

    @Override
    protected void onViewDetached() {
        mTunerService.removeTunable(this);
        mClockRegistry.unregisterClockChangeListener(mClockChangedListener);
        mClockEventController.unregisterListeners();
        setClock(null);

        mSecureSettings.unregisterContentObserver(mDoubleLineClockObserver);
        mSecureSettings.unregisterContentObserver(mCustomClockObserver);

        mKeyguardUnlockAnimationController.removeKeyguardUnlockAnimationListener(
                mKeyguardUnlockAnimationListener);
    }

    @Override
    public void onTuningChanged(String key, String newValue) {
        switch (key) {
            case LOCKSCREEN_WEATHER_ENABLED:
                mShowWeather =
                        TunerService.parseIntegerSwitch(newValue, false);
                updateWeatherView();
                break;
            case LOCKSCREEN_WEATHER_STYLE:
                mPixelStyle =
                        TunerService.parseIntegerSwitch(newValue, false);
                updateWeatherView();
                break;
            default:
                break;
        }
    }

    public void updateWeatherView() {
        mUiExecutor.execute(() -> {
            if (mCurrentWeatherView != null) {
                if (mShowWeather && mPixelStyle && !mOnlyClock) {
                    mCurrentWeatherView.enableUpdates();
                    mCurrentWeatherView.setVisibility(View.VISIBLE);
                } else {
                    mCurrentWeatherView.disableUpdates();
                    mCurrentWeatherView.setVisibility(View.GONE);
                }
            }
        });
    }

    void onLocaleListChanged() {
        if (mSmartspaceController.isEnabled() && !mShowWeather) {
            if (mSmartspaceController.isDateWeatherDecoupled()) {
                mDateWeatherView.removeView(mWeatherView);
                int index = mStatusArea.indexOfChild(mDateWeatherView);
                if (index >= 0) {
                    mStatusArea.removeView(mDateWeatherView);
                    addDateWeatherView(index);
                }
            }
            int index = mStatusArea.indexOfChild(mSmartspaceView);
            if (index >= 0) {
                mStatusArea.removeView(mSmartspaceView);
                addSmartspaceView(index);
            }
        }
    }

    private void addDateWeatherView(int index) {
        mDateWeatherView = (ViewGroup) mSmartspaceController.buildAndConnectDateView(mView);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                MATCH_PARENT, WRAP_CONTENT);
        int startPadding = getContext().getResources().getDimensionPixelSize(
                R.dimen.below_clock_padding_start);
        int endPadding = getContext().getResources().getDimensionPixelSize(
                R.dimen.below_clock_padding_end);
        mStatusArea.addView(mDateWeatherView, index, lp);
        addWeatherView();
    	mDateWeatherView.setPaddingRelative(startPadding, 0, endPadding, 0);
    }

    private void addWeatherView() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                WRAP_CONTENT, WRAP_CONTENT);
        int startPadding = getContext().getResources().getDimensionPixelSize(
                R.dimen.below_clock_padding_start);
        int endPadding = getContext().getResources().getDimensionPixelSize(
                R.dimen.below_clock_padding_end);
        mWeatherView = mSmartspaceController.buildAndConnectWeatherView(mView);
        // Place weather right after the date, before the extras
        final int index = mDateWeatherView.getChildCount() == 0 ? 0 : 1;
        mDateWeatherView.addView(mWeatherView, index, lp);
        mWeatherView.setPaddingRelative(startPadding, 0, 4, 0);
    }

    private void addSmartspaceView(int index) {
        mSmartspaceView = mSmartspaceController.buildAndConnectView(mView);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                MATCH_PARENT, WRAP_CONTENT);
        int startPadding = getContext().getResources().getDimensionPixelSize(
                R.dimen.below_clock_padding_start);
        int endPadding = getContext().getResources().getDimensionPixelSize(
                R.dimen.below_clock_padding_end);
        mStatusArea.addView(mSmartspaceView, index, lp);
        mSmartspaceView.setPaddingRelative(startPadding, 0, endPadding, 0);
        mKeyguardUnlockAnimationController.setLockscreenSmartspace(mSmartspaceView);
    }

    /**
     * Apply dp changes on font/scale change
     */
    public void onDensityOrFontScaleChanged() {
        mView.onDensityOrFontScaleChanged();
        mKeyguardSmallClockTopMargin =
                mView.getResources().getDimensionPixelSize(R.dimen.keyguard_clock_top_margin);
        mKeyguardLargeClockTopMargin =
                mView.getResources().getDimensionPixelSize(R.dimen.keyguard_large_clock_top_margin);
        mView.updateClockTargetRegions();
    }


    /**
     * Set which clock should be displayed on the keyguard. The other one will be automatically
     * hidden.
     */
    public void displayClock(@KeyguardClockSwitch.ClockSize int clockSize, boolean animate) {
        if (!mCanShowDoubleLineClock && clockSize == KeyguardClockSwitch.LARGE) {
            return;
        }

        mCurrentClockSize = clockSize;

        ClockController clock = getClock();
        boolean appeared = mView.switchToClock(clockSize, animate);
        if (clock != null && animate && appeared && clockSize == LARGE) {
            clock.getAnimations().enter();
        }
    }

    /**
     * Animates the clock view between folded and unfolded states
     */
    public void animateFoldToAod(float foldFraction) {
        ClockController clock = getClock();
        if (clock != null) {
            clock.getAnimations().fold(foldFraction);
        }
    }

    /**
     * Refresh clock. Called in response to TIME_TICK broadcasts.
     */
    void refresh() {
        if (mSmartspaceController != null) {
            mSmartspaceController.requestSmartspaceUpdate();
        }
        ClockController clock = getClock();
        if (clock != null) {
            clock.getSmallClock().getEvents().onTimeTick();
            clock.getLargeClock().getEvents().onTimeTick();
        }
    }

    /**
     * Update position of the view, with optional animation. Move the slice view and the clock
     * slightly towards the center in order to prevent burn-in. Y positioning occurs at the
     * view parent level. The large clock view will scale instead of using x position offsets, to
     * keep the clock centered.
     */
    void updatePosition(int x, float scale, AnimationProperties props, boolean animate) {
        x = getCurrentLayoutDirection() == View.LAYOUT_DIRECTION_RTL ? -x : x;

        PropertyAnimator.setProperty(mSmallClockFrame, AnimatableProperty.TRANSLATION_X,
                x, props, animate);
        PropertyAnimator.setProperty(mLargeClockFrame, AnimatableProperty.SCALE_X,
                scale, props, animate);
        PropertyAnimator.setProperty(mLargeClockFrame, AnimatableProperty.SCALE_Y,
                scale, props, animate);

        if (mStatusArea != null) {
            PropertyAnimator.setProperty(mStatusArea, AnimatableProperty.TRANSLATION_X,
                    x, props, animate);
        }
    }

    /**
     * Get y-bottom position of the currently visible clock on the keyguard.
     * We can't directly getBottom() because clock changes positions in AOD for burn-in
     */
    int getClockBottom(int statusBarHeaderHeight) {
        ClockController clock = getClock();
        if (clock == null) {
            return 0;
        }

        if (mLargeClockFrame.getVisibility() == View.VISIBLE) {
            // This gets the expected clock bottom if mLargeClockFrame had a top margin, but it's
            // top margin only contributed to height and didn't move the top of the view (as this
            // was the computation previously). As we no longer have a margin, we add this back
            // into the computation manually.
            int frameHeight = mLargeClockFrame.getHeight();
            int clockHeight = clock.getLargeClock().getView().getHeight();
            if (!mEnableCustomClock) {
                return frameHeight / 2 + clockHeight / 2 + mKeyguardLargeClockTopMargin / -2;
            } else {
            	return 0;
            }
            
        } else {
            // This is only called if we've never shown the large clock as the frame is inflated
            // with 'gone', but then the visibility is never set when it is animated away by
            // KeyguardClockSwitch, instead it is removed from the view hierarchy.
            // TODO(b/261755021): Cleanup Large Frame Visibility
            int clockHeight = clock.getSmallClock().getView().getHeight();
            if (!mEnableCustomClock) {
                return clockHeight + statusBarHeaderHeight + mKeyguardSmallClockTopMargin;
            } else {
            	return 0;
            }
        }
    }

    /**
     * Get the height of the currently visible clock on the keyguard.
     */
    int getClockHeight() {
        ClockController clock = getClock();
        if (clock == null) {
            return 0;
        }

        if (mLargeClockFrame.getVisibility() == View.VISIBLE) {
        	if (!mEnableCustomClock) {
                return clock.getLargeClock().getView().getHeight();
            } else {
            	return 0;
            }
        } else {
            // Is not called except in certain edge cases, see comment in getClockBottom
            // TODO(b/261755021): Cleanup Large Frame Visibility
            if (!mEnableCustomClock) {
                return clock.getSmallClock().getView().getHeight();
            } else {
            	return 0;
            }
        }
    }

    boolean isClockTopAligned() {
        // Returns false except certain edge cases, see comment in getClockBottom
        // TODO(b/261755021): Cleanup Large Frame Visibility
        return mLargeClockFrame.getVisibility() != View.VISIBLE;
    }

    private void updateAodIcons() {
        NotificationIconContainer nic = (NotificationIconContainer)
                mView.findViewById(
                        com.android.systemui.R.id.left_aligned_notification_icon_container);
        mNotificationIconAreaController.setupAodIcons(nic);
    }

    private void setClock(ClockController clock) {
        if (clock != null && mLogBuffer != null) {
            mLogBuffer.log(TAG, LogLevel.INFO, "New Clock");
        }

        mClockEventController.setClock(clock);
        mView.setClock(clock, mStatusBarStateController.getState());
    }

    @Nullable
    private ClockController getClock() {
        return mClockEventController.getClock();
    }

    private int getCurrentLayoutDirection() {
        return TextUtils.getLayoutDirectionFromLocale(Locale.getDefault());
    }

    private void updateDoubleLineClock() {
        mCanShowDoubleLineClock = mSecureSettings.getIntForUser(
            Settings.Secure.LOCKSCREEN_USE_DOUBLE_LINE_CLOCK, 1,
                UserHandle.USER_CURRENT) != 0;

        if (!mCanShowDoubleLineClock) {
            mUiExecutor.execute(() -> displayClock(KeyguardClockSwitch.SMALL, /* animate */ true));
        }
    }
    
    private void updateCustomClock() {
        mEnableCustomClock = mSecureSettings.getInt(
            Settings.Secure.CLOCK_LS, 0) == 1;
        
        ViewGroup.LayoutParams params = mSmallClockFrame.getLayoutParams();
        ViewGroup.LayoutParams params2 = mLargeClockFrame.getLayoutParams();
        LinearLayout.LayoutParams params3 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        RelativeLayout.LayoutParams params4 = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
    	params4.addRule(RelativeLayout.BELOW, mEnableCustomClock ? R.id.clock_ls : R.id.lockscreen_clock_view);  
        mStatusArea.setLayoutParams(params4);

        if (mEnableCustomClock) {
        	int clockStyle = Settings.System.getInt(getContext().getContentResolver(), "clock_style", 0);
            params.width = 0;
            params.height = 0;
            mSmallClockFrame.setLayoutParams(params);
            
            params2.width = 0;
            params2.height = 0;
            mLargeClockFrame.setLayoutParams(params2);
            int startPadding = getContext().getResources().getDimensionPixelSize(
                R.dimen.below_clock_padding_start);
            if (clockStyle == 0 || clockStyle == 2 || clockStyle == 6) {
                params3.gravity = Gravity.START;
                params3.setMargins(startPadding, 0, 0, 0);
            } else {
            	params3.gravity = Gravity.CENTER;
                params3.setMargins(0, 0, 0, 0);
            }
            mCurrentWeatherView.setLayoutParams(params3);
            mCustomClockFrame.setVisibility(View.VISIBLE);
        } else {
        	params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            mSmallClockFrame.setLayoutParams(params);
            
            params2.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params2.height = ViewGroup.LayoutParams.MATCH_PARENT;
            mLargeClockFrame.setLayoutParams(params2);
            int startPadding = getContext().getResources().getDimensionPixelSize(
                R.dimen.below_clock_padding_start);
            params3.gravity = Gravity.START;
            params3.setMargins(startPadding, 0, 0, 0);
            mCurrentWeatherView.setLayoutParams(params3);
        	mCustomClockFrame.setVisibility(View.GONE);
        }
    }

    private void setWeatherVisibility() {
        if (mWeatherView != null) {
            mUiExecutor.execute(
                    () -> mWeatherView.setVisibility(
                        mSmartspaceController.isWeatherEnabled() && !mShowWeather ? View.VISIBLE : View.GONE));
        }
    }

    /**
     * Sets the clipChildren property on relevant views, to allow the smartspace to draw out of
     * bounds during the unlock transition.
     */
    private void setClipChildrenForUnlock(boolean clip) {
        if (mStatusArea != null) {
            mStatusArea.setClipChildren(clip);
        }
    }

    @Override
    public void dump(@NonNull PrintWriter pw, @NonNull String[] args) {
        pw.println("currentClockSizeLarge=" + (mCurrentClockSize == LARGE));
        pw.println("mCanShowDoubleLineClock=" + mCanShowDoubleLineClock);
        mView.dump(pw, args);
        ClockController clock = getClock();
        if (clock != null) {
            clock.dump(pw);
        }
        final RegionSampler regionSampler = mClockEventController.getRegionSampler();
        if (regionSampler != null) {
            regionSampler.dump(pw);
        }
    }

    /** Gets the animations for the current clock. */
    @Nullable
    public ClockAnimations getClockAnimations() {
        ClockController clock = getClock();
        return clock == null ? null : clock.getAnimations();
    }
}

