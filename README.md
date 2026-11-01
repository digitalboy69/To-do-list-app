# TaskMaster

A native Android (Kotlin + Jetpack Compose) port of the dark "TaskMaster" web prototype.

- Add tasks with a title, optional details and a priority (High / Medium / Low)
- Check tasks off, delete them, filter by All / Pending / Completed
- Tasks are saved on the device and survive app restarts
- Same dark/indigo-purple look, with the "push back" effect behind the New Task sheet
- **Floating bubble**: an always-on-screen icon you can drag anywhere. Tap it to open a popup where you add tasks and tick them off, without leaving the app you're in

## Floating bubble

1. Open TaskMaster and turn on the **Floating bubble** switch below the title.
2. The first time, Android asks for **Display over other apps** (and notifications on Android 13+). Allow it and come back.
3. The bubble appears on screen. Drag it anywhere (it snaps to the nearest edge), tap it to open the quick popup, and tap **Open TaskMaster** inside the popup to jump to the full app.
4. Turn it off with the same switch, or with **Turn off** on the small "bubble is on" notification.

The bubble shows how many tasks are pending, comes back after a reboot if you left it on, and shares its task list with the app, so both always match.

## Build the APK with GitHub Actions

1. Push this project to a GitHub repo (branch `main`).
2. Open the **Actions** tab. The **Build APK** workflow runs on every push (or start it with **Run workflow**).
3. When it finishes, open the run and download **TaskMaster-debug-apk** from *Artifacts*. Unzip it and install `TaskMaster-debug.apk` on your phone (allow "install unknown apps").

To publish the APK on the **Releases** page, push a tag:

```bash
git tag v1.0.0
git push origin v1.0.0
```

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
  ui/TaskApp.kt            all screens and components
  ui/theme/Theme.kt        colors, gradient, theme
```

## Notes

- The workflow produces a **debug** APK, which is signed with the standard Android debug key and installs fine on any phone. For Play Store distribution you would add a release signing config with your own keystore (kept in GitHub Secrets).
- The prototype used the Poppins font. This app uses the system font to avoid bundling font files; to match exactly, drop the Poppins `.ttf` files into `app/src/main/res/font/` and reference them in a `FontFamily`.
- Package name / app id: `com.taskmaster.app` (change it in `app/build.gradle.kts` and move the source folders if you want your own).
