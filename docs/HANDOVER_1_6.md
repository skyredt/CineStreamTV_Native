# CineDBKTV 1.6.0 handover

Date: 2026-11-03 (Asia/Singapore).
Workspace: D:\CineStreamTV\cinedbktv-development
Branch: feature/home-browse-search-1.6
Baseline: 408b8bb117dcc8b71a52841124e2e1fc7e18c26b (1.5.0).
The local completion commit contains this handover and the detailed verification report; use git log -1 to identify it. Nothing was pushed.

## Delivered

One adaptive TV/phone debug APK, version 1.6.0/code 12:
D:\CineStreamTV\cinedbktv-development\CineDBKTV-1.6.0-ui-search-debug.apk
SHA-256: 02C1AF614E0B0ADB8FC6D53DCC8EA7C279CD006242B0C267FF5ACEB1CEA68BD0

86 JVM tests, zero failures, two optional skips. Lint: 0 errors, 5 warnings. App/test APK builds and signature verification pass. Final evidence: ui-search-16-completion.log. Detailed findings, exact changed-file groups and acceptance checklist: docs/UI_SEARCH_1_6_VERIFICATION.md.

## What changed

- Three legitimate live Cineby requests confirmed Smallville succeeds, a nonsense query returns an explicit empty result, and a blank query returns a prompt that the old parser rejected. Blank Cineby requests are now skipped with a non-error prompt. Malformed, access-restricted and temporary failures are distinguished with safe messages. Healthy BingeBang results and same-query retry are preserved.
- Home now uses a cinematic hero, actual discovery feeds and Continue Watching with saved episode labels/reported elapsed time. Provider-qualified IDs deduplicate rows; focus follows played titles into Continue.
- Browse uses categories, real loaded-batch filters, provider/type/sort controls and a responsive grid, without a hero. Year/region options appear only when supplied; Search remains available for wider provider querying. DBKU's real short-drama category remains; an unsupported generic Shorts type option is omitted.
- Existing adaptive navigation, details/episodes, three active providers and history remain. Playback engines, BingeBang adapter and player scripts are unchanged. Hongguo remains inactive.

## Not proven / next acceptance

The exact intermittent Samsung recording failure was not reproduced. The blank-query defect is a verified contributor, not proof of that recording's cause. No 1.6.0 physical-phone/TV rendering, D-pad, playback, subtitle or resume acceptance is claimed.

1. Transfer the APK normally to the Samsung S25 FE; open it in My Files, allow installation from that source if requested, and install. No developer options, ADB or remote control.
2. Test Smallville, a nonexistent query, clearing search and Retry. Confirm BingeBang remains usable on a Cineby failure.
3. Compare Home versus Browse in portrait/landscape; test Continue, filters/reset, opening details and returning, and English/中文 switching.
4. Smoke-check each provider's playback/subtitles/source switching and history. Physical Android TV remains a separate D-pad/focus/Back acceptance gate.
5. If Cineby fails again, record exact query, filters, message and retry outcome. Do not share cookies/tokens/raw HTML.

## Execution boundaries

Work stopped at the user's five-point cap, then resumed only with explicit authorization to finish validation/package/commit/handover. No additional feature work is authorized by this handover. Obtain a fresh usage balance before further work; respect the user's five-percentage-point hard limit. Do not infer a seven-point allowance from the older attachment. Do not restart repeated emulator recovery.

## Rebuild

Use the existing JDK 17, Android SDK and D:\.gradle cache. The successful low-memory Gradle arguments were --offline --no-daemon, -Dorg.gradle.internal.instrumentation.agent=false, and -Dorg.gradle.jvmargs=-Xmx384m -XX:ReservedCodeCacheSize=64m -XX:+UseSerialGC -XX:ActiveProcessorCount=1 -Dfile.encoding=UTF-8 (quote the whole JVM-args argument in PowerShell). Run assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest. No dependency upgrades are needed.
