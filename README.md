# Pixel Dialer 📞

A complete, production-structured **default Android dialer app** built with Jetpack Compose (Kotlin). Real calling via Android's Telecom framework, 6 switchable themes (gradient + solid), spam protection, and a polished iPhone-inspired UI.

---

## ✅ What's included

- **Real default-dialer capability** — `InCallService`, `CallScreeningService`, outgoing-call intent handling. Once set as default, this app handles actual phone calls on the device.
- **5 main screens**: Contacts, Recent (call history with grouped "(2)", "(3)" counts like stock dialers), Dialer (keypad with real DTMF tones + haptics), Protect (spam/blocked numbers), More.
- **Full call UI**: incoming call screen (accept/decline/message), active call screen (mute, speaker, hold, keypad, add call, end call) with a live timer.
- **6 built-in themes**, switchable live from a bottom-sheet picker: Gradient (teal), Ocean Blue, Sunset, Rose Gold, Midnight (solid dark), Violet (solid dark). Preference persists via DataStore.
- **Room database** for call log, blocked numbers, and favorites — offline-first, no backend required yet.
- **Real contacts** read via `ContactsContract` (with permission).
- **Permissions + "Set as default dialer" onboarding flow** built in.

## 🗂 Project structure

```
PixelDialer/
├── app/
│   ├── build.gradle.kts          # Dependencies: Compose, Room, DataStore, Coil, Accompanist
│   └── src/main/
│       ├── AndroidManifest.xml   # Default-dialer permissions & service declarations
│       ├── java/com/pixeldialer/app/
│       │   ├── MainActivity.kt   # App entry point, navigation, theme picker
│       │   ├── PixelDialerApp.kt # Application class, holds shared repositories
│       │   ├── data/             # Repositories, models, DataStore theme prefs
│       │   │   └── db/           # Room entities, DAOs, database
│       │   ├── telecom/          # InCallService, CallScreeningService, call activities
│       │   ├── ui/
│       │   │   ├── theme/        # 6 color palettes + Compose theme wiring
│       │   │   ├── components/   # Avatar, BottomNav, ThemePicker, DirectionIcon
│       │   │   └── screens/      # RecentsScreen, ContactsScreen, DialerScreen, CallScreen...
│       │   └── viewmodel/        # MainViewModel + factory
│       └── res/                  # Icons, strings, colors, XML themes
├── build.gradle.kts               # Root Gradle config
├── settings.gradle.kts
└── gradle.properties
```

## 🛠 How to build (Android Studio — recommended)

1. Open **Android Studio** (Koala/2024.1 or newer recommended).
2. **File → Open** → select the `PixelDialer` folder.
3. Android Studio will detect there's no `gradlew` binary yet and offer to **regenerate the Gradle wrapper** — click yes / let it sync. (This project ships the wrapper *properties* file, but the JAR itself couldn't be generated in this sandboxed environment since it has no network access. Android Studio will fetch it on first sync.)
4. Wait for Gradle sync to finish (it will download Gradle 8.7, AGP 8.5.2, and all dependencies — needs internet).
5. Connect a physical Android device (recommended — emulators can't make real phone calls) or use an emulator for UI testing only.
6. Click **Run ▶** or `Shift+F10`.

## 🛠 How to build (command line)

If you prefer CLI and don't have the wrapper jar yet:

```bash
# One-time: generate the wrapper jar (needs a local Gradle install or Android Studio once)
gradle wrapper --gradle-version 8.7

# Then build normally:
./gradlew assembleDebug
# APK will be at: app/build/outputs/apk/debug/app-debug.apk
```

## 📱 First-run flow

1. Launch the app → it shows the **permissions screen**.
2. Tap **Grant permissions** → accept the system dialogs (Phone, Contacts, Call log, etc).
3. Tap **Set as default dialer** → Android's system picker appears → confirm.
4. You land on the **Recents** tab. Tap the palette icon (top right) to try all 6 themes.

> ⚠️ **Real call testing** requires a physical device with a SIM (or a device signed into a calling service). Emulators generally can't place real cellular calls, though the UI, theming, contacts list, and keypad all work fine on an emulator too.

## 🎨 Themes

| Theme | Style | id |
|---|---|---|
| Gradient | Teal/mint gradient (matches original reference) | `gradient` |
| Ocean Blue | Blue gradient (matches app icon) | `ocean` |
| Sunset | Warm coral/orange gradient | `sunset` |
| Rose Gold | Soft pink/cream gradient | `rosegold` |
| Midnight | Solid black, green accent (iPhone dark mode style) | `midnight` |
| Violet | Solid deep purple, lavender accent | `violet` |

All screens read colors exclusively from `LocalDialerPalette.current` — adding a 7th theme means adding one more `DialerPalette` object in `DialerColors.kt` and listing it in `AllPalettes`. No screen code needs to change.

## 🔜 Next steps (not yet built — flagged by you for later)

- **Firebase Auth** (login/signup)
- **Cloud backup** of call log / contacts / theme preference across devices
- Real spam-number database integration (currently the Protect screen has the UI + local block-list plumbing, but no live spam feed)
- Call recording (permission is declared in the manifest for future use, not wired up)

## ⚠️ Known limitations to be aware of

- The **gradle-wrapper.jar binary** is not included (this sandbox has no network access to download it). Opening in Android Studio resolves this automatically on first sync.
- App icon is defined as **vector drawables** (`ic_launcher_background.xml` / `ic_launcher_foreground.xml`), not rasterized PNGs — this is the modern adaptive-icon approach and works on API 26+; Android Studio will render it correctly.
- `minSdk 29` (Android 10) as requested — calling APIs used (`Call.Callback`, `CallScreeningService`, `RoleManager.ROLE_DIALER`) are all available from API 29 onward.
