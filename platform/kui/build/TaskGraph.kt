package kui.build

/**
 * Directed Acyclic Graph (DAG) task execution engine (Phase 017).
 */
class TaskGraph {
    private val tasks = mutableMapOf<String, Task>()

    fun register(task: Task): TaskGraph {
        tasks[task.name] = task
        return this
    }

    fun getTask(name: String): Task? = tasks[name]

    /**
     * Resolves topological execution order. Detects cycles.
     */
    fun resolveExecutionOrder(targetTaskNames: List<String>): List<Task> {
        val result = mutableListOf<Task>()
        val visited = mutableSetOf<String>()
        val recursionStack = mutableSetOf<String>()

        fun dfs(taskName: String) {
            if (recursionStack.contains(taskName)) {
                throw IllegalStateException("Circular task dependency detected: '$taskName' is part of a cycle.")
            }
            if (visited.contains(taskName)) return

            val task = tasks[taskName]
                ?: throw IllegalArgumentException("Unresolved task dependency: '$taskName'")

            recursionStack.add(taskName)

            for (dep in task.dependencies) {
                dfs(dep)
            }

            recursionStack.remove(taskName)
            visited.add(taskName)
            result.add(task)
        }

        for (target in targetTaskNames) {
            dfs(target)
        }

        return result
    }

    fun resolveExecutionOrder(vararg targets: String): List<Task> =
        resolveExecutionOrder(targets.toList())
}
