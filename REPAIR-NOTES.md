# CineDBKTV 1.3.0

Original repair baseline: skyredt/CineStreamTV_Native commit 7558d5c2ac5c2b34ba83eeee4f9f4123b3e0ead0. Work is in this separate folder; the original archive and D:\CineStreamTV_Native were not overwritten. Source is versioned on the GitHub branch development/cinedbktv-1.3.

This is the native Android TV project. It retains the supplied Compose design, uses the configured Cineby and DBKU providers, and has no floating remote overlay. Phone launcher support has been removed following the updated TV-only requirement.

TV feedback changes in 1.3.0:

- DBKU episodes use compact text buttons, ranges of 50 episodes, and a jump-to-episode field. No episode thumbnails are shown for DBKU.
- Both native playback and the supported Cineby Xpass player use a shared draggable timeline. With remote focus on the timeline, Left/Right previews a position, holding accelerates, OK seeks, and Back cancels the preview.
- Cineby Up/Down controls Android media volume. These keys are handled by the app for the supported player.
- Cineby source failures automatically try another source from the already-selected title/episode, display a notice, preserve the last reported playback position where supported, and stop after all sources have been tried. Supported embeds also switch if no timeline becomes ready within 45 seconds. No unrelated video or sample is substituted.
- Country/region and Chinese genre filters apply only to DBKU. Cineby ignores those two filters. DBKU's published filtered catalog links supply the matching categories; in this mode title text is searched within loaded filtered pages. Load more to inspect older matches. The server's selected filters are checked before results receive those labels.
- App label and visible branding are CineDBKTV. The application ID stays com.cinestream.tv so existing installs can update.


## Build and install

Use PowerShell. Keep the emulator closed while building on this machine to reduce memory pressure.

```powershell
Set-Location 'D:\CineStreamTV\android-tv-repair'
.\Build.ps1
& 'D:\AndroidSdk\platform-tools\adb.exe' devices
& 'D:\AndroidSdk\platform-tools\adb.exe' -s emulator-5554 install -r '.\app\build\outputs\apk\debug\app-debug.apk'
& 'D:\AndroidSdk\platform-tools\adb.exe' -s emulator-5554 shell am start -n com.cinestream.tv/.MainActivity
```

To start the prepared TV emulator and install the APK: `.\Run-Demo.ps1 -Device TV`. Its historical script name does not mean that the application uses a demo catalog. To compile and run the live TV smoke checks: `.\Test-TV.ps1`. Allow the tests to finish without remote or mouse inputs. For a physical TV, replace `emulator-5554` with the device serial shown by `adb devices`.

Build.ps1 uses the verified Microsoft OpenJDK 17.0.21, Gradle 8.9, SDK platform 35, and Build Tools 36.0.0 on D:. Android Studio's Java 25 runtime is unsuitable for this Gradle version. Gradle caches and temporary files remain on D:. If importing on another computer, update these machine-specific tool paths and create local.properties for its SDK.

The debug APK is suitable for initial sideloading. Release shrinking/ProGuard configuration is present. An unsigned release can be built with `gradlew.bat assembleRelease` after setting the same JAVA_HOME/ANDROID_HOME/GRADLE_USER_HOME values as Build.ps1. No private release signing key was generated or changed.

## Remaining constraints

- Third-party availability and their embedded players can change. A listed title or episode does not by itself prove that every source plays. Source switching stays within the selected title/episode.
- Chinese input uses the Android system keyboard; enable a Chinese-capable TV keyboard. The included A–Z keypad is only an optional English input shortcut.
- Filters and sorting apply to loaded search results. Unknown country/genre/year metadata cannot match a selected filter. Some providers do not supply those fields. Popularity ranking is not supplied by these adapters; rating remains a separate option.
- Cineby Xpass supports the shared timeline through its published player message protocol. Other embedded hosts retain their own controls; a universal native timeline cannot be guaranteed for hosts without that protocol. Audio, quality and subtitle options remain limited to what the selected source provides. App notices cover app-selected source changes; embedded players may also perform their own internal failover. Cineby timeline seeking uses the provider’s play-at command and resumes playback.
- This revision still needs the user's physical-TV testing; verification outcomes below distinguish compilation from device tests.

See VERIFICATION.md for the actual build and test outcomes for this revision.
