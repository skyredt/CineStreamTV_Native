# CineDBKTV Phase 5J — authoritative handover

Updated 2 November 2026 (Asia/Singapore). Scope: **Attempt 1 verified; Attempt 2 requested but not implemented**.

## Attempt 2 request — no playback implementation performed

The user requested real playback after Attempt 1. Baseline was verified clean on `feature/hongguo-single-apk`, HEAD `62a7e90b7309cb0fbdd54a3095cc3a77102c03ef`. Live balance snapshot: **39% five-hour / 66% weekly remaining**. Only the baseline documents, Git state and usage were checked.

No real preparation request was sent, no protected key/media handling was executed, and no playback code, build, test, emulator or new APK was produced. The known custom protected-key/CENC-clearing route was not wired up or invoked as a way around content protection. Community-code reuse permission remains accepted as reported. Integration can proceed with a provider-authorized playable source or supported DRM session configuration; no such usable path was established here. This is a limitation on the requested implementation path, not evidence of a new server denial, proven unavailable entitlement or universal technical impossibility.

Episodes **1, 51, 88 and 90: NOT PERFORMED**, including all video, audio, seeking, fullscreen and control acceptance. Actual engine remains integrated from Attempt 1, but was not executed in this continuation. Hongguo playback remains disabled. Only this handover/history status is changed and committed locally; no push/distribution. Attempt 1 results below remain historical verified evidence, not Attempt 2 success.

## Attempt 1 result

**Real-engine integration: PASS. Hongguo video playback: NOT TESTED / disabled.**

The actual unchanged Hongguo Go executable is packaged in CineDBKTV's development APK. On an Android 35 x86_64 emulator, CineDBKTV verified its installed hash, launched its own child, parsed its readiness/status, checked its exact version, communicated with its real localhost API, stopped it, restarted it, forcibly terminated it, observed failure and cleanup, and successfully started/stopped a replacement child. No fixture substituted for these acceptance tests. No valid episode-preparation request, protected-media decryption or access-control workaround was invoked. Attempt 2 was subsequently requested; Phase 6 remains outside scope.

| Acceptance | Result |
| --- | --- |
| Real executables present in single application APK | PASS — three payloads match audited upstream hashes |
| Android-installed executable extraction | PASS — extractNativeLibs=true; nativeLibraryDir launch; x86_64 selected |
| Real Go child startup and readiness | PASS |
| Real status HTTP / Kotlin identity/version parsing | PASS — HTTP 200 |
| Normal shutdown / subsequent restart | PASS |
| Unexpected child termination / cleanup / recovery | PASS |
| No orphan CineDBKTV children | PASS — all three captured children dead; final process audit found no additional owned engine |
| Media endpoint/schema | Pinned-source contract verified; live invalid-quality 400 and POST 405 verified before preparation |
| Actual-core unprotected fixture / Media3 rendering | NOT PERFORMED — no supported arbitrary-fixture route found in inspected router; no synthetic substitute |
| Real Hongguo episodes 1 / 51 / 88 / 90 | NOT PERFORMED — separate playback acceptance gate |

## Repository and preservation

- Repository: `D:\CineStreamTV\cinedbktv-development`; branch `feature/hongguo-single-apk`.
- Starting HEAD: `db0d48e132e9be1af34cc9f23b31f1370c4caa0e`; starting tree was clean.
- Verified implementation is committed locally at closeout; full successor SHA and final status are recorded in the final response and `git rev-parse HEAD` / `git status --short`.
- Existing Cineby, DBKU, BingeBang, Hongguo catalog, unified search, navigation and Media3 UI/provider routing were not changed. Existing branches, history, stable/Phase4 APKs and draft PR #1 were not modified; no remote mutation or PR-status recheck occurred.
- Prior runtime APK archived before rebuild: `CineDBKTV-phase5j-runtime-dev-db0d48e.apk`, SHA-256 `2e254a8d792920dc3de499e0bad862f06ed42f634998cf6a313de3596e08cfb8`. Earlier archives remain untouched.
- No push, merge, publication, deployment or distribution.

## Exact upstream and package

- Source: https://github.com/aston314/hongguo-tv/tree/e1af6e0cbbc018bd198169d7beb4519d7b0a271a
- Origin release APK v0.2.138: SHA-256 `61a9b355e876a3ebfa9f484bd8b66859bcbac18343c42e8ac736a114755d6ae9`.
- Exact embedded/runtime-reported version: **`0.2.138-e1af6e0cbbc018bd198169d7beb4519d7b0a271a`**; Go runtime marker `go1.25.14`.
- Only three core executables and applicable Go/QR-code notices were imported. No community Android UI, MPV, NextLib/FFmpeg decoder bundle, updater, boot receiver, separate application or external backend was added. The executables inherently contain upstream Go-embedded web assets.

| APK path | Bytes | SHA-256 |
| --- | ---: | --- |
| `lib/arm64-v8a/libhongguotv.so` | 12,576,488 | `5ec423f97803cb36acedd0fb85925833cfdb37836547e4d61c82a146c7b7d100` |
| `lib/armeabi-v7a/libhongguotv.so` | 12,476,652 | `88668b4399700e0a3ff7e94c7f760c40d61c67b467d9d8af40908bd35b956a74` |
| `lib/x86_64/libhongguotv.so` | 13,352,008 | `f68197bfe7e9291039467b79fc15791609ccd4bd29df64802feba357f34ba2ee` |

