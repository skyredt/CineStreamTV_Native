# CineDBKTV catalogue overhaul — verification

Date: 2026-11-02 (Asia/Singapore). This report distinguishes source/build checks from Android runtime and physical-TV acceptance.

## Baseline and scope

- Workspace: `D:\CineStreamTV\cinedbktv-development`.
- Origin was verified to match the requested `skyredt/CineStreamTV_Native` repository.
- Started on clean branch `feature/hongguo-single-apk`, HEAD `ed5e309c7cb056c52e62d180aea9fe5e81e61016`.
- Existing Kotlin/Compose architecture, Gradle 8.9, JDK 17, SDK 35, minimum Android 26 retained.
- Version: 1.4.0, version code 10, debug variant. This is a debug-signed APK, not a store release.
- Exactly three active catalogue/search providers: Cineby, BingeBang and DBKU. Historical Hongguo source/artifacts retained; no Hongguo network requests or selectable catalogue controls.
- Native/embedded player implementations and both player JavaScript assets are unchanged. Playback routing continues through `ProvidersRepository.sources` and existing `EmbeddedPlayer`/`PlayerScreen`.

## Implemented

- First-run English/中文 chooser, SharedPreferences persistence, sidebar switching without restart. Catalogue choice does not change Android locale.
- Isolated English and Chinese home/search requests. English searches Cineby+BingeBang; Chinese searches DBKU. A failing search provider does not discard healthy results.
- Persistent TV focusable sidebar, live featured title, lazy poster rows, movie/series filtering, verified BingeBang trending and new-release feeds, loading/retry/empty/error states.
- Chinese navigation uses actual current DBKU sections: 陆剧, 日韩剧, 台泰剧, 短剧, 电影, 综艺, 动漫. No unsupported standalone 港剧/台剧 section is advertised.
- Search retains query, filters, results, scroll and selected result across details. Type/year/sort operate on real loaded results; DBKU country/Chinese genre browse uses the existing verified filter-link adapter. English hides DBKU-only filters. No invented popularity sort or provider pagination.
- Shared native details retain provider identity, poster, synopsis, actual available year/rating/genres/region/cast/season/episode data. Added original-title, director, update-time and rating-source fields. Unknown metadata is omitted. Movies hide episode/season controls; DBKU long lists retain 50-episode paging and jump.
- Persistent recent history (up to 100 titles), provider-qualified title/episode identity, progress when the existing player reports it, Continue/return-to-last-episode, per-title removal and per-catalogue clearing. Missing prior episodes are rejected rather than applying a saved timestamp to a different episode.
- Saveable screen state and focus requesters restore catalogue/search/details/history navigation state. Exact remote behavior still requires runtime acceptance below.

## Provider evidence

Public HTTP fetches on this date returned 200 for all three homepages, Cineby/BingeBang English searches and DBKU Chinese search.

- BingeBang's published script uses `/api/list/trending_movie_week`, `/api/list/trending_tv_week` and `/api/list/movie_now_playing`. The new-release endpoint returned 24 real movie records.
- DBKU `https://www.dbku.tv/voddetail/1951.html` returned 斗罗大陆 metadata including 2021, rating 8.9, 大陆, 杨振宇, update `2021-02-21 09:40:35`, cast and 40 episode links. A reduced public-page fixture tests those fields and confirms director changes are parsed, not hardcoded.
- Public-page success does not establish Android networking, playable media, subtitle selection or physical remote behavior.

## Automated checks

- Unit tests: 65 discovered, 0 failures, 2 existing optional fixture-dependent tests skipped.
- Lint: 0 errors, 6 warnings on the first completed full validation.
- `assembleDebug` and `assembleDebugAndroidTest` succeeded.
- Final small history-focus/test-timing changes: complete incremental validation passed (`catalogue-delivery-validation.log`, BUILD SUCCESSFUL). The final DBKU keyboard-focus correction also passed the full APK/test APK/unit-test/lint run (`catalogue-keyboard-validation.log`, BUILD SUCCESSFUL).
- `git diff --check` passes.
- Player implementation and player-script diffs are empty.

The first build failed from native memory exhaustion. A 512 MB retry compiled but exhausted D8's Java heap. A 768 MB build with Gradle's instrumentation agent disabled succeeded. No unrelated user programs or system memory settings were changed.

## Runtime acceptance

The Android 34 TV emulator (1280×720) booted successfully at 768 MB RAM with Windows Hypervisor acceleration. Both APKs installed successfully. `catalogue-runtime-verified.log` records seven passing checks:

1. First launch and D-pad/OK English selection with rendered live home.
2. Featured/poster action opens native details; Android Back restores home focus.
3. Catalogue preference persists across activity restart.
4. D-pad sidebar switches to Chinese without restarting; DBKU home renders.
5. Live English search returns Cineby and BingeBang results; Chinese search returns DBKU results.
6. Live DBKU reference includes cast, director and 40 episodes.
7. History serialization preserves provider identity, exact episode and reported position.

