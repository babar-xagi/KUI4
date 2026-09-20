package kui.compiler

import kui.cache.Hasher
import kui.cli.KuiVersion
import java.io.File

/**
 * Generates an immutable, comprehensive cache key for compilation tasks (Phase 047).
 */
data class CompilerCacheKey(
    val sourceHash: String,
    val classpathHash: String,
    val compilerVersion: String,
    val jvmTarget: String,
    val configHash: String,
    val extraFlags: List<String> = emptyList()
) {
    /**
     * The resulting 64-character SHA-256 fingerprint representing the exact compile input state.
     */
    val key: String by lazy {
        val payload = buildString {
            appendLine("sources=$sourceHash")
            appendLine("classpath=$classpathHash")
            appendLine("compiler=$compilerVersion")
            appendLine("jvm_target=$jvmTarget")
            appendLine("kui_version=${KuiVersion.VERSION_STRING}")
            appendLine("config=$configHash")
            appendLine("flags=${extraFlags.joinToString(",")}")
        }
        Hasher.hashString(payload)
    }

    companion object {
        fun compute(
            sources: List<File>,
            sourceBaseDir: File,
            classpath: ClasspathModel,
            compilerVersion: String,
            jvmTarget: String = "21",
            configFile: File? = null,
            extraFlags: List<String> = emptyList()
        ): CompilerCacheKey {
            val sourceHash = SourceHasher.hashSources(sources, sourceBaseDir)
            val classpathHash = classpath.computeFingerprint()
            val configHash = if (configFile != null && configFile.exists()) {
                Hasher.hashFile(configFile)
            } else {
                "DEFAULT_CONFIG"
            }

            return CompilerCacheKey(
                sourceHash = sourceHash,
                classpathHash = classpathHash,
                compilerVersion = compilerVersion,
                jvmTarget = jvmTarget,
                configHash = configHash,
                extraFlags = extraFlags
            )
        }
    }
}
