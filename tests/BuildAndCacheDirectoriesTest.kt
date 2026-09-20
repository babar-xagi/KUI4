package tests

import kui.build.BuildDirectories
import kui.cache.CacheDirectories
import java.io.File

/**
 * Verification test for Phase 014 (Build Dirs) and Phase 015 (Cache Dirs).
 */
fun main() {
    println("==================================================")
    println(" Phases 014 & 015: Build & Cache Directories")
    println("==================================================")

    var passed = 0
    var failed = 0

    fun check(name: String, condition: Boolean, details: String = "") {
        if (condition) {
            println("  [PASS] $name")
            passed++
        } else {
            println("  [FAIL] $name: $details")
            failed++
        }
    }

    val tempProject = File(System.getProperty("java.io.tmpdir"), "kui_dirs_test_" + System.currentTimeMillis())
    tempProject.mkdirs()

    try {
        // 1. Phase 014: Build Directories
        val buildDirs = BuildDirectories(tempProject)
        check("Build directory path is inside .kui/build", buildDirs.buildDir.path.contains(".kui"))

        buildDirs.ensureCreated()
        check("buildDir exists after ensureCreated", buildDirs.buildDir.isDirectory)
        check("classesDir exists after ensureCreated", buildDirs.classesDir.isDirectory)
        check("dexDir exists after ensureCreated", buildDirs.dexDir.isDirectory)
        check("reportsDir exists after ensureCreated", buildDirs.reportsDir.isDirectory)

        File(buildDirs.buildDir, "dummy.txt").writeText("dummy artifact")
        check("Dummy artifact created", File(buildDirs.buildDir, "dummy.txt").isFile)

        buildDirs.clean()
        check("clean() removes build output", !buildDirs.buildDir.exists())

        // 2. Phase 015: Cache Directories
        val cacheDirs = CacheDirectories(tempProject)
        check("Cache directory path is inside .kui/cache", cacheDirs.cacheDir.path.contains(".kui"))

        cacheDirs.ensureCreated()
        check("tasksCacheDir exists after ensureCreated", cacheDirs.tasksCacheDir.isDirectory)
        check("artifactsCacheDir exists after ensureCreated", cacheDirs.artifactsCacheDir.isDirectory)

        val taskCacheFile = cacheDirs.getTaskCacheFile("abc123sha")
        check("Task cache file path matches deterministic fingerprint", taskCacheFile.name == "abc123sha.json")

        cacheDirs.clean()
        check("clean() removes cache directory", !cacheDirs.cacheDir.exists())
    } finally {
        tempProject.deleteRecursively()
    }

    println("--------------------------------------------------")
    println("Summary: $passed PASSED, $failed FAILED")
    println("==================================================")

    if (failed > 0) System.exit(1) else println("RESULT: PASS")
}
