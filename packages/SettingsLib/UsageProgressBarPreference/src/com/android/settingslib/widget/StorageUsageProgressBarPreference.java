/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settingslib.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settingslib.widget.preference.usage.R;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StorageUsageProgressBarPreference extends Preference {

    private final Pattern mNumberPattern = Pattern.compile("[\\d]*[\\٫.,]?[\\d]+");
    private static final int ANIM_DURATION = 1200;

    private CharSequence mUsageSummary;
    private CharSequence mTotalSummary;
    private CharSequence mBottomSummary;
    private ImageView mCustomImageView;
    private int mPercent = -1;

    public StorageUsageProgressBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.preference_usage_progress_bar);
    }

    public StorageUsageProgressBarPreference(Context context) {
        this(context, null);
    }

    public void setUsageSummary(CharSequence usageSummary) {
        if (TextUtils.equals(mUsageSummary, usageSummary)) {
            return;
        }
        mUsageSummary = usageSummary;
        notifyChanged();
    }

    public void setTotalSummary(CharSequence totalSummary) {
        if (TextUtils.equals(mTotalSummary, totalSummary)) {
            return;
        }
        mTotalSummary = totalSummary;
        notifyChanged();
    }

    public void setBottomSummary(CharSequence bottomSummary) {
        if (TextUtils.equals(mBottomSummary, bottomSummary)) {
            return;
        }
        mBottomSummary = bottomSummary;
        notifyChanged();
    }

    public void setPercent(long usage, long total) {
        if (usage > total) {
            return;
        }
        if (total == 0L) {
            if (mPercent != 0) {
                mPercent = 0;
                notifyChanged();
            }
            return;
        }
        final int percent = (int) (usage / (double) total * 100);
        if (mPercent == percent) {
            return;
        }
        mPercent = percent;
        notifyChanged();
    }

    public <T extends ImageView> void setCustomContent(T imageView) {
        if (imageView == mCustomImageView) {
            return;
        }
        mCustomImageView = imageView;
        notifyChanged();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        final Context context = getContext();

        holder.setDividerAllowedAbove(false);
        holder.setDividerAllowedBelow(false);

        final TextView usageSummary = (TextView) holder.findViewById(R.id.usage_summary);
        usageSummary.setText(mUsageSummary);

        final TextView totalSummary = (TextView) holder.findViewById(R.id.total_summary);
        if (mTotalSummary != null) {
            totalSummary.setText(mTotalSummary);
        }

        final TextView bottomSummary = (TextView) holder.findViewById(R.id.bottom_summary);
        if (TextUtils.isEmpty(mBottomSummary)) {
            bottomSummary.setVisibility(View.GONE);
        } else {
            bottomSummary.setVisibility(View.VISIBLE);
            bottomSummary.setText(mBottomSummary);
        }

        final ProgressBar progressBar = (ProgressBar) holder.findViewById(android.R.id.progress);
        final ValueAnimator animator = ValueAnimator.ofInt(0, mPercent);
        if (mPercent > 0) {
            progressBar.setIndeterminate(false);
            animator.setDuration(ANIM_DURATION);
            animator.addUpdateListener(animation -> {
                int animProgress = (Integer) animation.getAnimatedValue();
                progressBar.setProgress(animProgress);
            });
            animator.start();
        }

        if (mPercent >= 51) {
            progressBar.setProgressTintList(context.getColorStateList(R.color.battery_low));
            progressBar.setProgressBackgroundTintList(context.getColorStateList(R.color.battery_low));
        } else if (mPercent >= 20) {
            progressBar.setProgressTintList(context.getColorStateList(R.color.battery_medium));
            progressBar.setProgressBackgroundTintList(context.getColorStateList(R.color.battery_medium));
        } else if (mPercent <= 19) {
            progressBar.setProgressTintList(context.getColorStateList(R.color.battery_high));
            progressBar.setProgressBackgroundTintList(context.getColorStateList(R.color.battery_high));
        }
    }

    private CharSequence enlargeFontOfNumber(CharSequence summary) {
        if (TextUtils.isEmpty(summary)) {
            return "";
        }

        final Matcher matcher = mNumberPattern.matcher(summary);
        if (matcher.find()) {
            final SpannableString spannableSummary = new SpannableString(summary);
            spannableSummary.setSpan(new AbsoluteSizeSpan(64, true /* dip */), matcher.start(),
                    matcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            return spannableSummary;
        }
        return summary;
    }
}
