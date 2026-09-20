package kui.project

import kui.config.ConfigDiagnostics
import kui.config.ConfigValidationResult
import java.io.File

/**
 * Handles 'kui info' command (Phase 009).
 */
object InfoCommand {
    fun execute(startDir: File = File(".")): Int {
        val root = ProjectFinder.findProjectRoot(startDir)
        if (root == null) {
            System.err.println("kui: Not in a KUI project (no 'kui.toml' found).")
            return 1
        }

        when (val result = ConfigDiagnostics.validateAndLoad(root)) {
            is ConfigValidationResult.Failure -> {
                System.err.println(result.formatErrorMessage())
                return 1
            }
            is ConfigValidationResult.Success -> {
                val cfg = result.config
                println("KUI Project Information:")
                println("  Name:            ${cfg.project.name}")
                println("  Version:         ${cfg.project.version}")
                if (cfg.project.applicationId != null) {
                    println("  Application ID:  ${cfg.project.applicationId}")
                }
                if (cfg.project.description != null) {
                    println("  Description:     ${cfg.project.description}")
                }
                println("  Min SDK:         ${cfg.android.minSdk}")
                println("  Target SDK:      ${cfg.android.targetSdk}")
                println("  UI Theme:        ${cfg.ui.theme}")
                println("  Project Root:    ${cfg.projectRoot.canonicalPath}")
                return 0
            }
        }
    }
}
