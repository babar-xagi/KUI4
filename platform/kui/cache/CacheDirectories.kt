package kui.cache

import java.io.File

/**
 * Deterministic cache directory layout for KUI (Phase 015).
 */
class CacheDirectories(val projectRoot: File) {
    val kuiDir: File = File(projectRoot, ".kui")
    val cacheDir: File = File(kuiDir, "cache")
    val tasksCacheDir: File = File(cacheDir, "tasks")
    val artifactsCacheDir: File = File(cacheDir, "artifacts")

    fun ensureCreated() {
        tasksCacheDir.mkdirs()
        artifactsCacheDir.mkdirs()
    }

    fun getTaskCacheFile(fingerprint: String): File {
        ensureCreated()
        return File(tasksCacheDir, "$fingerprint.json")
    }

    fun getArtifactCacheFile(key: String): File {
        ensureCreated()
        return File(artifactsCacheDir, key)
    }

    fun clean(): Boolean {
        return if (cacheDir.exists()) {
            cacheDir.deleteRecursively()
        } else {
            true
        }
    }
}
