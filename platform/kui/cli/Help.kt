package kui.cli

/**
 * Standardized help message formatter for KUI CLI.
 */
object Help {
    val TEXT: String = buildString {
        appendLine(KuiVersion.DISPLAY_NAME + " - Pure Kotlin Application Platform Toolchain")
        appendLine()
        appendLine("Usage:")
        appendLine("  kui [command] [options]")
        appendLine()
        appendLine("Commands:")
        appendLine("  new <name>     create project")
        appendLine("  doctor         verify environment")
        appendLine("  info           show project information")
        appendLine("  build          compile/package")
        appendLine("  run            build + install + launch")
        appendLine("  install        install current APK")
        appendLine("  launch         launch installed app")
        appendLine("  test           run tests")
        appendLine("  clean          remove generated build output")
        appendLine("  bench          benchmarks")
        appendLine("  profile        profiling report")
        appendLine("  ui-tree        print runtime UI tree later")
        appendLine("  help           show help for commands")
        appendLine()
        appendLine("Options:")
        appendLine("  -v, --version  print version information")
        append("  -h, --help     print this help message")
    }

    fun printHelp() {
        println(TEXT)
    }
}
