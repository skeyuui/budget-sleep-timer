package com.sleeptimer.tv;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * SharedPreferences-backed store for sleep schedules.
 * Uses org.json (built into Android SDK) for serialization — no external libraries.
 */
public class ScheduleStore {

    private static final String PREFS = "schedules";
    private static final String KEY_LIST = "schedule_list";
    private static final String KEY_NEXT_ID = "next_id";
    private static final String KEY_TIMER_TARGET = "timer_target";

    private final SharedPreferences prefs;

    public ScheduleStore(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public List<Schedule> getAll() {
        List<Schedule> list = new ArrayList<>();
        String json = prefs.getString(KEY_LIST, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                Schedule s = new Schedule();
                s.id = obj.getInt("id");
                s.hour = obj.getInt("hour");
                s.minute = obj.getInt("minute");
                s.repeatDays = obj.getInt("repeat");
                s.enabled = obj.getBoolean("enabled");
                s.nextFireTime = obj.optLong("nextFire", 0);
                list.add(s);
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    public void saveAll(List<Schedule> list) {
        JSONArray arr = new JSONArray();
        try {
            for (Schedule s : list) {
                JSONObject obj = new JSONObject();
                obj.put("id", s.id);
                obj.put("hour", s.hour);
                obj.put("minute", s.minute);
                obj.put("repeat", s.repeatDays);
                obj.put("enabled", s.enabled);
                obj.put("nextFire", s.nextFireTime);
                arr.put(obj);
            }
        } catch (Exception ignored) {
        }
        prefs.edit().putString(KEY_LIST, arr.toString()).apply();
    }

    private int nextId() {
        int id = prefs.getInt(KEY_NEXT_ID, 1);
        prefs.edit().putInt(KEY_NEXT_ID, id + 1).apply();
        return id;
    }

    public Schedule add(int hour, int minute, int repeatDays) {
        List<Schedule> list = getAll();
        Schedule s = new Schedule(nextId(), hour, minute, repeatDays, true);
        list.add(s);
        saveAll(list);
        return s;
    }

    public void remove(int id) {
        List<Schedule> list = getAll();
        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i).id == id) {
                list.remove(i);
                break;
            }
        }
        saveAll(list);
    }

    public Schedule getById(int id) {
        for (Schedule s : getAll()) {
            if (s.id == id) return s;
        }
        return null;
    }

    public void update(Schedule schedule) {
        List<Schedule> list = getAll();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id == schedule.id) {
                list.set(i, schedule);
                break;
            }
        }
        saveAll(list);
    }

    // --- Quick timer target ---

    public void setTimerTarget(long targetTime) {
        prefs.edit().putLong(KEY_TIMER_TARGET, targetTime).apply();
    }

    public long getTimerTarget() {
        return prefs.getLong(KEY_TIMER_TARGET, 0);
    }

    public void clearTimerTarget() {
        prefs.edit().remove(KEY_TIMER_TARGET).apply();
    }
}
