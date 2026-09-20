package kui.cli

fun main(args: Array<String>) {
    when (val invocation = CommandParser.parse(args)) {
        is ParsedInvocation.Empty -> {
            Help.printHelp()
        }
        is ParsedInvocation.ShowVersion -> {
            println(KuiVersion.DISPLAY_NAME)
        }
        is ParsedInvocation.ShowHelp -> {
            Help.printHelp(invocation.targetCommand)
        }
        is ParsedInvocation.UnknownCommand -> {
            System.err.println("kui: '${invocation.rawName}' is not a recognized command.")
            if (invocation.suggestion != null) {
                System.err.println("Did you mean: '${invocation.suggestion.commandName}'?")
            }
            System.err.println("Run 'kui --help' for available commands.")
            System.exit(1)
        }
        is ParsedInvocation.ExecuteCommand -> {
            executeCommand(invocation.command, invocation.args, invocation.flags)
        }
    }
}

private fun executeCommand(command: Command, args: List<String>, flags: Map<String, String>) {
    when (command) {
        Command.VERSION -> println(KuiVersion.DISPLAY_NAME)
        Command.HELP -> {
            val target = args.firstOrNull()?.let { Command.fromString(it) }
            Help.printHelp(target)
        }
        Command.INFO -> {
            val exitCode = kui.project.InfoCommand.execute()
            if (exitCode != 0) System.exit(exitCode)
        }
        Command.BENCH -> {
            val root = kui.project.ProjectFinder.findProjectRoot() ?: java.io.File(".")
            println("Running KUI Foundation Benchmark...")
            val stats = kui.build.FoundationBenchmark.run(root)
            println("Benchmark Completed (${stats.iterations} iterations):")
            println("  Min: %.3f ms".format(java.util.Locale.US, stats.minMs))
            println("  Avg: %.3f ms".format(java.util.Locale.US, stats.avgMs))
            println("  P95: %.3f ms".format(java.util.Locale.US, stats.p95Ms))
            println("  Max: %.3f ms".format(java.util.Locale.US, stats.maxMs))
            println("Report saved to .kui/build/reports/benchmark-foundation.json")
        }
        else -> {
            // Placeholder dispatcher for commands being built in subsequent phases
            println("kui: command '${command.commandName}' acknowledged.")
            if (args.isNotEmpty()) println("  Arguments: ${args.joinToString(", ")}")
            if (flags.isNotEmpty()) println("  Flags: $flags")
        }
    }
}
