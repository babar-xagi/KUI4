package tests

import kui.cache.DirectoryHasher
import kui.cache.Hasher
import java.io.File

/**
 * Verification test for Phase 012 (File Hashing) and Phase 013 (Directory Hashing).
 */
fun main() {
    println("==================================================")
    println(" Phases 012 & 013: Content & Directory Hashing")
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

    // 1. Phase 012: Stable String & File Hashing
    val stringHash1 = Hasher.sha256("Hello, KUI4!")
    val stringHash2 = Hasher.sha256("Hello, KUI4!")
    check("String hashing is deterministic", stringHash1 == stringHash2)
    check("SHA-256 output length is 64 hex chars", stringHash1.length == 64)

    val tempFile = File.createTempFile("kui_hash_test", ".txt")
    try {
        tempFile.writeText("Stable file content for hashing")
        val fileHash1 = Hasher.sha256(tempFile)
        val fileHash2 = Hasher.sha256(tempFile)
        check("File hashing is deterministic", fileHash1 == fileHash2)
        check("File hash matches byte hash", fileHash1 == Hasher.sha256(tempFile.readBytes()))
    } finally {
        tempFile.delete()
    }

    // 2. Phase 013: Deterministic & Ordering-Independent Tree Hashing
    val tempDirA = File(System.getProperty("java.io.tmpdir"), "kui_tree_test_a")
    val tempDirB = File(System.getProperty("java.io.tmpdir"), "kui_tree_test_b")
    tempDirA.mkdirs()
    tempDirB.mkdirs()

    try {
        // Create files in dir A in one order
        File(tempDirA, "file2.txt").writeText("Content 2")
        File(tempDirA, "file1.txt").writeText("Content 1")
        val subA = File(tempDirA, "sub").apply { mkdirs() }
        File(subA, "nested.txt").writeText("Nested content")

        // Create files in dir B in reverse order with same content
        val subB = File(tempDirB, "sub").apply { mkdirs() }
        File(subB, "nested.txt").writeText("Nested content")
        File(tempDirB, "file1.txt").writeText("Content 1")
        File(tempDirB, "file2.txt").writeText("Content 2")

        val hashA = DirectoryHasher.hashDirectory(tempDirA)
        val hashB = DirectoryHasher.hashDirectory(tempDirB)

        check("Directory hash is independent of creation/traversal ordering", hashA == hashB)
        check("Directory hash is 64 hex characters", hashA.length == 64)

        // Mutating one file changes the directory hash
        File(tempDirB, "file1.txt").writeText("Modified Content 1")
        val hashBModified = DirectoryHasher.hashDirectory(tempDirB)
        check("Mutating file alters directory hash", hashA != hashBModified)
    } finally {
        tempDirA.deleteRecursively()
        tempDirB.deleteRecursively()
    }

    println("--------------------------------------------------")
    println("Summary: $passed PASSED, $failed FAILED")
    println("==================================================")

    if (failed > 0) System.exit(1) else println("RESULT: PASS")
}
