$ErrorActionPreference = 'Stop'
$env:JAVA_HOME = 'D:\BuildTools\jdk17\jdk-17.0.21.0.2+1'
$env:GRADLE_USER_HOME = 'D:\.gradle'
$env:ANDROID_HOME = 'D:\AndroidSdk'
$env:TEMP = Join-Path $PSScriptRoot '.tmp'
$env:TMP = $env:TEMP
$adb = Join-Path $env:ANDROID_HOME 'platform-tools\adb.exe'
Push-Location $PSScriptRoot
try {
    # Compile before starting the memory-intensive TV emulator.
    & .\gradlew.bat --no-daemon --console=plain assembleDebug assembleDebugAndroidTest
    if ($LASTEXITCODE -ne 0) { throw 'Smoke-test APK compilation failed.' }
    & .\Run-Demo.ps1 -Device TV
    & $adb -s emulator-5554 install -r app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk
    if ($LASTEXITCODE -ne 0) { throw 'Smoke-test APK installation failed.' }
    $result = & $adb -s emulator-5554 shell am instrument -w com.cinestream.tv.test/com.cinestream.tv.TvSmokeRunner
    $result | Tee-Object -FilePath tv-smoke-results.txt
    if (-not ($result -match 'INSTRUMENTATION_CODE: -1')) { throw 'TV smoke tests failed. See tv-smoke-results.txt.' }
} finally { Pop-Location }
