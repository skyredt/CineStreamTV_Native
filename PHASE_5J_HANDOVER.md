# CineDBKTV Phase 5J handover — 2 November 2026 (Asia/Singapore)

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
