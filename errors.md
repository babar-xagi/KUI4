Windows PowerShell
Copyright (C) Microsoft Corporation. All rights reserved.

Try the new cross-platform PowerShell https://aka.ms/pscore6

PS C:\Users\DELL> kui doctor
==================================================
?  KUI Environment Doctor (kui version 0.1.0)
==================================================
Scanning toolchain and dependencies...

[PASS] Kotlin Compiler: 2.4.20
Path: C:\tools\kotlinc\bin\kotlinc.bat

[PASS] Java Runtime: 21.0.12.1
Path: C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot\bin\java.exe

[PASS] Android ADB: 1.0.41
Path: C:\Users\DELL\AppData\Local\Android\Sdk\platform-tools\adb.exe

--------------------------------------------------
STATUS: HEALTHY - Environment is ready for KUI builds.
PS C:\Users\DELL> kui devices
==================================================
? Android Devices (via ADB)
==================================================
ADB Path: C:\Users\DELL\AppData\Local\Android\Sdk\platform-tools\adb.exe

No connected devices or emulators found.

Troubleshooting tips:
1. Connect your Android phone with a USB data cable (not charge-only).
2. In phone Settings -> Developer Options -> Turn ON 'USB Debugging'.
3. (Xiaomi/Tecno/Realme): Also turn ON 'Install via USB'.
4. Set USB mode in phone notification shade to 'File Transfer' / 'MTP'.
5. Check phone screen for 'Allow USB debugging?' dialog and tap 'Allow'.
   PS C:\Users\DELL> kui devices
   ==================================================
   ? Android Devices (via ADB)
   ==================================================
   ADB Path: C:\Users\DELL\AppData\Local\Android\Sdk\platform-tools\adb.exe

Found 1 connected device(s):
[ONLINE]       108321541J013120     (Physical Device: TECNO_BG7)

PS C:\Users\DELL> d:
PS D:\> cd .\ui4_test\
PS D:\ui4_test> ls


    Directory: D:\ui4_test


Mode                 LastWriteTime         Length Name
----                 -------------         ------ ----
d-----         9/20/2026  11:22 PM                hello


PS D:\ui4_test> cd .\hello\
PS D:\ui4_test\hello> ls


    Directory: D:\ui4_test\hello


Mode                 LastWriteTime         Length Name
----                 -------------         ------ ----
d-----         9/20/2026  11:22 PM                .kui
d-----         9/20/2026  11:20 PM                assets
d-----         9/20/2026  11:22 PM                build
d-----         9/20/2026  11:20 PM                src
d-----         9/20/2026  11:20 PM                tests
-a----         9/20/2026  11:20 PM             26 .gitignore
-a----         9/20/2026  11:20 PM            188 kui.toml
-a----         9/20/2026  11:20 PM            294 README.md


PS D:\ui4_test\hello> kui clean
Cleaning project 'hello'...
[KUI] Clean complete: Removed build and cache artifacts.
PS D:\ui4_test\hello> kui run
[KUI] Building 'hello' (v0.1.0)
[KUI] Compiling 1 Kotlin source(s) with kotlinc 2.4.20...
[KUI] Compiled 1 source file(s) in 5164ms -> classes/
[KUI] Packaging APK: classes.dex + AndroidManifest.xml...
[KUI] Signed APK (v2): build\outputs\apk\debug\app-debug.apk (5090 bytes)
[KUI] BUILD SUCCESS (total: 13680ms)
[KUI] Target device: 108321541J013120 (TECNO_BG7)
[KUI] Installing app-debug.apk...
[KUI] Install: SUCCESS
[KUI] Launching com.example.hello/.MainActivity...
[KUI] Launch: SUCCESS (running on 108321541J013120)
PS D:\ui4_test\hello> kui run
[KUI] Building 'hello' (v0.1.0)
[KUI] Compiling 1 Kotlin source(s) with kotlinc 2.4.20...
[KUI] Compiled 1 source file(s) in 5164ms -> classes/
[KUI] Packaging APK: classes.dex + AndroidManifest.xml...
[KUI] Signed APK (v2): build\outputs\apk\debug\app-debug.apk (5046 bytes)
[KUI] BUILD SUCCESS (total: 8088ms)
[KUI] Target device: 108321541J013120 (TECNO_BG7)
[KUI] Installing app-debug.apk...
[KUI] Install: SUCCESS
[KUI] Launching com.example.hello/.MainActivity...
[KUI] Launch: SUCCESS (running on 108321541J013120)
PS D:\ui4_test\hello> cd ..
PS D:\ui4_test> ls


    Directory: D:\ui4_test


