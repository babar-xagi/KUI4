package kui.build

import java.io.File

/**
 * Standard build directory layout for KUI (Phase 014).
 */
class BuildDirectories(val projectRoot: File) {
    val kuiDir: File = File(projectRoot, ".kui")
    val buildDir: File = File(kuiDir, "build")
    val classesDir: File = File(buildDir, "classes")
    val dexDir: File = File(buildDir, "dex")
    val apkDir: File = File(buildDir, "apk")
    val reportsDir: File = File(buildDir, "reports")

    fun ensureCreated() {
        buildDir.mkdirs()
        classesDir.mkdirs()
        dexDir.mkdirs()
        apkDir.mkdirs()
        reportsDir.mkdirs()
    }

    fun clean(): Boolean {
        return if (buildDir.exists()) {
            buildDir.deleteRecursively()
        } else {
            true
        }
    }
}
