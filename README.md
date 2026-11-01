# TaskMaster

A native Android (Kotlin + Jetpack Compose) port of the dark "TaskMaster" web prototype.

- Add tasks with a title, optional details and a priority (High / Medium / Low)
- Check tasks off, delete them, filter by All / Pending / Completed
- Tasks are saved on the device and survive app restarts
- Same dark/indigo-purple look, with the "push back" effect behind the New Task sheet
- **Floating bubble**: an always-on-screen icon you can drag anywhere. Tap it to open a popup where you add tasks and tick them off, without leaving the app you're in

## Download the app

Get the latest APK from the **[Releases page](https://github.com/digitalboy69/To-do-list-app/releases/latest)**: open it, tap `TaskMaster.apk` under *Assets*, and install it (allow "install unknown apps" when asked).

## In-app updates

When the app opens it checks the latest GitHub Release. If a newer version exists, it asks **Update now** or **Later**. *Update now* downloads the APK inside the app (with a progress bar) and opens the Android installer; the first time, Android asks you to allow TaskMaster to install updates. *Later* hides the prompt until you open the app again. You can also tap **Check** in the card under the bubble switch at any time; it shows the installed version and tells you if you are up to date.

## Floating bubble

1. Open TaskMaster and turn on the **Floating bubble** switch below the title.
2. The first time, Android asks for **Display over other apps** (and notifications on Android 13+). Allow it and come back.
3. The bubble appears on screen. Drag it anywhere (it snaps to the nearest edge), tap it to open the quick popup, and tap **Open TaskMaster** inside the popup to jump to the full app.
4. Turn it off with the same switch, or with **Turn off** on the small "bubble is on" notification.

The bubble shows how many tasks are pending, comes back after a reboot if you left it on, and shares its task list with the app, so both always match.

## Build the APK with GitHub Actions

1. Push this project to a GitHub repo (branch `main`).
2. Open the **Actions** tab. The **Build APK** workflow runs on every push (or start it with **Run workflow**).
3. When it finishes, open the run and download **TaskMaster-apk** from *Artifacts*. Unzip it and install `TaskMaster.apk` on your phone (allow "install unknown apps").

Every push to `main` also publishes a new **Release** (`v1.0.<run number>`) with `TaskMaster.apk` attached, so the newest APK is always on the Releases page.

## Build locally

Open the folder in Android Studio (Ladybug or newer) and press Run, or with Gradle 8.9 and JDK 17 installed:

```bash
gradle assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Project layout

```
app/src/main/java/com/taskmaster/app/
  MainActivity.kt          entry point, edge-to-edge setup
  TaskViewModel.kt         state + actions (add / toggle / delete / filter)
  data/Task.kt             Task model, Priority, TaskFilter
  data/TaskRepository.kt   JSON persistence in SharedPreferences
  data/TaskStore.kt        shared task list used by the app and the bubble
  overlay/OverlayService.kt   foreground service that draws the bubble and popup
  overlay/QuickTasksPopup.kt  bubble + popup UI
  overlay/OverlaySettings.kt  on/off state and bubble position
  overlay/BootReceiver.kt     restores the bubble after reboot
  ui/BubbleToggle.kt       on/off switch and permission flow
  ui/UpdateUi.kt           "Check" card and "Update available" dialog
  update/UpdateChecker.kt  checks GitHub releases, downloads and installs the APK
  ui/TaskApp.kt            all screens and components
  ui/theme/Theme.kt        colors, gradient, theme
```

## Notes

- The workflow builds a signed **release** APK with R8 (code and resource shrinking). The signing key is kept in `.github/workflows/build-apk.yml` on purpose, so every build has the same signature and updates install over each other. That is fine for a personal sideloaded app; for anything public, replace it with your own private key via the repo secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD` and `KEY_ALIAS`.
- Because the app is installed outside the Play Store and asks to draw over other apps, Google Play Protect may warn about it. Tap **Install anyway** (or turn off *Scan apps with Play Protect* briefly).
- The prototype used the Poppins font. This app uses the system font to avoid bundling font files; to match exactly, drop the Poppins `.ttf` files into `app/src/main/res/font/` and reference them in a `FontFamily`.
- Package name / app id: `com.taskmaster.app` (change it in `app/build.gradle.kts` and move the source folders if you want your own).
