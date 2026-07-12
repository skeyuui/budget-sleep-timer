package com.sleeptimer.tv;

import android.app.Activity;
import android.app.AlarmManager;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;

import java.util.Calendar;
import java.util.List;

public class MainActivity extends Activity {

    private ScheduleStore store;
    private NumberPicker hourPicker, minutePicker;
    private LinearLayout schedulesContainer;
    private TextView timerCountdown;
    private Button cancelTimerBtn;

    // Day selection: 0=Sun, 1=Mon, ..., 6=Sat
    private final boolean[] selectedDays = new boolean[7];
    private final int[] dayBtnIds = {
            R.id.btn_sun, R.id.btn_mon, R.id.btn_tue,
            R.id.btn_wed, R.id.btn_thu, R.id.btn_fri, R.id.btn_sat
    };
    private Button[] dayButtons;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tickRunnable = new Runnable() {
        @Override
        public void run() {
            updateTimerCountdown();
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        store = new ScheduleStore(this);

        // --- Time pickers ---
        hourPicker = findViewById(R.id.hour_picker);
        hourPicker.setMinValue(0);
        hourPicker.setMaxValue(23);
        hourPicker.setWrapSelectorWheel(true);
        hourPicker.setFormatter(new NumberPicker.Formatter() {
            @Override
            public String format(int value) {
                return String.format("%02d", value);
            }
        });

        minutePicker = findViewById(R.id.minute_picker);
        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(59);
        minutePicker.setWrapSelectorWheel(true);
        minutePicker.setFormatter(new NumberPicker.Formatter() {
            @Override
            public String format(int value) {
                return String.format("%02d", value);
            }
        });

        // Default to current time
        Calendar now = Calendar.getInstance();
        hourPicker.setValue(now.get(Calendar.HOUR_OF_DAY));
        minutePicker.setValue(now.get(Calendar.MINUTE));

        // --- Day toggle buttons ---
        dayButtons = new Button[7];
        for (int i = 0; i < 7; i++) {
            dayButtons[i] = findViewById(dayBtnIds[i]);
            final int day = i;
            dayButtons[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedDays[day] = !selectedDays[day];
                    updateDayButtonStyles();
                }
            });
        }

