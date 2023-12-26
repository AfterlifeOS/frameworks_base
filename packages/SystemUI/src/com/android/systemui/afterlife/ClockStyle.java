package com.android.systemui.afterlife;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;
import com.android.systemui.R;

public class ClockStyle extends RelativeLayout {
	
	private Context mContext;
	private View clockOplus, clockIos, clockCos, clockCustom, clockCustom1, clockCustom2, clockCustom3;
	
	public ClockStyle(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
	}
	
	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		clockOplus = findViewById(R.id.keyguard_clock_style_oos);
		clockIos = findViewById(R.id.keyguard_clock_style_ios);
		clockCos = findViewById(R.id.keyguard_clock_style_cos);
		clockCustom = findViewById(R.id.keyguard_clock_style_custom);
		clockCustom1 = findViewById(R.id.keyguard_clock_style_custom1);
		clockCustom2 = findViewById(R.id.keyguard_clock_style_custom2);
		clockCustom3 = findViewById(R.id.keyguard_clock_style_custom3);
		new MyContentObserver(new Handler()).observe();
		updateClockView();
	}
	
	private void updateClockView() {
		int clockStyle = Settings.System.getInt(mContext.getContentResolver(), "clock_style", 0);
		if (clockStyle == 1) {
			clockOplus.setVisibility(View.GONE);
			clockIos.setVisibility(View.VISIBLE);
			clockCos.setVisibility(View.GONE);
			clockCustom.setVisibility(View.GONE);
			clockCustom1.setVisibility(View.GONE);
			clockCustom2.setVisibility(View.GONE);
			clockCustom3.setVisibility(View.GONE);
		} else if (clockStyle == 2) {
			clockOplus.setVisibility(View.GONE);
			clockIos.setVisibility(View.GONE);
			clockCos.setVisibility(View.VISIBLE);
			clockCustom.setVisibility(View.GONE);
			clockCustom1.setVisibility(View.GONE);
			clockCustom2.setVisibility(View.GONE);
			clockCustom3.setVisibility(View.GONE);
		} else if (clockStyle == 3) {
			clockOplus.setVisibility(View.GONE);
			clockIos.setVisibility(View.GONE);
			clockCos.setVisibility(View.GONE);
			clockCustom.setVisibility(View.VISIBLE);
			clockCustom1.setVisibility(View.GONE);
			clockCustom2.setVisibility(View.GONE);
			clockCustom3.setVisibility(View.GONE);
		} else if (clockStyle == 4) {
			clockOplus.setVisibility(View.GONE);
			clockIos.setVisibility(View.GONE);
			clockCos.setVisibility(View.GONE);
			clockCustom.setVisibility(View.GONE);
			clockCustom1.setVisibility(View.VISIBLE);
			clockCustom2.setVisibility(View.GONE);
			clockCustom3.setVisibility(View.GONE);
		} else if (clockStyle == 5) {
			clockOplus.setVisibility(View.GONE);
			clockIos.setVisibility(View.GONE);
			clockCos.setVisibility(View.GONE);
			clockCustom.setVisibility(View.GONE);
			clockCustom1.setVisibility(View.GONE);
			clockCustom2.setVisibility(View.VISIBLE);
			clockCustom3.setVisibility(View.GONE);
		} else if (clockStyle == 6) {
			clockOplus.setVisibility(View.GONE);
			clockIos.setVisibility(View.GONE);
			clockCos.setVisibility(View.GONE);
			clockCustom.setVisibility(View.GONE);
			clockCustom1.setVisibility(View.GONE);
			clockCustom2.setVisibility(View.GONE);
			clockCustom3.setVisibility(View.VISIBLE);
		} else {
			clockOplus.setVisibility(View.VISIBLE);
			clockIos.setVisibility(View.GONE);
			clockCos.setVisibility(View.GONE);
			clockCustom.setVisibility(View.GONE);
			clockCustom1.setVisibility(View.GONE);
			clockCustom2.setVisibility(View.GONE);
			clockCustom3.setVisibility(View.GONE);
		}
	}
	
	class MyContentObserver extends ContentObserver {
		public MyContentObserver(Handler h) {
			super(h);
		}
		
		public void observe() {
			ContentResolver cr = mContext.getContentResolver();
			cr.registerContentObserver(Settings.System.getUriFor("clock_style"), false, this);
		}
		
		@Override
		public void onChange(boolean selfChange) {
			updateClockView();
		}
    }
}