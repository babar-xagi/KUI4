package kui.device

import kui.build.BuildCommand
import kui.config.ConfigDiagnostics
import kui.project.ProjectFinder
import java.io.File

/**
 * Handlers for `kui run`, `kui install`, and `kui launch` CLI commands (Phases 178–180).
 */
object RunCommand {

    fun executeRun(args: List<String> = emptyList(), flags: Map<String, String> = emptyMap()): Int {
        val root = ProjectFinder.findProjectRoot()
        if (root == null) {
            System.err.println("kui: No 'kui.toml' found in current directory or parent directories.")
            return 1
        }

        // 1. Build APK first
        val buildExit = BuildCommand.execute(args, flags, projectRootOverride = root)
        if (buildExit != 0) return buildExit

        val config = when (val res = ConfigDiagnostics.validateAndLoad(root)) {
            is kui.config.ConfigValidationResult.Success -> res.config
            else -> return 1
        }

        val apkFile = File(root, "build/outputs/apk/debug/app-debug.apk")
        if (!apkFile.exists()) {
            System.err.println("[KUI] Error: APK not found at '${apkFile.path}'")
            return 1
        }

        // 2. Discover device
        val dm = DeviceManager()
        val devices = dm.listDevices()
        val onlineDevice = devices.firstOrNull { it.isOnline }
        if (onlineDevice == null) {
            val unauthorizedDevice = devices.firstOrNull { it.isUnauthorized }
            val offlineDevice = devices.firstOrNull { it.isOffline }

            if (unauthorizedDevice != null) {
                System.err.println()
                System.err.println("==================================================================")
                System.err.println("[KUI] ⚠️  ANDROID DEVICE ATTACHED BUT UNAUTHORIZED: ${unauthorizedDevice.serial}")
                System.err.println("==================================================================")
                System.err.println("ADB has detected your phone, but USB Debugging permission was not granted.")
                System.err.println()
                System.err.println("👉 ACTION REQUIRED ON YOUR PHONE SCREEN:")
                System.err.println("   1. Unlock your phone screen right now.")
                System.err.println("   2. A prompt 'Allow USB debugging?' has appeared.")
                System.err.println("   3. Check the box: ☑ 'Always allow from this computer'")
                System.err.println("   4. Tap 'Allow'.")
                System.err.println("   5. Re-run 'kui run'!")
                System.err.println("==================================================================")
                return 1
            } else if (offlineDevice != null) {
                System.err.println()
                System.err.println("==================================================================")
                System.err.println("[KUI] ⚠️  ANDROID DEVICE ATTACHED BUT OFFLINE: ${offlineDevice.serial}")
                System.err.println("==================================================================")
                System.err.println("👉 ACTION REQUIRED TO RECONNECT:")
                System.err.println("   1. Unplug and re-plug your USB cable.")
                System.err.println("   2. On your phone: Settings -> Developer Options -> turn 'USB Debugging' OFF and ON.")
                System.err.println("   3. Re-run 'kui run'!")
                System.err.println("==================================================================")
                return 1
            } else if (devices.isNotEmpty()) {
                val other = devices.first()
                System.err.println("[KUI] ⚠️ Device ${other.serial} detected in state: '${other.state}' (not 'device'/online).")
                System.err.println("[KUI] Reconnect USB or restart ADB to resolve.")
                return 1
            } else {
                println("[KUI] Note: No connected Android device or emulator detected via ADB.")
                println("[KUI] Signed APK is ready for manual install or deployment:")
                println("      ${apkFile.absolutePath}")
                println()
                println("[KUI] 📱 How to connect your Android phone for 1-click install & launch:")
                println("      1. Connect phone to laptop using a USB data cable (ensure it supports data transfer).")
                println("      2. On phone: Go to Settings -> About Phone -> Tap 'Build Number' 7 times to enable Developer Mode.")
                println("      3. Go to Settings -> System -> Developer Options:")
                println("         • Enable 'USB Debugging'.")
                println("         • (If present on Xiaomi/Tecno/Realme): Enable 'Install via USB'.")
                println("      4. Pull down phone notification shade, tap 'USB charging this device', and select 'File Transfer' / 'MTP'.")
                println("      5. When 'Allow USB debugging?' appears on screen, check 'Always allow' and tap 'Allow'.")
                println("      6. Run 'kui devices' to verify status, then re-run 'kui run'!")
                return 0
            }
        }

        println("[KUI] Target device: ${onlineDevice.serial} (${onlineDevice.model ?: onlineDevice.product ?: "Android Device"})")

        // 3. Install APK
        println("[KUI] Installing ${apkFile.name}...")
        val installRes = dm.installApk(apkFile, onlineDevice.serial)
        if (!installRes.isSuccess) {
            System.err.println("[KUI] Installation failed: ${installRes.error.ifBlank { installRes.output }}")
            return 1
        }
        println("[KUI] Install: SUCCESS")

        // 4. Launch App
        val pkgName = config.project.applicationId ?: "com.example.${config.project.name.lowercase().replace('-', '_')}"
        println("[KUI] Launching $pkgName/.MainActivity...")
        val launchRes = dm.launchApp(pkgName, ".MainActivity", onlineDevice.serial)
        if (!launchRes.isSuccess) {
            System.err.println("[KUI] Launch failed: ${launchRes.error.ifBlank { launchRes.output }}")
            return 1
        }
        println("[KUI] Launch: SUCCESS (running on ${onlineDevice.serial})")
        return 0
    }

