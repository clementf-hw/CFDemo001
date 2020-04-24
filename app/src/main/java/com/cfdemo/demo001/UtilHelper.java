package com.cfdemo.demo001;

import android.app.Activity;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

public class UtilHelper {
    public static void showText(final Activity activity, final String input) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                View txtLogView = activity.findViewById(R.id.txt_display);
                if (txtLogView instanceof TextView) {
                    ((TextView) txtLogView).setText(input);
                }
            }
        });
    }

    public static boolean stringVerify (String input) {
        if (input == null || TextUtils.isEmpty(input)){
            return false;
        }
        return true;
    }

}
