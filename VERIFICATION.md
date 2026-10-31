# Verification — 31 October 2026 (Singapore)

- Original archive unchanged: SHA256 `c98f6f0a3a5b5fb65b6e043eef7a96a8063c2d9b9be6a045a5b1c1010543e18b`.
- Archive inspection confirmed 18 embedded definitions, matching the web source, and missing `TvAppNavigation`, `MockData`, `CineStreamApp` definitions. Exporter-only strings/styles were omitted from the previous extraction.
- Existing native project backed up to `D:\CineStreamTV_Native_backup_20261031_225341` before changes. Retired original app source and malformed old version catalog also preserved there.
- Final command: `D:\CineStreamTV_Native\Build.ps1` (Gradle `assembleDebug testDebugUnitTest lintDebug`).
- Final build: **BUILD SUCCESSFUL**. 2 unit tests, 0 failures, 0 errors.
- Android lint: 0 errors, 9 warnings. Warnings concern newer dependency versions, backup declaration and raster density variants; no lint errors.
- APK: `D:\CineStreamTV_Native\app\build\outputs\apk\debug\app-debug.apk` (12,210,504 bytes).
- APK SHA256: `b70e74efec3ed71df6c5498f6e6589f69f46da61cea99aa186bf15d5a1e1000a`.
- `apksigner verify`: exit 0. This verifies the debug artifact's signature, not device installation.
- `aapt dump badging`: package `com.cinestream.tv`, version code 2, minSdk 26, target/compile SDK 35, phone and Leanback launch activities present, TV and touchscreen optional.
- Tool versions checked: Android Studio JBR 25.0.4; separate build JDK Microsoft 17.0.21.0.2; Gradle 8.9; AGP 8.6.1; Kotlin 2.0.21; Build Tools 36.0.0; independently installed SDK platform 35 revision 2. Gradle distribution checksum pinned to the official published checksum.
- First staging build encountered a Kotlin compiler daemon connection reset and succeeded using its in-process fallback. Final configuration uses in-process Kotlin compilation and two workers; the final destination build succeeded.
- Original MP4 samples: HTTP 403 from this computer, including a ranged GET for Big Buck Bunny. Catalog descriptions disclose this. Primary Apple HLS sample playlist: HTTP 200. These are desktop HTTP checks, not playback verification.
- ADB check: no devices connected. **Installation: not performed. Android device behavior: not verified.**
- Release configuration prepared, release artifact not built, no release signing key created or supplied. Debug signing is for development only.

Review `README.md` for exact build/install/start commands, integration requirements and remaining limitations. The original web exporter remains historical; use this native project.
