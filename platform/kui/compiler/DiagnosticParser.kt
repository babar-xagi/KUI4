package kui.compiler

import java.io.File

/**
 * Diagnostic severity level.
 */
enum class DiagnosticSeverity {
    ERROR,
    WARNING,
    INFO
}

/**
 * Structured compiler diagnostic message (Phase 041, 052).
 */
data class CompilerDiagnostic(
    val filePath: String,
    val line: Int,
    val column: Int,
    val severity: DiagnosticSeverity,
    val message: String,
    val rawText: String
)

/**
 * Parses raw compiler output into structured diagnostics and formats clean console feedback.
 */
object DiagnosticParser {

    // Regex 1: /path/to/file.kt:12:34: error: message
    private val PATTERN_STANDARD = Regex("""^(.*?\.kt):(\d+):(\d+):\s+(error|warning|info):\s+(.*)$""")

    // Regex 2: e: /path/to/file.kt: (12, 34): message
    private val PATTERN_PREFIXED = Regex("""^([ewi]):\s+(.*?\.kt):\s+\((\d+),\s*(\d+)\):\s+(.*)$""")

    /**
     * Parses the combined stdout and stderr from kotlinc into a list of diagnostics.
     */
    fun parse(output: String): List<CompilerDiagnostic> {
        val diagnostics = mutableListOf<CompilerDiagnostic>()

        for (line in output.lines()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            // Check standard format: File:line:col: severity: msg
            val standardMatch = PATTERN_STANDARD.find(trimmed)
            if (standardMatch != null) {
                val filePath = standardMatch.groupValues[1]
                val lineNum = standardMatch.groupValues[2].toIntOrNull() ?: 1
                val colNum = standardMatch.groupValues[3].toIntOrNull() ?: 1
                val sevStr = standardMatch.groupValues[4].lowercase()
                val msg = standardMatch.groupValues[5]
                val severity = when (sevStr) {
                    "error" -> DiagnosticSeverity.ERROR
                    "warning" -> DiagnosticSeverity.WARNING
                    else -> DiagnosticSeverity.INFO
                }
                diagnostics.add(CompilerDiagnostic(filePath, lineNum, colNum, severity, msg, trimmed))
                continue
            }

            // Check prefixed format: e: File: (line, col): msg
            val prefixedMatch = PATTERN_PREFIXED.find(trimmed)
            if (prefixedMatch != null) {
                val sevChar = prefixedMatch.groupValues[1]
                val filePath = prefixedMatch.groupValues[2]
                val lineNum = prefixedMatch.groupValues[3].toIntOrNull() ?: 1
                val colNum = prefixedMatch.groupValues[4].toIntOrNull() ?: 1
                val msg = prefixedMatch.groupValues[5]
                val severity = when (sevChar) {
                    "e" -> DiagnosticSeverity.ERROR
                    "w" -> DiagnosticSeverity.WARNING
                    else -> DiagnosticSeverity.INFO
                }
                diagnostics.add(CompilerDiagnostic(filePath, lineNum, colNum, severity, msg, trimmed))
                continue
            }
        }

        return diagnostics
    }

    /**
     * Formats diagnostics into concise, noise-free console messages (Phase 052).
     */
    fun formatConcise(diagnostics: List<CompilerDiagnostic>, projectRoot: File? = null): String {
        if (diagnostics.isEmpty()) return ""

        val rootCanonical = projectRoot?.canonicalPath?.replace('\\', '/')

        return buildString {
            for (diag in diagnostics) {
                var displayPath = diag.filePath.replace('\\', '/')
                if (rootCanonical != null && displayPath.startsWith(rootCanonical)) {
                    displayPath = displayPath.removePrefix(rootCanonical).removePrefix("/")
                }

                val tag = when (diag.severity) {
                    DiagnosticSeverity.ERROR -> "error"
                    DiagnosticSeverity.WARNING -> "warning"
                    DiagnosticSeverity.INFO -> "info"
                }

                appendLine("$displayPath:${diag.line}:${diag.column}: $tag: ${diag.message}")
            }
        }.trimEnd()
    }
}
