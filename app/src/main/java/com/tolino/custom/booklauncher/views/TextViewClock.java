package com.tolino.custom.booklauncher.views;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.tolino.custom.booklauncher.utils.LunarCalendar;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by WorldSkills2020 on 2/18/2020.
 */

public class TextViewClock extends TextView {
    public static final String TAG="TextViewClock";

    public TextViewClock(Context context) {
        super(context);
    }

    public TextViewClock(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TextViewClock(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    LunarCalendar lunar = null;
    Calendar solar = Calendar.getInstance();
    Handler hWnd = new Handler();

    Runnable updateTimeRunnable = new Runnable() {
        @Override
        public void run() {
            solar.setTimeInMillis(System.currentTimeMillis());
            Date d = solar.getTime();

            if(null!=getTag() && getTag().toString().equals("sdf1")){
                setText(sdf1.format(d));
                hWnd.postDelayed(this,1000);
            }else if(null!=getTag() && getTag().toString().equals("sdf2")){
                setText(sdf2.format(d));
                hWnd.postDelayed(this,60000);
            }else if(null!=getTag() && getTag().toString().equals("sdf3")){
                setText(sdf3.format(d));
                hWnd.postDelayed(this,60000);
            }else if(null!=getTag() && getTag().toString().equals("lunar")){
                try {
                    lunar = LunarCalendar.solar2Lunar(solar);
                    setText(lunar.getGeneralName().trim());
                }catch (Exception ex){
                    setText("农历超出范围");
                }
                hWnd.postDelayed(this,60000);
            }else{
                setText(sdf1.format(d));
                hWnd.postDelayed(this,1000);
            }
            Log.d(TAG, "run: update time");

        }
    };
    SimpleDateFormat sdf1 = new SimpleDateFormat("HH:mm");
    SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy年MM月dd日");
    SimpleDateFormat sdf3 = new SimpleDateFormat("EE");

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        setTimerState(true);
        Log.d(TAG, "onAttachedToWindow");
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        setTimerState(false);
        Log.d(TAG, "onDetachedFromWindow");
    }

    void setTimerState(boolean state){
        if(state){
            hWnd.post(updateTimeRunnable);
        }
        else{
            hWnd.removeCallbacks(updateTimeRunnable);
        }
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        setTimerState(visibility == VISIBLE);
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        setTimerState(visibility == VISIBLE);
    }
}
