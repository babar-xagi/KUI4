package kui.compiler

import kui.cache.Hasher
import java.io.File

/**
 * An entry on the compilation classpath.
 */
data class ClasspathEntry(
    val file: File,
    val description: String = ""
) {
    val exists: Boolean get() = file.exists()
}

/**
 * Deterministic model of the compilation classpath (Phase 044).
 */
class ClasspathModel {

    private val entries = mutableListOf<ClasspathEntry>()

    fun add(file: File, description: String = ""): ClasspathModel {
        if (entries.none { it.file.canonicalPath == file.canonicalPath }) {
            entries.add(ClasspathEntry(file, description))
        }
        return this
    }

    fun getEntries(): List<ClasspathEntry> = entries.toList()

    fun isEmpty(): Boolean = entries.isEmpty()

    /**
     * Platform-specific classpath string separated by File.pathSeparator.
     */
    fun asClasspathString(): String {
        return entries.map { it.file.canonicalPath }.joinToString(File.pathSeparator)
    }

    /**
     * Computes a deterministic SHA-256 fingerprint of the classpath entries.
     * Incorporates canonical path, file size, and last modified timestamp (or file hash).
     */
    fun computeFingerprint(): String {
        val lines = entries.map { entry ->
            val f = entry.file
            if (f.exists()) {
                "${f.name}:${f.length()}:${Hasher.hashFile(f)}"
            } else {
                "${f.name}:MISSING"
            }
        }
        return Hasher.hashString(lines.joinToString("\n"))
    }
}
