package kui.cli

/**
 * Enumeration of all recognized KUI CLI commands as defined in Section 9 of the roadmap.
 */
enum class Command(
    val commandName: String,
    val description: String,
    val usage: String
) {
    NEW("new", "create project", "kui new <name>"),
    DOCTOR("doctor", "verify environment", "kui doctor"),
    INFO("info", "show project information", "kui info"),
    BUILD("build", "compile/package", "kui build [options]"),
    RUN("run", "build + install + launch", "kui run [options]"),
    INSTALL("install", "install current APK", "kui install"),
    LAUNCH("launch", "launch installed app", "kui launch"),
    TEST("test", "run tests", "kui test"),
    CLEAN("clean", "remove generated build output", "kui clean"),
    BENCH("bench", "benchmarks", "kui bench [benchmark-name]"),
    PROFILE("profile", "profiling report", "kui profile"),
    UI_TREE("ui-tree", "print runtime UI tree later", "kui ui-tree"),
    HELP("help", "show help for commands", "kui help [command]"),
    VERSION("version", "print version information", "kui version");

    companion object {
        fun fromString(name: String): Command? =
            values().firstOrNull { it.commandName.equals(name, ignoreCase = true) }

        /**
         * Computes closest command using Levenshtein distance for friendly diagnostics.
         */
        fun findClosest(name: String): Command? {
            return values()
                .map { it to levenshteinDistance(name.lowercase(), it.commandName) }
                .filter { it.second <= 2 }
                .minByOrNull { it.second }
                ?.first
        }

        private fun levenshteinDistance(s1: String, s2: String): Int {
            val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
            for (i in 0..s1.length) dp[i][0] = i
            for (j in 0..s2.length) dp[0][j] = j
            for (i in 1..s1.length) {
                for (j in 1..s2.length) {
                    val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                    dp[i][j] = minOf(
                        dp[i - 1][j] + 1,
                        dp[i][j - 1] + 1,
                        dp[i - 1][j - 1] + cost
                    )
                }
            }
            return dp[s1.length][s2.length]
        }
    }
}
