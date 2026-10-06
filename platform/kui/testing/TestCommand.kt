package kui.testing

import kui.compiler.ClasspathModel
import kui.compiler.CompilerDiscovery
import kui.compiler.KotlinCompiler
import kui.compiler.KotlinProcessRunner
import kui.compiler.SourceDiscovery
import kui.compiler.UI4Bootstrap
import kui.project.ProjectFinder
import java.io.File

/** Runs JVM tests with top-level main functions; a nonzero exit fails the suite. */
object TestCommand {
    fun execute(projectRootOverride: File? = null): Int {
        val root = projectRootOverride ?: ProjectFinder.findProjectRoot()
        if (root == null) {
            System.err.println("kui: No kui.toml found. Run tests inside a KUI project.")
            return 1
        }
        val tests = SourceDiscovery.findSources(File(root, "tests"))
        if (tests.isEmpty()) {
            System.err.println("kui: No Kotlin tests found in tests/.")
            return 1
        }
        val environment = CompilerDiscovery.checkEnvironment()
        if (!environment.isReady) {
            environment.errors.forEach { System.err.println(it) }
            return 1
        }
        try {
            val output = File(root, ".kui/tests")
            if (output.exists() && !output.deleteRecursively()) error("Cannot clean test output: $output")
            output.mkdirs()
            val classpath = ClasspathModel()
            val isRepository = File(root, "platform/kui/cli/Main.kt").isFile
            val productionSources = if (isRepository) {
                SourceDiscovery.findSources(File(root, "platform"))
            } else {
                classpath.add(UI4Bootstrap.ensureApiJar(File(root, ".kui/build"), environment.kotlin.path), "UI4")
                SourceDiscovery.findSources(File(root, "src"))
            }
            val sources = productionSources + tests
            val compiled = KotlinCompiler.compile(sources, output, classpath, environment.kotlin.path, workingDir = root)
            if (!compiled.isSuccess) {
                System.err.println(compiled.rawOutput)
                return 1
            }
            // Discover compiled entry points, including @file:JvmName, without executing static initializers.
            val compilerHome = File(environment.kotlin.path).parentFile.parentFile
            val runtimeJars = File(compilerHome, "lib").listFiles().orEmpty()
                .filter { it.name.startsWith("kotlin-stdlib") && it.extension == "jar" }
            val runtimeClasspath = (listOf(output) + classpath.getEntries().map { it.file } + runtimeJars)
                .joinToString(File.pathSeparator) { it.absolutePath }
            val testNames = tests.map { it.name }.toSet()
            val entries = output.walkTopDown().filter { it.extension == "class" }.mapNotNull { file ->
                val parsed = kui.classfile.ClassFileReader.read(file)
                // Only test files supply entry points; application main is not a test.
                val bytes = parsed.attributes["SourceFile"]
                val sourceName = if (bytes != null && bytes.size == 2) {
                    parsed.getUtf8(((bytes[0].toInt() and 255) shl 8) or (bytes[1].toInt() and 255))
                } else null
                if (sourceName in testNames && parsed.methods.any {
                    it.name == "main" && it.descriptor == "([Ljava/lang/String;)V" && (it.accessFlags and 0x0009) == 0x0009
                }) parsed.thisClassName.replace('/', '.') else null
            }.sorted().toList()
            if (entries.isEmpty()) {
                System.err.println("kui: Tests must define a top-level fun main() or fun main(args: Array<String>).")
                return 1
            }
            var failed = 0
            for (entry in entries) {
                println("[KUI] Test: $entry")
                val result = KotlinProcessRunner.run(listOf(environment.java.path, "-ea", "-Dkui.home=${System.getProperty("kui.home", "")}", "-cp", runtimeClasspath, entry, root.absolutePath), root)
                print(result.stdout)
                System.err.print(result.stderr)
                if (!result.isSuccess) failed++
            }
            println("[KUI] ${entries.size - failed}/${entries.size} test programs passed.")
            return if (failed == 0) 0 else 1
        } catch (e: Exception) {
            System.err.println("kui: Test execution failed: ${e.message}")
            return 1
        }
    }
}
