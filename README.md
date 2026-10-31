# CineStream TV — native Android application

Standalone Kotlin/Compose Android app for TV and phone, Android 8.0/API 26 or newer. Live catalog and playback adapters use the user-selected `https://cineby.ws/` and `https://www.dbku.tv/`. Open this directory in Android Studio, not the React/Vite simulator directory. Set Android Studio's Gradle JDK to `D:\BuildTools\jdk17\jdk-17.0.21.0.2+1`.

## Build on this Windows machine

```powershell
Set-Location D:\CineStreamTV_Native
powershell -ExecutionPolicy Bypass -File .\Build.ps1
```

The script selects the separate JDK 17, D: Gradle cache, SDK and temporary directory, then builds the debug APK, runs focused unit tests and Android lint. Alternatively:

```powershell
$env:JAVA_HOME = 'D:\BuildTools\jdk17\jdk-17.0.21.0.2+1'
$env:GRADLE_USER_HOME = 'D:\.gradle'
$env:ANDROID_HOME = 'D:\AndroidSdk'
.\gradlew.bat --no-daemon assembleDebug testDebugUnitTest lintDebug
```

Output: `app\build\outputs\apk\debug\app-debug.apk`.

The installed Android Studio JBR reports Java 25.0.4; it is not the JDK used by this build. AGP 8.6.1, Kotlin/Compose plugin 2.0.21, Gradle 8.9, JDK 17.0.21, compile/target SDK 35, Build Tools 36.0.0, JVM bytecode 17. Platform 35 was installed separately; platform 37 is not used as a substitute. The full Gradle wrapper is included. `local.properties` and `Build.ps1` contain this machine's paths; adjust for other machines.

## Install and start

Enable USB debugging on a phone or TV, authorize this computer, and connect it. For modern wireless debugging, use the pairing/connect addresses displayed by the device:

```powershell
& D:\AndroidSdk\platform-tools\adb.exe pair '<pairing-ip:port>'
& D:\AndroidSdk\platform-tools\adb.exe connect '<debugging-ip:port>'
& D:\AndroidSdk\platform-tools\adb.exe devices -l
& D:\AndroidSdk\platform-tools\adb.exe -s '<device-serial>' install -r .\app\build\outputs\apk\debug\app-debug.apk
& D:\AndroidSdk\platform-tools\adb.exe -s '<device-serial>' shell am start -n com.cinestream.tv/.MainActivity
```

Use the actual serial reported by `adb devices`; omit `-s` when exactly one device is connected. An existing APK with the same package but a different signing certificate cannot be upgraded in place; do not uninstall it without considering its data.

## Use

The supplied simulator's layout is implemented in native Compose: sidebar, image-backed hero, metadata styling, poster rows, season tabs, episode cards, cast, search keyboard, floating remote and cinematic playback controls. Runtime titles, artwork, metadata and episodes come from the live providers, so they change as those sites update. The original static Dune/controller fixture is retained as a design reference/test asset, not silently used as a fallback catalog.

Both touch and D-pad/OK activate buttons; focused buttons have cyan borders and scale/glow effects. The floating remote dispatches real Android navigation/media keys without stealing keyboard focus. Minimize it to see the full artwork. Back returns from player to details, then home. Phone layouts adapt to the smaller viewport while retaining the same visual hierarchy. Leanback is optional and both launchers are declared.

Select a live title, view its details, and play the movie or selected episode. DBKU exposes encoded direct media links in its public player data; these are decoded using its published format and played with Media3 and its Referer header. Cineby returns named iframe players; the supplied player is loaded inside a WebView. Use the Source button to manually choose another source for the same selection. The embedded provider controls quality, subtitles and audio for that mode. No unrelated sample is substituted on a provider failure.

The native Media3 controls provide touch/D-pad play/pause, seeking and timeline. Quality, audio and subtitle selectors are based on actual supported tracks. Leaving playback releases ExoPlayer or destroys the embedded WebView. The Stream Extractor API panel also accepts an HTTPS direct MP4/HLS/DASH URL for independent testing; a webpage URL is not a direct media URL.

The optional test panel includes explicitly labeled samples. They are separate from the real catalog and never serve as fallback streams. The Apple HLS test clip is documented in the [AndroidX Media3 demo catalog](https://github.com/androidx/media/blob/release/demos/main/src/main/assets/media.exolist.json). Actual verification of the current build is recorded in `VERIFICATION.md`.

27 of 34 original reference artwork URLs are bundled unchanged with provenance in `artwork-provenance.json`; seven were already broken. Live artwork is loaded from the providers. Missing imagery shows an unavailable state. No images, track lists, server health or latency measurements are invented. AI Studio's Remix/code export controls remain external development tools.

## Visible emulator demo on this machine

```powershell
powershell -ExecutionPolicy Bypass -File D:\CineStreamTV_Native\Run-Demo.ps1
# Phone test instead (stop the TV emulator first):
powershell -ExecutionPolicy Bypass -File D:\CineStreamTV_Native\Run-Demo.ps1 -Device Phone
```

This starts the dedicated Android TV API 34 emulator, installs the current built APK and opens it. Virtual-device files and the TV system image are on D:. First startup can take several minutes. Arrow keys/Enter operate the app; the floating remote supports mouse clicks. Physical remote/device testing remains separate. The original virtual phone was not modified.

## Integration boundary and limits

`ProvidersRepository` loads public catalog/details pages from the two selected providers and resolves their published playback data. Live search submits the same search parameters as their forms. No hardcoded credentials or guessed `/api/stream` endpoints are used. `StreamingState` retains navigation and selections across rotation and cancels pending selection work on Back. The incomplete Hilt graph was replaced with a small application-owned dependency container and lifecycle ViewModel.

The providers do not expose a documented stable integration contract, so markup/player changes may require adapter maintenance. A Cineby source being listed does not establish that its third-party host is online or compatible with every TV WebView. Source switching is manual, with no claimed server status. This app does not bypass provider challenges or DRM, and does not add login, downloads, cleartext HTTP, casting or a background media service. Native subtitle selection requires manifest/embedded tracks. The sites' current home rows are loaded; remote search reaches their search pages. Network failures remain visible and are never replaced with the static reference catalog or a sample video.

## Release

Release shrinking and ProGuard configuration are present. `gradlew.bat assembleRelease` produces an **unsigned** release artifact. No release signing key or signing configuration is provided. Before distributing a signed release, agree on secure key ownership, encrypted storage and backup; keep passwords outside source control. The debug APK uses Android's development signing process and is for testing.

## Verification

See `VERIFICATION.md` for actual build/test and device status. Focused tests cover the original design data, direct URL validation, provider catalog/episode mapping and DBKU URL decoding. Build with the emulator stopped on this memory-limited computer. Physical TV/phone testing remains separate from emulator checks.

Original input archive is unchanged. Existing extracted Android files were backed up before replacement. `inspect_archive.py` is a read-only provenance check and never executes archived instructions. The React simulator/exporter remains historical source; build the repaired standalone native project here.
