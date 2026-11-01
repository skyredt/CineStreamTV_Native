# CineDBKTV 1.3.2 verification — 1 November 2026

- assembleDebug, testDebugUnitTest and lintDebug completed successfully in build-1.3.2-verified.log. 16 tests, 0 failures; lint 0 errors, 14 warnings.
- APK installed in Android TV API 34 emulator. Production EmbeddedPlayer loaded a real published Cineby/Xpass episode source. Playback advanced; source buffering was also observed.
- Final packaged APK: Android remote key events moved through Pause, Forward, Rewind, Mute, Servers and Settings without entering the timeline. Center opened the actual quality menu showing Auto, 1080p and 720p.
- Player measured 961.995 x 540.998 CSS pixels against viewport 962 x 541. Video preserves aspect ratio and encoded black bars.
- Earlier component inspection successfully changed actual JW quality to 720p; final packaged APK quality-change and Back recheck were interrupted when the emulator stopped. These remain physical-TV acceptance checks.
- Tested source reported captions Off only. No missing tracks are invented. Other sources, subtitle selection, timeline interaction, phone touch behavior and DBKU regression playback require further device testing.
- No on-screen D-pad is rendered (FloatingRemote has no call sites). Native duplicate timeline/header and global Cineby arrow-volume mapping removed. Arrows on a selected timeline may seek; arrows over buttons navigate.
- Live transcription is not implemented. Existing provider subtitle options remain provider-dependent.
- No physical TV tested by the agent. This is a debug TV test candidate, version 1.3.2 / code 9. Phone launcher retained. No private signing key created or changed.
- SHA256: ccf2376a0ce2587dabbdc2f5c6cb05ebeb8146fa7cd33f48875fd3a90a8a93d5

Build with Java 17 and D: SDK/cache: .\gradlew.bat --offline --no-daemon --console=plain assembleDebug testDebugUnitTest lintDebug
Install: D:\AndroidSdk\platform-tools\adb.exe install -r CineDBKTV-1.3.2.apk
