package kui.cli

/**
 * Standardized help message formatter for KUI CLI.
 */
object Help {
    val GENERAL_HELP: String = buildString {
        appendLine(KuiVersion.DISPLAY_NAME + " - Pure Kotlin Application Platform Toolchain")
        appendLine()
        appendLine("Usage:")
        appendLine("  kui [command] [options]")
        appendLine()
        appendLine("Commands:")
        for (cmd in Command.values()) {
            val displayName = if (cmd == Command.NEW) "new <name>" else cmd.commandName
            val paddedName = displayName.padEnd(14)
            appendLine("  $paddedName ${cmd.description}")
        }
        appendLine()
        appendLine("Options:")
        appendLine("  -v, --version  print version information")
        append("  -h, --help     print this help message")
    }

    val TEXT: String get() = GENERAL_HELP

    fun commandHelp(cmd: Command): String = buildString {
        appendLine("${cmd.commandName} - ${cmd.description}")
        appendLine()
        appendLine("Usage:")
        append("  ${cmd.usage}")
    }

    fun printHelp(target: Command? = null) {
        if (target == null) {
            println(GENERAL_HELP)
        } else {
            println(commandHelp(target))
        }
    }
}
