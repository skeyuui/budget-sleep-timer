# Budget Sleep Timer for Android TV 📺
[Leia em Português](README.pt-BR.md)

A minimal, ultra-lightweight Android TV app that puts your TV to sleep on a schedule. No bloated libraries, no trackers, just pure Android code.

**"Turn off every Friday at 1 AM"** · **"Turn off every day at 4 AM"** · **"Sleep in 30 minutes"**

## How it works

1. On first run, allow the app to have ADB access on your TV.
2. Set a time and toggle the days of the week (or use Daily/Weekdays/Weekends shortcuts).
3. Hit "Set Schedule" — done!

When the alarm fires, the app connects to the TV's internal ADB daemon on `localhost:5555` and sends the sleep command (`input keyevent 223`). If ADB isn't available, it falls back to a rooted `Runtime.exec()` command.

## Requirements

- Android TV device (API 21+)
- **USB Debugging / Network Debugging enabled** in Developer Options

## Build & Install

```bash
# Build
./gradlew assembleDebug

# Install
adb install app/build/outputs/apk/debug/app-debug.apk
```

## License

MIT
