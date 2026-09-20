package kui.cli

fun main(args: Array<String>) {
    // Running without args shows help (Phase 003)
    if (args.isEmpty()) {
        Help.printHelp()
        return
    }

    val first = args[0]

    // Version flag
    if (first == "--version" || first == "-v" || first == "version") {
        println(KuiVersion.DISPLAY_NAME)
        return
    }

    // Help flag or command
    if (first == "--help" || first == "-h" || first == "help") {
        Help.printHelp()
        return
    }

    println("Unknown command: ${args.joinToString(" ")}")
    println("Run 'kui --help' for available commands.")
}
