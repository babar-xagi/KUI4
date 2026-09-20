package kui.compiler

import java.io.File

/**
 * Discovers Kotlin source files deterministically for compilation (Phases 042, 043).
 */
object SourceDiscovery {

    /**
     * Recursively finds all `.kt` files inside the given directory and returns them
     * sorted deterministically by relative path.
     */
    fun findSources(baseDir: File): List<File> {
        if (!baseDir.exists() || !baseDir.isDirectory) {
            return emptyList()
        }

        val results = mutableListOf<File>()
        collectKotlinFiles(baseDir, results)

        // Deterministic sort by normalized relative path
        return results.sortedBy { file ->
            baseDir.toPath().relativize(file.toPath()).toString().replace('\\', '/')
        }
    }

    private fun collectKotlinFiles(dir: File, outList: MutableList<File>) {
        val entries = dir.listFiles() ?: return
        for (entry in entries) {
            if (entry.isDirectory) {
                // Ignore hidden directories or build directories if nested
                if (!entry.name.startsWith(".")) {
                    collectKotlinFiles(entry, outList)
                }
            } else if (entry.isFile && entry.name.endsWith(".kt", ignoreCase = true)) {
                outList.add(entry)
            }
        }
    }
}
