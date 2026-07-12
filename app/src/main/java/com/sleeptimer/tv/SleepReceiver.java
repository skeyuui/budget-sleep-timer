package com.sleeptimer.tv;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class SleepReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // Clear stored timer
        context.getSharedPreferences("sleep_prefs", Context.MODE_PRIVATE)
                .edit()
                .remove("target_time")
                .apply();

        // Send sleep keyevent via shell (equivalent to: adb shell input keyevent 223)
        try {
            Runtime.getRuntime().exec(new String[]{"sh", "-c", "input keyevent 223"});
        } catch (Exception e) {
            // Fallback: KEYCODE_POWER (26) toggles sleep on most TVs
            try {
                Runtime.getRuntime().exec(new String[]{"sh", "-c", "input keyevent 26"});
            } catch (Exception ignored) {
            }
        }
    }
}
