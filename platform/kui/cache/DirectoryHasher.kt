package kui.cache

import java.io.File
import java.security.MessageDigest

/**
 * Ordering-independent, stable directory tree hashing (Phase 013).
 */
object DirectoryHasher {
    val DEFAULT_EXCLUDES = setOf(".kui", ".git", ".idea", ".kotlin")

    data class FileHashEntry(val relativePath: String, val sha256: String)

    fun hashDirectory(
        directory: File,
        excludes: Set<String> = DEFAULT_EXCLUDES
    ): String {
        if (!directory.isDirectory) {
            throw IllegalArgumentException("Target is not a directory: ${directory.absolutePath}")
        }

        val entries = mutableListOf<FileHashEntry>()
        val baseCanonical = directory.canonicalFile

        fun collect(dir: File) {
            val children = dir.listFiles() ?: return
            for (child in children) {
                if (excludes.contains(child.name)) continue
                if (child.isDirectory) {
                    collect(child)
                } else if (child.isFile) {
                    val relativePath = child.canonicalFile.toRelativeString(baseCanonical).replace('\\', '/')
                    val fileHash = Hasher.sha256(child)
                    entries.add(FileHashEntry(relativePath, fileHash))
                }
            }
        }

        collect(baseCanonical)

        // Sort entries deterministically by relative path
        entries.sortBy { it.relativePath }

        val md = MessageDigest.getInstance("SHA-256")
        for (entry in entries) {
            md.update(entry.relativePath.toByteArray(Charsets.UTF_8))
            md.update(0.toByte())
            md.update(entry.sha256.toByteArray(Charsets.UTF_8))
            md.update(0.toByte())
        }

        return md.digest().joinToString("") { "%02x".format(it) }
    }
}
