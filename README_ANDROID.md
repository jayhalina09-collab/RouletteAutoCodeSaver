# Android version — Code Automation

This project converts the original Windows/PowerShell flow into an Android Studio app.

## Flow copied from the supplied files

1. Open a fresh page for:
   `https://www.scanpack.com/ch-qronstick2026-rc`
2. Wait for `JOIN NOW`.
3. Click `JOIN NOW`.
4. Handle `Accept All Cookies` when it appears.
5. Handle `YES, I AM` when it appears.
6. Look for an 8–12 character uppercase alpha-numeric code.
7. Require `REGISTER NOW` to be present before accepting the code.
8. Click `COPY CODE` when available.
9. Reject duplicate codes.
10. Save the newest code in Android SharedPreferences.
11. Keep the screen awake while the app is running.
12. Wait 5 minutes.
13. Start the flow again.

## Important difference from the Windows files

The Windows version controlled Chrome through ADB. A normal Android APK cannot use its own ADB connection to control Chrome the same way.

This Android version instead uses an Android WebView and JavaScript DOM automation, so the website is opened inside the app and the same visible text-driven flow is reproduced.

## Build

Open this folder in Android Studio, let Gradle sync, then:

Build > Build Bundle(s) / APK(s) > Build APK(s)

The APK can then be installed on the Android device.

## Files

- MainActivity.java — Android UI and WebView
- AutomationEngine.java — copied automation flow/state machine
- activity_main.xml — app interface
- AndroidManifest.xml — permissions/activity
- settings.gradle / build.gradle — Android Studio project configuration

## Stored data

The app stores:
- latest registration code
- all unique codes
- last timestamp

This is stored in the app's private SharedPreferences area.

## Notes

The exact website can change its HTML/buttons. If the website changes the wording or uses controls that WebView cannot click, the selectors in `AutomationEngine.java` will need to be updated.
