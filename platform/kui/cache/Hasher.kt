package kui.cache

import java.io.File
import java.io.InputStream
import java.security.MessageDigest

/**
 * High-performance SHA-256 content hashing utilities (Phase 012).
 */
object Hasher {
    fun sha256(bytes: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun sha256(string: String): String = sha256(string.toByteArray(Charsets.UTF_8))

    fun hashString(string: String): String = sha256(string)

    fun sha256(file: File): String {
        if (!file.isFile) throw IllegalArgumentException("Target is not a file: ${file.absolutePath}")
        val md = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        file.inputStream().use { input ->
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                md.update(buffer, 0, read)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    fun hashFile(file: File): String = sha256(file)
}
