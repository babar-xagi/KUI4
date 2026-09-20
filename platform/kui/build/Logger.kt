package kui.build

import java.io.PrintStream

enum class LogLevel(val priority: Int) {
    DEBUG(1),
    INFO(2),
    WARN(3),
    ERROR(4),
    QUIET(5)
}

/**
 * Standard logging facility for KUI (Phase 010).
 */
class KuiLogger(
    var level: LogLevel = LogLevel.INFO,
    var out: PrintStream = System.out,
    var err: PrintStream = System.err
) {
    fun debug(msg: String) {
        if (level.priority <= LogLevel.DEBUG.priority) out.println("[DEBUG] $msg")
    }

    fun info(msg: String) {
        if (level.priority <= LogLevel.INFO.priority) out.println("[INFO] $msg")
    }

    fun warn(msg: String) {
        if (level.priority <= LogLevel.WARN.priority) err.println("[WARN] $msg")
    }

    fun error(msg: String) {
        if (level.priority <= LogLevel.ERROR.priority) err.println("[ERROR] $msg")
    }
}

object Logger {
    var defaultLogger = KuiLogger()

    fun debug(msg: String) = defaultLogger.debug(msg)
    fun info(msg: String) = defaultLogger.info(msg)
    fun warn(msg: String) = defaultLogger.warn(msg)
    fun error(msg: String) = defaultLogger.error(msg)
}
