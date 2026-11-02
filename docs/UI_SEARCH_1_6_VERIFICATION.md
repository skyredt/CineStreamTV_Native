# CineDBKTV 1.6.0 — search reliability and Home/Browse

Date: 2026-11-03 (Asia/Singapore).

## Baseline and scope

The workspace was clean on `feature/adaptive-tv-mobile-1.5` at `408b8bb117dcc8b71a52841124e2e1fc7e18c26b`. Work is isolated on local branch `feature/home-browse-search-1.6`; previous APKs, reports and commits remain intact. Version is 1.6.0/code 12. No dependencies, APK flavors or providers were added. No push, PR or store publication was performed.

This task made no ADB/debugging connection to the Samsung phone and did not start the laptop emulator. Phone rendering/input/playback and physical Android TV acceptance remain separate gates.

## Cineby findings — evidence and limits

Three bounded, read-only HTTPS requests were made with the app's existing User-Agent and normal TLS verification. No authentication, anti-bot or rate-limit bypass was attempted. All returned HTTP 200 and remained on `cineby.ws/search`:

| Query | Observed response |
| --- | --- |
| `Smallville` | One `article.content-card`, valid result-count marker and actual TV detail link |
| Deliberately nonexistent `zzqvnoresult8675309` | Zero cards and an explicit `0 results` marker |
| Blank query | Search-library prompt/form, zero cards, no result-count marker |

The previous parser required cards or a result-count marker. The captured blank-query response therefore reproduces the exact previous rejection path. The mobile UI can submit blank queries when text is cleared or a filter is changed. This is a **verified defect and a plausible contributor**, not proof of the cause of the user's particular recording. The recording's exact failed request/response was not available, and its intermittent nonblank failure was not reproduced. The successful Smallville response confirms Cineby is not permanently unavailable. No markup-change claim is made.

Only reduced public result/form snippets are committed as regression fixtures. Full response bodies are local ignored investigation artifacts; no cookies, response headers, tokens or raw HTML are printed in the app or error logs.

## Search correction

- Blank/whitespace Cineby queries are not sent. A non-error prompt asks for a title; other provider capabilities continue independently.
- Valid cards retain provider identity and actual detail URLs. Invalid cards are not silently converted into a successful empty list.
- Empty results require an explicit zero-result marker. Missing/contradictory markup is an error, not a fabricated empty response.
- Recognizable challenge/access pages and HTTP 401/403/429 are classified as access restrictions. Timeouts/I/O and HTTP 408/5xx are temporary failures. Other unexpected response failures remain distinct.
- Search-result titles containing words such as “Access denied” are not treated as challenge headings merely because those words occur in a query title.
- UI messages are short, static and actionable; raw exception details are not surfaced. Existing Retry repeats the current query/filters without restarting. A prompt returned for a nonblank query is unexpected rather than asking the user to re-enter an already supplied title.
- Cineby failure preserves healthy BingeBang results. No provider failure mutates query, filters, navigation or another provider's results.

## Home: discovery

TV and mobile share the discovery section-selection model but keep adaptive navigation and sizing.

- Cinematic artwork/gradient hero, actual title/provider/available metadata, short synopsis, explicit Play/Continue and More Details actions. No autoplay trailer.
- Prominent Continue Watching row when suitable history exists: artwork, title, saved episode label when available, reported elapsed position and Continue/Return action.
- Zero-position episode history offers Return, not a false exact-resume claim. Entries lacking both progress and an episode selection are omitted.
- Elapsed time is the progress indicator; no duration/percentage is invented when the existing bridge supplies only position. Legacy entries without an episode label display a generic last-episode label. The optional episode-label field is backward compatible.
- Actual provider discovery feeds use their supplied labels: BingeBang trending/new releases, Cineby movies/series, DBKU sections. Cineby's aggregate row is omitted when its specific movie/series rows are available.
- Provider-qualified IDs deduplicate across discovery rows and exclude featured/Continue duplicates. Similar titles from different providers are retained independently.
- TV focus restoration follows a title if it moves into Continue Watching after playback. Mobile has vertical discovery sections with swipeable poster rows, not a duplicate Browse grid.

