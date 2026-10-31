# CineDBKTV 1.3.1

Debug APK compiled successfully; 16 unit tests passed and Android lint reports 0 errors. This is a TV testing candidate, not a claim of complete device verification. See VERIFICATION.md for the precise limits.

Implemented: compact DBKU episode ranges and jump field; shared draggable timeline for native media and supported Cineby Xpass playback; Cineby remote volume, scrubbing and play/pause; Menu source selection; same-selection source fallback with a notice; DBKU-only country and Chinese genre filters; CineDBKTV display name.

Private signing keys, APKs, caches, emulator captures and local SDK configuration are excluded from source control. The original main commit remains in repository history. Source is maintained on development/cinedbktv-1.3 and synchronized to main after build verification.

Version 1.3.1 restores the phone launcher and makes Leanback optional. TV launcher and landscape interface are retained. Version code increments to 8 for installation over 1.3.0. Main remains unchanged; this development branch contains the updates.
