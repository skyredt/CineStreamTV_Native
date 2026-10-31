"""Package reviewed source only; never include caches, signing keys or build intermediates."""
from pathlib import Path
from hashlib import sha256
from zipfile import ZipFile, ZIP_DEFLATED
import shutil
import xml.etree.ElementTree as ET

root = Path('D:/CineStreamTV_Native')
stage = Path(__file__).parent
apk = root / 'app/build/outputs/apk/debug/app-debug.apk'
log = (root / 'build-verification.log').read_text(encoding='utf-8-sig')
assert 'BUILD SUCCESSFUL' in log
tests = ET.parse(root / 'app/build/test-results/testDebugUnitTest/TEST-com.cinestream.tv.CatalogTest.xml').getroot()
assert tests.attrib['failures'] == tests.attrib['errors'] == '0'
lint = (root / 'app/build/reports/lint-results-debug.txt').read_text(encoding='utf-8')
assert '0 errors' in lint
report = f'''# Verification — 31 October 2026 (Singapore)

- Original archive unchanged: SHA256 `c98f6f0a3a5b5fb65b6e043eef7a96a8063c2d9b9be6a045a5b1c1010543e18b`.
- Archive inspection confirmed 18 embedded definitions, matching the web source, and missing `TvAppNavigation`, `MockData`, `CineStreamApp` definitions. Exporter-only strings/styles were omitted from the previous extraction.
- Existing native project backed up to `D:\\CineStreamTV_Native_backup_20261031_225341` before changes. Retired original app source and malformed old version catalog also preserved there.
- Final command: `D:\\CineStreamTV_Native\\Build.ps1` (Gradle `assembleDebug testDebugUnitTest lintDebug`).
- Final build: **BUILD SUCCESSFUL**. {tests.attrib['tests']} unit tests, 0 failures, 0 errors.
- Android lint: {lint.strip().splitlines()[-1]}. Warnings concern newer dependency versions, backup declaration and raster density variants; no lint errors.
- APK: `{apk}` ({apk.stat().st_size:,} bytes).
- APK SHA256: `{sha256(apk.read_bytes()).hexdigest()}`.
- `apksigner verify`: exit 0. This verifies the debug artifact's signature, not device installation.
- `aapt dump badging`: package `com.cinestream.tv`, version code 2, minSdk 26, target/compile SDK 35, phone and Leanback launch activities present, TV and touchscreen optional.
- Tool versions checked: Android Studio JBR 25.0.4; separate build JDK Microsoft 17.0.21.0.2; Gradle 8.9; AGP 8.6.1; Kotlin 2.0.21; Build Tools 36.0.0; independently installed SDK platform 35 revision 2. Gradle distribution checksum pinned to the official published checksum.
- First staging build encountered a Kotlin compiler daemon connection reset and succeeded using its in-process fallback. Final configuration uses in-process Kotlin compilation and two workers; the final destination build succeeded.
- Original MP4 samples: HTTP 403 from this computer, including a ranged GET for Big Buck Bunny. Catalog descriptions disclose this. Primary Apple HLS sample playlist: HTTP 200. These are desktop HTTP checks, not playback verification.
- ADB check: no devices connected. **Installation: not performed. Android device behavior: not verified.**
- Release configuration prepared, release artifact not built, no release signing key created or supplied. Debug signing is for development only.

Review `README.md` for exact build/install/start commands, integration requirements and remaining limitations. The original web exporter remains historical; use this native project.
'''
(stage / 'VERIFICATION.md').write_text(report, encoding='utf-8')
shutil.copyfile(apk, stage.parent / 'CineStreamTV-debug.apk')
with ZipFile(stage.parent / 'CineStreamTV-Native-repaired.zip', 'w', ZIP_DEFLATED) as z:
    for p in stage.rglob('*'):
        rel = p.relative_to(stage)
        if not p.is_file() or any(part in {'.tmp', '.gradle', 'build'} for part in rel.parts):
            continue
        if p.suffix in {'.jks', '.keystore'} or p.name == 'build-verification.log':
            continue
        z.write(p, Path('CineStreamTV_Native') / rel)
print(report)
