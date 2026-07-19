# OneRetroGameLauncher

Stop scrolling your library. Start finishing it.

## Build

Requires JDK 21 and Android SDK.

```bash
export JAVA_HOME=/usr/lib/jvm/temurin-21-jdk
make doctor
make build          # fdroid debug APK
make test
make run            # install + launch on a connected device/emulator
```

APK: `app/build/outputs/apk/fdroid/debug/app-fdroid-debug.apk`

In Settings / onboarding, set the ROMs absolute path (ES-DE layout: one folder per system). For local testing you can use a path like `/mnt/.../ROMs (Favorites)`.