All three were rehashed inside the final APK. ARM payloads are packaging/integrity-verified but were not device-executed in this task. Upstream provides no 32-bit x86 executable; this native APK targets the three listed ABIs. The earlier x86-only TV AVD is unsuitable; runtime acceptance used x86_64. Physical TV/phone and 16-KB-page device verification remain NOT PERFORMED.

## Implemented changes

- `app/src/main/jniLibs/{arm64-v8a,armeabi-v7a,x86_64}/libhongguotv.so`: actual unchanged audited cores.
- `app/build.gradle.kts`: legacy JNI extraction and no stripping of the executable, preserving exact hashes.
- `HongguoCoreInstallation.kt`: pinned version and per-ABI hashes; installed nativeLibraryDir execution; actual `-port 0`, generated `-instance-id`, `-parent-pid`, `-data-dir`; real `MINIREEL_CACHE_DIR` and `HONGGUO_TV_DATA_DIR`; private transient cache and persistent no-backup data.
- `HongguoCoreRuntime.kt`: actual core defaults to no bearer contract; existing child ownership/readiness/version/timeout/cancellation/crash/restart handling reused. Optional bearer mode retained only for earlier independent fixture checks; it is not used by the real engine.
- `HongguoCoreHttp.kt`: bounded, nonredirecting 127.0.0.1 transport; corrected documentation about upstream authentication.
- `HongguoRealCoreCheck.kt` plus runner dispatch `hongguoRealCore=true`: real installed core startup/API/normal shutdown/restart/crash/cleanup/recovery acceptance.
- Existing host and synthetic Android checks explicitly retain their own fixture authentication option; they are not real-engine acceptance evidence.
- `app/src/main/assets/hongguo-notices/`: provenance, Go BSD licence and go-qrcode MIT licence.
- `docs/HONGGUO_ENGINE_API.md`: actual startup/status/play-info request and response schemas, observed-vs-source-only boundaries and fixture limitation.

Runtime entry is `HongguoCoreInstallation.runtime(context)` followed by `start()`/`stop()`. It is demand-driven, not automatically started at boot. No Hongguo player call site or protected-preparation adapter is enabled. The debug build retains the existing cleartext exception for exactly 127.0.0.1; release runtime startup was not tested. Upstream status has no bearer authentication: owned child/UUID/hash/version checks prevent adopting a discovered service, but loopback does not provide application-exclusive peer authentication. Do not claim it does.

## Validation and evidence

- **60 host tests total: 58 passed, 2 skipped, 0 failures/errors.** Includes Cineby/DBKU/BingeBang provider regression fixtures, Hongguo catalog/registration/metadata/readiness and lifecycle tests. Opt-in live upstream unit tests skipped. No new live playback regression of existing providers was performed.
- **Debug app build PASS; instrumentation build PASS; Android lint PASS, 0 errors / 6 existing warnings.** No new lint finding in the runtime/real-core check.
- Actual packaged core acceptance on Android 35 x86_64: **all six reported checks PASS**, three real child launches. Binary hash verification plus generated instance/readiness pipe and actual status response tie the tests to CineDBKTV's own installed core.
- Live API validation: invalid quality returned 400, POST returned 405. Source checks establish both exit before preparation dispatch. Successful preparation response/Range rendering was intentionally NOT tested.
- Final process audit showed only the previously installed standalone community application's own core under its own parent (`rocks.aston.hongguotv`), not an orphan CineDBKTV child. That application was not used for acceptance. The test emulator was shut down after verification.
- Ignored logs: `phase5j-attempt1-validation.log`, `phase5j-attempt1-real-core.log`, `phase5j-attempt1-emulator.log`, `phase5j-attempt1-emulator-error.log`.
- Build and emulator work were serialized. Constrained Gradle: offline/no-daemon/max-workers=1, 768 MiB heap, SerialGC/ActiveProcessorCount=2, in-process Kotlin. JDK `D:\BuildTools\jdk17\jdk-17.0.21.0.2+1`, SDK `D:\AndroidSdk`, cache `D:\.gradle`, Gradle `D:\CineStreamTV\.tools\gradle-8.9\bin\gradle.bat`. Emulator: existing API35 x86_64 phone AVD, 1536 MiB, 2 cores, no snapshot/window/audio, GPU off.

## Development APK

**`D:\CineStreamTV\cinedbktv-development\CineDBKTV-phase5j-attempt1-engine-dev-20261102.apk`**

- Size: **44,287,706 bytes**.
- SHA-256: **`951b7d73d1df59708aa42786a4d958174370a8febc921e2a45df674110bc18e5`**.
- Identical build output: `app/build/outputs/apk/debug/app-debug.apk`.
- Verified for Attempt 1 engine integration only; not verified for Hongguo video playback. No distribution performed. Instrumentation APK is a test harness, not a companion required by the application.

## Remaining gates and usage

Attempt 1 has no remaining blocking engineering issue for the tested x86_64 lifecycle/API scope. Media preparation and live protected playback remain separate, unperformed gates. Accept the user's reported community-developer modification/reuse permission; do not repeatedly reopen general permission or feasibility discussions. Do not infer upstream content authorization or bypass restrictions. No protected keys were extracted and no protected-video preparation was requested here.

Start balances at 05:51:54 UTC: **50% five-hour / 68% weekly remaining**. Checkpoints: **47%/67%** (3/1 consumed), **44%/67%** (6/1 consumed), pre-closeout **41%/66%** (9/2 consumed). Values are rounded account-wide snapshots, not continuous/task-exclusive accounting. Reserve and weekly threshold remain intact. Final closeout snapshot is reported in the final response. The subsequent user prompt authorizes Attempt 2 within its restrictions; no Phase 6, push, merge or distribution is authorized.
