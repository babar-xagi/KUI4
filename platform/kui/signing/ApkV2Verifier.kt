package kui.signing

import java.io.ByteArrayInputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.KeyFactory
import java.security.Signature
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.X509EncodedKeySpec

/**
 * Result of APK v2 Signature verification (Phase 175).
 */
sealed class VerificationResult {
    data class Success(
        val certSubject: String,
        val algorithm: String
    ) : VerificationResult()

    data class Failure(
        val reason: String
    ) : VerificationResult()
}

/**
 * Pure Kotlin APK Signature Scheme v2 Verifier (Phase 175).
 * Validates cryptographic signatures, verifies 1MB chunked SHA-256 tree digests,
 * and detects any byte tampering across APK entries, central directory, or signing block.
 */
object ApkV2Verifier {

    fun verifyFile(file: File): VerificationResult =
        verify(file.readBytes())

    /**
     * Verifies the APK v2 signature of the given APK byte array.
     */
    fun verify(apkBytes: ByteArray): VerificationResult {
        try {
            // 1. Find EOCD
            val eocdOffset = ApkV2Signer.findEocdRecord(apkBytes)
                ?: return VerificationResult.Failure("Missing End of Central Directory record")

            val cdOffset = ApkV2Signer.getUInt32LE(apkBytes, eocdOffset + 16).toInt()
            val cdSize = ApkV2Signer.getUInt32LE(apkBytes, eocdOffset + 12).toInt()

            if (cdOffset + cdSize > eocdOffset) {
                return VerificationResult.Failure("Central Directory overlaps with or extends past EOCD")
            }

            // 2. Locate APK Signing Block immediately before Central Directory
            if (cdOffset < 24) {
                return VerificationResult.Failure("APK file too small to contain APK Signing Block")
            }

            // Check 16-byte magic
            val magicStart = cdOffset - 16
            for (i in 0 until 16) {
                if (apkBytes[magicStart + i] != ApkV2Signer.APK_SIG_BLOCK_MAGIC_BYTES[i]) {
                    return VerificationResult.Failure("APK Signing Block magic string not found before Central Directory")
                }
            }

            // Read footer block size (8 bytes before magic)
            val blockSize2 = getUInt64LE(apkBytes, cdOffset - 24)
            val sigBlockStartLong = (cdOffset - 8) - blockSize2
            if (sigBlockStartLong < 0 || sigBlockStartLong > cdOffset - 24) {
                return VerificationResult.Failure("Invalid APK Signing Block size: $blockSize2")
            }
            val sigBlockStart = sigBlockStartLong.toInt()

            // Read header block size (first 8 bytes of signing block)
            val blockSize1 = getUInt64LE(apkBytes, sigBlockStart)
            if (blockSize1 != blockSize2) {
                return VerificationResult.Failure("APK Signing Block header size ($blockSize1) and footer size ($blockSize2) do not match")
            }

            // 3. Scan ID-value pairs for APK Signature Scheme v2 (ID 0x7109871a)
            var pairOffset = sigBlockStart + 8
            val pairsEnd = cdOffset - 24
            var v2BlockValue: ByteArray? = null

            while (pairOffset < pairsEnd) {
                val pairLen = getUInt64LE(apkBytes, pairOffset)
                if (pairLen < 4 || pairOffset + 8 + pairLen > pairsEnd) {
                    return VerificationResult.Failure("Malformed ID-value pair in APK Signing Block")
                }
                val pairId = ApkV2Signer.getUInt32LE(apkBytes, pairOffset + 8).toInt()
                if (pairId == ApkV2Signer.SIGNATURE_SCHEME_V2_BLOCK_ID) {
                    v2BlockValue = apkBytes.copyOfRange(pairOffset + 12, (pairOffset + 8 + pairLen).toInt())
                    break
                }
                pairOffset += (8 + pairLen).toInt()
            }

            if (v2BlockValue == null) {
                return VerificationResult.Failure("APK Signature Scheme v2 block (ID 0x7109871a) not found in signing block")
            }

            // 4. Parse APK Signature Scheme v2 block
            val bb = ByteBuffer.wrap(v2BlockValue).order(ByteOrder.LITTLE_ENDIAN)
            val signersLen = bb.int
            val signerStart = bb.position()

            // First signer
            val signerLen = bb.int
            val signedDataLen = bb.int
            val signedDataBytes = ByteArray(signedDataLen)
            bb.get(signedDataBytes)

            // Read signatures
            val signaturesLen = bb.int
            val sigEntryLen = bb.int
            val sigAlgorithm = bb.int
            val sigBytesLen = bb.int
            val signatureBytes = ByteArray(sigBytesLen)
            bb.get(signatureBytes)

            // Read public key
            val pubKeyLen = bb.int
            val pubKeyBytes = ByteArray(pubKeyLen)
            bb.get(pubKeyBytes)

            // Parse signed data internal structures
            val sd = ByteBuffer.wrap(signedDataBytes).order(ByteOrder.LITTLE_ENDIAN)
            val digestsLen = sd.int
            val digestEntryLen = sd.int
            val digestAlgo = sd.int
            val digestBytesLen = sd.int
            val expectedDigest = ByteArray(digestBytesLen)
            sd.get(expectedDigest)

            // Certificates
            val certsLen = sd.int
            val certLen = sd.int
            val certBytes = ByteArray(certLen)
            sd.get(certBytes)

            val certFactory = CertificateFactory.getInstance("X.509")
            val cert = certFactory.generateCertificate(ByteArrayInputStream(certBytes)) as X509Certificate

            // 5. Verify cryptographic signature over signedDataBytes
            val keyFactory = KeyFactory.getInstance("RSA")
            val publicKey = keyFactory.generatePublic(X509EncodedKeySpec(pubKeyBytes))

            val verifier = Signature.getInstance("SHA256withRSA")
            verifier.initVerify(publicKey)
            verifier.update(signedDataBytes)
            if (!verifier.verify(signatureBytes)) {
                return VerificationResult.Failure("Cryptographic signature verification failed (corrupt or forged signature)")
            }

            // 6. Recompute APK chunked SHA-256 tree digest across Section 1, Section 3, Section 4
            val section1 = apkBytes.copyOfRange(0, sigBlockStart)
            val section3 = apkBytes.copyOfRange(cdOffset, cdOffset + cdSize)
            val section4 = apkBytes.copyOfRange(eocdOffset, apkBytes.size)

            // Temporarily set CD offset in EOCD back to sigBlockStart (as it was before signing)
            ApkV2Signer.setUInt32LE(section4, 16, sigBlockStart.toLong())

            val recomputedDigest = ApkV2Signer.computeApkDigest(listOf(section1, section3, section4))

            if (!expectedDigest.contentEquals(recomputedDigest)) {
                return VerificationResult.Failure(
                    "APK content digest mismatch: Archive entries, Central Directory, or EOCD have been tampered with"
                )
            }

            return VerificationResult.Success(
                certSubject = cert.subjectX500Principal.name,
                algorithm = "SHA256withRSA (APK v2)"
            )
        } catch (e: Exception) {
            return VerificationResult.Failure("APK v2 verification error: ${e::class.simpleName}: ${e.message}")
        }
    }

    private fun getUInt64LE(bytes: ByteArray, offset: Int): Long {
        var result = 0L
        for (i in 0..7) {
            result = result or ((bytes[offset + i].toLong() and 0xFF) shl (i * 8))
        }
        return result
    }
}
