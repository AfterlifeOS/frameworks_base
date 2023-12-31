package com.android.systemui.afterlife;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings.System;
import android.provider.Settings;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.view.*;
import android.net.Uri;
import java.util.Calendar;
import android.content.res.Configuration;
import android.content.BroadcastReceiver;
import com.android.systemui.R;

public class TimeHeaderImage
  extends LinearLayout
{
	private static final int TIME_PAGI = 6;
	private static final int TIME_SIANG = 11;
	private static final int TIME_SORE = 15;
	private static final int TIME_PETANG = 18;
	private static final int TIME_MALAM = 19;
	private static final int TIME_DINI = 1;
	private static final int TIME_FAJAR = 4;
	private static final Calendar CAL_AKHIR_TAHUN = Calendar.getInstance();
	private static final Calendar CAL_AWAL_TAHUN = Calendar.getInstance();
	private static BroadcastReceiver tickReceiver;
    private boolean mAttached, mImageHeaderEnabled;
  
  public TimeHeaderImage(Context paramContext, AttributeSet paramAttributeSet)
  {
    super(paramContext, paramAttributeSet);
    
    tickReceiver=new BroadcastReceiver(){
    @Override
    public void onReceive(Context context, Intent intent) {
    if(intent.getAction().compareTo(Intent.ACTION_TIME_TICK)==0)
    {
      updateBackground();
    }
    
    }
  };

    CAL_AKHIR_TAHUN.set(Calendar.MONTH, 11);
    CAL_AKHIR_TAHUN.set(Calendar.DAY_OF_MONTH, 31);
    CAL_AWAL_TAHUN.set(Calendar.MONTH, 0);
    CAL_AWAL_TAHUN.set(Calendar.DAY_OF_MONTH, 1);
  }
  
  @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
		new SettingsObserver(new Handler()).observe();
		updateBackground();
	}
	
  private static boolean isItToday(Calendar paramCalendar)
  {
    Calendar localCalendar = Calendar.getInstance();
    return (localCalendar.get(Calendar.MONTH) == paramCalendar.get(Calendar.MONTH)) && (localCalendar.get(Calendar.DAY_OF_MONTH) == paramCalendar.get(Calendar.DAY_OF_MONTH));
  }
  
  private void updateBackground() {
    ContentResolver resolver = mContext.getContentResolver();
		mImageHeaderEnabled = Settings.System.getIntForUser(resolver, "header_image", 0, UserHandle.USER_CURRENT) == 1;
	if (mImageHeaderEnabled)
    {
       setVisibility(View.VISIBLE);
    } else 
    {
        setVisibility(View.GONE);
    } 
    String str = "imageHeader";
    
    if (isItToday(CAL_AKHIR_TAHUN) || isItToday(CAL_AWAL_TAHUN)) {
      str = "header_tahun_baru";
      setBackground(getResources().getDrawable(R.drawable.header_tahun_baru));
    }
    int i = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
      if ((i >= TIME_FAJAR) && (i < TIME_PAGI)) {
        str = "header_fajar";
        setBackground(getResources().getDrawable(R.drawable.header_fajar));
      } else if ((i >= TIME_PAGI) && (i < TIME_SIANG)) {
        str = "header_pagi";
        setBackground(getResources().getDrawable(R.drawable.header_pagi));
      } else if ((i >= TIME_SIANG) && (i < TIME_SORE)) {
        str = "header_siang";
        setBackground(getResources().getDrawable(R.drawable.header_siang));

      } else if ((i >= TIME_SORE) && (i < TIME_PETANG)) {
        str = "header_sore";
        setBackground(getResources().getDrawable(R.drawable.header_sore));

      } else if ((i >= TIME_PETANG) && (i < TIME_MALAM)) {
        str = "header_petang";
        setBackground(getResources().getDrawable(R.drawable.header_petang));

      } else if ((i >= TIME_MALAM) && (i < TIME_DINI)) {
        str = "header_malam";
        setBackground(getResources().getDrawable(R.drawable.header_malam));

      } else if ((i >= TIME_DINI) && (i < TIME_FAJAR)) {
        str = "header_dini";
        setBackground(getResources().getDrawable(R.drawable.header_dini));
      }
      
  }
  
  @Override
  protected void onAttachedToWindow()
  {
    super.onAttachedToWindow();
    if (!mAttached)
    {
      mAttached = true;
      //Register the broadcast receiver to receive TIME_TICK
      mContext.registerReceiver(tickReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));
      IntentFilter localIntentFilter = new IntentFilter();
      localIntentFilter.addAction("android.intent.action.TIME_TICK");
      localIntentFilter.addAction("android.intent.action.TIME_SET");
      localIntentFilter.addAction("android.intent.action.TIMEZONE_CHANGED");
      updateBackground();
     }
  }
  
  @Override
  protected void onDetachedFromWindow()
  {
    super.onDetachedFromWindow();
    if (mAttached)
    {
  	mAttached = false;
      //unregister broadcast receiver.
      if(tickReceiver!=null)
      mContext.unregisterReceiver(tickReceiver);
      IntentFilter localIntentFilter = new IntentFilter();
      localIntentFilter.addAction("android.intent.action.TIME_TICK");
      localIntentFilter.addAction("android.intent.action.TIME_SET");
      localIntentFilter.addAction("android.intent.action.TIMEZONE_CHANGED");
    }
  }
  
  @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setAlpha(0);
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            setAlpha(1);
        }
    }
    
    public final class SettingsObserver extends ContentObserver {
		public SettingsObserver(Handler handler) {
			super(handler);
		}
		
		public final void observe() {
			ContentResolver contentResolver = mContext.getContentResolver();
			contentResolver.registerContentObserver(Settings.System.getUriFor("header_image"), false, this);
		}
		
		@Override
		public void onChange(boolean selfChange) {
			updateBackground();
		}
    }
}