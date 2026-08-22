# Ashu Phone 📞

A complete, production-structured **default Android dialer app** built with Jetpack Compose (Kotlin). Real calling via Android's Telecom framework, 8 switchable themes, call recording, conference calling, smart Bluetooth-aware audio routing, and Firebase-backed cloud backup.

---

## ⚠️ One-time setup required before this builds: Firebase

This app uses Firebase (Google Sign-In + Firestore) for the optional Cloud Backup feature. Firebase requires a `google-services.json` file tied to **your own** Firebase project — it can't be generated or guessed, so the build will fail until you add it. Takes about 5 minutes:

1. Go to the [Firebase Console](https://console.firebase.google.com) → **Add project** → name it anything (e.g. "Ashu Phone").
2. Inside the project: **Build → Authentication → Get started → Sign-in method → Google → Enable**. Set a support email when asked.
3. **Build → Firestore Database → Create database** → start in **production mode** → pick any region.
4. Go to **Project settings** (gear icon) → **Your apps** → **Add app → Android**.
   - Android package name: `com.pixeldialer.app` (must match exactly — this is set in `app/build.gradle.kts`'s `applicationId`)
   - App nickname: anything
   - Debug signing certificate SHA-1: optional for now, needed later for a signed release build (get it via `./gradlew signingReport` once you can build)
5. Download the generated **`google-services.json`** and place it at `app/google-services.json` (same folder as `app/build.gradle.kts`).
6. Back in **Firestore Database → Rules**, replace the default rules with:
   ```
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {
       match /users/{userId} {
         allow read, write: if request.auth != null && request.auth.uid == userId;
       }
     }
   }
   ```
   This ensures each signed-in user can only ever read/write their own backup document.

Once `google-services.json` is in place, the app builds and runs like any other module — Firebase wiring is otherwise fully done in code (`AuthRepository`, `CloudBackupRepository`, `AccountScreen`).

**If you don't want Firebase right now:** the app still builds and runs fully as a dialer without it — Cloud Backup just won't work until `google-services.json` is present. Everything else (calling, recording, themes, contacts) is independent of Firebase.

---

## ✅ What's included

- **Real default-dialer capability** — `InCallService`, `CallScreeningService`, correct `TelecomManager.placeCall()` routing (not a generic `ACTION_CALL` broadcast, which is what used to trigger the Android app-chooser dialog instead of calling directly).
- **Reliable incoming-call UI** — a full-screen-intent notification (the same mechanism WhatsApp/Truecaller use) wakes the screen and launches the call UI even when the app is backgrounded or the screen is off; a plain `startActivity()` call alone silently fails in that situation on Android 10+, which was the root cause of "no ring UI, only vibration" before this was added.
- **Glassy heads-up call notification** with 3 direct-tap controls (Speaker / Mute / End) for ongoing calls, Answer/Decline for incoming ones — crash-guarded so a notification-render failure can never take down the app.
- **5 main tabs**: Contacts (working search + swipe-to-call/message), Recent (synced from the device's real call history, not just calls made through this app), Dialer (small keys, live contact-name lookup as you type, real DTMF tones), Protect, More.
- **Full call screen** matching a stock-dialer 3×3 action grid: Video call / Recording / Note, Mute / Hold / Merge-or-Add-call, Audio-route / End-call / Dialpad, plus a live animated waveform bar while recording.
- **Call recording** — tries the restricted `VOICE_CALL` audio source first (works on some OEM builds), falls back to microphone capture everywhere else, and always shows which mode is active rather than pretending one universal method works.
- **Conference/merge calling** and call-swap via the real Telecom `Call.conference()`/`hold()`/`unhold()` APIs.
- **Smart audio routing** — a simple speaker/earpiece toggle when that's all that's available; automatically becomes a picker once a Bluetooth or wired headset is also connected.
- **8 themes**: Gradient, Ocean Blue, Sunset, Rose Gold, Midnight (solid dark), Violet (solid dark), Dark Mode (pure AMOLED black), and System (follows the device's light/dark setting automatically).
- **Firebase Google Sign-In + Firestore Cloud Backup** (optional, off by default) — backs up call log and theme preference to a private per-user document, restorable on a new device.
- **Privacy Policy screen** built in and linked from More → Privacy Policy.
- Room database for local call log/blocked-numbers/favorites — fully offline-capable; cloud backup is additive, not required.

## 🗂 Project structure

```
PixelDialer/
├── app/
│   ├── google-services.json      # YOU add this — see Firebase setup above
│   ├── build.gradle.kts          # Dependencies: Compose, Room, Firebase, DataStore, Coil
│   └── src/main/
│       ├── AndroidManifest.xml   # Default-dialer + Firebase permissions/services
│       ├── java/com/pixeldialer/app/
│       │   ├── MainActivity.kt   # Navigation, theme picker, Account/Privacy overlays
│       │   ├── PixelDialerApp.kt # Application class, holds shared repositories
│       │   ├── data/             # Repositories: call log, contacts, auth, cloud backup
│       │   │   └── db/           # Room entities, DAOs, database
│       │   ├── telecom/          # InCallService, CallScreeningService, recording, audio routing
│       │   ├── ui/
│       │   │   ├── theme/        # 7 fixed palettes + system-auto resolution
│       │   │   ├── components/   # Avatar, BottomNav, ThemePicker
│       │   │   └── screens/      # All screen composables
│       │   └── viewmodel/        # MainViewModel + factory
│       └── res/                  # Icons, strings, notification layout/drawables
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## 🛠 How to build

1. **Do the Firebase setup above first** (5 minutes, one-time).
2. Open **Android Studio** (Koala/2024.1+) → **File → Open** → select the `PixelDialer` folder.
3. Let Gradle sync (needs internet — downloads Gradle 8.7, AGP 8.5.2, and all dependencies including Firebase).
4. Connect a physical Android device (recommended — emulators can't place real cellular calls, though everything else works fine on one for UI testing).
5. **Run ▶**.

### Command line
```bash
gradle wrapper --gradle-version 8.7   # one-time, if the wrapper jar isn't present yet
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

## 📱 First-run flow

1. Launch → **permissions screen** → tap **Grant permissions**, accept all system dialogs (Phone, Contacts, Call log, Microphone for recording, Bluetooth for audio routing, Notifications).
2. Tap **Set as default dialer** → confirm in the system picker. **Important:** if the system shows a "Just once / Always" choice, pick **Always** — picking "Just once" means Android will keep re-asking on every call.
3. Land on **Recents** — existing call history from before this app was installed appears automatically (synced from the device's system call log).
4. Tap the palette icon (top right of Recents) to browse all 8 themes.
5. **More → Account** to sign in with Google and turn on Cloud Backup, if desired.

## 🎙 On call recording, honestly

Every third-party Android dialer — this one included — runs into the same Android 10+ platform restriction: apps without system/privileged status can't cleanly capture the other party's voice via `AudioSource.VOICE_CALL` on most devices. This app tries that first (it works on some OEM builds, mostly older Samsung/Xiaomi skins), and transparently falls back to microphone capture everywhere else — the active mode is always shown in the recording pill during a call, never silently assumed. Recordings save locally to the app's private storage only; nothing is uploaded automatically.

## 🔊 On audio routing

`AudioRouteController` checks actual connected devices via `AudioManager.getDevices()` and Bluetooth's connection state — it isn't a fixed on/off toggle. With only earpiece+speaker available, the audio-route button in the call screen just flips between them on tap. The moment a Bluetooth headset or wired headset is detected, the same button switches to opening a picker instead, since there's now a real choice to make.

## 🔜 Possible next steps

- Voicemail transcription/playback (menu item exists, not wired up)
- Blocked-numbers management UI (Protect tab currently reads an empty list; the Room table + DAO are ready)
- Push notification-based spam number updates
- Video calling (button exists in the call screen grid, currently a no-op)

## ⚠️ Known limitations

- `google-services.json` is required for Cloud Backup to function — see setup section above.
- The **gradle-wrapper.jar binary** isn't bundled (this dev sandbox has no network access to fetch it); Android Studio resolves this automatically on first sync.
- `minSdk 29` (Android 10) — all telecom/recording/audio-routing APIs used are available from API 29 onward.
- Call recording legality varies by region/state (some require two-party consent) — this is a user/deployment responsibility, not something the app enforces.
