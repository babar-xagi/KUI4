package kui.project

import kui.cli.KuiVersion
import kui.config.ConfigDiagnostics
import kui.config.ConfigValidationResult
import kui.config.TomlReader
import java.io.File

data class ProjectGenerationResult(
    val success: Boolean,
    val projectDir: File,
    val createdFiles: List<File>,
    val errors: List<String> = emptyList()
)

/**
 * Automated project generator engine for KUI (Milestone B, Phases 021-035).
 */
object ProjectGenerator {

    private val PROJECT_NAME_REGEX = Regex("^[a-zA-Z][a-zA-Z0-9_-]*$")
    private val APPLICATION_ID_REGEX = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$")

    fun validateProjectName(name: String): String? {
        if (name.isBlank()) return "Project name cannot be empty."
        if (!PROJECT_NAME_REGEX.matches(name)) {
            return "Invalid project name '$name'. Must start with a letter and contain only alphanumeric characters, dashes, or underscores."
        }
        return null
    }

    fun validateApplicationId(appId: String): String? {
        if (appId.isBlank()) return "Application ID cannot be empty."
        if (!APPLICATION_ID_REGEX.matches(appId)) {
            return "Invalid application ID '$appId'. Must be in package format (e.g. 'com.example.myapp')."
        }
        return null
    }

    fun generate(options: NewProjectOptions): ProjectGenerationResult {
        // 1. Validate project name (Phase 021)
        val nameError = validateProjectName(options.name)
        if (nameError != null) {
            return ProjectGenerationResult(false, options.targetDir, emptyList(), listOf(nameError))
        }

        // 2. Validate application ID (Phase 028 & Phase 029)
        val resolvedAppId = options.resolveApplicationId()
        val appIdError = validateApplicationId(resolvedAppId)
        if (appIdError != null) {
            return ProjectGenerationResult(false, options.targetDir, emptyList(), listOf(appIdError))
        }

        // 3. Collision check: Root must not exist with files (Phase 022)
        val root = options.targetDir
        if (root.exists()) {
            val contents = root.listFiles()
            if (contents != null && contents.isNotEmpty()) {
                return ProjectGenerationResult(
                    false,
                    root,
                    emptyList(),
                    listOf("Directory '${root.canonicalPath}' already exists and is not empty.")
                )
            }
        } else {
            root.mkdirs()
        }

        val created = mutableListOf<File>()

        // 4. Generate kui.toml (Phase 023, 030, 031, 032)
        val kuiToml = File(root, "kui.toml")
        val tomlContent = buildString {
            appendLine("[project]")
            appendLine("name = \"${options.name}\"")
            appendLine("version = \"${options.version}\"")
            appendLine("version_code = ${options.versionCode}")
            appendLine("application_id = \"$resolvedAppId\"")
            appendLine("generator_version = \"${KuiVersion.VERSION_STRING}\"")
            appendLine()
            appendLine("[android]")
            appendLine("min_sdk = ${options.minSdk}")
            appendLine("target_sdk = ${options.targetSdk}")
            appendLine()
            appendLine("[ui]")
            appendLine("theme = \"system\"")
        }
        kuiToml.writeText(tomlContent)
        created.add(kuiToml)

        // Verify TOML parses back cleanly (Phase 023)
        val parsedDoc = TomlReader.parse(kuiToml)
        if (parsedDoc.getString("project", "name") != options.name) {
            return ProjectGenerationResult(false, root, created, listOf("Generated kui.toml failed validation."))
        }

        // 5. Generate src/main.kt (Phase 024)
        val srcDir = File(root, "src").apply { mkdirs() }
        val mainKt = File(srcDir, "main.kt")
        val mainContent = buildString {
            appendLine("import ui4.*")
            appendLine()
            appendLine("fun main() = app {")
            appendLine("    screen {")
            appendLine("        center {")
            appendLine("            text(\"Hello, ${options.name}! 👋\")")
            appendLine("        }")
            appendLine("    }")
            appendLine("}")
        }
        mainKt.writeText(mainContent)
        created.add(mainKt)

        // 6. Generate README.md (Phase 027, 035)
        val readme = File(root, "README.md")
        val readmeContent = buildString {
            appendLine("# ${options.name}")
            appendLine()
            appendLine("Created with KUI (version ${KuiVersion.VERSION_STRING}).")
            appendLine()
            appendLine("## Quick Start")
            appendLine()
            appendLine("```powershell")
            appendLine("kui run")
            appendLine("```")
            appendLine()
            appendLine("## Project Layout")
            appendLine("- `kui.toml`: Project metadata and configuration")
            appendLine("- `src/main.kt`: UI4 declarative application entry point")
            if (!options.minimal) {
                appendLine("- `assets/`: App images and font resources")
                appendLine("- `tests/`: Automated unit and UI tests")
            }
        }
        readme.writeText(readmeContent)
        created.add(readme)

        // 7. Non-minimal additions (Assets, Tests, .gitignore) (Phase 025, 026, 033)
        if (!options.minimal) {
            val assetsDir = File(root, "assets").apply { mkdirs() }
            val imagesDir = File(assetsDir, "images").apply { mkdirs() }
            val fontsDir = File(assetsDir, "fonts").apply { mkdirs() }
            File(imagesDir, ".gitkeep").createNewFile()
            File(fontsDir, ".gitkeep").createNewFile()
            created.add(assetsDir)

            val testsDir = File(root, "tests").apply { mkdirs() }
            val appTestKt = File(testsDir, "AppTest.kt")
            val appTestContent = buildString {
                appendLine("package ${resolvedAppId.substringAfterLast('.')}")
                appendLine()
                appendLine("fun main() {")
                appendLine("    println(\"Running ${options.name} tests: PASS\")")
                appendLine("}")
            }
            appTestKt.writeText(appTestContent)
            created.add(appTestKt)

            val gitignore = File(root, ".gitignore")
            gitignore.writeText(buildString {
                appendLine(".kui/")
                appendLine("*.class")
                appendLine("*.jar")
                appendLine("*.log")
            })
            created.add(gitignore)
        }

        // 8. Final Config Verification (Phase 034)
        when (val validation = ConfigDiagnostics.validateAndLoad(root)) {
            is ConfigValidationResult.Failure -> {
                return ProjectGenerationResult(false, root, created, listOf(validation.formatErrorMessage()))
            }
            is ConfigValidationResult.Success -> {
                return ProjectGenerationResult(true, root, created)
            }
        }
    }
}
