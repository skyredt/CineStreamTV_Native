# CineDBKTV 1.3.1 — phone launcher update

- Build succeeded: assembleDebug, testDebugUnitTest, lintDebug. The 16 existing tests remain passing (up-to-date); Android lint reports 0 errors and 14 warnings.
- APK binary inspected with aapt: version code 8, version 1.3.1, application label CineDBKTV, both standard LAUNCHER and LEANBACK_LAUNCHER activities present, Leanback optional.
- APK signature verified with apksigner. Application ID and signing identity unchanged so this updates the existing app. TV interface and landscape orientation unchanged.
- This revision was not installed or tested on a phone by the agent. The launcher configuration was verified in the compiled APK.
- APK SHA256: c72bfe30846bcc1fc75d4f224440fc194811846777154efa218cebe5cef3c416

Previous revision verification follows.

# CineDBKTV 1.3.0 verification — 1 November 2026 (Singapore)

- Final build: BUILD SUCCESSFUL. Tasks: assembleDebug, testDebugUnitTest, lintDebug, assembleDebugAndroidTest. See build-1.3-delivery.log locally.
- Unit tests: 16 passed; 0 failures/errors. Covers provider mapping, captured Lanterns and Chinese search results, exact episode links, DBKU-only filter scope, episode ranges, seek bounds/acceleration, and source failover exhaustion.
- Android lint: 0 errors, 14 warnings. Warnings mainly concern available dependency upgrades, TV resource densities, fixed landscape, WebView JavaScript and backup declarations.
- Final APK signature verified with apksigner. Package com.cinestream.tv, version code 7, version 1.3.0, label CineDBKTV, minimum SDK 26, target/compile SDK 35, TV launcher.
- Emulator: APK installation and startup succeeded. Live search and Lanterns details were visually observed. The full accessibility-based smoke suite did NOT complete; it was stopped after slow navigation to respect the requested budget. Process-crashed output in the smoke log followed deliberate test termination and is not evidence of a spontaneous application crash. The final remote-focus adjustments were compiled and checked, but have NOT been verified on a device.
- Physical TV: not tested for version 1.3.0. User testing remains required for Cineby remote volume, seeking, source fallback, and very long DBKU episode lists.
- Native playback and the Cineby Xpass host share a timeline. Other embedded hosts retain provider controls. App source-switch notices cannot observe a provider's internal CDN/source changes. DBKU filtered search checks loaded filtered catalog pages, with explicit load-more support.
- Original archive and existing native project preserved. Source backup: ../backups/v1.2.0_20261101_043808. No release signing key generated or changed.

APK SHA256: b37372c6c74bcf3da7ce46878efbbf7749fd2edaea4cb609b37391b77cc3ba52
