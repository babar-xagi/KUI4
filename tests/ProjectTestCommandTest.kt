package tests

import kui.testing.TestCommand
import java.nio.file.Files
import java.io.File

fun main() {
    val root = Files.createTempDirectory("kui-project-tests-").toFile()
    try {
        File(root, "kui.toml").writeText("[project]\nname = \"tests\"\nversion = \"0.1.0\"\n")
        check(TestCommand.execute(root) != 0) { "Missing tests must fail" }
        val tests = File(root, "tests").apply { mkdirs() }
        File(tests, "Passing.kt").writeText("@file:JvmName(\"PassingTests\")\npackage smoke\nfun main() { check(2 + 2 == 4); println(\"PASS\") }")
        File(tests, "Second.kt").writeText("package smoke\nfun main() { check(ui4.Color.White.argb != 0L) }")
        File(root, "src").mkdirs()
        File(root, "src/main.kt").writeText("fun main() { error(\"Application main must not run as a test\") }")
        check(TestCommand.execute(root) == 0) { "Passing JVM test programs must pass" }
        File(tests, "Failing.kt").writeText("package smoke\nfun main() { error(\"Expected test failure\") }")
        check(TestCommand.execute(root) != 0) { "Failed test program must fail the command" }
        File(tests, "Invalid.kt").writeText("this is invalid Kotlin")
        check(TestCommand.execute(root) != 0) { "Test compilation failure must fail the command" }
        println("Summary: 4 PASSED, 0 FAILED")
    } finally { root.deleteRecursively() }
}
