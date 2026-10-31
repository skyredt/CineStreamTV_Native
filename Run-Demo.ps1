param([ValidateSet('TV','Phone')][string]$Device = 'TV')
$ErrorActionPreference = 'Stop'
$env:ANDROID_AVD_HOME = 'D:\AndroidEmulator\avd'
$env:ANDROID_EMULATOR_HOME = 'D:\AndroidEmulator'
$env:TEMP = Join-Path $PSScriptRoot '.tmp'
$env:TMP = $env:TEMP
New-Item -ItemType Directory -Force $env:TEMP | Out-Null
$adb = 'D:\AndroidSdk\platform-tools\adb.exe'
$emulator = 'C:\Users\jasmi\AppData\Local\Android\Sdk\emulator\emulator.exe'
$devices = & $adb devices
if (-not ($devices -match 'emulator-5554')) {
    $avd = if ($Device -eq 'TV') { 'CineStream_Test_TV' } else { 'CineStream_Test_Phone35' }
    Start-Process $emulator -ArgumentList '-avd',$avd,'-no-snapshot','-gpu','swiftshader','-feature','-Vulkan','-port','5554'
}
$ready = $false
for ($attempt = 0; $attempt -lt 60; $attempt++) {
    $boot = & $adb -s emulator-5554 shell getprop sys.boot_completed 2>$null
    if ($boot -eq '1') { $ready = $true; break }
    Start-Sleep -Seconds 5
}
if (-not $ready) { throw 'Android did not finish booting within 5 minutes. Check emulator window and available memory.' }
& $adb -s emulator-5554 install -r (Join-Path $PSScriptRoot 'app\build\outputs\apk\debug\app-debug.apk')
if ($LASTEXITCODE -ne 0) { throw 'APK install failed. Build the debug APK with Build.ps1 first.' }
& $adb -s emulator-5554 shell am force-stop com.cinestream.tv
& $adb -s emulator-5554 shell am start -f 0x10008000 -n com.cinestream.tv/.MainActivity
if ($LASTEXITCODE -ne 0) { throw 'App launch failed.' }
