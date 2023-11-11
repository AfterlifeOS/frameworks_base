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

import android.content.*;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;

import com.android.systemui.R;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.FalsingManager;
import com.android.systemui.qs.dagger.QSScope;
import com.android.systemui.settings.UserTracker;
import com.android.systemui.util.ViewController;

import javax.inject.Inject;

/**
 * Controller for {@link QSFooterView}.
 */
@QSScope
public class QSFooterViewController extends ViewController<QSFooterView> implements QSFooter {

    private final UserTracker mUserTracker;
    private final QSPanelController mQsPanelController;
    private final PageIndicator mPageIndicator;
    private final View mSettingsButton, mEditButton, mRunningServiceButton, mInterfaceButton;
    private final FalsingManager mFalsingManager;
    private final ActivityStarter mActivityStarter;

    @Inject
    QSFooterViewController(QSFooterView view,
            UserTracker userTracker,
            FalsingManager falsingManager,
            ActivityStarter activityStarter,
            QSPanelController qsPanelController) {
        super(view);
        mUserTracker = userTracker;
        mQsPanelController = qsPanelController;
        mFalsingManager = falsingManager;
        mActivityStarter = activityStarter;

        mPageIndicator = mView.findViewById(R.id.footer_page_indicator);
        mEditButton = mView.findViewById(android.R.id.edit);
        mSettingsButton = mView.findViewById(R.id.settings_button);
		mRunningServiceButton = mView.findViewById(R.id.running_services_button);
		mInterfaceButton = mView.findViewById(R.id.interface_button);
    }

    @Override
    protected void onViewAttached() {
        mEditButton.setOnClickListener(view -> {
            if (mFalsingManager.isFalseTap(FalsingManager.LOW_PENALTY)) {
                return;
            }
            mActivityStarter
                    .postQSRunnableDismissingKeyguard(() -> mQsPanelController.showEdit(view));
        });
        mSettingsButton.setOnClickListener(mSettingsOnClickListener);
		mRunningServiceButton.setOnClickListener(mSettingsOnClickListener);
		mInterfaceButton.setOnClickListener(mSettingsOnClickListener);
        mQsPanelController.setFooterPageIndicator(mPageIndicator);
        mView.updateEverything();
    }
    
    private final View.OnClickListener mSettingsOnClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			if (mFalsingManager.isFalseTap(FalsingManager.LOW_PENALTY)) {
				return;
			}
			
			if (v == mSettingsButton) {
				startSettingsActivity();
			} else if (v == mRunningServiceButton) {
				startRunningServicesActivity();
			} else if (v == mInterfaceButton) {
				startAfterLabActivity();
			}
		}
	};

    @Override
    protected void onViewDetached() {}

    @Override
    public void setVisibility(int visibility) {
        mView.setVisibility(visibility);
        mEditButton.setClickable(visibility == View.VISIBLE);
    }

    @Override
    public void setExpanded(boolean expanded) {
        mView.setExpanded(expanded);
    }

    @Override
    public void setExpansion(float expansion) {
        mView.setExpansion(expansion);
    }

    @Override
    public void setKeyguardShowing(boolean keyguardShowing) {
        mView.setKeyguardShowing();
    }

    @Override
    public void disable(int state1, int state2, boolean animate) {
        mView.disable(state2);
    }
    
    private void startSettingsActivity() {
		mActivityStarter.startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS), true /* dismissShade */);
    }
	
	private void startRunningServicesActivity() {
		Intent intent = new Intent();
		intent.setClassName("com.android.settings", "com.android.settings.Settings$DevRunningServicesActivity");
		mActivityStarter.startActivity(intent, true /* dismissShade */);
    }
	
	private void startAfterLabActivity() {
    Intent nIntent = new Intent(Intent.ACTION_MAIN);
    nIntent.setClassName("com.android.settings", "com.android.settings.Settings$AfterlabSettingsActivity");
    mActivityStarter.startActivity(nIntent, true /* dismissShade */);
    }
}
