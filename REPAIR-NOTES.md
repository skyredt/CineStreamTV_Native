# CineDBKTV 1.3.2

Native Kotlin/Compose application for Android TV, with a phone launcher for initial testing. The app name is CineDBKTV; the application ID remains com.cinestream.tv so existing installations update. Both launchers are retained, Leanback is optional, and the original TV layout remains landscape.

## Playback controls

Cineby uses the selected provider's web player as a single fullscreen playback surface. There is no second app timeline or permanent app header. Video proportions are preserved; cinematic black bars in the source may remain.

For the supported Xpass web player, arrow keys move the visible selection among its own controls. OK activates the selected control. Seeking occurs only from the selected web-player timeline or its own seek buttons. Up/Down no longer changes device volume. The gear menu exposes the quality options actually supplied by the source; the app does not invent 720p/1080p variants. Subtitles are selectable only when supplied by the selected source. Automatic speech-to-text/live captions are not included.

Menu opens the app's source chooser. Back first closes an open web-player settings menu/fullscreen view, then returns to title details. Failed Cineby sources can advance within the same selected title/episode with a notice. Embedded providers may also manage their own internal switching.

Other embedded hosts retain their own controls. The Xpass layout/navigation adapter cannot guarantee identical behavior for every third-party host.

DBKU retains its working native controls, compact text episode buttons, ranges of 50 episodes and jump field. No episode thumbnails are displayed for DBKU. Country and Chinese genre filters apply only to DBKU; Cineby ignores those two filters. DBKU filtered searches inspect loaded filtered catalog pages; load more to inspect older titles. No catalog selection silently falls back to a sample video.

## Build and install

Use PowerShell and keep the emulator closed while building on this machine:

```powershell
Set-Location 'D:\CineStreamTV\android-tv-repair'
.\Build.ps1
& 'D:\AndroidSdk\platform-tools\adb.exe' devices
& 'D:\AndroidSdk\platform-tools\adb.exe' -s emulator-5554 install -r '.\app\build\outputs\apk\debug\app-debug.apk'
& 'D:\AndroidSdk\platform-tools\adb.exe' -s emulator-5554 shell am start -n com.cinestream.tv/.MainActivity
```

For a physical device, replace emulator-5554 with its serial from adb devices. Run-Demo.ps1 starts the prepared TV emulator and installs the APK; its historical name does not mean the app uses a demo catalog.

Build.ps1 uses Microsoft OpenJDK 17.0.21, Gradle 8.9, SDK platform 35 and Build Tools 36.0.0 on D:. Android Studio's Java 25 runtime is unsuitable for this Gradle version. Update machine-specific tool paths and local.properties when importing elsewhere. Caches and temporary files stay on D:.

The debug APK is suitable for sideloading. Release shrinking/ProGuard configuration remains present. No private release signing key was generated or changed. The original archive and D:\CineStreamTV_Native were preserved; backups are under D:\CineStreamTV\backups.

See VERIFICATION.md for the exact checks and limits for this build. Source is maintained on development/cinedbktv-1.3 in the existing GitHub repository; main remains unchanged pending approval.
