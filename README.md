# Budget Sleep Timer for Android TV 📺
[Leia em Português](README.pt-BR.md)

A minimal, ultra-lightweight Android TV app that puts your TV to sleep on a schedule. No bloated libraries, no trackers, just pure Android code.

**"Turn off every Friday at 1 AM"** · **"Turn off every day at 4 AM"** · **"Sleep in 30 minutes"**

## Features

- **Zero Background Memory:** Uses the native Android `AlarmManager`. When the app is closed, it consumes absolutely zero RAM until the exact minute the timer triggers.
- **Multiple Actions:** Choose between standard **Power (26)**, **Standby (223)**, or **Hibernate (276)** depending on your TV model's support.
- **Built-in ADB Keys:** Automatically generates and manages its own RSA keys for local ADB execution—no external apps needed.
- **Stackable Quick Timers:** Press +5, +15, +30 to quickly stack a one-shot countdown timer.

## How it works

1. On first run, a prompt will appear asking for ADB debugging permission. Check "Always allow from this computer" and hit OK.
2. Set a time and toggle the days of the week (or use Daily/Weekdays/Weekends shortcuts).
3. Select your preferred sleep action (Power, Standby, or Hibernate).
4. Hit "Set Schedule" — done!

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
