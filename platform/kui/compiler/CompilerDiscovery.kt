package kui.compiler

import java.io.File
import java.util.Locale

/**
 * Information about a discovered external or SDK tool.
 */
data class ToolInfo(
    val name: String,
    val path: String,
    val version: String,
    val isValid: Boolean,
    val message: String = ""
)

/**
 * Combined environment health report.
 */
data class EnvironmentReport(
    val kotlin: ToolInfo,
    val java: ToolInfo,
    val adb: ToolInfo?,
    val isReady: Boolean,
    val errors: List<String>
)

/**
 * Discovers and validates Kotlin compiler, JDK runtime, and Android SDK tools (Phases 036, 037, 038).
 */
object CompilerDiscovery {

    private val isWindows: Boolean =
        System.getProperty("os.name")?.lowercase(Locale.US)?.contains("windows") == true

    /**
     * Discovers the official Kotlin compiler (kotlinc).
     * Validates that Kotlin version is >= 2.0.0.
     */
    fun findKotlinc(): ToolInfo {
        val candidates = mutableListOf<String>()

        // 1. Check explicit environment variables
        System.getenv("KOTLIN_HOME")?.let { candidates.add(File(it, if (isWindows) "bin/kotlinc.bat" else "bin/kotlinc").path) }
        System.getenv("KOTLINC_HOME")?.let { candidates.add(File(it, if (isWindows) "bin/kotlinc.bat" else "bin/kotlinc").path) }

        // 2. Common Windows paths
        if (isWindows) {
            candidates.add("C:\\tools\\kotlinc\\bin\\kotlinc.bat")
            candidates.add("C:\\Program Files\\kotlinc\\bin\\kotlinc.bat")
            candidates.add("C:\\kotlinc\\bin\\kotlinc.bat")
            System.getenv("LOCALAPPDATA")?.let {
                candidates.add(File(it, "Programs/IntelliJ IDEA/plugins/Kotlin/kotlinc/bin/kotlinc.bat").path)
            }
        } else {
            candidates.add("/usr/local/bin/kotlinc")
            candidates.add("/usr/bin/kotlinc")
            candidates.add("/opt/kotlinc/bin/kotlinc")
        }

        // 3. Search PATH
        val pathExec = if (isWindows) "kotlinc.bat" else "kotlinc"
        val fromPath = findExecutableInPath(pathExec)
        if (fromPath != null) {
            candidates.add(0, fromPath)
        }

        // Locate first existing executable
        val executable = candidates.map { File(it) }.firstOrNull { it.exists() && it.isFile }
            ?: return ToolInfo(
                name = "Kotlin Compiler",
                path = "NOT_FOUND",
                version = "unknown",
                isValid = false,
                message = "kotlinc was not found in PATH or standard locations (C:\\tools\\kotlinc\\bin)."
            )

        // Read version from kotlinc -version
        val (exitCode, stdout, stderr) = runProcessQuiet(listOf(executable.absolutePath, "-version"))
        val combinedOutput = "$stdout\n$stderr".trim()
        val versionRegex = Regex("""(?:kotlinc-jvm|Kotlin version)\s+([0-9]+\.[0-9]+\.[0-9]+)""")
        val match = versionRegex.find(combinedOutput)

        val versionStr = match?.groupValues?.get(1) ?: run {
            // Fallback line search
            combinedOutput.lines().firstOrNull { it.contains("kotlinc", ignoreCase = true) } ?: "unknown"
        }

        val isValidVersion = isKotlinVersionSupported(versionStr)
        val message = if (isValidVersion) {
            "Valid Kotlin 2.x compiler detected."
        } else {
            "Detected Kotlin version '$versionStr', but KUI requires Kotlin >= 2.0.0."
        }

        return ToolInfo(
            name = "Kotlin Compiler",
            path = executable.absolutePath,
            version = versionStr,
            isValid = isValidVersion,
            message = message
        )
    }

