# Sleep Timer for Android TV

Minimal Android TV app that puts your TV to sleep after a set duration.

## How it works

1. Pick a preset duration (15 / 30 / 45 / 60 / 90 / 120 min)
2. App schedules an exact alarm via `AlarmManager`
3. When the alarm fires, a `BroadcastReceiver` runs `input keyevent 223` (`KEYCODE_SLEEP`) via shell
4. TV goes to sleep

No background services. No libraries. No dependencies beyond the Android SDK.

## Architecture

```
MainActivity ──▶ AlarmManager.setExactAndAllowWhileIdle()
                         │
                    (alarm fires)
                         │
                  SleepReceiver ──▶ Runtime.exec("input keyevent 223")
```

- **2 Java files**: `MainActivity.java`, `SleepReceiver.java`
- **0 dependencies**: pure `android.*` APIs
- **Theme**: `Theme.Material.NoActionBar` (built into the framework)

## Requirements

- Android TV device (API 21+)
- The device must allow `input keyevent` execution from app context.
  Most Android TV boxes (especially rooted ones) support this out of the box.
  If `keyevent 223` doesn't work on your device, the app falls back to `keyevent 26` (POWER).

## Build

```bash
./gradlew assembleRelease
```

APK output: `app/build/outputs/apk/release/app-release-unsigned.apk`

## Install via ADB

```bash
adb install app/build/outputs/apk/release/app-release-unsigned.apk
```

## License

MIT
