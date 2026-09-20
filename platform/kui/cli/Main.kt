package kui.cli

fun main(args: Array<String>) {
    // Handle version flag
    if (args.contains("--version") || args.contains("-v") || (args.isNotEmpty() && args[0] == "version")) {
        println(KuiVersion.DISPLAY_NAME)
        return
    }

    // Default minimal CLI entry (will be expanded in Phase 003)
    if (args.isEmpty()) {
        println(KuiVersion.DISPLAY_NAME)
        println("Usage: kui [command] [options]")
        println("Try 'kui --version' for version information.")
        return
    }

    println("Unknown command: ${args.joinToString(" ")}")
}
