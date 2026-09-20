package kui.apk

import kui.axml.ManifestGenerator
import kui.config.KuiConfig
import kui.dex.ClassToDexCompiler
import kui.dex.DexClass
import kui.signing.ApkV2Signer
import kui.signing.ApkV2Verifier
import kui.signing.VerificationResult
import java.io.File

/**
 * Result of packaging and signing an APK (Phase 176).
 */
data class PackagingResult(
    val isSuccess: Boolean,
    val outputFile: File?,
    val apkSize: Long = 0,
    val dexSize: Long = 0,
    val entryCount: Int = 0,
    val message: String = ""
)

/**
 * Orchestrates full Android packaging pipeline (Phase 176):
 * 1. Bytecode -> classes.dex (ClassToDexCompiler)
 * 2. Manifest -> AndroidManifest.xml (ManifestGenerator)
 * 3. Asset packaging (ApkWriter)
 * 4. 4-byte zipalign (ApkWriter)
 * 5. APK v2 Signing (ApkV2Signer)
 * 6. Cryptographic Verification & Tamper Check (ApkV2Verifier)
 */
object PackagingTask {

    fun execute(
        projectRoot: File,
        classesDir: File,
        config: KuiConfig,
        outputApk: File = File(projectRoot, "build/outputs/apk/debug/app-debug.apk")
    ): PackagingResult {
        try {
            // 1. Compile JVM .class files to classes.dex
            val dexBytes = if (classesDir.exists() && (classesDir.listFiles()?.isNotEmpty() == true)) {
                ClassToDexCompiler.compileDirectory(classesDir)
            } else {
                // Generate minimal fallback dex
                ClassToDexCompiler.compileClasses(emptyList<DexClass>())
            }

            // 2. Generate binary AndroidManifest.xml
            val pkgName = config.project.applicationId ?: "com.example.${config.project.name.lowercase().replace('-', '_')}"
            val manifestBytes = ManifestGenerator.generateBinaryManifest(
                packageName = pkgName,
                versionCode = 1,
                versionName = config.project.version,
                minSdk = config.android.minSdk,
                targetSdk = config.android.targetSdk,
                appLabel = config.project.name
            )

            // 3. Scan Assets directory
            val assetsMap = mutableMapOf<String, ByteArray>()
            val assetsDir = File(projectRoot, "assets")
            if (assetsDir.exists() && assetsDir.isDirectory) {
                assetsDir.walkTopDown().filter { it.isFile }.forEach { assetFile ->
                    val relPath = assetFile.relativeTo(assetsDir).path.replace('\\', '/')
                    assetsMap[relPath] = assetFile.readBytes()
                }
            }

            // 4. Build 4-byte zipaligned APK
            val rawApkBytes = ApkWriter.buildApk(
                manifestBytes = manifestBytes,
                dexBytes = dexBytes,
                assets = assetsMap
            )

            // Verify alignment
            if (!ApkWriter.verifyAlignment(rawApkBytes)) {
                return PackagingResult(
                    isSuccess = false,
                    outputFile = null,
                    message = "Packaging error: APK entries failed 4-byte zipalign check"
                )
            }

            // 5. Sign with APK Signature Scheme v2
            val signedApkBytes = ApkV2Signer.sign(rawApkBytes)

            // 6. Verify signature and integrity
            when (val verResult = ApkV2Verifier.verify(signedApkBytes)) {
                is VerificationResult.Failure -> {
                    return PackagingResult(
                        isSuccess = false,
                        outputFile = null,
                        message = "Signature verification failed: ${verResult.reason}"
                    )
                }
                is VerificationResult.Success -> {
                    // Valid!
                }
            }

            // 7. Write to output file
            outputApk.parentFile?.mkdirs()
            outputApk.writeBytes(signedApkBytes)

            val entries = ApkWriter.listEntries(signedApkBytes)

            return PackagingResult(
                isSuccess = true,
                outputFile = outputApk,
                apkSize = signedApkBytes.size.toLong(),
                dexSize = dexBytes.size.toLong(),
                entryCount = entries.size,
                message = "APK packaged, aligned, and signed successfully (${signedApkBytes.size} bytes)"
            )
        } catch (e: Exception) {
            return PackagingResult(
                isSuccess = false,
                outputFile = null,
                message = "Packaging failed: ${e.message}"
            )
        }
    }
}
