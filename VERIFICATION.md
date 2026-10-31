# Verification of version 1.2.0

- Based on GitHub commit 7558d5c2ac5c2b34ba83eeee4f9f4123b3e0ead0, repaired in a separate directory. Original archive and existing native project preserved.
- Debug compilation: BUILD SUCCESSFUL. Version code 6, min SDK 26, target/compile SDK 35. TV launcher and required Leanback; touchscreen optional.
- Unit tests: 12 passed, 0 failures, 0 errors. Includes captured live Lanterns results, Chinese DBKU results, exact episode links, filtering and pagination.
- Android lint: 0 errors, 14 warnings. Includes dependency updates, vector/density resources, TV landscape orientation, WebView JavaScript and backup declarations. No dependency migration was attempted.
- APK signature verified successfully with apksigner.
- Live desktop provider checks: Cineby lists Lanterns (2026), eight episodes, and returns twelve source options for episode 1. DBKU Chinese search returns 尚公主 and its detail page lists twenty episodes. These checks do not prove playback on a device.
- Test APK compiled successfully. Emulator boot was still in progress when Computer Use was stopped with Escape. The test-launch script was then cancelled. Installation and emulator behavior of this APK are NOT verified. Physical-TV behavior of this revision is NOT verified.
- Two earlier build attempts ran out of Windows commit memory. Reduced Gradle memory, serial GC and closing Chrome allowed the final build and checks to pass.
- No release signing key generated or modified. No GitHub push performed.

APK SHA256: dc3d04b541b4251b87d925783d033eac40b73b55ea62148a77942f28c6be6253
