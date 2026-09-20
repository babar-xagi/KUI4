package kui.cli

/**
 * Result of parsing command line arguments.
 */
sealed class ParsedInvocation {
    object Empty : ParsedInvocation()
    object ShowVersion : ParsedInvocation()
    data class ShowHelp(val targetCommand: Command? = null) : ParsedInvocation()
    data class ExecuteCommand(
        val command: Command,
        val args: List<String>,
        val flags: Map<String, String>
    ) : ParsedInvocation()
    data class UnknownCommand(
        val rawName: String,
        val suggestion: Command?
    ) : ParsedInvocation()
}

/**
 * Robust, zero-dependency argument parser for KUI CLI.
 */
object CommandParser {
    fun parse(args: Array<String>): ParsedInvocation {
        if (args.isEmpty()) return ParsedInvocation.Empty

        val first = args[0].trim()

        if (first == "--version" || first == "-v") return ParsedInvocation.ShowVersion
        if (first == "--help" || first == "-h") {
            val sub = args.getOrNull(1)?.let { Command.fromString(it) }
            return ParsedInvocation.ShowHelp(sub)
        }

        val cmd = Command.fromString(first)
        if (cmd != null) {
            if (cmd == Command.HELP) {
                val sub = args.getOrNull(1)?.let { Command.fromString(it) }
                return ParsedInvocation.ShowHelp(sub)
            }
            if (cmd == Command.VERSION) {
                return ParsedInvocation.ShowVersion
            }

            val positional = mutableListOf<String>()
            val flags = mutableMapOf<String, String>()
            var i = 1
            while (i < args.size) {
                val token = args[i]
                if (token.startsWith("--")) {
                    val eqIndex = token.indexOf('=')
                    if (eqIndex != -1) {
                        val key = token.substring(2, eqIndex)
                        val value = token.substring(eqIndex + 1)
                        flags[key] = value
                    } else if (i + 1 < args.size && !args[i + 1].startsWith("-")) {
                        flags[token.substring(2)] = args[i + 1]
                        i++
                    } else {
                        flags[token.substring(2)] = "true"
                    }
                } else if (token.startsWith("-") && token.length > 1) {
                    flags[token.substring(1)] = "true"
                } else {
                    positional.add(token)
                }
                i++
            }
            return ParsedInvocation.ExecuteCommand(cmd, positional, flags)
        }

        val suggestion = Command.findClosest(first)
        return ParsedInvocation.UnknownCommand(first, suggestion)
    }
}
