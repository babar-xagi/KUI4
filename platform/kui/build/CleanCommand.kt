package kui.build

import kui.cache.CacheDirectories
import kui.project.ProjectFinder
import java.io.File

/**
 * Handles `kui clean` command (Phase 050).
 */
object CleanCommand {

    fun execute(
        args: List<String> = emptyList(),
        flags: Map<String, String> = emptyMap(),
        projectRootOverride: File? = null
    ): Int {
        val root = projectRootOverride ?: ProjectFinder.findProjectRoot()
        if (root == null) {
            System.err.println("kui: No 'kui.toml' found in current directory or any parent directories.")
            return 1
        }

        val buildDirs = BuildDirectories(root)
        val cacheDirs = CacheDirectories(root)

        println("Cleaning project '${root.name}'...")
        val buildCleaned = buildDirs.clean()
        val cacheCleaned = cacheDirs.cacheDir.deleteRecursively()

        if (buildCleaned && (cacheCleaned || !cacheDirs.cacheDir.exists())) {
            println("[KUI] Clean complete: Removed build and cache artifacts.")
            return 0
        } else {
            System.err.println("[KUI] Warning: Failed to cleanly remove some build artifacts.")
            return 1
        }
    }
}
