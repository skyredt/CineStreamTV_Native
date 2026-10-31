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
