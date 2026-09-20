package kui.doctor

import kui.cli.KuiVersion
import kui.compiler.CompilerDiscovery

/**
 * Executes the `kui doctor` environment diagnostic command (Phases 036, 037, 038).
 */
object DoctorCommand {

    fun execute(flags: Map<String, String> = emptyMap()): Int {
        println("==================================================")
        println(" 🩺  KUI Environment Doctor (${KuiVersion.DISPLAY_NAME})")
        println("==================================================")
        println("Scanning toolchain and dependencies...")
        println()

        val report = CompilerDiscovery.checkEnvironment()

        // Kotlin
        if (report.kotlin.isValid) {
            println("  [PASS] Kotlin Compiler: ${report.kotlin.version}")
            println("         Path: ${report.kotlin.path}")
        } else {
            println("  [FAIL] Kotlin Compiler: ${report.kotlin.message}")
            if (report.kotlin.path != "NOT_FOUND") {
                println("         Path: ${report.kotlin.path}")
            }
        }
        println()

        // Java
        if (report.java.isValid) {
            println("  [PASS] Java Runtime: ${report.java.version}")
            println("         Path: ${report.java.path}")
        } else {
            println("  [FAIL] Java Runtime: ${report.java.message}")
            if (report.java.path != "NOT_FOUND") {
                println("         Path: ${report.java.path}")
            }
        }
        println()

        // ADB
        if (report.adb != null && report.adb.isValid) {
            println("  [PASS] Android ADB: ${report.adb.version}")
            println("         Path: ${report.adb.path}")
        } else {
            println("  [WARN] Android ADB: Not detected (optional until on-device deployment).")
        }
        println()

        println("--------------------------------------------------")
        return if (report.isReady) {
            println("STATUS: HEALTHY - Environment is ready for KUI builds.")
            0
        } else {
            System.err.println("STATUS: UNHEALTHY - Please address the issues listed above.")
            1
        }
    }
}