Hero/Continue actions load the actual details before invoking the existing playback coordinator; provider-specific routes/engines are unchanged.

## Browse: structured exploration

- Separate filter controls, loaded-result count and responsive poster grid; no large featured banner or Continue row.
- Actual catalogue categories, movie/series type (shorts remain a real DBKU category), English provider selection, available genre/category, year and region values, and provider-order/title/year/rating sorting over real loaded data.
- Filter options requiring metadata appear only when that metadata is supplied in the batch. The UI explicitly says filters/sorting apply to the loaded catalogue batch. It does not claim global/server-side catalogue coverage.
- Reset and Search Providers actions, catalogue Retry, empty-state guidance and partial-provider warnings.
- DBKU keeps its actual categories (陆剧, 日韩剧, 台泰剧, 短剧, 电影, 综艺, 动漫 where present). No independent 港剧/台剧 feed is invented.
- Broader DBKU region/genre search continues through the existing Search adapter. Browse does not invent unknown years/regions or perform hidden mass detail-fetches just to populate filters.
- TV gets Browse in the persistent sidebar; its other category entries also use the structured grid. Filter buttons retain focus borders, menus support normal Compose navigation, and saved grid/result state supports return from details. Mobile retains its bottom navigation, touch menus, portrait/landscape behavior and safe-area handling.

## Preserved functionality

Exactly Cineby, BingeBang and DBKU remain active. Hongguo remains inactive. Shared models/preferences/history, details/episodes, adaptive device selection, rotation behavior and existing search state are retained.

`EmbeddedPlayer.kt`, `Playback.kt`, `BingeBang.kt`, `tv-bingebang-player.js` and `tv-web-player.js` have no changes from the 1.5.0 baseline. BingeBang OpenSubtitles/server/file-picker behavior is not rewritten. `Providers.kt` changes are restricted to search classification/parsing; source resolution is unchanged. Existing TV acceptance expectations are updated for the new More Details hero action without removing the navigation/history checks.

## Tests and build

The initial final-source JVM run completed **86 tests, zero failures, two existing optional fixture-dependent skips**. All ten new Cineby search tests and all six discovery/Browse tests passed. A final UI-only correction removes the unsupported generic Shorts type option while retaining DBKU's actual 短剧 category; final incremental APK/lint results are recorded below when complete. New coverage includes captured successful/empty/prompt responses, malformed/contradictory responses, challenge recognition, HTTP/network classifications, partial results and retry; discovery section/deduplication policy; Continue/history/catalogue scoping; combined Browse filters; navigation/category policies and focus moving to Continue. Existing adaptive/provider/playback regression tests are retained.

No build/JVM result is a claim of actual Samsung rendering, advancing video, subtitle selection or TV D-pad usability. Runtime tests were not executed on the unstable laptop emulator. The Android TV test APK is compiled when the final validation completes.

The first 512 MB build was terminated by Windows native-memory exhaustion before compilation. A 384 MB/single-processor retry is used without altering user apps or system memory settings.

## Acceptance matrix

| Requirement | Status / evidence |
| --- | --- |
| Valid Cineby results | Captured live Smallville result; regression test pending final run |
| Genuine empty versus malformed/access/temporary | Explicit classifier/parser cases; final tests below |
| Healthy BingeBang partial results and Retry | Repository regression tests; physical phone retry still pending |
| Distinct Home and Browse | Separate discovery rows/hero/Continue versus filter/grid implementations |
| Actual content and no invented rankings | Provider data and labels retained; provider-qualified deduplication |
| English/Chinese behavior | Shared preference/provider policies retained; scope/category tests |
| TV D-pad and focus | Existing sidebar/controls retained with Browse; focus model tests; physical runtime pending |
| Mobile touch/navigation | Existing adaptive shell retained; touch filter/grid/discovery actions; physical runtime pending |
| Playback/subtitles | Engines/scripts/source resolution unchanged; fresh physical verification pending |
| Automated checks | Pending final result below |
| APK/hash/signature | Pending final artifact inspection below |