    fun executeInstall(args: List<String> = emptyList(), flags: Map<String, String> = emptyMap()): Int {
        val root = ProjectFinder.findProjectRoot()
        if (root == null) {
            System.err.println("kui: No 'kui.toml' found.")
            return 1
        }
        val apkFile = File(root, "build/outputs/apk/debug/app-debug.apk")
        if (!apkFile.exists()) {
            println("[KUI] APK not found. Running build first...")
            val buildExit = BuildCommand.execute(args, flags, projectRootOverride = root)
            if (buildExit != 0) return buildExit
        }

        val dm = DeviceManager()
        val onlineDevice = findOnlineOrDiagnose(dm) ?: return 1

        println("[KUI] Installing to ${onlineDevice.serial}...")
        val res = dm.installApk(apkFile, onlineDevice.serial)
        if (!res.isSuccess) {
            System.err.println("[KUI] Installation failed: ${res.error.ifBlank { res.output }}")
            return 1
        }
        println("[KUI] Install: SUCCESS")
        return 0
    }

    fun executeLaunch(args: List<String> = emptyList(), flags: Map<String, String> = emptyMap()): Int {
        val root = ProjectFinder.findProjectRoot()
        if (root == null) {
            System.err.println("kui: No 'kui.toml' found.")
            return 1
        }
        val config = when (val res = ConfigDiagnostics.validateAndLoad(root)) {
            is kui.config.ConfigValidationResult.Success -> res.config
            else -> return 1
        }

        val dm = DeviceManager()
        val onlineDevice = findOnlineOrDiagnose(dm) ?: return 1

        val pkgName = config.project.applicationId ?: "com.example.${config.project.name.lowercase().replace('-', '_')}"
        println("[KUI] Launching $pkgName...")
        val res = dm.launchApp(pkgName, ".MainActivity", onlineDevice.serial)
        if (!res.isSuccess) {
            System.err.println("[KUI] Launch failed: ${res.error.ifBlank { res.output }}")
            return 1
        }
        println("[KUI] Launch: SUCCESS")
        return 0
    }

    private fun findOnlineOrDiagnose(dm: DeviceManager): Device? {
        val devices = dm.listDevices()
        val onlineDevice = devices.firstOrNull { it.isOnline }
        if (onlineDevice != null) return onlineDevice

        val unauthorized = devices.firstOrNull { it.isUnauthorized }
        val offline = devices.firstOrNull { it.isOffline }
        when {
            unauthorized != null -> {
                System.err.println("[KUI] ⚠️ Device ${unauthorized.serial} is UNAUTHORIZED.")
                System.err.println("[KUI] Check your phone screen now and tap 'Allow USB debugging'!")
            }
            offline != null -> {
                System.err.println("[KUI] ⚠️ Device ${offline.serial} is OFFLINE.")
                System.err.println("[KUI] Reconnect USB cable or toggle USB debugging.")
            }
            devices.isNotEmpty() -> {
                System.err.println("[KUI] ⚠️ Device ${devices.first().serial} is in '${devices.first().state}' state.")
            }
            else -> {
                System.err.println("[KUI] Error: No online Android device found via ADB. Run 'kui devices' for info.")
            }
        }
        return null
    }
}
