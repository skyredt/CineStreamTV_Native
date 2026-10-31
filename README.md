# CineStream TV 1.2.0

Repair baseline: skyredt/CineStreamTV_Native commit 7558d5c2ac5c2b34ba83eeee4f9f4123b3e0ead0. Work is in this separate folder; the original archive and D:\CineStreamTV_Native were not overwritten. No changes have been pushed to GitHub.

This is the native Android TV project. It retains the supplied Compose design, uses the configured Cineby and DBKU providers, and has no floating remote overlay. Phone launcher support has been removed following the updated TV-only requirement.

Repairs include the missing filter models, a malformed Kotlin string, the Cineby search parser, DBKU category/country/year mapping, provider failure reporting, real next-page links, and Chinese Unicode search. The Taiwan/Thailand category now uses DBKU's actual category rather than being labelled Hong Kong/Taiwan. Both providers retain exact episode URLs. No catalog selection falls back to a sample video.

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
- Filters and sorting apply to loaded search results. Unknown country/genre/year metadata cannot match a selected filter. Some providers do not supply those fields. Popularity data is not exposed by these adapters, so no popularity ranking is fabricated. Rating is labelled separately.
- Cineby iframe players control their own available audio, quality, and subtitle options. Native direct-media playback exposes only available tracks. Host-specific remote media support varies.
- This revision needs testing on the user's physical TV; previous physical-TV feedback concerned an earlier APK.

See VERIFICATION.md for the actual build and test outcomes for this revision.
