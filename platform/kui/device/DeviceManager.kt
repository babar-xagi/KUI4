package kui.device

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/**
 * Representation of an Android physical device or emulator (Phase 177).
 */
data class Device(
    val serial: String,
    val state: String,
    val model: String? = null,
    val product: String? = null,
    val transportId: String? = null
) {
    val isOnline: Boolean get() = state.equals("device", ignoreCase = true)
    val isEmulator: Boolean get() = serial.startsWith("emulator-")
}

/**
 * Result of executing an ADB command.
 */
data class AdbResult(
    val exitCode: Int,
    val output: String,
    val error: String
) {
    val isSuccess: Boolean get() = exitCode == 0
}

/**
 * Pure Kotlin ADB Device Manager and command runner (Phases 177–179).
 */
class DeviceManager(
    val adbPath: String = findAdbPath()
) {

    /**
     * Discovers connected Android devices and emulators via `adb devices -l` (Phase 177).
     */
    fun listDevices(): List<Device> {
        val result = executeAdb("devices", "-l")
        if (!result.isSuccess) return emptyList()
        return parseDeviceList(result.output)
    }

    /**
     * Installs an APK on the specified device or the only connected device (Phase 178).
     */
    fun installApk(apkFile: File, serial: String? = null): AdbResult {
        if (!apkFile.exists()) {
            return AdbResult(1, "", "APK file not found: ${apkFile.absolutePath}")
        }

        val targetSerial = serial ?: listDevices().firstOrNull { it.isOnline }?.serial
        val args = mutableListOf<String>()
        if (targetSerial != null) {
            args.add("-s")
            args.add(targetSerial)
        }
        args.add("install")
        args.add("-r")
        args.add(apkFile.absolutePath)

        val result = executeAdb(*args.toTypedArray())
        val output = result.output + "\n" + result.error
        val isSuccess = result.isSuccess && output.contains("Success", ignoreCase = true)
        return AdbResult(
            exitCode = if (isSuccess) 0 else (if (result.exitCode != 0) result.exitCode else 1),
            output = result.output,
            error = result.error
        )
    }

    /**
     * Launches the main activity of an application via `am start` (Phase 179).
     */
    fun launchApp(
        packageName: String,
        activityName: String = ".MainActivity",
        serial: String? = null
    ): AdbResult {
        val targetSerial = serial ?: listDevices().firstOrNull { it.isOnline }?.serial
        val targetComponent = if (activityName.startsWith(".")) "$packageName/$activityName" else "$packageName/.$activityName"

        val args = mutableListOf<String>()
        if (targetSerial != null) {
            args.add("-s")
            args.add(targetSerial)
        }
        args.add("shell")
        args.add("am")
        args.add("start")
        args.add("-n")
        args.add(targetComponent)

        val result = executeAdb(*args.toTypedArray())
        val output = result.output + "\n" + result.error
        val isError = output.contains("Error:", ignoreCase = true) || output.contains("does not exist", ignoreCase = true)
        return AdbResult(
            exitCode = if (result.isSuccess && !isError) 0 else 1,
            output = result.output,
            error = result.error
        )
    }

    /**
     * Executes an ADB command with timeout.
     */
    fun executeAdb(vararg args: String, timeoutSeconds: Long = 15): AdbResult {
        return try {
            val cmd = mutableListOf(adbPath)
            cmd.addAll(args)
            val process = ProcessBuilder(cmd)
                .redirectErrorStream(false)
                .start()

            val stdoutReader = process.inputStream.bufferedReader()
            val stderrReader = process.errorStream.bufferedReader()

            val stdout = StringBuilder()
            val stderr = StringBuilder()

            val outThread = Thread { stdoutReader.forEachLine { stdout.appendLine(it) } }
            val errThread = Thread { stderrReader.forEachLine { stderr.appendLine(it) } }
            outThread.start()
            errThread.start()

            val completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
            if (!completed) {
                process.destroyForcibly()
                return AdbResult(-1, stdout.toString().trim(), "ADB command timed out after ${timeoutSeconds}s")
            }

            outThread.join(2000)
            errThread.join(2000)

            AdbResult(process.exitValue(), stdout.toString().trim(), stderr.toString().trim())
        } catch (e: Exception) {
            AdbResult(-1, "", e.message ?: "Failed to execute ADB")
        }
    }

    companion object {
        /**
         * Resolves the path to the `adb` executable from environment or standard SDK locations.
         */
        fun findAdbPath(): String {
            val envAndroidHome = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
            if (!envAndroidHome.isNullOrBlank()) {
                val candidate = File(envAndroidHome, "platform-tools/adb" + if (isWindows()) ".exe" else "")
                if (candidate.exists()) return candidate.absolutePath
            }

            val defaultWindows = File(System.getProperty("user.home"), "AppData/Local/Android/Sdk/platform-tools/adb.exe")
            if (defaultWindows.exists()) return defaultWindows.absolutePath

            return if (isWindows()) "adb.exe" else "adb"
        }

        private fun isWindows(): Boolean =
            System.getProperty("os.name")?.lowercase()?.contains("windows") == true

        /**
         * Parses output lines from `adb devices -l`.
         * Example line:
         * "emulator-5554          device product:sdk_gphone64_arm64 model:sdk_gphone64_arm64 device:emu64a transport_id:1"
         */
        fun parseDeviceList(output: String): List<Device> {
            val devices = mutableListOf<Device>()
            val lines = output.lines()
            for (rawLine in lines) {
                val line = rawLine.trim()
                if (line.isEmpty() || line.startsWith("List of devices") || line.startsWith("*")) continue

                val tokens = line.split(Regex("\\s+"))
                if (tokens.size >= 2) {
                    val serial = tokens[0]
                    val state = tokens[1]
                    var model: String? = null
                    var product: String? = null
                    var transportId: String? = null

                    for (i in 2 until tokens.size) {
                        val tok = tokens[i]
                        if (tok.startsWith("model:")) model = tok.removePrefix("model:")
                        if (tok.startsWith("product:")) product = tok.removePrefix("product:")
                        if (tok.startsWith("transport_id:")) transportId = tok.removePrefix("transport_id:")
                    }

                    devices.add(
                        Device(
                            serial = serial,
                            state = state,
                            model = model,
                            product = product,
                            transportId = transportId
                        )
                    )
                }
            }
            return devices
        }
    }
}
