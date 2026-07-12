package com.sleeptimer.tv;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.List;

/**
 * Re-arms all enabled alarms after device reboot.
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }

        ScheduleStore store = new ScheduleStore(context);
        List<Schedule> schedules = store.getAll();

        for (Schedule s : schedules) {
            if (s.enabled) {
                s.computeNextFireTime();
                store.update(s);
                SleepReceiver.armAlarm(context, s);
            }
        }

        // Re-arm quick timer if it's still in the future
        long timerTarget = store.getTimerTarget();
        if (timerTarget > System.currentTimeMillis()) {
            SleepReceiver.armTimer(context, timerTarget);
        } else if (timerTarget != 0) {
            store.clearTimerTarget();
        }
    }
}
