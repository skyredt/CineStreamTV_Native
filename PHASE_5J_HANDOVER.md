# CineDBKTV Phase 5J — current handover

Updated: 2 November 2026 (Asia/Singapore). Read this file first; [history and evidence](PHASE_5J_HISTORY.md) preserves all previous handover content and experiments.

## Current outcome

- **Embedded playback engine: BLOCKED / not packaged. Hongguo playback: NO. Single-APK Hongguo playback achieved: NO.**
- Existing Cineby/DBKU/BingeBang functionality and Hongguo catalog/search/details/episode navigation are unchanged. Hongguo playback guards remain enabled.
- Working independent Kotlin components: device registration and isolated playback metadata resolver (live-verified previously). New independently written readiness/status bridge is protocol groundwork only; it neither starts the core nor requests media preparation.
- Actual source/binary inspection confirms local single-APK embedding is technically feasible: the core is an Android Go PIE child executable with loopback HTTP, not a JNI library. No companion app or external backend is inherently required. No community code/native binary/player dependency was incorporated.

## Git and preservation

- Repository: `D:\CineStreamTV\cinedbktv-development`; branch **feature/hongguo-single-apk**.
- Latest verified code commit: `73d7d049a4a8f517e6134a59cd9ac22bd520bc11`. This summary/history are a documentation-only successor commit; current HEAD is `git rev-parse HEAD`, and the final response records its SHA.
- Prior implementation: `e7141a44a7c0937ff41b83573975505453e5c3e8` (metadata), `ef8275d95935ca1c6e8295b80903de8e6600cc58` (registration). All prior investigation commits remain intact.
- Development branch remains Phase4 `87e5e90d20c7d9eef1b5434f04c91c83c7bcc8bf`; companion branch remains `a914a7ff6fbb42ff4b5ed5106b1477ee336e37cb`.
- No push/merge/publish/deployment. PR #1 was rechecked: open/draft/unmerged, development head remains Phase4 `87e5e90`; no GitHub mutation was performed. Final working-tree state is checked after the documentation commit.
- Existing named stable/Phase4 APKs remain untouched. The prior debug output was archived before rebuilding as `CineDBKTV-phase5j-metadata-dev-e7141a4.apk`, SHA-256 `81f3569a77ea0591b2f4edbffba704e836933cabf265ae9984dbb7bedd725553`. Other known paths/hashes and rollback details: history section 9. Do not reset/delete prior work.

## Components and exact outstanding dependency

| Component | State |
| --- | --- |
| `HongguoRegistration.kt` | Actual anonymous registration accepted for CineDBKTV package; private-store adapter, bounded retries. Android persistence still needs device verification. |
| `HongguoPlaybackMetadata.kt` | On-demand resolver; correct requested-episode association and direct/nested protection shape handling. Episode1 metadata: 140.714 seconds, four protected options. No key values returned; no playable-source assertion. |
| `HongguoCoreEndpoint.kt` | New owned-child readiness/status validation. No launcher, bundled core, media route, manifest/network-policy change or production call site. Android runtime probing remains unverified. |
| Core/native media preparation | **Not incorporated.** Existing core performs the protected-key/CENC preparation. Required permitted media-access/handling contract remains unestablished under the user's constraints. |

The reported WhatsApp developer reuse permission is preserved in history section 14; its existence is accepted as reported, but it is not independently verified permission for third-party content/dependencies. The specific blocked operation is the core's protected-key/media preparation, not the Go language, absence of an SDK, or localhost packaging. Do not decode protected keys, bypass DRM/payment/locks or claim the installed community app authorizes reuse of its sessions/media. Official SDK integration is only an alternative; an SDK licence is not an inherent dependency of the standalone Go core.

## Embedding facts that should not be investigated again

- `CoreService` launches `nativeLibraryDir/libhongguotv.so` using `ProcessBuilder(-port 0, -instance-id UUID, -parent-pid PID)` and private cache/data directories. Readiness is read from stdout; status must match child instance and version. IPC is HTTP on 127.0.0.1, not JNI.
- Three ABIs: arm64-v8a, armeabi-v7a, x86_64. Core-only measured addition: **38,405,148 extracted / 15,659,791 compressed bytes** in upstream APK. No 32-bit x86 binary; the x86 TV AVD is unsuitable. Actual final CineDBKTV size/ABI compatibility is not verified.
- Upstream uses native extraction/legacy packaging and broad cleartext/service/boot/update permissions. Do not copy these wholesale. An approved in-app lifecycle adapter needs narrow loopback network policy and owned-process shutdown. Existing policy remains unchanged.
- Do not import community UI, updater, boot receivers, MPV or NextLib merely to get media preparation. Root Go dependency is MIT go-qrcode; upstream standard Android player includes GPL-3.0 NextLib/FFmpeg notices. No root general licence was found in the pinned tree. Detailed notices, ELF measurements and minimal-package analysis: history section 15.
- Encryption evidence: `schm=cenc`, `encv`, `frma`, `tenc` for four sampled episode1 qualities; actual KIDs/PSSH/DRM UUID/track-key match are not retained/verified. Direct `spade_a` exists on rows; older nested-only negative diagnostics were misleading. Full call chain: history sections 13 and 15.

