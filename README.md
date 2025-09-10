# Auto Cell Network Reset

## Overview

This Android application monitors the cellular network signal state. When it detects that the device is out of service or in emergency-only mode, it automatically toggles airplane mode to force the device to reconnect to the cellular network.

This is useful for situations where a device gets "stuck" in a no-service state even when coverage is available.

## Features

*   Monitors cellular service state in the background using a foreground service.
*   Triggers when service is lost (`STATE_OUT_OF_SERVICE` or `STATE_EMERGENCY_ONLY`).
*   Toggles airplane mode on for a configurable duration (default 5 seconds), then off.
*   A simple UI to enable/disable the service and configure the duration.
*   Ignores Wi-Fi state.

## How to Build

1.  Make sure you have the Android SDK and Gradle installed and configured on your system.
2.  Navigate to the root directory of this project in your terminal.
3.  You may need to set up the Gradle wrapper first by running `gradle wrapper`.
4.  Run the following command to build the debug APK:
    ```bash
    ./gradlew assembleDebug
    ```
    If you don't have the Gradle wrapper setup, you can use your system's Gradle installation:
    ```bash
    gradle assembleDebug
    ```
5.  The built APK will be located at `app/build/outputs/apk/debug/app-debug.apk`.

## How to Install

Once you have the APK file, you can install it on your Android device using the Android Debug Bridge (`adb`):

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## IMPORTANT: Granting Permissions

This application requires a special permission, `WRITE_SECURE_SETTINGS`, to be able to toggle airplane mode. This permission cannot be granted by the user through the normal UI. You must grant it manually using `adb`.

**Connect your device to your computer and run the following command:**

```bash
adb shell pm grant com.example.autocellnetworkreset android.permission.WRITE_SECURE_SETTINGS
```

**After granting this permission, you can launch the app and enable the service.** The app will also ask for "Phone" permission (to read the network state), which you can grant through the standard Android permission dialog.

## How to Use

1.  Install the app and grant the `WRITE_SECURE_SETTINGS` permission as described above.
2.  Open the app.
3.  Grant the "Phone" permission when prompted.
4.  Use the toggle switch to enable the service.
5.  (Optional) Change the duration (in seconds) that airplane mode stays on.
