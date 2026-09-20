package kui.project

import java.io.File

/**
 * Handles 'kui new <name>' command (Milestone B).
 */
object NewCommand {
    fun execute(args: List<String>, flags: Map<String, String>, workingDir: File = File(".")): Int {
        if (args.isEmpty()) {
            System.err.println("kui new: Missing project name.")
            System.err.println("Usage: kui new <name> [--minimal] [--app-id=<id>]")
            return 1
        }

        val name = args[0]
        val targetDir = File(workingDir, name)

        val options = NewProjectOptions(
            name = name,
            targetDir = targetDir,
            applicationId = flags["app-id"] ?: flags["application-id"],
            version = flags["version"] ?: "0.1.0",
            versionCode = flags["version-code"]?.toIntOrNull() ?: 1,
            minSdk = flags["min-sdk"]?.toIntOrNull() ?: 24,
            targetSdk = flags["target-sdk"]?.toIntOrNull() ?: 36,
            minimal = flags.containsKey("minimal") || flags.containsKey("m")
        )

        val result = ProjectGenerator.generate(options)
        if (!result.success) {
            System.err.println("kui new error:")
            for (err in result.errors) {
                System.err.println("  - $err")
            }
            return 1
        }

        println("Created project '${options.name}' at: ${result.projectDir.canonicalPath}")
        println()
        println("Next steps:")
        println("  cd ${options.name}")
        println("  kui run")
        return 0
    }
}
