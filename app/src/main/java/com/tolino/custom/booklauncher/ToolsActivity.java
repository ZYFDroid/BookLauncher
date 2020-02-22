package com.tolino.custom.booklauncher;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.tolino.custom.booklauncher.tools.FtpActivity;
import com.tolino.custom.booklauncher.utils.AndroidUtils;

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;

import java.lang.reflect.Method;
import java.util.List;

public class ToolsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tools);
    }

    public static void expandStatusBar(Context context) {
        try {
            Object statusBarManager = context.getSystemService("statusbar");
            Method collapse;
            if (Build.VERSION.SDK_INT <= 16) {
                collapse = statusBarManager.getClass().getMethod("expand");

            } else {
                collapse = statusBarManager.getClass().getMethod("expandPanels");
            }
            collapse.invoke(statusBarManager);
        } catch (Exception localException) {
            localException.printStackTrace();
            AndroidUtils.Toast(context,"您的安卓版本不支持此功能");
        }
    }

    Handler hWnd = new Handler();

    public void onTorchClick(View view) {
        Window window = this.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL;
        window.setAttributes(lp);
        Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS,255);
        hWnd.postDelayed(new Runnable() {
            @Override
            public void run() {
                Window window = ToolsActivity.this.getWindow();
                WindowManager.LayoutParams lp = window.getAttributes();
                lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
                window.setAttributes(lp);
            }
        },500);
    }

    public void onNotificationClick(View view) {
        expandStatusBar(this);
    }

    private static final String TAG = "ToolActivity";

    public void cleanMemory(View v){
        killAll();
    }

    public void killAll(){

        if(Build.VERSION.SDK_INT >= 21){
            AndroidUtils.Toast(this,"您的安卓版本不支持此功能");
            return;
        }

        ActivityManager activityManager = (ActivityManager)
                getSystemService(Context.ACTIVITY_SERVICE);
        //获取系统中所有正在运行的进程
        List<ActivityManager.RunningAppProcessInfo> appProcessInfos = activityManager
                .getRunningAppProcesses();
        int count=0;//被杀进程计数
        String nameList="";//记录被杀死进程的包名
        long beforeMem = getAvailMemory(this);//清理前的可用内存
        Log.i(TAG, "清理前可用内存为 : " + beforeMem);

        for (ActivityManager.RunningAppProcessInfo appProcessInfo:appProcessInfos) {
            nameList="";
            if( appProcessInfo.processName.contains("com.android.system")
                    ||appProcessInfo.pid==android.os.Process.myPid())//跳过系统 及当前进程
                continue;
            String[] pkNameList=appProcessInfo.pkgList;//进程下的所有包名
            for(int i=0;i<pkNameList.length;i++){
                String pkName=pkNameList[i];
                activityManager.killBackgroundProcesses(pkName);//杀死该进程
                count++;//杀死进程的计数+1
                nameList+="  "+pkName;
            }
            Log.i(TAG, nameList+"---------------------");
        }

        long afterMem = getAvailMemory(this);//清理后的内存占用
        AndroidUtils.Msgbox(this,"杀死 " + (count+1) + " 个进程, "
                + formatFileSize(afterMem - beforeMem),"内存清理","关闭");

        Log.i(TAG, "清理后可用内存为 : " + afterMem);
        Log.i(TAG, "清理进程数量为 : " + count+1);
    }


    /*
     * *获取可用内存大小
     */
    private long getAvailMemory(Context context) {
        // 获取android当前可用内存大小
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        return mi.availMem;
    }

    /*
     * *字符串转换 long-string KB/MB
     */
    private String formatFileSize(long number){
        if(number>0) {
            return "释放"+Formatter.formatFileSize(this, number)+"内存";
        }else if(number<0) {
            return "失去"+Formatter.formatFileSize(this, -number)+"内存";
        }else{
            return "什么都没有发生";
        }
    }

    public void onAboutClick(View view) {
        AndroidUtils.Msgbox(this,"暂无","关于","关闭");
    }

    public void onBackClick(View view) {
        finish();
    }

    public void openFtp(View view) {
        startActivity(new Intent(this, FtpActivity.class));
    }
}
