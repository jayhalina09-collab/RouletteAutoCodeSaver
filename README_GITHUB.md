# AndroidCodeAutomation — GitHub-only build

This project is prepared to build the Android APK entirely with GitHub Actions. Android Studio is NOT required, and a Gradle wrapper is NOT required because the workflow installs Gradle 8.7 automatically.

## Upload
Upload the **contents** of this folder to the root of your GitHub repository. The repository root must contain `app/`, `build.gradle`, and `settings.gradle`.

## Build
1. Open the repository on GitHub.
2. Open **Actions**.
3. Select **Build Android APK**.
4. Click **Run workflow**.
5. Wait for the green check.
6. Open the successful run.
7. Under **Artifacts**, download `AndroidCodeAutomation-debug`.
8. Extract the artifact and install `app-debug.apk` on your Android device.

The workflow also automatically runs whenever code is pushed to `main`.

## Flow
The Android automation flow is implemented in `app/src/main/java/com/example/androidcodeautomation/AutomationEngine.java` and the original Windows source files are preserved under `original_windows/`.
