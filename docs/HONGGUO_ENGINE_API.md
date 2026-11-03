# Embedded Hongguo engine contract — Attempt 1

Pinned source: `aston314/hongguo-tv`, revision
`e1af6e0cbbc018bd198169d7beb4519d7b0a271a`, release v0.2.138.
The unchanged executables report version
`0.2.138-e1af6e0cbbc018bd198169d7beb4519d7b0a271a`.

## Installation and process contract

Three packaged executables: `lib/{arm64-v8a,armeabi-v7a,x86_64}/libhongguotv.so`.
These are Go PIE programs, not JNI libraries. Legacy native packaging extracts
the selected ABI to Android's installed `nativeLibraryDir`; writable app data
is not used for executable extraction. Kotlin verifies the installed SHA-256
before `ProcessBuilder` executes it. No community Android UI/decoder/updater
is included. The executable inherently contains upstream Go-embedded web assets.

Arguments: `-port 0 -instance-id <new UUID> -parent-pid <Android PID>
-data-dir <private persistent directory>`.
Environment: `MINIREEL_CACHE_DIR=<private per-run media directory>`,
`HONGGUO_TV_DATA_DIR=<private persistent directory>`, and private HOME/TMPDIR.
Readiness stdout: `HONGGUO_TV_READY <port> <instance UUID>`.
All client URLs are constructed as `http://127.0.0.1:<validated port>/`.

The real status API has no bearer-token contract. Kotlin does not supply one
or require fictitious 401/403 responses. Trust is based on the retained child,
its private readiness pipe, generated instance, pinned binary hash and exact
reported version. Loopback restricts network exposure, but does not authenticate
other applications on the same device. Do not claim application-exclusive IPC.
Only the development build currently permits cleartext for exactly 127.0.0.1.

## Non-preparation API

- `GET /api/v1/health`: HTTP 200, `{"ok":true}`. Not sufficient for identity.
- `GET /api/v1/status`: HTTP 200 with `ok`, `service` (`hongguo-tv-core`),
  `version`, `instance_id`, `implementation` (`go-api-adapter+embedded-playback`),
  `started_at`, and string-array `capabilities`.
- Unsupported methods on these handlers return 405.

Kotlin's existing `HongguoCoreEndpoint` validates actual status service,
instance and exact version. Redirects and oversized status bodies are rejected.
The core is started on demand through `HongguoCoreInstallation.runtime`, not
automatically at boot, and Hongguo playback guards remain in place.

## Playback interface — source verified, protected preparation NOT invoked

Pinned sources: `core/cmd/hongguotv/main.go` (`server.playInfo`) and
`core/cmd/hongguotv/hongguo_client.go` (`PlayInfo`).

`GET /api/v1/play/info` (alias `/api/v1/PlayInfo`) query:

| Name | Meaning |
| --- | --- |
| `series_id` | Selected Hongguo series identifier |
| `vid` | Selected episode's video identifier, not its display ordinal |
| `quality` | Normalized quality selection; invalid values return 400 before preparation |
| `codec_mode` | Normalized codec selection; invalid values return 400 before preparation |
| `mode` | `range` selects Range preparation; other/default selects full preparation |
| `fallback` | `1` enables the upstream fallback option |
| `prefetch` | In Range mode, `1` selects its non-waiting/prefetch behavior |

Successful JSON shape, **not obtained in Attempt 1**:

```json
{
  "ok": true,
  "data": {
    "path": "<prepared local media path>",
    "vid": "<selected video identifier>",
    "quality": "<optional>",
    "qualities": [],
    "video_width": 0,
    "video_height": 0,
    "video_codec": "<optional>",
    "mode": "<optional>",
    "range_fallback": "<optional>"
  }
}
```

All fields after `vid`/`path` are optional in the Go struct; example zeros and
empty lists above illustrate types, not an actual response. A preparation
failure is HTTP 502 with `ok:false`, `error`, `stage`, and a structured
`preparation` diagnostic (cached preparation-error helper). Generic request
validation errors use `ok:false,error:<message>`. The inspected router exposes
no separate percentage-progress/job-poll API; do not invent one.

The real-core Android acceptance check sends only an invalid quality request
(400) and POST (405); both exit before metadata lookup/preparation. Successful
media preparation, Range delivery and rendered Hongguo video are separate gates.

## Unprotected fixture

No public fixture-upload/arbitrary-local-file route was found in the inspected
router. Attempt 1 does not manufacture internal cache records or use a synthetic
server to substitute for this engine. Actual-core fixture delivery/Media3 playback
is therefore NOT PERFORMED. Earlier synthetic PCM checks are separate history.