        // --- Day preset shortcuts ---
        findViewById(R.id.btn_preset_daily).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyDayPreset(Schedule.REPEAT_DAILY);
            }
        });
        findViewById(R.id.btn_preset_weekdays).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyDayPreset(Schedule.REPEAT_WEEKDAYS);
            }
        });
        findViewById(R.id.btn_preset_weekends).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyDayPreset(Schedule.REPEAT_WEEKENDS);
            }
        });

        // --- Set schedule ---
        findViewById(R.id.btn_set_schedule).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addSchedule();
            }
        });

        // --- Active schedules ---
        schedulesContainer = findViewById(R.id.schedules_container);

        // --- Quick timer ---
        setupTimerButton(R.id.btn_15, 15);
        setupTimerButton(R.id.btn_30, 30);
        setupTimerButton(R.id.btn_45, 45);
        setupTimerButton(R.id.btn_60, 60);
        setupTimerButton(R.id.btn_90, 90);
        setupTimerButton(R.id.btn_120, 120);

        timerCountdown = findViewById(R.id.timer_countdown);
        cancelTimerBtn = findViewById(R.id.btn_cancel_timer);
        cancelTimerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelTimer();
            }
        });

        updateDayButtonStyles();
        refreshSchedules();
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(tickRunnable);
        refreshSchedules();
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(tickRunnable);
    }

    // ===================== Day selection =====================

    private void applyDayPreset(int bitmask) {
        for (int i = 0; i < 7; i++) {
            selectedDays[i] = (bitmask & (1 << i)) != 0;
        }
        updateDayButtonStyles();
    }

    private void updateDayButtonStyles() {
        int sel = getResources().getColor(R.color.accent);
        int unsel = getResources().getColor(R.color.surface);
        for (int i = 0; i < 7; i++) {
            dayButtons[i].setBackgroundColor(selectedDays[i] ? sel : unsel);
        }
    }

    private int getRepeatBitmask() {
        int mask = 0;
        for (int i = 0; i < 7; i++) {
            if (selectedDays[i]) mask |= (1 << i);
        }
        return mask;
    }

    // ===================== Schedule management =====================

    private void addSchedule() {
        int hour = hourPicker.getValue();
        int minute = minutePicker.getValue();
        int repeat = getRepeatBitmask();

        Schedule schedule = store.add(hour, minute, repeat);
        SleepReceiver.armAlarm(this, schedule);
        refreshSchedules();
    }

    private void refreshSchedules() {
        schedulesContainer.removeAllViews();
        List<Schedule> schedules = store.getAll();

        if (schedules.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No schedules");
            empty.setTextSize(16);
            empty.setTextColor(getResources().getColor(R.color.text_secondary));
            schedulesContainer.addView(empty);
            return;
        }

        for (final Schedule s : schedules) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(16), dp(10), dp(16), dp(10));
            row.setBackgroundColor(getResources().getColor(R.color.surface));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(6);
            row.setLayoutParams(lp);

            // Time
            TextView time = new TextView(this);
            time.setText(s.getTimeString());
            time.setTextSize(22);
            time.setTextColor(getResources().getColor(R.color.text_primary));
            time.setTypeface(Typeface.MONOSPACE);
            time.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            row.addView(time);

            // Repeat label
            TextView repeat = new TextView(this);
            repeat.setText(s.getRepeatLabel());
            repeat.setTextSize(14);
            repeat.setTextColor(getResources().getColor(R.color.text_secondary));
            repeat.setPadding(dp(8), 0, dp(12), 0);
            row.addView(repeat);

            // Remove button
            Button rm = new Button(this);
            rm.setText("\u2715");
            rm.setTextSize(14);
            rm.setTextColor(getResources().getColor(R.color.text_primary));
            rm.setBackgroundColor(getResources().getColor(R.color.cancel));
            rm.setMinimumWidth(dp(44));
            rm.setMinimumHeight(dp(36));
            rm.setFocusable(true);
            rm.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SleepReceiver.disarmAlarm(MainActivity.this, s.id);
                    store.remove(s.id);
                    refreshSchedules();
                }
            });
            row.addView(rm);

            schedulesContainer.addView(row);
        }
    }

    // ===================== Quick timer =====================

    private void setupTimerButton(int id, final int minutes) {
        findViewById(id).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setQuickTimer(minutes);
            }
        });
    }

    private void setQuickTimer(int minutes) {
        long target = System.currentTimeMillis() + (minutes * 60 * 1000L);
        store.setTimerTarget(target);
        SleepReceiver.armTimer(this, target);
        updateTimerCountdown();
    }

    private void cancelTimer() {
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        am.cancel(SleepReceiver.getTimerPendingIntent(this));
        store.clearTimerTarget();
        timerCountdown.setVisibility(View.GONE);
        cancelTimerBtn.setVisibility(View.GONE);
    }

    private void updateTimerCountdown() {
        long target = store.getTimerTarget();
        if (target == 0 || target <= System.currentTimeMillis()) {
            if (target != 0) store.clearTimerTarget();
            timerCountdown.setVisibility(View.GONE);
            cancelTimerBtn.setVisibility(View.GONE);
            return;
        }

        long rem = target - System.currentTimeMillis();
        long h = rem / 3600000;
        long m = (rem % 3600000) / 60000;
        long s = (rem % 60000) / 1000;

        timerCountdown.setText(h > 0
                ? String.format("%d:%02d:%02d", h, m, s)
                : String.format("%02d:%02d", m, s));
        timerCountdown.setVisibility(View.VISIBLE);
        cancelTimerBtn.setVisibility(View.VISIBLE);
    }

    private int dp(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }
}