Mode                 LastWriteTime         Length Name
----                 -------------         ------ ----
d-----         9/20/2026  11:22 PM                hello


PS D:\ui4_test> kui new instaapp
Created project 'instaapp' at: D:\ui4_test\instaapp

Next steps:
cd instaapp
kui run
PS D:\ui4_test> cd .\instaapp\
PS D:\ui4_test\instaapp> code .
PS D:\ui4_test\instaapp> kui run
[KUI] Building 'instaapp' (v0.1.0)
[KUI] Compiling 1 Kotlin source(s) with kotlinc 2.4.20...
[KUI] Compiled 1 source file(s) in 5404ms -> classes/
[KUI] Packaging APK: classes.dex + AndroidManifest.xml...
[KUI] Signed APK (v2): build\outputs\apk\debug\app-debug.apk (5062 bytes)
[KUI] BUILD SUCCESS (total: 16364ms)
[KUI] Target device: 108321541J013120 (TECNO_BG7)
[KUI] Installing app-debug.apk...
[KUI] Install: SUCCESS
[KUI] Launching com.example.instaapp/.MainActivity...
[KUI] Launch: SUCCESS (running on 108321541J013120)
PS D:\ui4_test\instaapp> kui run
[KUI] Building 'instaapp' (v0.1.0)
[KUI] Compiling 1 Kotlin source(s) with kotlinc 2.4.20...
[KUI] Compiled 1 source file(s) in 5486ms -> classes/
[KUI] Packaging APK: classes.dex + AndroidManifest.xml...
[KUI] Signed APK (v2): build\outputs\apk\debug\app-debug.apk (5066 bytes)
[KUI] BUILD SUCCESS (total: 8449ms)
[KUI] Target device: 108321541J013120 (TECNO_BG7)
[KUI] Installing app-debug.apk...
[KUI] Install: SUCCESS
[KUI] Launching com.example.instaapp/.MainActivity...
[KUI] Launch: SUCCESS (running on 108321541J013120)
PS D:\ui4_test\instaapp> kui run
[KUI] Building 'instaapp' (v0.1.0)
[KUI] Compiling 1 Kotlin source(s) with kotlinc 2.4.20...
[KUI] Compilation failed:
src/main.kt:5:19: error: unresolved reference 'mutableStateOf'.
src/main.kt:9:17: error: no parameter with name 'padding' found.
src/main.kt:9:32: error: no parameter with name 'backgroundColor' found.
src/main.kt:9:50: error: unresolved reference 'Color'.
src/main.kt:10:46: error: unresolved reference 'Alignment'.
src/main.kt:15:54: error: unresolved reference 'Color'.
src/main.kt:16:54: error: unresolved reference 'Color'.
src/main.kt:17:41: error: unresolved reference 'Color'.
src/main.kt:23:37: error: enum types cannot be instantiated.
src/main.kt:24:45: error: no parameter with name 'fontSize' found.
src/main.kt:25:45: error: no parameter with name 'fontWeight' found.
src/main.kt:25:58: error: unresolved reference 'FontWeight'.
src/main.kt:26:45: error: no parameter with name 'color' found.
src/main.kt:31:25: error: none of the following candidates is applicable:
src/main.kt:31:49: error: no parameter with name 'backgroundColor' found.
src/main.kt:31:67: error: unresolved reference 'Color'.
src/main.kt:32:42: error: unresolved reference 'mutableStateOf'.
src/main.kt:32:42: error: unresolved reference 'value'.
src/main.kt:34:25: error: none of the following candidates is applicable:
src/main.kt:34:45: error: no parameter with name 'backgroundColor' found.
src/main.kt:34:63: error: unresolved reference 'Color'.
src/main.kt:37:25: error: none of the following candidates is applicable:
src/main.kt:37:49: error: no parameter with name 'backgroundColor' found.
src/main.kt:37:67: error: unresolved reference 'Color'.
src/main.kt:38:42: error: unresolved reference 'mutableStateOf'.
src/main.kt:38:42: error: unresolved reference 'value'.
PS D:\ui4_test\instaapp> kui run
[KUI] Building 'instaapp' (v0.1.0)
[KUI] Compiling 1 Kotlin source(s) with kotlinc 2.4.20...
[KUI] Compilation failed:
src/main.kt:7:42: error: unresolved reference 'Alignment'.
src/main.kt:11:48: error: unresolved reference 'copy' on receiver of type 'TextStyle'.
src/main.kt:11:61: error: unresolved reference 'Color'.
PS D:\ui4_test\instaapp> kui run
[KUI] Building 'instaapp' (v0.1.0)
[KUI] Compiling 1 Kotlin source(s) with kotlinc 2.4.20...
[KUI] Compilation failed:
src/main.kt:8:42: error: unresolved reference 'Alignment'.
PS D:\ui4_test\instaapp> kui run
[KUI] Building 'instaapp' (v0.1.0)
[KUI] Compiling 1 Kotlin source(s) with kotlinc 2.4.20...
[KUI] Compiled 1 source file(s) in 5213ms -> classes/
[KUI] Packaging APK: classes.dex + AndroidManifest.xml...
[KUI] Signed APK (v2): build\outputs\apk\debug\app-debug.apk (5190 bytes)
[KUI] BUILD SUCCESS (total: 8097ms)
[KUI] Target device: 108321541J013120 (TECNO_BG7)
[KUI] Installing app-debug.apk...
[KUI] Install: SUCCESS
[KUI] Launching com.example.instaapp/.MainActivity...
[KUI] Launch: SUCCESS (running on 108321541J013120)
PS D:\ui4_test\instaapp> kui run
[KUI] Building 'instaapp' (v0.1.0)
[KUI] Compiling 1 Kotlin source(s) with kotlinc 2.4.20...
[KUI] Compiled 1 source file(s) in 5109ms -> classes/
[KUI] Packaging APK: classes.dex + AndroidManifest.xml...
[KUI] Signed APK (v2): build\outputs\apk\debug\app-debug.apk (5342 bytes)
[KUI] BUILD SUCCESS (total: 8162ms)
[KUI] Target device: 108321541J013120 (TECNO_BG7)
[KUI] Installing app-debug.apk...
[KUI] Install: SUCCESS
[KUI] Launching com.example.instaapp/.MainActivity...
[KUI] Launch: SUCCESS (running on 108321541J013120)
PS D:\ui4_test\instaapp> kui run
[KUI] Building 'instaapp' (v0.1.0)
[KUI] Compiling 1 Kotlin source(s) with kotlinc 2.4.20...
[KUI] Compilation failed:
src/main.kt:9:9: error: no value passed for parameter 'label'.
src/main.kt:10:17: error: no parameter with name 'text' found.
src/main.kt:11:17: error: no parameter with name 'backgroundColor' found.
src/main.kt:11:35: error: unresolved reference 'Color'.
src/main.kt:12:17: error: no parameter with name 'textColor' found.
src/main.kt:12:29: error: unresolved reference 'Color'.
PS D:\ui4_test\instaapp> kui run
[KUI] Building 'instaapp' (v0.1.0)
[KUI] Compiling 1 Kotlin source(s) with kotlinc 2.4.20...
[KUI] Compilation failed:
src/main.kt:9:9: error: no value passed for parameter 'label'.
src/main.kt:10:17: error: no parameter with name 'text' found.
src/main.kt:11:17: error: no parameter with name 'backgroundColor' found.
src/main.kt:11:35: error: unresolved reference 'Color'.
src/main.kt:12:17: error: no parameter with name 'textColor' found.
src/main.kt:12:29: error: unresolved reference 'Color'.
PS D:\ui4_test\instaapp>