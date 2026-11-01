$ErrorActionPreference = 'Stop'
$env:ANDROID_AVD_HOME = 'D:\AndroidEmulator\avd'
$env:ANDROID_EMULATOR_HOME = 'D:\AndroidEmulator'
$adb = 'D:\AndroidSdk\platform-tools\adb.exe'
$emulator = 'C:\Users\jasmi\AppData\Local\Android\Sdk\emulator\emulator.exe'
$created = -not ((& $adb devices) -match 'emulator-5554')
try {
    if ($created) {
        Start-Process $emulator -WindowStyle Hidden -ArgumentList '-avd','CineStream_Test_TV','-no-window','-no-audio','-no-snapshot','-memory','1024','-cores','2','-gpu','swiftshader','-feature','-Vulkan','-port','5554' -RedirectStandardOutput (Join-Path $PSScriptRoot 'emulator-smoke.log') -RedirectStandardError (Join-Path $PSScriptRoot 'emulator-smoke-error.log')
    }
    $ready = $false
    for ($i = 0; $i -lt 60; $i++) {
        $boot = & $adb -s emulator-5554 shell getprop sys.boot_completed 2>$null
        if ($boot -eq '1') { $ready = $true; break }
        Start-Sleep -Seconds 5
    }
    if (-not $ready) { throw 'TV emulator did not finish booting within five minutes.' }
    Write-Output 'TV emulator booted.'
    & $adb -s emulator-5554 install -r (Join-Path $PSScriptRoot 'app\build\outputs\apk\debug\app-debug.apk')
    if ($LASTEXITCODE -ne 0) { throw 'Application installation failed.' }
    & $adb -s emulator-5554 install -r (Join-Path $PSScriptRoot 'app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk')
    if ($LASTEXITCODE -ne 0) { throw 'Test installation failed.' }
    & $adb -s emulator-5554 logcat -c
    $result = & $adb -s emulator-5554 shell am instrument -w com.cinestream.tv.test/com.cinestream.tv.TvSmokeRunner
    $result | Tee-Object -FilePath (Join-Path $PSScriptRoot 'tv-smoke-results.txt')
    if ((& $adb -s emulator-5554 get-state 2>$null) -eq 'device') {
        & $adb -s emulator-5554 shell screencap -p /sdcard/cinedbktv-smoke.png
        & $adb -s emulator-5554 pull /sdcard/cinedbktv-smoke.png (Join-Path $PSScriptRoot 'tv-smoke.png')
        & $adb -s emulator-5554 logcat -d -b crash | Set-Content (Join-Path $PSScriptRoot 'tv-smoke-crash.log')
    }
    if (-not ($result -match 'INSTRUMENTATION_CODE: -1')) { throw 'TV smoke test did not pass.' }
} finally {
    if ($created) { & $adb -s emulator-5554 emu kill }
}
