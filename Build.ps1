param([switch]$Install, [string]$Serial)
$ErrorActionPreference = 'Stop'
$env:JAVA_HOME = 'D:\BuildTools\jdk17\jdk-17.0.21.0.2+1'
$env:GRADLE_USER_HOME = 'D:\.gradle'
$env:ANDROID_HOME = 'D:\AndroidSdk'
$env:TEMP = Join-Path $PSScriptRoot '.tmp'
$env:TMP = $env:TEMP
New-Item -ItemType Directory -Force $env:TEMP | Out-Null
Push-Location $PSScriptRoot
try {
    & .\gradlew.bat --no-daemon --console=plain assembleDebug testDebugUnitTest lintDebug
    if ($LASTEXITCODE -ne 0) { throw 'Android build or checks failed.' }
    if ($Install) {
        $adbArgs = @()
        if ($Serial) { $adbArgs += @('-s', $Serial) }
        & "$env:ANDROID_HOME\platform-tools\adb.exe" @adbArgs install -r app\build\outputs\apk\debug\app-debug.apk
        if ($LASTEXITCODE -ne 0) { throw 'ADB installation failed.' }
    }
} finally { Pop-Location }
