package com.tolino.custom.booklauncher.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.tolino.custom.booklauncher.LauncherActivity;
import com.tolino.custom.booklauncher.R;

/**
 * Created by WorldSkills2020 on 2/21/2020.
 */

public class AndroidUtils {
    public static int[] measureView(final View view) {
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        if (lp == null) {
            lp = new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
        int widthSpec = ViewGroup.getChildMeasureSpec(0, 0, lp.width);
        int lpHeight = lp.height;
        int heightSpec;
        if (lpHeight > 0) {
            heightSpec = View.MeasureSpec.makeMeasureSpec(lpHeight, View.MeasureSpec.EXACTLY);
        } else {
            heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        }
        view.measure(widthSpec, heightSpec);
        return new int[]{view.getMeasuredWidth(), view.getMeasuredHeight()};
    }

    public static void Msgbox(Context ctx,String message,String title,String okButton){
        new AlertDialog.Builder(ctx).setTitle(title).setMessage(message)
                .setPositiveButton(okButton, null).create().show();
    }

    public static void Toast(Context ctx,String message){
        Toast tw = Toast.makeText(ctx,message,Toast.LENGTH_LONG);
        View v = LayoutInflater.from(ctx).inflate(R.layout.frame_toast,null,false);
        ((TextView)v.findViewById(R.id.txtToast)).setText(message);
        tw.setView(v);
        tw.show();
    }

}