## Latest validation

- **55 tests total: 53 passed, 2 skipped, 0 failures/errors.** All six new readiness/status tests passed. Two opt-in upstream network tests were not requested. These are protocol fixtures, not actual core startup or playback acceptance.
- **Debug build PASS; lint PASS, 0 errors / 6 existing warnings**, no findings in the new bridge. No emulator was started; no community core or media endpoint was executed.
- New ordinary validation build: `app/build/outputs/apk/debug/app-debug.apk`, SHA-256 `17328ebe3b07c85d3a23fbd736af2ca0244882d9b4c7afe0094e8c3cab70cac7`. It contains no embedded core and does not enable Hongguo playback. The archived prior metadata APK is preserved above; this is not a successful single-APK playback deliverable.

| Episode | Exact video ID from existing catalog fixture | In-CineDBKTV playback acceptance |
| --- | --- | --- |
| 1 | 7695729320861387838 | NOT PERFORMED |
| 51 | 7695729319137528894 | NOT PERFORMED |
| 88 | 7695729345238666302 | NOT PERFORMED |
| 90 | 7695729410976009278 | NOT PERFORMED |

Series: 7695727473111092286, 《一个人的长征》. Later IDs are fixture values, not freshly verified. No new core/emulator execution; no video/audio, seeking, pause/resume, fullscreen, 30-second or physical-TV acceptance. Do not count protocol fixtures or historical community-app playback as CineDBKTV playback.

## Smallest next implementation steps

1. Establish a permitted protected-media adapter/core distribution scope for the exact intended component. Do not repeat registration, metadata, encryption or packaging discovery. If unavailable, preserve the disabled state.
2. Once that gate is satisfied, supply one approved matching-ABI executable with required notices. Implement the owned-child launcher/lifecycle using the existing readiness/status bridge; allow cleartext only for the required loopback origin, not globally. Resolve unsupported ABI behavior before changing APK compatibility.
3. Add a small series/episode-to-prepared-local-source adapter and connect existing Media3/episode navigation. Do not copy community UI. Verify engine status, actual video identity and playback before removing guards. No entire Go rewrite or external service is required merely for packaging.
4. Run episodes 1/51/88/90 for at most 30 seconds each, then focused regressions, build/lint, serializing emulator/build work. No push/merge/release without authorization.

## Resources and evidence

- This task started **81% five-hour / 73% weekly remaining**. Five-minute checkpoint: 78%/72% (3/1 points used); intermediate pre-validation reading: 76%/72% (5/1 used). 5% reserve and 7-point weekly approval threshold remain in force.
- Final closeout before documentation commit: **72% five-hour / 71% weekly remaining**, measured consumption **9 / 2 percentage points**. The 5% reserve is intact and the 7-point weekly approval threshold was not reached. No further development, build or investigation follows this handover.
- Continuous measurement is unavailable; these are account-wide rounded tool snapshots. Stop development at 5% and prepare handover before 7%; no automatic next phase.
- Original archived handover: 49848 working-copy bytes; SHA-256 `8215868300833a44c82e735d98b0a46269cd9cfe0e352f3d0cdacc4f46bae78e`. Exact prefix preservation was verified before Git normalization. Canonical LF form: 49848 bytes, SHA-256 `8215868300833a44c82e735d98b0a46269cd9cfe0e352f3d0cdacc4f46bae78e`; this normalized prefix can be checked across checkouts. Subsequent history section15 adds this task's evidence. No historical content was discarded.
- Relevant local logs/evidence: `phase5j-engine-bridge-tests.log`, `phase5j-engine-bridge-build-lint.log`, `.tmp/phase5j-evidence/embedding-artifact-audit.json` (ignored, not in fresh clone). Source links and older test/build/environment details are in history sections 9, 13–15.