## Samsung acceptance — no ADB

1. Transfer `CineDBKTV-1.6.0-ui-search-debug.apk` normally to the phone. Open it in My Files, allow installation from that source if prompted, and install the upgrade.
2. On English Search, try `Smallville`, a deliberately nonexistent title, and clearing the query. Empty/prompt states should not show a parser warning. If Cineby fails, check that BingeBang results remain and Retry works with the same query/filters.
3. Compare Home and Browse in portrait and landscape. Home should have hero/discovery rows; Browse should have compact filters and a grid, with clear loaded-batch scope.
4. Play a title/episode, return to Home, and check Continue artwork, episode label and available elapsed position. Continue should return to the same provider/title/episode where supported. Reopen the app to check legacy/new history persistence.
5. Apply/reset type, provider, available genre/year/region and sort filters; open a result and return. Verify category/filter/scroll state remains usable. Use Search Providers for wider searching.
6. Switch to 中文 and repeat discovery/category/search checks. Confirm actual DBKU categories rather than invented standalone feeds.
7. Smoke-check each provider's existing playback, subtitles and source switching. Physical Android TV still needs sidebar/D-pad/filter-menu/grid/Back/focus checks.

For a remaining Cineby failure, report the exact query, selected filters, provider message, whether Retry helped, and whether it followed clearing/changing a query. Do not provide cookies, tokens or raw HTML. The original recording's specific intermittent cause remains unconfirmed unless reproduced with suitable evidence.

## Changed files

- `app/build.gradle.kts`: version 1.6.0/code 12.
- `Providers.kt`, `SearchFailures.kt`, `SearchModels.kt`: evidence-supported blank-query handling, parsing and safe failure classification/status.
- `DiscoveryModels.kt`, `DiscoveryScreens.kt`: shared Home selection/Continue/filter policies and adaptive discovery/Browse presentations.
- `CatalogueExperience.kt`: backward-compatible saved episode label.
- `NativeApplication.kt`: shared hero/Continue routing and TV Home/Browse destinations; remove duplicate catalogue-warning overlay.
- `MobileScreens.kt`: delegate Home/Browse to the new distinct presentations, display search prompts, update Settings version.
- `DesignScreens.kt`: TV Browse sidebar entry.
- `CinebySearchReliabilityTest.kt`, `DiscoveryBrowseTest.kt`: focused regressions.
- `cineby-16-smallville.html`, `cineby-16-empty.html`, `cineby-16-blank.html`: reduced public-page fixtures.
- `CatalogueExperienceCheck.kt`: updated hero label/action and episode-label persistence assertion.
- This report. Java/Kotlin files are under `app/src/main/java/com/cinestream/tv` or the existing test source sets.

## Final completion and handover

The user authorized completion after the five-point stop. The frozen final source (including the type-filter correction and composable naming fix) passed the complete build: assembleDebug, testDebugUnitTest, lintDebug and assembleDebugAndroidTest. Final JVM results: 86 tests, zero failures, two existing optional skips. Final lint: 0 errors, 5 warnings. Evidence: ui-search-16-completion.log. APK signature verification passed; the delivery APK hash matches the validated build.

APK: D:\CineStreamTV\cinedbktv-development\CineDBKTV-1.6.0-ui-search-debug.apk
SHA-256: 02C1AF614E0B0ADB8FC6D53DCC8EA7C279CD006242B0C267FF5ACEB1CEA68BD0

No phone/TV runtime verification was performed for this version. No ADB connection, emulator recovery, push or PR was performed. The original recording's exact intermittent failure remains unconfirmed; the blank-query rejection was reproduced and corrected.

Handover: docs/HANDOVER_1_6.md. Further work should start with manual Samsung acceptance, not more unrelated implementation or emulator recovery. Preserve this APK and all earlier artifacts.
