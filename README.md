# Budget Sleeper
## Sleep Scheduler for Android TV

Minimal Android TV app that puts your TV to sleep on a schedule.

**"Turn off every Friday at 1 AM"** · **"Turn off every day at 4 AM"** · **"Sleep in 30 minutes"**

## How it works

1. Set a time with the hour/minute picker
2. Toggle which days of the week (or use Daily/Weekdays/Weekends shortcuts)
3. Hit "Set Schedule" — done

When the alarm fires, the app connects to the TV's own ADB daemon on `localhost:5555` and sends `input keyevent 223` (KEYCODE_SLEEP). If ADB isn't available, it falls back to `Runtime.exec()`.

## Architecture

```
MainActivity ──▶ AlarmManager.setExactAndAllowWhileIdle()
                         │
                    (alarm fires)
                         │
                  SleepReceiver ──▶ AdbClient (TCP localhost:5555)
                         │              │
                         │         CNXN → OPEN "shell:input keyevent 223"
                         │              │
                    (if recurring)      ▼
                         │         TV goes to sleep
                  re-arms next occurrence
```

```
BootReceiver ──▶ re-arms all active schedules after reboot
```

### Source files

| File | Lines | Purpose |
|------|-------|---------|
| `AdbClient.java` | ~130 | Minimal ADB protocol over TCP |
| `Schedule.java` | ~90 | Data model with day-of-week bitmask |
| `ScheduleStore.java` | ~100 | SharedPreferences + org.json persistence |
| `MainActivity.java` | ~230 | Scheduler UI with NumberPickers + day toggles |
| `SleepReceiver.java` | ~100 | Alarm handler, ADB exec, recurrence re-arm |
| `BootReceiver.java` | ~30 | Re-arms schedules after reboot |

**0 external dependencies** — pure `android.*` + `java.*` + `org.json` (built into Android).

## Requirements

- Android TV device (API 21+)
- ADB TCP enabled on the device (most budget TVs have this on `localhost:5555` by default)
- If ADB TCP isn't available, the app falls back to `Runtime.exec("input keyevent 223")` which works on rooted devices

## Build

```bash
./gradlew assembleRelease
```

APK: `app/build/outputs/apk/release/app-release-unsigned.apk`

## Install

```bash
adb install app/build/outputs/apk/release/app-release-unsigned.apk
```

## License

MIT
