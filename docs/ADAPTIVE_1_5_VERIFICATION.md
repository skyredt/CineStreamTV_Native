# CineDBKTV 1.5.0 — adaptive TV and phone verification

Date: 2026-11-02, Asia/Singapore.

## Scope and recoverable baseline

The workspace was on `feature/catalogue-navigation-overhaul` at `ed5e309c7cb056c52e62d180aea9fe5e81e61016`, with the expected uncommitted 1.4.0 catalogue overhaul. Those 16 files were preserved in local checkpoint commit `3dcac5d` before new edits. The adaptive work is on `feature/adaptive-tv-mobile-1.5`. No reset, deletion of old APKs, push, PR or store publication was performed.

The historical 1.4.0 report remains `docs/CATALOGUE_OVERHAUL_VERIFICATION.md`; its runtime results are baseline evidence, not new 1.5.0 device passes.

## One application, two presentations

- One application ID (`com.cinestream.tv`), codebase and APK, version 1.5.0/code 11.
- Android configuration UI mode selects TV. Screen width never turns a phone/tablet into a television.
- TV keeps its sidebar, D-pad focus behavior, horizontal catalogue rows, existing details and search screens.
- Phone gets Home/Browse/Search/History/Settings bottom navigation, touch poster grids, a featured card, scrollable filters and metadata, and touch season/episode selection. Episode buttons are paged in batches of at most 50.
- English/中文 switching is available in the phone header and Settings. First launch has a touch catalogue chooser. The same persisted preference and history are used by both presentations.
- Phone browsing follows user orientation. Playback requests sensor landscape/fullscreen; leaving playback restores user orientation. Configuration handling retains the activity, shared state and keyed player container during rotation/size changes. Actual device continuity still requires acceptance.
- Phone UI applies safe drawing and keyboard insets. The TV UI is selected by UI mode even on a small television; wide phones retain touch navigation.

## Shared behavior and playback boundaries

Provider adapters, metadata models, provider-qualified IDs, catalogue/search, filters, source resolution, history/preferences and playback coordination remain shared. No provider was added, merged by title or reactivated. Exactly Cineby, BingeBang and DBKU are active.

`Providers.kt`, `BingeBang.kt`, `Playback.kt`, `tv-bingebang-player.js` and `tv-web-player.js` are unchanged from the 1.4.0 checkpoint. The existing Media3 and embedded playback engines are retained.

The embedded container changes are limited to:

- Keep hardware D-pad interception in TV mode; let the phone WebView handle touch/keyboard normally.
- Expose a mobile Back button and a touch source picker when the coordinator has multiple resolved sources.
- Implement Android's `WebChromeClient.onShowFileChooser` with the system activity-result picker, returning selected file URIs/cancellation to the existing provider input. The earlier container had no file chooser handler. This enables the Android container path needed for provider subtitle-file inputs; it is **not** proof that BingeBang's upload UI works on the Samsung phone.
- Existing provider subtitle, language, server and quality controls/scripts remain intact. Capabilities still depend on the provider, media source and installed Android System WebView.

No authentication, DRM, signed-URL or playback restriction bypass was attempted. No provider JavaScript changed. No broad storage permission was added.

## Compatibility inspection

Source and merged manifests were inspected. Leanback and touchscreen features are optional, and both Android TV and ordinary Android launcher categories remain. The global landscape restriction was removed; the adaptive activity handles orientation/screen-size/keyboard/UI-mode configuration changes. Minimum Android 26 and target/compile SDK 35 are unchanged. No APK flavors or dependencies were added.

Final APK: `D:\CineStreamTV\cinedbktv-development\CineDBKTV-1.5.0-adaptive-debug.apk` (44,407,838 bytes, debug variant).

SHA-256: `E6E7593CEFED07712AC756304DB293648A2C59D0677489699B4FDF70431D15C3`.

Android SDK `aapt2 dump badging` confirms package/version, minimum SDK 26, target SDK 35, both launcher entries, optional touchscreen/Leanback and `arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`. `apksigner verify` exited 0. The final merged manifest has no fixed landscape restriction. Physical installation is pending; static compatibility is not proof of Samsung installation or playback.

## Automated evidence

`adaptive-final-validation.log`: **BUILD SUCCESSFUL**, 78 actionable tasks (20 executed, 58 up-to-date), in 5m 58s.

- `assembleDebug`: PASS.
- `testDebugUnitTest`: 70 tests, zero failures, two existing optional fixture-dependent skips (68 executed/passed).
- All five new adaptive tests: PASS.
- `lintDebug`: zero errors, five warnings.
- `assembleDebugAndroidTest`: PASS; existing TV instrumentation/regression runners retained. They were not executed on a device/emulator in this task.
- `git diff --check`: PASS.
- Final APK signature verification: PASS.

Build command (PowerShell, from the project directory, using existing JDK/SDK/cache):

```powershell
$env:JAVA_HOME = 'D:\BuildTools\jdk17\jdk-17.0.21.0.2+1'
$env:GRADLE_USER_HOME = 'D:\.gradle'
$env:ANDROID_HOME = 'D:\AndroidSdk'
.\gradlew.bat --offline --no-daemon '-Dorg.gradle.internal.instrumentation.agent=false' '-Dorg.gradle.jvmargs=-Xmx512m -XX:ReservedCodeCacheSize=96m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8' --console=plain assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest
```

New `AdaptiveNavigationTest` covers:

1. TV UI-mode selection, including night-mode flags; ordinary/car devices remain non-TV.
2. Grid policy at 320×640, 360×800, 412×915, 640×320, 800×360, 915×412 and 1280×720; column count and minimum cell width bounds.
3. All five core phone navigation destinations, without duplicates.
4. English provider feed availability and Chinese provider categories without invented feeds.
5. Long-list episode paging, including 2,000 episodes.

