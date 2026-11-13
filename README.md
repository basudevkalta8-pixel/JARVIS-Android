# J.A.R.V.I.S. 2.0 — Native Android 15 Companion App (Vivo T2 Pro 5G)

This is the native **Kotlin + Jetpack Compose** companion project for **Android 15 (API 35)**, designed specifically to connect securely to your existing J.A.R.V.I.S. Gemini backend without exposing your `GEMINI_API_KEY` inside the APK.

## Why a Native Android APK?
Web browsers (like Chrome on Android) run in a secure sandbox and **cannot** access:
- Rear LED Flashlight hardware (`CameraManager.setTorchMode`)
- Installed applications list (`PackageManager`)
- Native Clock Alarms (`AlarmClock.ACTION_SET_ALARM`)
- Background / Foreground Microphone Service (`FOREGROUND_SERVICE_MICROPHONE`)
- Direct Contact queries (`READ_CONTACTS`)

This native Kotlin companion unlocks all of those safely on your **Vivo T2 Pro 5G (Android 15)**.

## How to Turn This Project Into an Installable `.apk`

Because Android `.apk` files must be compiled with Java 17 + Android SDK 35 (`aapt2`, `d8`, `apksigner`), you can build the `.apk` using either method below:

### Method A: Free Cloud APK Build (No PC / Android Studio Required)
1. Unzip `JARVIS-Android15-Companion.zip`.
2. Create a free repository on **GitHub** and upload the contents of `jarvis-android15-companion/` (including the `.github/workflows/build-apk.yml` file).
3. Open the **Actions** tab on your GitHub repository → select **Build JARVIS Android 15 APK** → click **Run workflow**.
4. Wait ~2 minutes for the green checkmark, then download the **`JARVIS-2.0-Android15-APK`** artifact (`app-debug.apk`) directly onto your **Vivo T2 Pro 5G** and tap it to install!

### Method B: Build Locally in Android Studio (PC / Mac)
1. **Unzip & Open in Android Studio**:
   - Unzip `JARVIS-Android15-Companion.zip` and open the folder in **Android Studio** (Ladybug or newer).
2. **Wait for Gradle Sync**:
   - Android Studio will automatically download Jetpack Compose, Gradle 8.9, & OkHttp dependencies.
3. **Verify Backend URL**:
   - In `app/build.gradle.kts`, `DEFAULT_BACKEND_URL` points to your live Gemini backend (`https://ais-pre-k5qqkvobzqrmfpdy4ze6ob-394792062733.asia-southeast1.run.app`).
   - You can also change this anytime inside the app's **Settings (⚙️)** icon.
4. **Build or Install APK on Vivo T2 Pro 5G**:
   - **Direct APK file:** Click **Build → Build Bundle(s) / APK(s) → Build APK(s)**. Android Studio creates `app/build/outputs/apk/debug/app-debug.apk`.
   - **Or Direct USB Run:** On Vivo T2 Pro 5G, go to **Settings → About phone → Software information** and tap **Build number 7 times** → enable **USB Debugging** in Developer Options → click **Run (▶)**.
5. **Vivo FuntouchOS 15 Wake-Word Tip**:
   - Android 15 requires a visible notification while a microphone foreground service runs.
   - To keep the optional `"Hey JARVIS"` listener from being killed by Vivo battery optimization while active: **Settings → Battery → Background power consumption management → J.A.R.V.I.S. 2.0 → Don't restrict**.

