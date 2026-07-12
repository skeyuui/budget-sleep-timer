package com.sleeptimer.tv;

import java.util.Calendar;

/**
 * Data model for a sleep alarm schedule.
 */
public class Schedule {

    public static final int REPEAT_NONE = 0;
    public static final int REPEAT_DAILY = 0x7F;      // all 7 bits: Su-Sa
    public static final int REPEAT_WEEKDAYS = 0x3E;    // bits 1-5: Mo-Fr
    public static final int REPEAT_WEEKENDS = 0x41;    // bits 0,6: Su,Sa

    public int id;
    public int hour;       // 0–23
    public int minute;     // 0–59
    public int repeatDays; // bitmask: bit 0=Sun, 1=Mon, ..., 6=Sat. 0=one-shot
    public boolean enabled;
    public long nextFireTime;

    public Schedule() {
    }

    public Schedule(int id, int hour, int minute, int repeatDays, boolean enabled) {
        this.id = id;
        this.hour = hour;
        this.minute = minute;
        this.repeatDays = repeatDays;
        this.enabled = enabled;
        this.nextFireTime = computeNextFireTime();
    }

    public boolean isRecurring() {
        return repeatDays != REPEAT_NONE;
    }

    /**
     * Computes the next fire time from now.
     * For one-shot: if the time has passed today, schedules for tomorrow.
     * For recurring: finds the next matching day of week.
     */
    public long computeNextFireTime() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        long now = System.currentTimeMillis();

        if (repeatDays == REPEAT_NONE) {
            // One-shot: if time passed today, schedule tomorrow
            if (cal.getTimeInMillis() <= now) {
                cal.add(Calendar.DAY_OF_YEAR, 1);
            }
        } else {
            // Recurring: find the next day that matches the bitmask
            for (int i = 0; i < 8; i++) {
                if (cal.getTimeInMillis() > now) {
                    int dow = cal.get(Calendar.DAY_OF_WEEK); // 1=Sun, 2=Mon, ...
                    int bit = dow - 1; // 0=Sun, 1=Mon, ...
                    if ((repeatDays & (1 << bit)) != 0) {
                        break;
                    }
                }
                cal.add(Calendar.DAY_OF_YEAR, 1);
            }
        }

        this.nextFireTime = cal.getTimeInMillis();
        return this.nextFireTime;
    }

    public String getRepeatLabel() {
        if (repeatDays == REPEAT_NONE) return "Once";
        if (repeatDays == REPEAT_DAILY) return "Daily";
        if (repeatDays == REPEAT_WEEKDAYS) return "Weekdays";
        if (repeatDays == REPEAT_WEEKENDS) return "Weekends";

        StringBuilder sb = new StringBuilder();
        String[] days = {"Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"};
        for (int i = 0; i < 7; i++) {
            if ((repeatDays & (1 << i)) != 0) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(days[i]);
            }
        }
        return sb.toString();
    }

    public String getTimeString() {
        return String.format("%02d:%02d", hour, minute);
    }
}
