# CineDBKTV Phase 5J handover — 2 November 2026 (Asia/Singapore)

**Latest work:** See section 14 for the reported verbal reuse permission, new isolated metadata resolver, validation and current resource accounting. Sections 1–13 preserve earlier evidence and historical readings. Playback is still disabled; the resolver does not change provider routing or decode protected keys.

## 1. Status and stopping point

- **Playback working inside CineDBKTV: NO. Hongguo playback enabled: NO.**
- Hongguo catalog, Chinese search, details and episode navigation work from Phase 4. The existing Cineby, DBKU and BingeBang providers and search/sidebar layout have not been changed by Phase 5J.
- Anonymous registration is independently implemented in Kotlin and verified against the server. Episode 1 playback metadata was retrieved by a separate disposable diagnostic; a production metadata resolver has NOT been implemented or wired into provider routing.
- All four previously inspected media qualities have CENC-encrypted visual tracks. No content keys were decoded and no video decrypted. No supported Media3 licence configuration or confirmed clear alternative has been established.
- This continuation stopped investigation early to preserve the user's five-hour reserve. No additional development, build, playback test or alternative approach is authorized after this handover without a new user instruction.

## 2. Repository and Git

- Repository: `D:\CineStreamTV\cinedbktv-development` (`skyredt/CineStreamTV_Native`).
- Branch: `feature/hongguo-single-apk`.
- Implementation HEAD/start of this continuation: `ef8275d95935ca1c6e8295b80903de8e6600cc58`, `Add isolated Hongguo anonymous registration component`.
- Working tree was clean at start and after investigation; only this handover is intended to become a tracked change in this continuation. No application source changes were made in this continuation.
- This document is intended for a local documentation-only successor commit. Obtain its exact commit with `git log -1 --format=%H -- PHASE_5J_HANDOVER.md`; obtain current HEAD with `git rev-parse HEAD`. The final chat response records the successor SHA.
- Local implementation commit `ef8275d` is not pushed. The isolated feature branch has no upstream. No pushes, merges, publications or deployments were performed.
- Preserved branches: `development/cinedbktv-1.3` at `87e5e90d20c7d9eef1b5434f04c91c83c7bcc8bf`; `feature/hongguo-companion-handoff` at `a914a7ff6fbb42ff4b5ed5106b1477ee336e37cb`.
- PR [#1](https://github.com/skyredt/CineStreamTV_Native/pull/1) was rechecked this continuation: open, draft, unmerged; head `development/cinedbktv-1.3` at `87e5e90`, base `main` at `7558d5c2ac5c2b34ba83eeee4f9f4123b3e0ead0`. It does not contain the Phase 5J local commit.

## 3. Implemented files and relevant existing components

| File under repository root | Responsibility/status |
| --- | --- |
| `app/src/main/java/com/cinestream/tv/HongguoRegistration.kt` | Dormant independent Kotlin component: random anonymous identity, registration envelope/signature, bounded HTTP transport retries, nonzero ID validation, redacted object representations, private preferences adapter, explicit invalidation. No confirmed upstream TTL; none invented. No player or provider calls it. |
| `app/src/test/java/com/cinestream/tv/HongguoRegistrationTest.kt` | Seven tests: registration envelope integrity, own-app request identity, response validation/redaction, cached identity reuse/invalidation, bounded retry/rejection, storage failure/cancellation, opt-in real registration. |
| `VERIFICATION.md` | Prior committed Phase 5J results and limitations. Read the correction in section 5 below alongside these notes. |
| `app/src/main/java/com/cinestream/tv/Hongguo.kt` | Existing on-demand website catalog/search/details parsing, native Chinese titles and opaque episode IDs. |
| `app/src/main/java/com/cinestream/tv/Providers.kt` | Existing provider dispatch; explicitly rejects Hongguo playback. Do not remove the guard before a supported usable source is verified. |
| `app/src/main/java/com/cinestream/tv/NativeApplication.kt` | Existing state/navigation and Hongguo playback guard. |
| `app/src/main/java/com/cinestream/tv/DbkuEpisodes.kt` | Existing episode ranges/jump; Hongguo remains catalog-only. |
| `app/src/main/java/com/cinestream/tv/Playback.kt` | Existing Media3 `DefaultHttpDataSource`/`DefaultMediaSourceFactory`, plain `MediaItem.fromUri(url)`. No Hongguo DRM session or licence setup. |
| `app/src/main/java/com/cinestream/tv/EmbeddedPlayer.kt` | Existing embedded playback; no Phase 5J changes or new prototype. |

No Go component, player library, backend, companion workflow or production playback resolver was added. Registration-envelope encryption/decryption is distinct from decrypting protected video.

## 4. Registration evidence

- Endpoint: `POST https://log.snssdk.com/service/2/device_register/`.
- Declared package/version: `com.cinestream.tv`, `1.3.2` / code 9. Uses the observed service application ID 8662, freshly generated anonymous identifiers and request signing/envelope construction. It does not claim package `com.phoenix.read`.
- Actual HTTP 200 responses contained valid, nonzero device and installation identifiers. Acceptance was checked by parsing IDs, not by HTTP status alone. This succeeded in the standalone diagnostic and the opt-in Kotlin unit test.
- No account login, account credentials, captured sessions or cookies were used. Actual returned IDs and raw response bodies are not saved in Git or diagnostic logs.
- The live Kotlin test verified identity retention/reuse in a memory store and explicit invalidation. Android private-preference retention across process/device restarts has NOT been emulator/device verified. The existing manifest disables application backup.
- Successful registration demonstrates technical acceptance of the tested client profile. It does not by itself settle permission for protected-media key processing or general redistribution.

## 5. Playback metadata and corrected interpretation

Drama: **《一个人的长征》**, series ID `7695727473111092286`.

| Episode | Video ID from existing catalog fixture | Playback result inside CineDBKTV |
| --- | --- | --- |
| 1 | `7695729320861387838` | NOT PERFORMED; metadata only. No observed video/audio duration. |
| 51 | `7695729319137528894` | NOT PERFORMED. |
| 88 | `7695729345238666302` | NOT PERFORMED. |
| 90 | `7695729410976009278` | NOT PERFORMED. |

The latter three IDs were read from the existing 90-entry `app/src/test/resources/hongguo-detail.json` fixture during handover preparation, not re-fetched live this continuation. Revalidate their current episode association through the catalog before future playback testing; never guess IDs.

- Metadata endpoint: `POST https://api5-normal-sinfonlineb.fqnovel.com/novel/player/multi_video_model/v1/` using registered anonymous identity. No previously captured authenticated request was replayed.
- Episode 1 success: HTTP 200, business code `0`, model duration **140.714 seconds**, qualities **360p / 480p / 540p / 720p**, codec `bytevc1` (HEVC). Duration is metadata, NOT watched playback.
- Prior bounded header inspection: each quality returned HTTP 206 for Range; first 65,536 bytes were inspected in memory. Structural MP4 parsing found `encv`, `frma`, `schm` with scheme `cenc`, and `tenc`. No episode file was retained.
- This continuation made no media requests. It first received two empty HTTP 200 metadata responses; one final fresh anonymous identity yielded successful metadata and fallback metadata. The intermittent empty response is an availability/recovery concern, not evidence of an authorization denial.
- **Important correction:** earlier diagnostic fields `hasEncryptionMetadata=false` / `hasWrappedContentKey=false` inspected nested `row.encrypt_info.spade_a` only. The successful schema-only pass found key metadata DIRECTLY on every row in BOTH the initial decoded `video_model` and fallback model:
  - `video_list.video_1` through `video_4`: `encrypt` (Boolean), `encryption_method` (String), `gear_des_key` (String), `spade_a` (nonempty String); row field names also include `kid`.
  - Model-level `key_seed` is present. `url_expire` is also a model/row field; its actual expiry semantics have not been verified.
  - The old `hasWrappedContentKey=false` values must NOT be interpreted as absence of a wrapped key. The diagnostic summary code was corrected after the schema capture, without another request.
- Fallback metadata path: `video_info.data`; initial `video_model` is a JSON-encoded object and must be parsed before field inspection. The returned fallback URL was followed only on an allowlisted HTTPS provider host; it and media URLs were neither logged nor retained.
- The schema pass looked recursively for licence/license, DRM, PSSH, key, encryption, clear, Widevine, PlayReady and FairPlay-related field names/types. No licence/DRM endpoint field was identified in the inspected models; the older narrower count of HTTPS `license` fields was zero. This is a scoped observation, NOT proof no such mechanism exists anywhere.
- The schema pass saved field names/types/presence only, not protected field values. No content key was extracted/decoded, no key-unwrapping implementation was added, no CENC video was decrypted, and no DRM/payment/episode-lock control was bypassed.
- No accessible clear-media alternative was established. Do not infer a clear source solely from missing nested `encrypt_info`.

## 6. Tests, builds and acceptance limits

Previously completed at `ef8275d` (source unchanged this continuation):

- **41 unit tests, 0 failures, 0 errors, 0 skipped**, including seven new registration tests and existing provider regressions.
- Opt-in live registration emitted only `HONGGUO_REGISTRATION_ACCEPTED: valid IDs returned; memory retention verified; no identifiers logged`.
- `assembleDebug`: PASS. `lintDebug`: PASS, **0 errors / 6 existing warnings**, no new registration-file lint finding.
- First combined validation failed due to native JVM memory exhaustion. Separate single-worker, smaller JVM runs succeeded; it was not an established application regression.
- Logs, ignored: `phase5j-unit-validation.log`, `phase5j-build-lint.log`, initial failure `phase5j-registration-validation.log`.

This continuation: no application changes, unit-suite rerun, build, lint run or emulator startup. Only bounded metadata diagnostics and preservation checks were performed.

For **all episodes 1, 51, 88 and 90**, the requested 30-second in-CineDBKTV video/audio, fullscreen, pause/resume, seeking, near-end seek, progression and error-recovery acceptance tests are **NOT PERFORMED**. Physical-TV and phone playback acceptance are unverified. No full episode was played. Historical community-app results do not count as CineDBKTV playback success.

## 7. Exact unresolved dependency

Technical access to registration and episode metadata is demonstrated. The missing step is an established **permitted, usable protected-media key/licence workflow** compatible with CineDBKTV, or a verified clear-media response. The returned proprietary wrapped-key fields cannot be passed directly to the current plain-URI Media3 setup. No standard DRM scheme/UUID/licence endpoint/session configuration was demonstrated.

CENC encryption alone does not prove a particular DRM system, legal prohibition, or impossibility. Conversely, server-returned wrapped metadata alone does not establish permission to implement custom key unwrapping/CENC clearing. Do not repeat the prior overbroad claims that absence of an SDK or an empty unsigned response proves integration impossible. Do not decode protected keys or copy the unlicensed community decryptor as a workaround.

## 8. Smallest next steps, in priority order

1. Read this handover and check current limits/Git. Preserve the minimum 5% five-hour reserve unless the user explicitly changes it; do not automatically resume from this document.
2. Establish provider-supported documentation or permission for the returned key-provisioning format, or a supported DRM licence/session mechanism. Investigate only that concrete dependency. Do not repeat catalog, registration, browser media discovery or community-app acceptance investigations.
3. If a supported mechanism is established, map its actual scheme/licence configuration into Media3, or verify an actual clear source. Reuse the existing player; no Go service is inherently required by the registration work already completed.
4. Only after a usable permitted source is verified: add an isolated Hongguo resolver, handle direct row fields correctly, preserve failure/cancellation isolation, and connect episode routing. Handle real URL expiration/identity rejection from observed responses, not guessed TTLs.
5. Test episodes 1, 51, 88, 90 inside CineDBKTV for **at most 30 seconds each**, including controls/audio where observable. Seek near the end where practical. Do not repeatedly replay successful tests or claim full-series access.
6. Run relevant regressions, unit tests, debug build and lint sequentially with emulator work. Commit only coherent verified changes. No push, merge, production release or companion APK without new authorization.

## 9. Preservation, local evidence and rollback

Hashes were rechecked unchanged during this continuation:

| APK | SHA-256 |
| --- | --- |
| `D:\CineStreamTV\android-tv-repair\CineDBKTV-1.3.2.apk` | `ccf2376a0ce2587dabbdc2f5c6cb05ebeb8146fa7cd33f48875fd3a90a8a93d5` |
| `D:\CineStreamTV\cinedbktv-development\CineDBKTV-phase4-catalog-dev-20261101.apk` | `d4df99f07defca5d62b97aeee706207dfa5d339d94e00fbeaa6fe4b4166a2c10` |
| `D:\CineStreamTV\cinedbktv-development\app\build\outputs\apk\debug\app-debug.apk` (registration validation build; Hongguo playback disabled) | `80898a15b20e38ec24363d59873e763c2696864dd27352190b21163d070efe63` |

The historical root-level `CineDBKTV-Phase5I-companion-debug-a914a7f.apk` was not found at its recorded path in the preceding continuation; its current location/hash cannot be confirmed. No APK was deleted or moved by this continuation. Do not claim every historical APK has been reverified.

Ignored local evidence (not included in a fresh clone):

- `.tmp/phase5j-evidence/registration-fallback-availability.json`: earlier sanitized metadata/header summary; contains the nested-field false-negative described above.
- `.tmp/phase5j-evidence/registration-structural-protection.json`: actual MP4 protection-box evidence.
- `.tmp/phase5j-evidence/continuation-metadata-schema.json`: this continuation's two empty metadata responses.
- `.tmp/phase5j-evidence/continuation-metadata-schema-final.json`: successful schema-only evidence; no secret values or media content.
- `.tmp/phase5j-evidence/RegistrationProbe.java`: disposable diagnostic, edited only in ignored storage. `SCHEMA_ONLY=true` disables media requests. It uses a fresh identity because prior probes intentionally retained none. It is NOT production application code; avoid rerunning solved investigations.
- Community reference revision `e1af6e0`, cached under `.tmp/phase5f-evidence`, `.tmp/phase5g-evidence`, `.tmp/phase5h-evidence`. Root reuse rights remain unverified. Substantial code must not be copied without establishing rights.

No uncommitted application source changes exist. The documentation successor commit should leave Git clean; ignored diagnostic edits remain local by design. Rollback should preserve branches/APKs and use an isolated checkout at Phase 4 `87e5e90` if needed; do not hard-reset or delete existing work. `ef8275d` adds a dormant component only and has not changed production provider behavior.

Build environment, if later needed: Java `D:\BuildTools\jdk17\jdk-17.0.21.0.2+1`, SDK `D:\AndroidSdk`, cache `D:\.gradle`, verified Gradle `D:\CineStreamTV\.tools\gradle-8.9\bin\gradle.bat`. Use offline, no-daemon, max-workers=1; SerialGC/ActiveProcessorCount=2. Successful split runs used 512 MiB Gradle heap for tests and 768 MiB for build/lint, with `JAVA_TOOL_OPTIONS=-XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Xss512k`. No Gradle project configuration was changed for this. Opt-in registration test uses `CINEDBKTV_REGISTER_LIVE=1`; ordinary unit runs skip that network test. Serialize builds and emulator work.

## 10. Resource accounting and reserve

- Actual starting snapshot: **15% five-hour / 76% weekly remaining** at 04:21:55 Singapore time, 2 November 2026. The prior chat's 77% weekly figure was historical.
- Latest closeout snapshot before document creation: **13% five-hour / 76% weekly remaining**. Observed consumption to that snapshot: **2 / 0 percentage points**. Final administrative closeout readings are recorded below before committing.
- Maximum continuation allocation: 10 five-hour percentage points INCLUDING handover. Protected minimum: **5% remaining**. Begin handover before 7%; never intentionally spend the reserve. Weekly approval threshold: projected 7 points.
- Readings are accessible account-wide rounded percentages, not exact per-request measurements. Continuous measurement is unavailable; checks were made conservatively around the short bounded diagnostic operations. Investigation ended early rather than launching builds/emulators or approaching the reserve. This investigation ended within the first five-minute interval; no five-minute checkpoint was due at that stopping point.
- Final closeout balance before the documentation commit: **12% five-hour / 76% weekly remaining**. Measured continuation consumption: **3 / 0 percentage points**. The protected 5% reserve remains intact. No further development, investigation, build or test follows this handover; only documentation commit/status confirmation and the final response remain.

## 11. Next Codex prompt

> Continue CineDBKTV Phase 5J only after reading `D:\CineStreamTV\cinedbktv-development\PHASE_5J_HANDOVER.md` and checking actual limits/Git. Branch `feature/hongguo-single-apk`, implemented registration commit `ef8275d95935ca1c6e8295b80903de8e6600cc58`; latest successor is handover documentation. Mandatory one APK, no companion/backend; preserve providers/APKs/branches/PR. Registration and episode 1 metadata access work, but playback remains disabled. Initial AND fallback video rows directly contain `spade_a` and other key fields; older nested-only false-negative diagnostics are wrong. Four qualities have CENC-encrypted MP4 tracks. No supported licence/key-handling workflow or clear alternative has been established. Investigate only that dependency, without extracting protected keys, bypassing DRM/locks or copying unlicensed code. Do not repeat solved registration/browser/community tests. If a supported source is established, reuse Media3 and test episodes 1/51/88/90 for at most 30 seconds each inside CineDBKTV. Preserve at least 5% five-hour allowance; stop early for handover. Do not push, merge, publish or proceed to Phase 6 without authorization.

## 12. Latest continuation: community encrypted-media architecture review

### Current status and repository

- **Playback working: NO. Hongguo playback enabled: NO.** Last completed application implementation remains the independent registration component at `ef8275d95935ca1c6e8295b80903de8e6600cc58`.
- Latest continuation started with clean Git on `feature/hongguo-single-apk`, HEAD `4c856450610fd5895e9c5f92adf47e96a53f5660` (previous handover commit). This update is documentation only; its successor SHA is available with `git log -1 --format=%H -- PHASE_5J_HANDOVER.md` and in the final response.
- No registration or live metadata/media requests were repeated. No application code, dependency, player, provider routing or APK was changed. No build, test, emulator, push, merge, publication or deployment was performed.
- PR #1 was not re-fetched in this short pass. Its latest verified status, from the previous continuation, remains open/draft/unmerged with development head `87e5e90`; this continuation performed no GitHub mutations.

### Evidence and what it establishes

1. The pinned community `release-minireel.go` cache identifies `miniReelDecodeContentKey` (line 1277), metadata key handling (around line 1050), and `prepareMiniReelMedia` (line 433). The preparation path conditionally calls `decryptMiniReelMP4` when a source key exists (around lines 483–485), then exposes the prepared local file. The key-decoding algorithm was not executed or ported.
2. Its cached `minireel_range.go` indexes initialization/sample information and carries a source key into an on-demand transform stage. The inspected code includes an explicit error for an encrypted track without a CENC key. This is local media preparation, not evidence of an Android licence-server exchange. That older cache was used only for corroboration; current pinned documentation below confirms the architecture.
3. The pinned [Range playback documentation](https://github.com/aston314/hongguo-tv/blob/e1af6e0/docs/range-playback-experiment.md) explicitly describes local CENC sample decryption before the player reads the stream, with a full-file preparation fallback. Thus community playback success does not demonstrate direct playback of the original encrypted CDN URL by an unconfigured Media3 player.
4. The cached pinned Android `release-PlayerBridge.java`, around lines 171–186, constructs a normal MP4/HLS media item and starts ExoPlayer. The inspected bridge has no standard DRM/licence configuration. The upstream core is responsible for preparing its media.
5. The pinned [standard Media3 documentation](https://github.com/aston314/hongguo-tv/blob/e1af6e0/docs/standard-media3.md) describes HTML5-first playback with bounded Media3 decoder fallback. It mentions newer Media3/NextLib software decoding and GPL-3.0 distribution obligations for that dependency. Decoder fallback and those licensing notes do not establish a provider grant for protected-content key processing. No dependency was imported into CineDBKTV.
6. [Official Media3 DRM documentation](https://developer.android.com/media/media3/exoplayer/drm) describes Android MediaDrm sessions with an identified DRM scheme and appropriate configuration. Its format table lists Widevine CENC with DASH/FMP4 HLS and ClearKey CENC with DASH. The currently observed progressive MP4 plus proprietary wrapped fields is not an already-verified configuration from that table. A simple DRM switch or guessed ClearKey assignment is not justified; actual scheme, provisioning and format compatibility must be established.

The two pinned community documents are cached locally at `.tmp/phase5j-evidence/standard-media3.md` and `.tmp/phase5j-evidence/range-playback-experiment.md`. Initial web-tool retrieval failed; ordinary public raw-file retrieval succeeded. No protected service request, key value or video content was involved in these documentation reads.

### Missing dependency and minimum justified implementation

- A provider-supported or otherwise verified permitted key-provisioning/licence workflow has **not** been established. No such grant or documented licence exchange was identified in the specific architecture documents reviewed. This is not a claim that none exists elsewhere or that every form of CENC processing is prohibited.
- A legitimately accessible clear-media alternative remains **unverified**. No additional episodes, qualities or speculative endpoints were requested in this pass. Existing evidence still identifies CENC on all four sampled qualities of episode 1.
- The community method depends on custom key handling plus local CENC media preparation before normal playback. Implementing that protected portion would require establishing permission and a usable integration contract first; the community application's operation alone does not satisfy this gate.
- After that gate, the smallest production components would be: (a) an isolated metadata resolver using the existing registered identity, direct-row key/protection metadata and real expiry/error fields; (b) a supported protection/session handler and compatible packaging, or an approved component that provides genuinely playable media; (c) connection to the existing Media3/player/episode navigation. No independent need for a Go language component, companion app, extra backend or player rewrite has been demonstrated.

### Changes, tests and preservation

- Tracked change: this handover update only. Application implementation, regression behavior and existing APK files were untouched. Previous branch/APK/hash preservation evidence remains in section 9; hashes were not recalculated in this documentation-only pass.
- Prior results remain **41 passing unit tests; successful debug build; lint 0 errors / 6 warnings**. These were not rerun and should not be presented as new results.
- Episodes **1, 51, 88 and 90: all in-CineDBKTV playback acceptance tests NOT PERFORMED**. No video rendering, audible output, 30-second observation, pause/resume, seek, fullscreen or progression was newly tested. No full episode was played. No new regression is established by this read-only review, but live existing-provider regressions were not retested.
- No uncommitted application changes exist. Only the handover is intended for a local successor commit. Do not reset/delete `ef8275d`, previous branches or APKs; rollback considerations remain in section 9.

### Latest resource ledger and stopping rule

- Actual start: **11% five-hour / 76% weekly remaining**, 04:30:29 Singapore time, 2 November 2026. The prior reported 12% was historical.
- Investigation stop snapshot: **10% / 76% remaining**, consumption **1 / 0 percentage points**. Investigation was stopped proactively for handover; no build/emulator work was launched.
- Final closeout before the documentation commit: **9% five-hour / 75% weekly remaining**; measured consumption **2 / 1 percentage points**. The protected 5% reserve remains safe. No further investigation, implementation, build or test follows this handover; only the local documentation commit/status confirmation and final response remain.
- Readings are account-wide rounded balances. They are not continuous or exact per-operation measurements. The focused investigation ended within its first five-minute interval; interim balances were reported before the interval elapsed. Final 5% remains protected; do not resume automatically after handover.

### Exact next continuation steps

1. Check actual usage/Git and read sections 5, 7 and 12. Preserve the 5% reserve. Do not repeat registration, header inspection or community playback acceptance.
2. Establish the provider's permitted key/protection integration contract or a genuinely clear alternative. An implementation instruction alone must not be treated as provider authorization for custom protected-key extraction. Avoid another broad endpoint/source search.
3. If the contract provides standard MediaDrm playback, verify its actual scheme, licence provisioning and packaging against CineDBKTV's Media3 version before wiring routing. If it instead requires proprietary preparation, establish reuse/implementation rights and permission for that handling before writing it. Do not copy the community decryptor or guess a ClearKey conversion.
4. Only after a permitted usable source is verified, implement the minimal resolver/handler and then the four requested in-app tests, capped at 30 seconds each. Otherwise preserve the disabled state and report the precise unresolved dependency. No push/merge/publish without authorization.

Next prompt: **Continue from section 12 of `PHASE_5J_HANDOVER.md` on `feature/hongguo-single-apk`. Registration works; playback is disabled. Community playback prepares/decrypts CENC locally before ordinary HTML5/Media3 playback; it does not demonstrate a reusable standard licence exchange. Verify a permitted protection/key contract or clear source before implementing. Do not repeat solved investigations or extract protected keys. Check actual limits and preserve at least 5% five-hour remaining; update handover early. No companion/backend, pushes, merges or publication.**

## 13. Encryption investigation — 2 November 2026, 09:33 Singapore time

### Outcome and scope

- **Encryption identified: YES, `cenc`. Community preparation process understood: YES at function/data-flow level. Usable permitted key/licence mechanism established: NO. CineDBKTV playback working/enabled: NO.**
- Start: clean `feature/hongguo-single-apk` at `2671a05462dc0e813092469ac33b648be8783033`. Last application implementation remains `ef8275d`; this investigation changes only the handover. The local successor documentation commit is returned in the final response and is discoverable with `git log -1 --format=%H -- PHASE_5J_HANDOVER.md`.
- No registration tests, live metadata discovery, media requests, key decoding, video decryption, builds, emulator runs or application edits were performed. Source excerpts were read from the existing cache and checked where needed against revision `e1af6e0` through the connected GitHub read-only tool. No protected keys or credentials were obtained or disclosed.

### A. Confirmed scheme versus missing initialization evidence

The existing sanitized `registration-structural-protection.json` records `encv`, `frma`, `schm` with value **`cenc`**, and `tenc` for all four episode-1 qualities. This confirms the reported protection scheme for those sampled tracks; `cbcs` was not reported. The community documentation describes its supported path as CENC/AES-CTR. Neither observation establishes a specific DRM product or authorization grant.

The retained metadata schema contains a row field named `kid`, plus direct `spade_a`, `encryption_method`, `encrypt`, `gear_des_key` and model `key_seed`. **Actual KID values, `tenc.default_KID`, PSSH contents/system IDs and DRM UUIDs were not retained in these diagnostics.** No key-ID match or DRM-initialization match can therefore be claimed from the saved evidence. A `tenc` marker is not itself a PSSH/licence endpoint. Absence of PSSH from this limited saved summary does not establish absence from the complete media. The current source file's sample-encryption layout is also not fully established by those retained markers.

### B. Function-level community call chain

Source references below are pinned to `e1af6e0`; they describe the community implementation, not executed CineDBKTV playback. They intentionally omit key-decoding/decryption recipes and protected values.

| Stage | Function/reference | Responsibility and dependency |
| --- | --- | --- |
| Local playback request | [`server.playInfo`, main.go:374](https://github.com/aston314/hongguo-tv/blob/e1af6e0/core/cmd/hongguotv/main.go#L374) | Normalizes requested quality/codec mode and dispatches full or Range preparation. This is the app's local route, not a provider licence server. |
| Full-path adapter | [`hongguoClient.PlayInfoQualityMode`, hongguo_client.go:881](https://github.com/aston314/hongguo-tv/blob/e1af6e0/core/cmd/hongguotv/hongguo_client.go#L881), `embeddedMiniReelPlayInfo`:948 | Default internal callback leads to `resolveMiniReelMediaQuality`; explicit codec handling leads to `resolveMiniReelMediaQualityMode`. Returns prepared path and media/quality metadata. |
| Range adapter | [`PlayInfoRangeQualityMode`, hongguo_client.go:927](https://github.com/aston314/hongguo-tv/blob/e1af6e0/core/cmd/hongguotv/hongguo_client.go#L927) | Calls `resolveMiniReelMediaRangeQualityMode`. |
| Preparation job dispatch | `resolveMiniReelMediaQualityMode` / `resolveMiniReelMediaWithMode` / `resolveMiniReelMediaWithTokenMode`, minireel.go:307/318/333; Range counterpart minireel_range.go:55 | Selects preparation callback, coordinates locally cached jobs and bounded waiting; does not itself establish upstream rights. |
| Metadata retrieval | `miniReelNativeOptions` :802 -> `miniReelNativeOptionsWithIdentityCache` :818 -> device-aware attempt chain -> `miniReelNativeOptionsAttemptWithClientMode` :912 | Uses registered identity; fetches episode model, parses its JSON-encoded `video_model`, lists source rows. Registration is already solved in CineDBKTV and was not repeated. |
| Source/model interpretation | minireel.go around :980–1084, `miniReelFetchFallbackModelWithClient`, `miniReelMapsFromAny` :1891 | Selects model rows, optionally follows returned fallback only when no initial rows exist, builds options with media URL, codec, dimensions, quality, duration and optional locally decoded key. The maps helper only enumerates rows; it does not normalize key-field placement. |
| Quality choice | `selectMiniReelQuality`, minireel.go:785 | Chooses an offered quality or allowed fallback, retaining that option's associated URL/key. |
| Full-file preparation | `prepareMiniReelMedia`, minireel.go:433 | Resolves options, selects one, downloads media into private cache and conditionally invokes its proprietary preparation path when an option key exists. This work precedes the player. |
| Range preparation | `prepareMiniReelRangeMedia`, minireel_range.go:83 -> `openMiniReelRangeSource` -> [`indexMiniReelRangeMP4` :268](https://github.com/aston314/hongguo-tv/blob/e1af6e0/core/cmd/hongguotv/minireel_range.go#L268) | Checks supported Range/container prerequisites and associates the selected option key with a source/sample index. Unsupported encrypted layout or absent required key is an error; this is not an automatic MediaDrm session. |
| Prepared media delivery | `handleMiniReelMedia`, minireel.go:244 -> `serveMiniReelRangeMedia`, minireel_range.go:532 -> `readAndTransform`, or prepared file serving | Exposes app-local `/minireel-proxy/media/...mp4`. In the encrypted path, this is media prepared by the local core, not the original protected CDN source. |
| Android rendering | cached pinned `PlayerBridge.startCandidate`, around :171–186 | Creates an ordinary MP4/HLS MediaItem for the prepared source and invokes ExoPlayer. No reviewed bridge licence exchange or `MediaItem.DrmConfiguration` handles the original wrapped metadata. HTML5 follows the same prepared-media contract. |

The metadata parser, Range indexer, route dispatcher and client adapters were corroborated with direct pinned GitHub excerpts. Relevant Git blob IDs returned by that read: `minireel.go` `80ade25fd161027353d290165f78517e1876954c`; `minireel_range.go` `07cb408001767f0482f6a44958dddd343afa2df2`; `main.go` `7c06ded4def018e6d3132f276d6f67b61106ab08`; `hongguo_client.go` `c531f73761a4074b3dc2ff3202f7a3b78f581344`. Existing cached line references outside these excerpts are supporting source evidence, not a full independent audit.

### C. Key handling and newly identified compatibility gaps

- **Application-specific workflow:** metadata handling invokes `miniReelDecodeContentKey` for an encoded field and attaches a resulting key to a selected source option. No standard licence-service exchange was identified in that reviewed path. The decoder was not run or copied; no protected key was recovered. This source architecture alone does not establish permission to reproduce it.
- **Server-provided information:** source/quality/protection fields and wrapped metadata, plus the original MP4 protection structures. **Locally generated/derived information:** anonymous client identity (already implemented), source selection, cache/job identifiers, file/sample indexing and application-specific preparation state. Cache tokens are not proof of upstream authorization. Do not confuse a locally derived content key with a locally generated unrelated decryption key.
- **Schema mismatch confirmed:** pinned minireel.go reads nested `row.encrypt_info.spade_a` around lines 1011/1050. CineDBKTV's retained initial and fallback responses instead have direct row `spade_a`. `miniReelMapsFromAny` simply enumerates objects. Therefore the reference parser is not a drop-in adapter for CineDBKTV's observed response. This does NOT prove the community APK is broken: its upstream response may differ, and the successful APK behavior remains historical external-app evidence.
- **Association is source-option/file based, not a verified KID registry:** the same selected option key is supplied to preparation for the media file and its supported encrypted tracks. The inspected Range indexer iterates encrypted tracks and requires a suitable key, but does not compare track `default_KID` against the metadata `kid` or implement a KID-to-key licence map. The inspected full-file preparation has the same single selected-key association. No key-rotation or multi-key-track support was established. Do not adopt this assumption for CineDBKTV without a verified contract/fixture.
- A missing nested field must never be interpreted as proof of clear media. The source's clear-path branch and the live diagnostic's direct-field schema do not establish an accessible clear alternative. Existing evidence still says the sampled four qualities are encrypted.

### D. Android compatibility and exact unresolved questions

CineDBKTV's current `Playback.kt:61–65` supplies a plain URI to `DefaultMediaSourceFactory` and has no Hongguo protection handler; `Providers.kt:117` still guards Hongguo playback. Ordinary decoder/remote/fullscreen controls can be reused once playable media exists. The community's Go language is not an inherent requirement; its missing functional role is media/protection preparation, not UI or a new catalogue service.

The [official Media3 DRM documentation](https://developer.android.com/media/media3/exoplayer/drm), already reviewed in section 12, requires an actual DRM scheme/session setup for protected playback and lists supported scheme/container combinations. It does not establish compatibility with these proprietary fields. No guessed ClearKey UUID, custom key extraction or DRM workaround was introduced.

Unresolved questions are now specific:

1. What provider-authorized key-provisioning/licence contract applies to `spade_a`/`kid` for this independent client, if any?
2. Is there a standard MediaDrm scheme, initialization payload and compatible source format, or an approved in-APK preparation component with established reuse rights?
3. What are the actual track KIDs, their association with source metadata, and the supported key-rotation/multi-track behavior? Retained schema names cannot answer this.
4. Does a legitimately accessible clear source exist? None was demonstrated; no new service requests were made here.

**One smallest next action:** obtain the provider-supported provisioning specification (or an authorized integration/test fixture) for the returned `spade_a`/`kid` format, including the track-key association and permitted playback handling. This is the missing dependency to resolve before an in-app proof of concept. Another player library or a wholesale Go port does not supply that contract.

If that gate succeeds, independently implement only (1) the correctly shaped metadata resolver using existing registration, (2) the verified authorized protection/session or approved media adapter, and (3) existing player/episode routing. Until then, keep playback disabled. No compatible permitted mechanism was established in this investigation, so the user's proof-of-concept gate was not reached.

### Tests, preservation and resource closeout

- Source/data-flow inspection only. No new application/unit/build/lint/emulator tests. Prior 41 passing tests, successful debug build and 0-error/6-warning lint remain historical verified results.
- Episodes **1, 51, 88, 90: all in-CineDBKTV 30-second playback acceptance tests NOT PERFORMED**, including video/audio, seeking, pause/resume and error handling. No entire episode was played.
- Only this documentation changes. Existing providers, search UI, registration implementation, APKs, branches and Git history are untouched. No push/merge/publish or GitHub mutation; PR draft/unmerged status remains the previously verified status, not a newly fetched claim.
- Actual starting balances: **100% five-hour / 75% weekly remaining** at 09:33:52 Singapore time. The five-hour window had reset; the previous 9% figure was historical. No usage-reset credit was invoked by this investigation.
- Investigation stop snapshot: **97% / 75%**, observed consumption **3 / 0 percentage points**. Protected 5% reserve is safe. Continuous measurement is unavailable; account-wide rounded balances were checked conservatively. Investigation stopped within the first five-minute interval; intermediate progress/balances were reported before the interval elapsed.
- Final closeout before documentation commit: **96% five-hour / 75% weekly remaining**, measured consumption **4 / 0 percentage points**. Protected reserve remains intact. After preparing this handover, only documentation commit/status confirmation and final response remain; no new investigation or development follows automatically.

## 14. Reported reuse permission and isolated metadata resolver — 2 November 2026

### Permission record

The user reports a WhatsApp group call with the community application's developers, who verbally permitted modifying their APK and reusing their implementation for CineDBKTV after discussing its multi-provider/multilingual design. This is recorded as user-reported verbal permission; no written grant or independent identity/scope verification was supplied. Its existence is not rejected merely because it was verbal. Public repository visibility alone is not the permission relied on here. The reported developer grant is accepted for planning community-code reuse within rights they control; it is not represented as a separate Hongguo/content-provider authorization or a licence for third-party SDK dependencies.

The new resolver is independently written and imports no community code. No protected key-unwrapping or media-decryption implementation was copied, run or added.

### Implementation delivered in this increment

Start: clean `feature/hongguo-single-apk` at `a7e59060cb4ae33ee532caa73b716d4f759fe011`.

| File | Purpose |
| --- | --- |
| `app/src/main/java/com/cinestream/tv/HongguoPlaybackMetadata.kt` | Isolated on-demand metadata resolver using the existing registered identity and CineDBKTV package/version profile. Strict requested-episode association; parses JSON-string/object models and object/array rows; supports direct and nested protection metadata; bounded two-attempt transport/empty-response recovery, HTTPS-only source validation, sanitized errors, cancellation before work, response-size limits and redacted representations. |
| `app/src/test/java/com/cinestream/tv/HongguoPlaybackMetadataTest.kt` | Eight focused tests for both observed protection shapes, unverified-clear handling, response/episode rejection, unsafe URLs/codecs, request identity, bounded recovery, pre-start cancellation, and opt-in live metadata verification. Synthetic URLs/key labels occur only in tests; no production sample-source fallback exists. |
| `PHASE_5J_HANDOVER.md` | Permission record, exact remaining dependency, test evidence and continuation instructions. |

`HongguoPlaybackMetadata.forApplication(context)` composes the existing registration component with the network resolver; it is NOT called from provider dispatch or UI. The result contains selected media URLs in memory and basic source metadata, but no wrapped/decrypted key fields. Protection metadata is inspected only for presence. Options are classified `PROTECTED` or `UNVERIFIED`; no metadata-only option is labelled verified-clear/playable. `Result.requirePlayable()` always fails explicitly until a supported media path is verified. Missing encryption flags never imply permission or decodability.

The resolver does not fetch media/fallback URLs, cache media URLs across resolutions, guess expiration periods, automatically replace an identity after rejection, or implement a DRM session. Existing Hongguo guards/notices and other providers remain unchanged. No SDK/Go/native library, companion app, cloud service or new permission was introduced.

### Supported SDK candidate: new concrete evidence, not established Hongguo compatibility

The [official Volcengine private-encryption documentation](https://docs.volcengine.com/docs/video-on-demand/private-encryption-legacy?lang=en) documents an Android DirectUrl source with a server-issued protected playback authorization, handled inside its own player SDK. This is an in-APK candidate that avoids an independently reproduced protected-key decoder. The documented authorization flow is not proof that Hongguo's `spade_a` can be substituted for the SDK's authorization field.

The [SDK FAQ](https://docs.volcengine.com/docs/video-on-demand/player-sdk-faq?lang=en) specifies AppID/package/validity checks, and [Android integration instructions](https://docs.volcengine.com/docs/video-on-demand/android-player-integration?lang=zh) require the SDK licence setup. No valid SDK entitlement bound to `com.cinestream.tv` or compatible provider-issued playback authorization has been established for this project. No licence account, paid subscription, trial or provider credential was created or purchased, and no SDK was downloaded/integrated. This candidate may require provider participation; no external backend was introduced to manufacture authorization.

**Remaining dependency:** establish a supported protection adapter and valid playback authorization for the exact Hongguo source/metadata format. The reported community-code reuse grant does not supply those SDK assets or establish the provider's protected-media contract. CENC by itself still does not prove a particular DRM system or legal prohibition. No claim of impossibility is made.

### Validation and playback status

- Unit suite: **49 tests total, 48 passed, 1 skipped, 0 failures/errors**. All eight new metadata tests passed. The skipped test was the separate opt-in live registration test; it was intentionally not requested again. The new live metadata test obtains a fresh anonymous identity as its prerequisite, not as another registration investigation.
- Actual live Kotlin resolver result: requested episode `7695729320861387838`, metadata duration **140.714 seconds**, **4 options**, all classified **PROTECTED**. The only emitted diagnostic was `HONGGUO_METADATA_VERIFIED: requested episode, duration=140.714, options=4; protected playback remains disabled`. No media request, key decode or video playback occurred.
- `assembleDebug`: PASS. `lintDebug`: PASS, **0 errors / 6 existing warnings**, no findings in the new resolver. Tests and build/lint ran sequentially with the previously verified smaller JVM configuration. Logs: ignored `phase5j-metadata-tests.log` and `phase5j-metadata-build-lint.log`; result XML is under `app/build/test-results/testDebugUnitTest/`.
- The application factory/private-preference composition and Android-device network behavior have not been emulator verified; the production resolver class was exercised by the live JVM test. Pre-start cancellation is tested; immediate interruption of an in-flight socket is not claimed.
- Current validation APK: `D:\CineStreamTV\cinedbktv-development\app\build\outputs\apk\debug\app-debug.apk`, SHA-256 **`81f3569a77ea0591b2f4edbffba704e836933cabf265ae9984dbb7bedd725553`**. This regenerates the ordinary build output; section 9's older `80898...` hash is a historical registration-only build, not the current file. No stable/Phase 4 artifact was overwritten. Both stable and Phase 4 hashes were rechecked against section 9 and matched.

No in-CineDBKTV video/audio playback is established. Episodes **1, 51, 88 and 90: requested playback/controls/30-second acceptance tests NOT PERFORMED**. Hongguo playback remains disabled. Stable and Phase 4 APK files are not build outputs of this increment and are not overwritten. A newly built ordinary debug APK, if validation completes, is a resolver-validation artifact with playback still disabled; it must not be advertised as the single-APK playback deliverable.

### Next action and resource closeout

The next dependent action is to establish a licensed in-app protection adapter and compatible provider-issued authorization (or another verified permitted playable source). For the official SDK candidate, the concrete missing inputs are the target-package SDK licence/entitlement and a documented supported playback authorization. Do not paste credentials in chat. Without those inputs or another established permitted path, retain the existing playback guards; do not bypass protection or repurpose the community app's sessions/keys/stream.

Once a supported source is available, connect this resolver to an isolated Hongguo player adapter, reusing episode IDs/navigation and existing controls where compatible. Then perform the four requested in-app tests for at most 30 seconds each and the relevant regressions/build/lint. No new player backend, companion application, push, merge or release is authorized by this handover.

- Actual starting balances for this increment: **94% five-hour / 74% weekly remaining**. The protected 5% minimum and 7-point weekly approval threshold remain in force.
- Five-minute progress checkpoint: **91% / 74%**, consumption **3 / 0 points**; tests were running, reserve safe, estimated remaining validation/closeout 5–10 minutes. Account-wide rounded balances are accessible but not continuously measurable.
- Final balance before commit: **85% five-hour / 73% weekly remaining**; observed consumption **9 / 1 percentage points**. The 5% reserve remains intact and the 7-point weekly threshold was not reached. The commit containing this section is discoverable with `git log -1 --format=%H -- PHASE_5J_HANDOVER.md` and is returned in the final response. Do not push or resume another development step after closeout without a new instruction.


## 15. Embedded engine packaging investigation — 2 November 2026

This section follows the complete original handover content, verified byte-for-byte before Git line-ending normalization. The new concise `PHASE_5J_HANDOVER.md` is authoritative for current status; earlier sections are historical snapshots, not instructions to rerun completed investigations.

### Native runtime and Android boundary

- Pinned source: [Android core launcher](https://github.com/aston314/hongguo-tv/blob/e1af6e0/android/app/src/main/java/rocks/aston/hongguotv/CoreService.java), [endpoint contract](https://github.com/aston314/hongguo-tv/blob/e1af6e0/android/app/src/main/java/rocks/aston/hongguotv/CoreEndpoint.java), [native build script](https://github.com/aston314/hongguo-tv/blob/e1af6e0/scripts/build_android_binaries.sh).
- `libhongguotv.so` is a Go PIE executable, despite its library-style name. The build uses `-buildmode=pie`, CGO and Android NDK toolchains; it is launched using `ProcessBuilder`, not `System.loadLibrary`/JNI. Static ELF inspection of all three APK entries confirms ET_DYN, a nonzero entry, Android linker interpreter and Go build-info marker. No native process was launched in CineDBKTV.
- `CoreService` resolves the binary from its own application's `nativeLibraryDir`. It invokes `-port 0`, a newly generated `-instance-id`, and `-parent-pid`; it supplies private `MINIREEL_CACHE_DIR` and persistent `HONGGUO_TV_DATA_DIR`. The service reads readiness from child stdout. Destruction terminates the process; this is an app-local process, not a remote backend or second application.
- Readiness line: `HONGGUO_TV_READY <port> <instance>`. The endpoint is `http://127.0.0.1:<port>/`. `/api/v1/health` returns only an OK flag; `/api/v1/status` supplies service, instance and version. The new independent Kotlin bridge requires the latter to match the expected child/version. It does not scan ports or trust a generic health result.
- The HTTP playback API passes series/video/quality to the preparation job; the player gets an app-local MP4 route. The previously documented protected-key/CENC preparation remains inside this Go core. Moving the same binary into another APK does not remove that dependency or establish permission for that protected-media operation.
- The upstream service also starts library/account/radar/UI-related components. The packaged executable is not a separately isolated media-only library. Binary reuse would include those unused subsystems; reducing it requires a targeted source build, not just removing community UI Java classes.

### Size, ABI and platform findings

The inspected official-release APK is v0.2.138/code 137, 30,830,458 bytes, SHA-256 `61a9b355e876a3ebfa9f484bd8b66859bcbac18343c42e8ac736a114755d6ae9` (matches the previously verified published checksum).

| ABI | Core bytes extracted | Compressed bytes in that APK | Interpreter |
| --- | ---: | ---: | --- |
| arm64-v8a | 12,576,488 | 5,026,586 | `/system/bin/linker64` |
| armeabi-v7a | 12,476,652 | 5,253,303 | `/system/bin/linker` |
| x86_64 | 13,352,008 | 5,379,902 | `/system/bin/linker64` |

Core total: 38,405,148 bytes extracted; 15,659,791 compressed. These are measured component sizes, not a measured final CineDBKTV APK size; repackaging/alignment/compression may differ. Native FFmpeg/player libraries are separate from this core and are not inherently required if a compatible prepared stream can be played by existing Media3.

- Upstream builds only arm64-v8a, armeabi-v7a and x86_64; no 32-bit x86 binary is present. The existing API34 x86 TV AVD cannot exercise that binary. The API35 x86_64 AVD is ABI-compatible, but no new emulator test was performed. Packaging only those native ABIs could narrow Android installation compatibility; preserve unsupported-device behavior rather than silently excluding previously supported devices.
- Upstream uses `extractNativeLibs=true` and legacy native packaging so the executable has an installed native-library path. CineDBKTV has not adopted these settings or packaged any binary. The build script selects Linux/macOS NDK host toolchains; it has no Windows-host branch. This is an upstream script limitation, not proof that adapting a Windows build is impossible.
- Upstream's manifest permits global cleartext HTTP and adds foreground-service/boot/update/install/TV-channel permissions. Do not copy it wholesale. A future owned-child client needs a scoped loopback exception; current CineDBKTV cleartext policy is unchanged, and actual Android HTTP status probing is not yet enabled/verified. Boot receivers, updater, installation permission and community UI are not needed for an in-app-only playback session. Foreground-service requirements depend on the approved lifecycle design.
- Reuse existing CineDBKTV Compose screens, four-provider search, episode IDs/range/jump controls, Media3 view and remote controls. Do not import the community WebView UI or a second launcher activity. Go language itself is not a technical blocker; one-APK packaging is technically feasible, but has not been achieved for Hongguo playback.

### Licences and incorporation gate

- The user-reported verbal developer permission remains recorded in history section 14. It is not independently verified distribution permission for every binary/dependency. The pinned tree contains no root licence or root notice file establishing a general redistribution grant; do not convert public availability into such a grant.
- Root `go.mod` declares Go 1.25 and `github.com/skip2/go-qrcode` at `da1b6568686e`. Its [declared licence](https://github.com/skip2/go-qrcode/blob/da1b6568686e/LICENSE) is MIT, copyright Tom Harwood, with notice retention. The Go toolchain/runtime's own distribution notices would also need inclusion in a packaged executable distribution; no Go runtime was copied here.
- [Android Gradle configuration](https://github.com/aston314/hongguo-tv/blob/e1af6e0/android/app/build.gradle) declares standard Media3 1.11.1 and NextLib Media3 Extensions 1.11.1-0.17.0. The actual APK contains `assets/media3-ffmpeg-notices.txt`, explicitly identifying the bundled NextLib/FFmpeg libraries as GPL-3.0. Its pinned standard-player documentation requires corresponding source/licence/build materials for distribution. Do not import this player bundle merely to get the Go core; CineDBKTV already has Media3. The MPV flavour is separate and is not required.
- No community source or binary was incorporated into CineDBKTV. Only the public readiness/status contract was independently implemented. The source's protected-key decoding and CENC preparation cannot be included under the user's no-protected-key-extraction/no-DRM-circumvention constraints without establishing a permitted protected-media integration path. This is the specific unresolved component, not a generic claim that all encryption is forbidden.
- The earlier official SDK candidate is an alternative, not a technical prerequisite for this community-engine architecture. No Volcengine SDK licence is required merely to run this Go executable. Neither that fact nor the reported developer grant establishes the upstream protected-media permission/contract.

### Independent work and evidence

- Added `HongguoCoreEndpoint.kt`: strict owned-child readiness parsing, fixed loopback origin, exact service/instance/version verification through `/api/v1/status`, bounded nonredirecting status transport, safe error messages and pre-start cancellation. It has no process launcher, media API, key handling or playback/UI call site. This is protocol groundwork, NOT an integrated playback engine.
- Added six protocol/security tests. Transport responses are test fixtures; they are not claims of real core startup or video playback. Network tests against Hongguo were intentionally not repeated.
- Preserved the previous ordinary metadata debug build at `CineDBKTV-phase5j-metadata-dev-e7141a4.apk`, SHA-256 `81f3569a77ea0591b2f4edbffba704e836933cabf265ae9984dbb7bedd725553`, before running a new build. Stable and Phase4 named APKs were not overwritten.
- Local static audit script/output: `.tmp/phase5j-evidence/audit_engine.py` and `embedding-artifact-audit.json` (ignored; not in a fresh clone). It reads ZIP/ELF metadata in memory, never extracts/executes the core, and records no upstream keys or credentials. Abridged measured facts are preserved above.
- Source entry points reviewed: `CoreService.startProcess/consumeLogs/onDestroy`, `CoreEndpoint.fromReadyLine/matches`, core `main/status`, Android native build/Gradle/manifest files. No repeated registration, metadata or encryption experiments were needed.

The existing core's play-info API takes series/video/quality parameters and uses its own internal anonymous identity cache; it does not expose an input for CineDBKTV's registered identifiers. Reusing the Kotlin registration state inside that binary would require an explicit supported adapter/contract change, not replaying captured sessions. Catalog IDs, Compose navigation and Media3 remain reusable independently.

Verified bridge code commit: `73d7d049a4a8f517e6134a59cd9ac22bd520bc11`. Validation: 55 tests total, 53 passed, 2 opt-in network tests skipped, 0 failures/errors; debug build passed; lint 0 errors and 6 existing warnings. Tests used protocol fixtures, not a real community engine. This task's resource closeout: 81%/73% starting to 72%/71% remaining (five-hour/weekly), 9/2 percentage points observed. No reserve or weekly threshold was reached. All four in-CineDBKTV episode acceptance tests remain unperformed; no new core/media execution occurred. The concise handover records current paths, component state and next steps.

## 16. 2 November 2026 — stopped owned-child implementation session

Started at clean `feature/hongguo-single-apk`, HEAD `506d76055634ea6b600161af03912bc82501333d`. Implemented independent dormant process lifecycle, authenticated-loopback transport, installed-binary verification seam, preparation boundary, host child fixture tests and an unexecuted Android fixture check. No community binary/core or protected preparation was invoked. User explicitly stopped work to conserve usage; Gradle processes were terminated and all implementation remains uncommitted. Subsequent instruction authorized handover documentation only.

Host tests: 60 total / 58 passed / 2 skipped / 0 failures or errors. Final serialized invocation reached app and instrumentation APK assembly; lint did not complete. No emulator or actual provider playback testing. Generated app-debug.apk: 27,237,286 bytes, SHA-256 `2e254a8d792920dc3de499e0bad862f06ed42f634998cf6a313de3596e08cfb8`; unverified and not distributed. Prior readiness APK archived as `CineDBKTV-phase5j-readiness-dev-506d760.apk`, SHA-256 `17328ebe3b07c85d3a23fbd736af2ca0244882d9b4c7afe0094e8c3cab70cac7`.

Live usage: start 71% five-hour / 71% weekly; handover-only follow-up 62%/70% before documentation (9/1 points consumed). Exact current implementation, limitations, artifacts and stop instruction are maintained in PHASE_5J_HANDOVER.md; no lengthy historical duplication is required.

## 17. 2 November 2026 — lint and Android runtime verification continuations

The resumed lint check passed with 0 errors / 6 warnings; no new-runtime/fixture findings. Official Volcengine demo requires an AppId/License, but its applicability to the Hongguo catalog remains unverified and it is not a mandatory dependency of the Go architecture. A combined documentation/commit command was rejected by automatic approval review with only “blocked by policy”; it did not execute. This did not establish a technical playback failure or universal external impossibility.

After further user instruction to continue, existing APKs were installed in an Android 35 x86_64 emulator. The existing `hongguoRuntime=true` instrumentation check passed owned test-child readiness/authentication, anonymous request rejection, Media3 unprotected PCM READY/pause/resume/seek/ENDED and child/file cleanup. No community engine/protected preparation, actual Hongguo video, audio-output observation or episodes 1/51/88/90 acceptance occurred. Evidence: ignored `phase5j-runtime-android-check.log`.

Windows commit exhaustion initially prevented emulator launch; 512 MB TV boot/shutdown stalled. User authorized closing apps; graceful close requests were sent without dismissing unsaved-work prompts. Only our stalled emulator/ADB clients were force-terminated. A 1.5 GB phone emulator then booted, passed the test and was shut down. Existing production source and APK hash were unchanged. Latest continuation usage: start 57% five-hour / 69% weekly; checkpoint 54%/68%; pre-closeout 51%/68% (6/1 points consumed). Current details are authoritative in PHASE_5J_HANDOVER.md.

## 18. 2 November 2026 — Attempt 1 actual Go engine integration

Starting clean HEAD `db0d48e132e9be1af34cc9f23b31f1370c4caa0e` on feature/hongguo-single-apk. Packaged the three unchanged audited v0.2.138 executables; final APK payload hashes match the earlier audit. Binary marker and real status agree on `0.2.138-e1af6e0cbbc018bd198169d7beb4519d7b0a271a`. Corrected the launcher to actual upstream flags/data/cache environment and removed the assumed bearer requirement from real-core operation. Legacy extraction/no-stripping preserves installed integrity. Detailed protocol is in docs/HONGGUO_ENGINE_API.md; no protected preparation recipe was implemented or invoked.

Serialized host/build/lint passed: 60 total tests, 58 passed/2 skipped/0 failures; debug app and instrumentation APK builds; lint 0 errors/6 warnings. Real installed Go core on Android 35 x86_64 passed readiness/status/version/hash, non-preparation 400/405 validation, normal shutdown, restart, forced termination cleanup and recovery restart. All three captured children exited. Final process audit found no CineDBKTV orphan; an unrelated preinstalled community application's own child was not acceptance evidence. Emulator then shut down. No core-supported arbitrary fixture route found; actual-core unprotected media/Media3 check NOT PERFORMED. All four live Hongguo video episodes remain NOT PERFORMED.

Verified development APK: CineDBKTV-phase5j-attempt1-engine-dev-20261102.apk, 44,287,706 bytes, SHA-256 `951b7d73d1df59708aa42786a4d958174370a8febc921e2a45df674110bc18e5`. Previous runtime APK archived before rebuild; existing branches/history/stable APKs/PR untouched. No push/merge/distribution. Usage start 50% five-hour/68% weekly; pre-closeout 41%/66% (9/2 points consumed). Current handover is authoritative; Attempt 2/Phase 6 not started.
