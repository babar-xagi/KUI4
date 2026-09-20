package kui.project

import java.io.File

/**
 * Finds KUI project root directory by searching upwards for kui.toml.
 */
object ProjectFinder {
    const val CONFIG_FILENAME = "kui.toml"

    fun findProjectRoot(startDir: File = File(".")): File? {
        var current: File? = startDir.canonicalFile
        var levels = 0
        while (current != null && levels < 25) {
            val candidate = File(current, CONFIG_FILENAME)
            if (candidate.isFile) {
                return current
            }
            current = current.parentFile
            levels++
        }
        return null
    }

    fun requireProjectRoot(startDir: File = File(".")): File {
        return findProjectRoot(startDir)
            ?: throw IllegalStateException("Could not locate '$CONFIG_FILENAME' in ${startDir.canonicalPath} or any parent directory.")
    }
}
