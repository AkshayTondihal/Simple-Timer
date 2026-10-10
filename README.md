# Simple Timer (Android)

A minimal countdown timer app.

- **Timer setting:** pick hours / minutes / seconds with the wheels (remembered between launches)
- **Start / Stop buttons:** Stop cancels a running timer or silences the alarm
- **Custom sound:** "Choose sound" lets you pick any audio file on your phone;
  "Default sound" goes back to the system alarm tone. The sound loops until you press Stop.

Note: the timer runs while the app is open (the screen stays on during a countdown).
It does not run as a background service.

## Get the APK

### Option A: Android Studio
1. Install Android Studio and choose **File > Open**, then select this folder.
2. Wait for the Gradle sync to finish.
3. Choose **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
4. The file is at `app/build/outputs/apk/debug/app-debug.apk`. Copy it to your phone and open it
   (you may need to allow "Install unknown apps").

### Option B: GitHub (no installs)
1. Create a new GitHub repository and upload everything in this folder
   (including the hidden `.github` folder).
2. Open the **Actions** tab, then **Build APK**, and wait for the green check.
3. Open the finished run and download the **simple-timer-apk** artifact (a zip containing `app-debug.apk`).
