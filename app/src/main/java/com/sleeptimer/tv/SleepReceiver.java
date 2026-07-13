package com.sleeptimer.tv;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/**
 * Fires when a scheduled alarm or quick timer triggers.
 * Sends the sleep command via ADB, then re-arms recurring schedules.
 */
public class SleepReceiver extends BroadcastReceiver {

    static final String EXTRA_SCHEDULE_ID = "schedule_id";
    static final String EXTRA_IS_TIMER = "is_timer";
    private static final int ADB_PORT = 5555;

    @Override
    public void onReceive(Context context, Intent intent) {
        final PendingResult result = goAsync(); // keep process alive for network call

        boolean isTimer = intent.getBooleanExtra(EXTRA_IS_TIMER, false);
        int scheduleId = intent.getIntExtra(EXTRA_SCHEDULE_ID, -1);

        final ScheduleStore store = new ScheduleStore(context);
        final Context ctx = context.getApplicationContext();

        if (isTimer) {
            store.clearTimerTarget();
        } else if (scheduleId >= 0) {
            Schedule schedule = store.getById(scheduleId);
            if (schedule != null && schedule.isRecurring()) {
                schedule.computeNextFireTime();
                store.update(schedule);
                armAlarm(ctx, schedule);
            } else if (schedule != null) {
                store.remove(scheduleId); // one-shot: done
            }
        }

        // Send sleep command on background thread (network not allowed on main)
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    AdbCrypto.AdbKeyPair keys = AdbCrypto.loadOrGenerateKeys(ctx);
                    
                    int action = store.getGlobalAction();
                    String cmd = "input keyevent 223"; // STANDBY
                    if (action == ScheduleStore.ACTION_POWER) {
                        cmd = "input keyevent 26";
                    } else if (action == ScheduleStore.ACTION_HIBERNATE) {
                        cmd = "input keyevent 276";
                    }

                    if (!AdbClient.sendShellCommand(ADB_PORT, cmd, keys)) {
                        // Fallback to direct shell exec
                        try {
                            Runtime.getRuntime().exec(new String[]{"sh", "-c", cmd});
                        } catch (Exception e) {}
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    result.finish();
                }
            }
        }).start();
    }

    // --- Static helpers used by MainActivity, BootReceiver ---

    public static void armAlarm(Context context, Schedule schedule) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = getSchedulePendingIntent(context, schedule.id);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, schedule.nextFireTime, pi);
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, schedule.nextFireTime, pi);
        }
    }

    public static void armTimer(Context context, long targetTime) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = getTimerPendingIntent(context);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetTime, pi);
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, targetTime, pi);
        }
    }

    public static void disarmAlarm(Context context, int scheduleId) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(getSchedulePendingIntent(context, scheduleId));
    }

    public static PendingIntent getTimerPendingIntent(Context context) {
        Intent intent = new Intent(context, SleepReceiver.class);
        intent.putExtra(EXTRA_IS_TIMER, true);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getBroadcast(context, 0, intent, flags);
    }

    static PendingIntent getSchedulePendingIntent(Context context, int scheduleId) {
        Intent intent = new Intent(context, SleepReceiver.class);
        intent.putExtra(EXTRA_SCHEDULE_ID, scheduleId);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        // Offset request code to avoid collision with timer (requestCode=0)
        return PendingIntent.getBroadcast(context, scheduleId + 100, intent, flags);
    }
}
