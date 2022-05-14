package com.zark.bbandroid.utils;

import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

public final class CommonHelper {
   public static void showMessage(final AppCompatActivity activity, final CharSequence msg) {
      if (activity == null || TextUtils.isEmpty(msg)) return;

      activity.runOnUiThread(new Runnable() {
         @Override
         public void run() {
            Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show();
         }
      });
   }

   public static void showMessage(final AppCompatActivity activity, @StringRes int msgId) {
      if (activity != null)
         showMessage(activity, activity.getString(msgId));
   }
}
