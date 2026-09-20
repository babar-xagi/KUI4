package kui.project

import java.io.File

/**
 * Configuration options for generating a new UI4 project (Milestone B).
 */
data class NewProjectOptions(
    val name: String,
    val targetDir: File,
    val applicationId: String? = null,
    val version: String = "0.1.0",
    val versionCode: Int = 1,
    val minSdk: Int = 24,
    val targetSdk: Int = 36,
    val minimal: Boolean = false
) {
    /**
     * Resolves canonical Application ID (Phase 028 & Phase 029).
     */
    fun resolveApplicationId(): String {
        if (!applicationId.isNullOrBlank()) {
            return applicationId.trim()
        }
        val cleanName = name.lowercase().replace('-', '_').replace(Regex("[^a-z0-9_]"), "")
        return "com.example.$cleanName"
    }
}
