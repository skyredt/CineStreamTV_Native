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

## Phase 5J incremental registration — 2 November 2026

- `HongguoRegistration` is an independently written, dormant Kotlin registration component. It is not connected to catalog/provider/player routing. Hongguo playback remains disabled.
- Anonymous registration accepted CineDBKTV's own package/version profile: HTTP 200 with nonzero device and installation IDs. No official-app package impersonation, account credentials, captured sessions or cookies were used. Identifiers are redacted from diagnostics; the application adapter uses private preferences, with application backup already disabled.
- Registration has bounded transport retries, response size/digest/ID validation, explicit invalidation, and no invented upstream expiration period. Private Android preference retention still requires device verification; the live JVM check verifies in-memory retention and reuse.
- A separate disposable diagnostic obtained episode 1 metadata for 《一个人的长征》: business code 0, duration 140.714 seconds, four HEVC/bytevc1 qualities (360p/480p/540p/720p). Intermittent empty metadata responses were observed.
- HTTP Range returned 206. Structural inspection of the first 64 KiB of each quality found encrypted visual sample entries (`encv`), original-format boxes (`frma`), `schm=cenc` and `tenc`. Media prefixes were inspected in memory, not saved.
- The returned fallback metadata contained wrapped-key fields, with no license URL found by the diagnostic. Keys were not decoded, and video was not decrypted. CENC alone does not establish the legal/authorization status; an authorized key/license workflow or confirmed clear-media alternative remains unresolved.
- No playback within CineDBKTV was demonstrated for episodes 1, 51 or 90. D-pad, seeking, fullscreen, audio, progression and physical-TV acceptance are unverified for Hongguo. No companion workflow, Go service or playback dependency was added.
- Validation: 41 unit tests passed (7 new registration tests, including opt-in live registration), debug build passed, lint passed with 0 errors and 6 existing warnings. The first combined run failed due to native JVM memory exhaustion; separate single-worker test and build/lint runs succeeded. No emulator was started for this increment.
