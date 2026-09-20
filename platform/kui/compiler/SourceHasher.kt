package kui.compiler

import kui.cache.Hasher
import java.io.File

/**
 * Computes deterministic cryptographic hashes for source trees (Phase 046).
 */
object SourceHasher {

    /**
     * Hashes all provided source files deterministically.
     * Combines each file's relative path and content bytes.
     */
    fun hashSources(sources: List<File>, baseDir: File): String {
        if (sources.isEmpty()) {
            return Hasher.hashString("EMPTY_SOURCES")
        }

        // Sort files by relative path
        val sorted = sources.sortedBy { baseDir.toPath().relativize(it.toPath()).toString().replace('\\', '/') }
        val sb = StringBuilder()

        for (file in sorted) {
            val relPath = baseDir.toPath().relativize(file.toPath()).toString().replace('\\', '/')
            val contentHash = Hasher.hashFile(file)
            sb.append(relPath).append(":").append(contentHash).append("\n")
        }

        return Hasher.hashString(sb.toString())
    }
}
