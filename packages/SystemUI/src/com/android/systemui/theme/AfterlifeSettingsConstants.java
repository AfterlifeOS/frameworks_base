/*
 * Copyright (C) 2023-2024 The RisingOS Android Project
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
package com.android.systemui.theme;

import android.provider.Settings;

public class AfterlifeSettingsConstants {
    public static final String LOCKSCREEN_WIDGETS_ENABLED = "lockscreen_widgets_enabled";
    public static final String LOCKSCREEN_WIDGETS = "lockscreen_widgets";
    public static final String LOCKSCREEN_WIDGETS_EXTRAS = "lockscreen_widgets_extras";
    public static final String USER_SELECTED_RESOLUTION = "user_selected_resolution";
    public static final String STATUS_BAR_BATTERY_STYLE = Settings.System.STATUS_BAR_BATTERY_STYLE;
    public static final String QS_BATTERY_STYLE = Settings.System.QS_BATTERY_STYLE;
    public static final String QS_SHOW_BATTERY_PERCENT = Settings.System.QS_SHOW_BATTERY_PERCENT;
    public static final String CLOCK_STYLE = "clock_style";

    public static final String[] SYSTEM_SETTINGS_KEYS = {
        LOCKSCREEN_WIDGETS_ENABLED,
        LOCKSCREEN_WIDGETS,
        LOCKSCREEN_WIDGETS_EXTRAS,
        USER_SELECTED_RESOLUTION,
        STATUS_BAR_BATTERY_STYLE,
        QS_BATTERY_STYLE,
        QS_SHOW_BATTERY_PERCENT,
        CLOCK_STYLE
    };
}