The first acceptance run had an off-screen Back-button assertion: the loaded details screen had scrolled to its focused Play button. The corrected assertion checks the loaded Play control; the rerun passes. There was no app crash in the emulator crash log.

No physical Android TV is attached. The initial 1536 MB emulator launch was rejected by Windows commit pressure (approximately 329 MB available versus 1536 MB required). The smaller emulator succeeded after the build completed.

The opt-in instrumentation check is `TvSmokeRunner` with `-e catalogueExperience true`. It exercises first launch using D-pad/OK, persisted catalogue selection, details/Back, sidebar switching, live isolated searches, DBKU reference metadata and history serialization. **It clears the target app's catalogue/history preferences; run it on the test emulator.** The existing `-e bingeProvider true` check separately tests advancing BingeBang playback, server switching and Back behavior. Subtitle regression must also be checked on a real TV.

## Acceptance matrix

| Criterion | Evidence / status |
| --- | --- |
| First-launch English/中文 chooser | PASS on TV emulator |
| Selection survives restart | PASS across emulator activity restart; force-stop/relaunch also PASS (Chinese home reopened without chooser) |
| Switch catalogue without restart | PASS via D-pad on TV emulator |
| Both live homepages | PASS: English and Chinese live homes render on TV emulator |
| D-pad categories | PASS for home and catalogue-switch traversal; exhaustive category traversal pending |
| English two-provider search | PASS: unit isolation tests + live Android adapter searches |
| Chinese DBKU search | PASS: unit isolation test + live Android DBKU search |
| Working filters | Real loaded-result filters and existing DBKU browse adapter; fixture tests pass; interactive filter traversal pending |
| Posters open details | PASS for featured action; complete poster-grid traversal pending |
| Actual metadata | PASS: live Android DBKU reference + parsing regression test |
| Seasons/episodes | Live 40-episode DBKU reference parsed; native 47-episode list and D-pad jump/range navigation verified; exhaustive multi-season traversal pending |
| Existing player routing | Existing sources/players retained; BingeBang S01E02 playback/server switching PASS on Android; Cineby/DBKU native-route recheck limited as detailed below |
| BingeBang subtitles | Player/subtitle scripts unchanged; advancing BingeBang playback and server switching PASS; subtitle language/file/upload controls need physical-TV recheck |
| Back/focus restoration | PASS for details-to-home Back/focus; search/episode restoration implemented, exhaustive UI recheck pending |
| Loading/failure/empty states | Implemented; partial failure/isolation tests pass |
| APK build | Debug build passes |
| Automated tests | 65 tests, 0 failures, 2 skipped |
| Emulator/device | PASS: Android 34 TV emulator at 768 MB; no crash observed |
| Physical TV | Not performed; no device attached |

## Build and install

From PowerShell:

```powershell
Set-Location 'D:\CineStreamTV\cinedbktv-development'
$env:JAVA_HOME = 'D:\BuildTools\jdk17\jdk-17.0.21.0.2+1'
$env:GRADLE_USER_HOME = 'D:\.gradle'
$env:ANDROID_HOME = 'D:\AndroidSdk'
.\gradlew.bat --offline --no-daemon '-Dorg.gradle.internal.instrumentation.agent=false' '-Dorg.gradle.jvmargs=-Xmx768m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8' assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest
& 'D:\AndroidSdk\platform-tools\adb.exe' devices -l
& 'D:\AndroidSdk\platform-tools\adb.exe' -s YOUR_DEVICE_SERIAL install -r 'D:\CineStreamTV\cinedbktv-development\CineDBKTV-1.4.0-catalogue-debug.apk'
& 'D:\AndroidSdk\platform-tools\adb.exe' -s YOUR_DEVICE_SERIAL shell am start -n com.cinestream.tv/.MainActivity
```

Use the actual serial from `adb devices -l`. `install -r` preserves app data. Do not uninstall to resolve a signing mismatch without preserving any desired local history.

On TV, verify both chooser options, restart persistence, all sidebar items, search/filter results, a movie and a series on each provider, episode selection, Play/Continue, source switching, subtitles, Back to details/search, restored focus, history removal/clearing and an unavailable-provider case. For BingeBang specifically recheck OpenSubtitles language/file selection and external subtitle loading.

## Remaining limits

- No standalone DBKU 港剧/台剧 catalogue is claimed; the actual provider groups Taiwan with Thailand.
- Type/year/sort filters apply to returned batches; BingeBang search pagination is unavailable in the existing adapter. DBKU country/genre browse searches title text within filtered pages, not a global server-side combined keyword index.
- No exact resume claim for players that do not report position or accept the existing resume bridge.
- DBKU country/genre browse retains the existing adapter coverage (movie/drama/animation/short types); no independent variety browse-filter capability is claimed.
- For the numeric episode keyboard, use Done/checkmark (or Enter) to return focus to episode ranges; the first Android Back may only dismiss the keyboard.
- No physical-TV acceptance or new subtitle/playback claim follows from a successful build.

