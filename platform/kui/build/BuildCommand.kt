package kui.build

import kui.cache.CacheDirectories
import kui.cache.Hasher
import kui.compiler.ClasspathModel
import kui.compiler.CompilerCacheKey
import kui.compiler.CompilerDiscovery
import kui.compiler.DiagnosticParser
import kui.compiler.KotlinCompiler
import kui.compiler.SourceDiscovery
import kui.compiler.UI4Bootstrap
import kui.config.ConfigDiagnostics
import kui.config.TomlReader
import kui.project.ProjectFinder
import java.io.File

/**
 * Executes the `kui build` command (Phases 049, 051, 052, 053, 054).
 */
object BuildCommand {

    fun execute(
        args: List<String> = emptyList(),
        flags: Map<String, String> = emptyMap(),
        projectRootOverride: File? = null
    ): Int {
        val totalTimer = Stopwatch.start()

        val root = projectRootOverride ?: ProjectFinder.findProjectRoot()
        if (root == null) {
            System.err.println("kui: No 'kui.toml' found in current directory or any parent directories.")
            return 1
        }

        val verbose = flags.containsKey("verbose") || flags.containsKey("v") || args.contains("--verbose")
        val cleanFirst = flags.containsKey("clean") || args.contains("--clean")

        if (cleanFirst) {
            CleanCommand.execute(projectRootOverride = root)
        }

        val buildDirs = BuildDirectories(root)
        val cacheDirs = CacheDirectories(root)
        buildDirs.ensureCreated()
        cacheDirs.ensureCreated()

        // 1. Validate kui.toml
        val tomlFile = File(root, "kui.toml")
        val configResult = ConfigDiagnostics.validateAndLoad(root)
        val config = when (configResult) {
            is kui.config.ConfigValidationResult.Failure -> {
                System.err.println(configResult.formatErrorMessage())
                return 1
            }
            is kui.config.ConfigValidationResult.Success -> configResult.config
        }

        val projectName = config.project.name
        val projectVersion = config.project.version

        println("[KUI] Building '$projectName' (v$projectVersion)")

        // 2. Discover Kotlin compiler
        val kotlinc = CompilerDiscovery.findKotlinc()
        if (!kotlinc.isValid) {
            System.err.println("[KUI] Error: ${kotlinc.message}")
            System.err.println("Run 'kui doctor' for environment diagnostics.")
            return 1
        }

        // 3. Discover project sources (Phase 042, 043)
        val srcDir = File(root, "src")
        val sources = SourceDiscovery.findSources(srcDir)
        if (sources.isEmpty()) {
            System.err.println("[KUI] Error: No Kotlin source files (*.kt) found in '${srcDir.path}'.")
            return 1
        }

        // 4. Bootstrap UI4 API library jar (Phase 045)
        val ui4Jar = try {
            UI4Bootstrap.ensureApiJar(buildDirs.buildDir, kotlinc.path)
        } catch (e: Exception) {
            System.err.println("[KUI] Error bootstrapping UI4 API: ${e.message}")
            return 1
        }

        // 5. Construct Classpath (Phase 044)
        val classpath = ClasspathModel()
        classpath.add(ui4Jar, "UI4 API Runtime")

        // 6. Check Compiler Cache Key (Phase 046, 047)
        val cacheKey = CompilerCacheKey.compute(
            sources = sources,
            sourceBaseDir = srcDir,
            classpath = classpath,
            compilerVersion = kotlinc.version,
            jvmTarget = "21",
            configFile = tomlFile
        )

        val taskCacheFile = cacheDirs.getTaskCacheFile(cacheKey.key)
        val classesDir = buildDirs.classesDir
        val isCached = taskCacheFile.exists() && classesDir.exists() && (classesDir.listFiles()?.isNotEmpty() == true)

        if (isCached && !cleanFirst) {
            println("[KUI] Compile Kotlin: UP-TO-DATE (cached ${cacheKey.key.take(8)})")
            println("[KUI] BUILD SUCCESS (total: ${totalTimer.elapsedMillis()}ms)")
            return 0
        }

        // 7. Compile Kotlin sources (Phase 040, 048, 051)
        println("[KUI] Compiling ${sources.size} Kotlin source(s) with kotlinc ${kotlinc.version}...")

        if (verbose) {
            println("[KUI:DEBUG] kotlinc: ${kotlinc.path}")
            println("[KUI:DEBUG] classpath: ${classpath.asClasspathString()}")
            println("[KUI:DEBUG] sources: ${sources.map { it.name }}")
        }

        val compileTimer = Stopwatch.start()
        val compileResult = KotlinCompiler.compile(
            sources = sources,
            outputDir = classesDir,
            classpath = classpath,
            kotlincPath = kotlinc.path,
            jvmTarget = "21",
            verbose = verbose,
            workingDir = root
        )
        val compileDurationMs = compileTimer.elapsedMillis()

        if (verbose && compileResult.commandLine.isNotEmpty()) {
            println("[KUI:DEBUG] command: ${compileResult.commandLine.joinToString(" ")}")
        }

        // 8. Handle Failures & Concise UX (Phase 052)
        if (!compileResult.isSuccess) {
            System.err.println("[KUI] Compilation failed:")
            val concise = DiagnosticParser.formatConcise(compileResult.diagnostics, root)
            if (concise.isNotBlank()) {
                System.err.println(concise)
            } else {
                System.err.println(compileResult.rawOutput.ifBlank { "Unknown compiler error." })
            }
            return 1
        }

        // 9. Save cache metadata (Phase 048)
        taskCacheFile.parentFile?.mkdirs()
        taskCacheFile.writeText("key=${cacheKey.key}\ncompiled_at=${System.currentTimeMillis()}\n")

        println("[KUI] Compiled ${sources.size} source file(s) in ${compileDurationMs}ms -> ${classesDir.name}/")

        // 10. Package, zipalign, and sign APK (Phases 169-176)
        println("[KUI] Packaging APK: classes.dex + AndroidManifest.xml...")
        val packageResult = kui.apk.PackagingTask.execute(
            projectRoot = root,
            classesDir = classesDir,
            config = config
        )

        if (!packageResult.isSuccess) {
            System.err.println("[KUI] Packaging failed: ${packageResult.message}")
            return 1
        }

        println("[KUI] Signed APK (v2): ${packageResult.outputFile?.relativeTo(root)?.path ?: "app-debug.apk"} (${packageResult.apkSize} bytes)")
        println("[KUI] BUILD SUCCESS (total: ${totalTimer.elapsedMillis()}ms)")

        return 0
    }
}