These are JVM policy/geometry checks, **not screenshots, rendered Compose layout tests or touch/device acceptance**. Robolectric was not cached in the offline build environment; no new framework/dependency was introduced just for this task. Existing JVM provider/playback tests are retained. Existing Android TV regression runners are preserved and compiled where possible, but were not executed for 1.5.0 because this task does not depend on recovering the unstable laptop emulator.

The initial 768 MB JVM failed from Windows native-memory exhaustion. The build was retried at 512 MB with a smaller code cache. One mobile search brace was corrected from compiler feedback. No phone, emulator or ADB session was started during this adaptive task.

## Manual Samsung S25 FE installation — no ADB

1. Transfer `CineDBKTV-1.5.0-adaptive-debug.apk` to the phone using Quick Share, normal cloud/file transfer, or USB **file transfer**.
2. In Samsung **My Files**, open the APK (usually in Downloads).
3. If Android requests it, allow installation from that file-manager source, then return to Install.
4. Install and open **CineDBKTV** from the ordinary app launcher.
5. Choose English or 中文 if prompted. An upgrade may retain the previously selected catalogue and local history.

No developer options, USB debugging, wireless debugging, ADB connection or remote control is required. If installation fails, report the exact message; avoid uninstalling an existing copy before deciding whether its local history should be retained.

## Phone acceptance checklist

- [ ] Install and launch from the normal launcher; verify a portrait touch interface and bottom navigation.
- [ ] Choose English, close/reopen the app, then switch to 中文 from the header or Settings.
- [ ] Browse both catalogues and available categories; swipe the grids; open posters.
- [ ] Search an English title and check Cineby and BingeBang provider results. Search a Chinese title and check DBKU. Exercise type/year/sort and applicable DBKU region/genre filters; return from details and check retained search state.
- [ ] Open a movie and a series on each provider. Check actual metadata, synopsis, seasons and a later episode. Movies should not show episode controls.
- [ ] Start playback; verify fullscreen landscape, touch controls, Android Back and returning to the correct title/catalogue. Test a source/server change where offered.
- [ ] On BingeBang test OpenSubtitles language selection, subtitle-file selection, external subtitle-file loading through Android's picker, and captions displayed over advancing video.
- [ ] Rotate the phone during browsing and playback; check sizing, cutouts/system bars and playback continuity. Try closing/reopening the app and Continue where progress is reported.
- [ ] Open History, return to a watched title, remove an entry and clear the selected catalogue's history.
- [ ] Report provider, title, season/episode, action, portrait/landscape state and exact visible error for any issue. A screenshot or short screen recording is useful.

## Acceptance matrix

| Requirement | Evidence / status |
| --- | --- |
| Shared codebase and one APK | PASS: shared implementation, one signed/debug APK; actual installation pending |
| Automatic TV/mobile selection | UI-mode policy and JVM tests; device acceptance pending |
| TV sidebar/D-pad/Back/focus preserved | Existing TV presentation retained; 1.5.0 runtime regression pending |
| Phone touch navigation | Implemented mobile controls; physical test pending |
| English/中文 on both layouts | Shared preference/state with adaptive choosers; physical test pending |
| Three-provider catalogue/search | Adapters unchanged; JVM regression suite passes; physical phone network pending |
| Responsive show details and episodes | Implemented lazy/paged phone UI; geometry tests; visual/device acceptance pending |
| Existing playback routes | Shared coordinator and native player unchanged; embedded-container changes described above; physical playback pending |
| Subtitle controls retained | Scripts unchanged and file-picker callback added; phone language/file/caption acceptance pending |
| History and available resume | Shared existing persistence/callbacks; phone playback reporting pending |
| TV Android regression tests | PASS compilation; runtime not executed on an emulator in this task |
| Automated checks | PASS: APK + Android-test APK, 70 JVM tests/zero failures/two skipped, lint zero errors/five warnings |
| Phone-compatible APK | PASS static package/signature/ARM64 checks; actual Samsung install pending |
| Manual instructions | Provided above; no ADB |

## Remaining limits and TV acceptance

- Samsung S25 FE installation, rendered layouts, touch interactions, rotation continuity, all-provider playback, subtitles and actual history/resume have **not** been device-tested. The user will perform these checks.
- No functional equivalence claim is made for third-party embedded players. A provider can expose different mobile controls or refuse playback; source restrictions remain respected.
- Exact resume still requires the existing player to report position and accept its resume bridge.
- Existing filter limits remain: type/year/sort operate on loaded results, BingeBang search has one adapter batch, and DBKU region/genre browse is limited to the existing adapter's supported types/pages.
- Android TV still needs a 1.5.0 physical-TV regression: sidebar/category focus, details/Back restoration, long episodes, Cineby/DBKU native-route playback, subtitle controls, source switching and resume. The successful 1.4.0 catalogue/BingeBang tests are not presented as 1.5.0 passes.

## Changed files

- `app/build.gradle.kts`: version only.
- `app/src/main/AndroidManifest.xml`: adaptive orientation/configuration handling.
- `AdaptivePolicy.kt`: device-mode, grid, categories and episode-page policies.
- `AdaptiveApplication.kt`: automatic presentation, orientation and fullscreen/system-bar policy.
- `MobileScreens.kt`: phone chooser, shell, catalogue, search/filters, details/episodes, history and settings.
- `NativeApplication.kt`: adaptive root and common coordinator connections.
- `EmbeddedPlayer.kt`: touch-mode key handling, native Back and system file chooser; no engine rewrite.
- `AdaptiveNavigationTest.kt`: focused adaptive policy/navigation tests.
- This report. All paths above are under the existing Android project.