    /**
     * Discovers Java runtime / JDK.
     * Validates that JDK version is >= 17 (recommended 21+).
     */
    fun findJava(): ToolInfo {
        val candidates = mutableListOf<String>()

        System.getenv("JAVA_HOME")?.let {
            candidates.add(File(it, if (isWindows) "bin/java.exe" else "bin/java").path)
        }

        if (isWindows) {
            candidates.add("C:\\Program Files\\Eclipse Adoptium\\jdk-21.0.12.101-hotspot\\bin\\java.exe")
            candidates.add("C:\\Program Files\\Java\\jdk-21\\bin\\java.exe")
        }

        val pathExec = if (isWindows) "java.exe" else "java"
        val fromPath = findExecutableInPath(pathExec)
        if (fromPath != null) {
            candidates.add(0, fromPath)
        }

        val executable = candidates.map { File(it) }.firstOrNull { it.exists() && it.isFile }
            ?: return ToolInfo(
                name = "Java Runtime",
                path = "NOT_FOUND",
                version = "unknown",
                isValid = false,
                message = "java executable was not found in PATH or JAVA_HOME."
            )

        val (exitCode, stdout, stderr) = runProcessQuiet(listOf(executable.absolutePath, "-version"))
        val output = "$stdout\n$stderr".trim()
        val versionRegex = Regex("""version\s+\"([0-9]+(?:\.[0-9]+)*)""")
        val match = versionRegex.find(output)

        val versionStr = match?.groupValues?.get(1) ?: "unknown"
        val majorVersion = versionStr.split('.').firstOrNull()?.toIntOrNull() ?: 0
        val isValid = majorVersion >= 17

        val message = if (isValid) {
            "JDK $versionStr meets minimum requirement (>= 17)."
        } else {
            "Detected Java $versionStr, but KUI requires JDK >= 17."
        }

        return ToolInfo(
            name = "Java Runtime",
            path = executable.absolutePath,
            version = versionStr,
            isValid = isValid,
            message = message
        )
    }

    /**
     * Discovers Android SDK platform-tools adb.
     */
    fun findAdb(): ToolInfo? {
        val candidates = mutableListOf<String>()

        System.getenv("ANDROID_HOME")?.let {
            candidates.add(File(it, if (isWindows) "platform-tools/adb.exe" else "platform-tools/adb").path)
        }
        System.getenv("ANDROID_SDK_ROOT")?.let {
            candidates.add(File(it, if (isWindows) "platform-tools/adb.exe" else "platform-tools/adb").path)
        }

        if (isWindows) {
            val localAppData = System.getenv("LOCALAPPDATA") ?: "C:\\Users\\DELL\\AppData\\Local"
            candidates.add(File(localAppData, "Android\\Sdk\\platform-tools\\adb.exe").path)
        }

        val pathExec = if (isWindows) "adb.exe" else "adb"
        val fromPath = findExecutableInPath(pathExec)
        if (fromPath != null) {
            candidates.add(0, fromPath)
        }

        val executable = candidates.map { File(it) }.firstOrNull { it.exists() && it.isFile } ?: return null

        val (exitCode, stdout, stderr) = runProcessQuiet(listOf(executable.absolutePath, "version"))
        val output = "$stdout\n$stderr".trim()
        val versionRegex = Regex("""version\s+([0-9]+\.[0-9]+\.[0-9]+)""")
        val match = versionRegex.find(output)
        val versionStr = match?.groupValues?.get(1) ?: "unknown"

        return ToolInfo(
            name = "Android ADB",
            path = executable.absolutePath,
            version = versionStr,
            isValid = true,
            message = "Android Debug Bridge ready."
        )
    }

    /**
     * Performs a complete environment check.
     */
    fun checkEnvironment(): EnvironmentReport {
        val kotlinInfo = findKotlinc()
        val javaInfo = findJava()
        val adbInfo = findAdb()

        val errors = mutableListOf<String>()
        if (!kotlinInfo.isValid) errors.add(kotlinInfo.message)
        if (!javaInfo.isValid) errors.add(javaInfo.message)

        val isReady = kotlinInfo.isValid && javaInfo.isValid

        return EnvironmentReport(
            kotlin = kotlinInfo,
            java = javaInfo,
            adb = adbInfo,
            isReady = isReady,
            errors = errors
        )
    }

    private fun isKotlinVersionSupported(versionStr: String): Boolean {
        val parts = versionStr.split('.').mapNotNull { it.toIntOrNull() }
        if (parts.isEmpty()) return false
        val major = parts[0]
        return major >= 2
    }

    private fun findExecutableInPath(name: String): String? {
        val pathEnv = System.getenv("PATH") ?: return null
        val separator = if (isWindows) ";" else ":"
        val directories = pathEnv.split(separator)
        for (dir in directories) {
            val file = File(dir.trim(), name)
            if (file.exists() && file.isFile) {
                return file.absolutePath
            }
        }
        return null
    }

    private fun runProcessQuiet(cmd: List<String>): Triple<Int, String, String> {
        return try {
            val process = ProcessBuilder(cmd)
                .redirectErrorStream(false)
                .start()
            val stdout = process.inputStream.bufferedReader().readText()
            val stderr = process.errorStream.bufferedReader().readText()
            val code = process.waitFor()
            Triple(code, stdout, stderr)
        } catch (e: Exception) {
            Triple(-1, "", e.message ?: "Process execution failed")
        }
    }
}
