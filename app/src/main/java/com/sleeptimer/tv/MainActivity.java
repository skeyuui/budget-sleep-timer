package com.sleeptimer.tv;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {

    private static final String PREFS = "sleep_prefs";
    private static final String KEY_TARGET = "target_time";

    private TextView statusText;
    private Button cancelButton;
    private SharedPreferences prefs;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable tickRunnable = new Runnable() {
        @Override
        public void run() {
            updateCountdown();
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        statusText = findViewById(R.id.status_text);
        cancelButton = findViewById(R.id.btn_cancel);

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelTimer();
            }
        });

        setupButton(R.id.btn_15, 15);
        setupButton(R.id.btn_30, 30);
        setupButton(R.id.btn_45, 45);
        setupButton(R.id.btn_60, 60);
        setupButton(R.id.btn_90, 90);
        setupButton(R.id.btn_120, 120);
    }

    private void setupButton(int id, final int minutes) {
        findViewById(id).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTimer(minutes);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(tickRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(tickRunnable);
    }

    private void setTimer(int minutes) {
        long targetTime = System.currentTimeMillis() + (minutes * 60 * 1000L);
        prefs.edit().putLong(KEY_TARGET, targetTime).apply();

        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = getSleepPendingIntent();

        // Cancel any existing alarm first
        am.cancel(pi);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetTime, pi);
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, targetTime, pi);
        }

        updateCountdown();
    }

    private void cancelTimer() {
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        am.cancel(getSleepPendingIntent());
        prefs.edit().remove(KEY_TARGET).apply();
        statusText.setText("No timer set");
        cancelButton.setVisibility(View.GONE);
    }

    private void updateCountdown() {
        long target = prefs.getLong(KEY_TARGET, 0);
        if (target == 0) {
            statusText.setText("No timer set");
            cancelButton.setVisibility(View.GONE);
            return;
        }

        long remaining = target - System.currentTimeMillis();
        if (remaining <= 0) {
            prefs.edit().remove(KEY_TARGET).apply();
            statusText.setText("No timer set");
            cancelButton.setVisibility(View.GONE);
            return;
        }

        long h = remaining / 3600000;
        long m = (remaining % 3600000) / 60000;
        long s = (remaining % 60000) / 1000;

        if (h > 0) {
            statusText.setText(String.format("%d:%02d:%02d", h, m, s));
        } else {
            statusText.setText(String.format("%02d:%02d", m, s));
        }
        cancelButton.setVisibility(View.VISIBLE);
    }

    private PendingIntent getSleepPendingIntent() {
        Intent intent = new Intent(this, SleepReceiver.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getBroadcast(this, 0, intent, flags);
    }
}
