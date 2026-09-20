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
            println("[KUI] Note: No connected Android device or emulator detected via ADB.")
            println("[KUI] Signed APK is ready for manual install or deployment:")
            println("      ${apkFile.absolutePath}")
            println("[KUI] Connect a device via USB (with USB Debugging enabled) or start an emulator and re-run 'kui run'.")
            return 0
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
        val onlineDevice = dm.listDevices().firstOrNull { it.isOnline }
        if (onlineDevice == null) {
            System.err.println("[KUI] Error: No online Android device found.")
            return 1
        }

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
        val onlineDevice = dm.listDevices().firstOrNull { it.isOnline }
        if (onlineDevice == null) {
            System.err.println("[KUI] Error: No online Android device found.")
            return 1
        }

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
}
