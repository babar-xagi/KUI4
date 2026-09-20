package kui.config

import java.io.File

data class ProjectIdentity(
    val name: String,
    val version: String,
    val applicationId: String? = null,
    val description: String? = null
)

data class AndroidConfig(
    val minSdk: Int = 24,
    val targetSdk: Int = 36
)

data class UiConfig(
    val theme: String = "system"
)

data class KuiConfig(
    val project: ProjectIdentity,
    val android: AndroidConfig,
    val ui: UiConfig,
    val projectRoot: File
)

data class DiagnosticIssue(
    val field: String,
    val message: String,
    val lineNumber: Int? = null
)

sealed class ConfigValidationResult {
    data class Success(val config: KuiConfig) : ConfigValidationResult()
    data class Failure(val issues: List<DiagnosticIssue>) : ConfigValidationResult() {
        fun formatErrorMessage(): String = buildString {
            appendLine("Configuration Error in kui.toml:")
            for (issue in issues) {
                val loc = if (issue.lineNumber != null) " (line ${issue.lineNumber})" else ""
                appendLine("  - [${issue.field}]$loc: ${issue.message}")
            }
        }
    }
}

object ConfigDiagnostics {
    fun validateAndLoad(projectRoot: File): ConfigValidationResult {
        val configFile = File(projectRoot, "kui.toml")
        if (!configFile.isFile) {
            return ConfigValidationResult.Failure(
                listOf(DiagnosticIssue("file", "kui.toml not found at ${configFile.absolutePath}"))
            )
        }

        val doc = TomlReader.parse(configFile)
        val issues = mutableListOf<DiagnosticIssue>()

        // 1. Validate [project] name
        val nameEntry = doc.findEntry("project", "name")
        val name = nameEntry?.value
        if (name.isNullOrBlank()) {
            issues.add(DiagnosticIssue("project.name", "Missing required field 'name'", nameEntry?.lineNumber))
        } else if (!Regex("^[a-zA-Z0-9_-]+$").matches(name)) {
            issues.add(DiagnosticIssue("project.name", "Invalid project name '$name'. Must contain only alphanumeric characters, dashes, and underscores.", nameEntry.lineNumber))
        }

        // 2. Validate [project] version
        val versionEntry = doc.findEntry("project", "version")
        val version = versionEntry?.value
        if (version.isNullOrBlank()) {
            issues.add(DiagnosticIssue("project.version", "Missing required field 'version'", versionEntry?.lineNumber))
        }

        // 3. Validate [android] SDKs
        val minSdkEntry = doc.findEntry("android", "min_sdk")
        val targetSdkEntry = doc.findEntry("android", "target_sdk")

        val minSdk = minSdkEntry?.value?.toIntOrNull() ?: 24
        val targetSdk = targetSdkEntry?.value?.toIntOrNull() ?: 36

        if (minSdk < 14) {
            issues.add(DiagnosticIssue("android.min_sdk", "Minimum SDK must be at least 14 (got $minSdk)", minSdkEntry?.lineNumber))
        }
        if (targetSdk < minSdk) {
            issues.add(DiagnosticIssue("android.target_sdk", "Target SDK ($targetSdk) cannot be smaller than min_sdk ($minSdk)", targetSdkEntry?.lineNumber))
        }

        if (issues.isNotEmpty()) {
            return ConfigValidationResult.Failure(issues)
        }

        val identity = ProjectIdentity(
            name = name!!,
            version = version!!,
            applicationId = doc.getString("project", "application_id"),
            description = doc.getString("project", "description")
        )

        val android = AndroidConfig(minSdk = minSdk, targetSdk = targetSdk)
        val ui = UiConfig(theme = doc.getString("ui", "theme") ?: "system")

        return ConfigValidationResult.Success(KuiConfig(identity, android, ui, projectRoot))
    }
}
