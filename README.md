# RC Realtime Test

**Minimal** Android project to isolate Firebase Remote Config **Realtime** against
project `kiabi-17f3b`, without any of the Kiabi app plumbing (no custom OkHttp, no
DataDome, no interceptors, no app-level proxy). Just the Firebase SDK.

Goal: determine whether the issue is on the **Firebase project/backend** side or in the
**Kiabi app**.

## Versions (aligned with app-kiabi-android)
- Gradle 9.4.1 · AGP 9.2.1 · compileSdk 36 · minSdk 28
- Firebase BOM **34.14.1** · google-services **4.4.4**
- `applicationId = com.pictime.kiabi.activity.staging` (reuses the staging Firebase app)

## Secrets

`app/google-services.json` is **not included** in this repo (it's git-ignored) since it
contains the Firebase API key in plain text.

To build this project, drop your own `app/google-services.json` (downloaded from the
Firebase console for project `kiabi-17f3b`) into the `app/` folder first.

## Testing realtime
1. Launch the app, keep it in the **foreground**. Wait for the `Listener registered` line.
2. Firebase console → Remote Config → change **any** parameter → **Publish**.
3. Observe (on screen or in logcat):
   - `onUpdate — updatedKeys=[...]` → **realtime works** → the issue is in the Kiabi app.
   - `onError — code=...` → we get the exact error code.
   - **Nothing** after 1–2 min → bug reproduced outside the Kiabi app → **project/backend
     issue** → Firebase support ticket.

Note: `fetchAndActivate OK` should appear at startup (proves auth/basic network are
fine). If even that fails, it's a network/key issue, not realtime.

## Observed logcat output

This is the app startup: the listener registers successfully (Firebase Installations
works, the realtime handler is created):

```
11:25:17.761 FirebaseApp             Device unlocked: initializing all Firebase APIs for app [DEFAULT]
11:25:17.777 FirebaseInitProvider    FirebaseApp initialization successful
11:25:17.917 FA                      To enable faster debug mode event logging run:
                                       adb shell setprop debug.firebase.analytics.app com.pictime.kiabi.activity.staging
11:25:17.955 RCTEST                  Firebase Installation ID = f94aoqfiSXWLUTYZ47Bhr2
11:25:19.190                         Listener registered: com.google.firebase.remoteconfig.internal.ConfigRealtimeHandler$ConfigUpdateListenerRegistrationInternal@c0cffd5
11:25:19.192                         Setup complete. >>> Publish a change in the Firebase console and wait here. <<<
```