## Delivery artifact

`D:\CineStreamTV\cinedbktv-development\CineDBKTV-1.4.0-catalogue-debug.apk`

SHA-256: `FB8E88AFC286E4AB950514E2783C5D225FD71E145F01A45F1A6CA19CA6713D67`.

Local implementation branch: `feature/catalogue-navigation-overhaul` (based on the baseline above). Existing named APK artifacts were preserved.

## Emulator test commands

On the dedicated test emulator (the catalogue check resets only that emulator app's catalogue/history preferences):

```powershell
& 'D:\AndroidSdk\platform-tools\adb.exe' -s emulator-5554 install -r 'D:\CineStreamTV\cinedbktv-development\app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk'
& 'D:\AndroidSdk\platform-tools\adb.exe' -s emulator-5554 shell am instrument -w -e catalogueExperience true com.cinestream.tv.test/com.cinestream.tv.TvSmokeRunner
& 'D:\AndroidSdk\platform-tools\adb.exe' -s emulator-5554 shell am instrument -w -e bingeProvider true com.cinestream.tv.test/com.cinestream.tv.TvSmokeRunner
```

## Playback regression and native follow-up

`catalogue-bingebang-playback.log` records five passes: live movie/TV rows; live search/details and exact S01E02 identity; advancing Android WebView playback and multiple servers; D-pad second-server selection with continued playback; Back closes server settings before requesting return to native details. The existing test overrides the activity content to isolate the existing player; it does not by itself validate every native catalogue-to-player route.

A manual force-stop/relaunch opened the persisted Chinese catalogue without a chooser. D-pad OK opened 兰香如故 native details with actual metadata and 47 episodes. D-pad Down reached the jump control. This exposed an existing TV numeric-keyboard focus problem; the final patch adds Done/Back dismissal to a focused, visible episode-range control, with scrolling for long lists. Final runtime recheck confirms Done/Enter dismisses the numeric keyboard and focuses the active range (`1–47` in the checked title). Android's first Back can dismiss the IME before the app's Back callback runs, leaving the field focused; use the keyboard Done checkmark/Enter to continue to the episode range. This path is fully remote-operable. Native DBKU episode playback is checked separately below.

## Files changed

- `app/build.gradle.kts`: version 1.4.0/code 10 only; no dependency upgrade.
- `CatalogueExperience.kt`: catalogue modes/preferences, local history, chooser, catalogue home and history UI.
- `NativeApplication.kt`: catalogue-aware state/navigation and history integration with existing playback callbacks.
- `DesignScreens.kt`: provider-aware sidebar, shared metadata/details, provider labels, search filter availability and saved episode focus.
- `DesignCatalog.kt`: optional original-title/director/update/rating-source fields.
- `Providers.kt`: isolated three-provider home/search, verified new-release feed and metadata extraction.
- `BingeBang.kt`: original-title/director/rating-source metadata; playback route unchanged.
- `DbkuEpisodes.kt`: resume indicator, focus restoration and TV numeric-keyboard exit behavior.
- `SearchModels.kt`: TV Series filter label.
- `CatalogueExperienceTest.kt` + `dbku-reference-detail.html`: five focused regressions and reduced public-page fixture.
- `BingeBangTest.kt`: partial feed-failure expectation includes new-release feed.
- `HongguoTest.kt`: existing active-search expectation now confirms retired provider is absent; historical adapter tests remain.
- `CatalogueExperienceCheck.kt` + `TvSmokeRunner.kt`: opt-in/default catalogue acceptance dispatch; historical special-purpose runner flags retained.
- This verification report.

## Final checkpoint and unresolved acceptance

The approved seven weekly percentage points were reached (40% used at start, 47% used at stop; five-hour window 44% used). Further implementation/testing stopped. Changes remain uncommitted on `feature/catalogue-navigation-overhaul`; no push or PR was made.

The final native DBKU playback attempt made the reduced-memory emulator/ADB unresponsive. Its result is **unverified**, not a confirmed application playback failure. The exact emulator processes started for this task were stopped to release memory. Do not treat that attempt as a playback/resume pass.

Verified: final APK/test APK build, 65 unit tests with zero failures/two skipped, lint zero errors/six warnings, seven catalogue acceptance checks on the emulator, five existing BingeBang playback/server/Back checks, force-stop catalogue persistence, native DBKU details/47-episode controls, and Done/Enter returning from the numeric keyboard to the episode range.

Remaining: native Cineby and DBKU end-to-end playback/resume recheck; exhaustive interactive category/filter/episode/search-focus testing; BingeBang subtitle language/file/external-upload recheck; physical-TV acceptance. The current APK is a development deliverable, not a fully accepted production release. A stable emulator or physical Android TV is the smallest next step for these remaining checks.
