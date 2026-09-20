package tests

import kui.cli.Command
import kui.cli.CommandParser
import kui.cli.Help
import kui.cli.ParsedInvocation

/**
 * Verification test for Phase 004 (Command Parser) and Phase 005 (Help Command).
 */
fun main() {
    println("==================================================")
    println(" Phase 004 & 005: Command Parser & Help Tests")
    println("==================================================")

    var passed = 0
    var failed = 0

    fun check(name: String, condition: Boolean, details: String = "") {
        if (condition) {
            println("  [PASS] $name")
            passed++
        } else {
            println("  [FAIL] $name: $details")
            failed++
        }
    }

    // 1. Subcommand parsing
    val parseBuild = CommandParser.parse(arrayOf("build"))
    check("Parses 'build' command", parseBuild is ParsedInvocation.ExecuteCommand && parseBuild.command == Command.BUILD)

    val parseNew = CommandParser.parse(arrayOf("new", "my-app", "--template", "minimal"))
    check("Parses 'new' with args and flags",
        parseNew is ParsedInvocation.ExecuteCommand &&
        parseNew.command == Command.NEW &&
        parseNew.args == listOf("my-app") &&
        parseNew.flags["template"] == "minimal"
    )

    // 2. Flags with equals syntax: --flag=value
    val parseFlags = CommandParser.parse(arrayOf("test", "--filter=MyTest", "-q"))
    check("Parses --key=value and single-dash flags",
        parseFlags is ParsedInvocation.ExecuteCommand &&
        parseFlags.flags["filter"] == "MyTest" &&
        parseFlags.flags["q"] == "true"
    )

    // 3. Unknown command & fuzzy suggestions (Phase 004)
    val parseTypo = CommandParser.parse(arrayOf("buld"))
    check("Identifies unknown command 'buld'", parseTypo is ParsedInvocation.UnknownCommand && parseTypo.rawName == "buld")
    val typoSuggestion = (parseTypo as? ParsedInvocation.UnknownCommand)?.suggestion
    check("Suggests 'build' for typo 'buld'", typoSuggestion == Command.BUILD)

    val parseClen = CommandParser.parse(arrayOf("clen"))
    val clenSuggestion = (parseClen as? ParsedInvocation.UnknownCommand)?.suggestion
    check("Suggests 'clean' for typo 'clen'", clenSuggestion == Command.CLEAN)

    // 4. Structured help command (Phase 005)
    val helpBuild = CommandParser.parse(arrayOf("help", "build"))
    check("Parses 'help build'",
        helpBuild is ParsedInvocation.ShowHelp && helpBuild.targetCommand == Command.BUILD
    )
    val buildHelpText = Help.commandHelp(Command.BUILD)
    check("Command help includes usage and description",
        buildHelpText.contains("compile/package") && buildHelpText.contains("kui build [options]")
    )

    val helpGeneral = CommandParser.parse(arrayOf("help"))
    check("Parses general 'help'",
        helpGeneral is ParsedInvocation.ShowHelp && helpGeneral.targetCommand == null
    )

    println("--------------------------------------------------")
    println("Summary: $passed PASSED, $failed FAILED")
    println("==================================================")

    if (failed > 0) System.exit(1) else println("RESULT: PASS")
}
