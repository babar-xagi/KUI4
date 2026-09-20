package kui.signing

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * APK Signing configuration holding a private key and its corresponding X.509 certificate (Phase 173).
 */
data class SigningConfig(
    val privateKey: PrivateKey,
    val certificate: X509Certificate
)

/**
 * Helper to generate self-signed RSA debug keys and X.509 certificates in pure Kotlin without external tools.
 */
object DebugKeyGenerator {

    private var cachedConfig: SigningConfig? = null

    @Synchronized
    fun getOrCreateDebugKey(): SigningConfig {
        cachedConfig?.let { return it }
        val keyPair = generateKeyPair()
        val cert = generateSelfSignedCertificate(keyPair)
        val config = SigningConfig(keyPair.private, cert)
        cachedConfig = config
        return config
    }

    fun generateKeyPair(): KeyPair {
        val gen = KeyPairGenerator.getInstance("RSA")
        gen.initialize(2048)
        return gen.generateKeyPair()
    }

    /**
     * Pure Kotlin DER X.509 v3 Certificate Builder (Phase 173).
     */
    fun generateSelfSignedCertificate(
        keyPair: KeyPair,
        subjectCn: String = "KUI Android Debug",
        validityDays: Int = 3650
    ): X509Certificate {
        val now = System.currentTimeMillis()
        val notBefore = Date(now - 1000L * 60 * 60 * 24) // Yesterday
        val notAfter = Date(now + 1000L * 60 * 60 * 24 * validityDays)

        val tbsDer = buildTbsCertificate(
            serial = BigInteger.valueOf(now),
            subjectCn = subjectCn,
            notBefore = notBefore,
            notAfter = notAfter,
            publicKey = keyPair.public
        )

        // Sign TBS with private key
        val signer = Signature.getInstance("SHA256withRSA")
        signer.initSign(keyPair.private)
        signer.update(tbsDer)
        val signatureBytes = signer.sign()

        // Wrap into Certificate SEQUENCE
        val certOut = DerOutputStream()
        certOut.writeRawBytes(tbsDer)
        // AlgorithmIdentifier for SHA256withRSA
        certOut.writeSha256WithRsaAlgorithmId()
        // Signature BIT STRING
        certOut.writeBitString(signatureBytes)

        val finalDer = certOut.toSequence()

        val certFactory = CertificateFactory.getInstance("X.509")
        return certFactory.generateCertificate(ByteArrayInputStream(finalDer)) as X509Certificate
    }

    private fun buildTbsCertificate(
        serial: BigInteger,
        subjectCn: String,
        notBefore: Date,
        notAfter: Date,
        publicKey: PublicKey
    ): ByteArray {
        val out = DerOutputStream()

        // Version: [0] EXPLICIT INTEGER 2 (v3)
        val verInner = DerOutputStream()
        verInner.writeInteger(BigInteger.valueOf(2))
        out.writeTaggedExplicit(0, verInner.toByteArray())

        // Serial Number
        out.writeInteger(serial)

        // Signature Algorithm: sha256WithRSAEncryption
        out.writeSha256WithRsaAlgorithmId()

        // Issuer DN: CN=subjectCn
        out.writeDistinguishedName(subjectCn)

        // Validity: notBefore, notAfter
        out.writeValidity(notBefore, notAfter)

        // Subject DN: same as issuer
        out.writeDistinguishedName(subjectCn)

        // SubjectPublicKeyInfo (from Java PublicKey.encoded directly)
        out.writeRawBytes(publicKey.encoded)

        return out.toSequence()
    }
}

/**
 * Minimalist DER Output Stream helper for generating X.509 structures.
 */
class DerOutputStream : ByteArrayOutputStream() {

    fun writeRawBytes(bytes: ByteArray) {
        write(bytes)
    }

    fun writeLength(length: Int) {
        if (length < 128) {
            write(length)
        } else if (length < 256) {
            write(0x81)
            write(length)
        } else if (length < 65536) {
            write(0x82)
            write((length ushr 8) and 0xFF)
            write(length and 0xFF)
        } else {
            write(0x83)
            write((length ushr 16) and 0xFF)
            write((length ushr 8) and 0xFF)
            write(length and 0xFF)
        }
    }

    fun toSequence(): ByteArray {
        val content = toByteArray()
        val seq = ByteArrayOutputStream()
        seq.write(0x30)
        val lengthStream = DerOutputStream()
        lengthStream.writeLength(content.size)
        seq.write(lengthStream.toByteArray())
        seq.write(content)
        return seq.toByteArray()
    }

    fun writeInteger(value: BigInteger) {
        val bytes = value.toByteArray()
        write(0x02)
        writeLength(bytes.size)
        write(bytes)
    }

    fun writeBitString(bytes: ByteArray) {
        write(0x03)
        writeLength(bytes.size + 1)
        write(0x00) // unused bits count
        write(bytes)
    }

    fun writeTaggedExplicit(tag: Int, content: ByteArray) {
        write(0xA0 or (tag and 0x1F))
        writeLength(content.size)
        write(content)
    }

    fun writeSha256WithRsaAlgorithmId() {
        // 1.2.840.113549.1.1.11 with NULL param
        // 30 0D 06 09 2A 86 48 86 F7 0D 01 01 0B 05 00
        val bytes = byteArrayOf(
            0x30, 0x0D,
            0x06, 0x09, 0x2A, 0x86.toByte(), 0x48, 0x86.toByte(), 0xF7.toByte(), 0x0D, 0x01, 0x01, 0x0B,
            0x05, 0x00
        )
        write(bytes)
    }

    fun writeDistinguishedName(cn: String) {
        // SEQUENCE of SET of SEQUENCE { OID 2.5.4.3 (commonName), UTF8String }
        val cnBytes = cn.toByteArray(StandardCharsets.UTF_8)
        val atv = DerOutputStream()
        atv.write(byteArrayOf(0x06, 0x03, 0x55, 0x04, 0x03)) // OID 2.5.4.3
        atv.write(0x0C) // UTF8String
        atv.writeLength(cnBytes.size)
        atv.write(cnBytes)
        val atvSeq = atv.toSequence()

        val setOut = ByteArrayOutputStream()
        setOut.write(0x31) // SET
        val setLenStream = DerOutputStream()
        setLenStream.writeLength(atvSeq.size)
        setOut.write(setLenStream.toByteArray())
        setOut.write(atvSeq)
        val rdnSet = setOut.toByteArray()

        val nameOut = ByteArrayOutputStream()
        nameOut.write(0x30) // SEQUENCE
        val nameLenStream = DerOutputStream()
        nameLenStream.writeLength(rdnSet.size)
        nameOut.write(nameLenStream.toByteArray())
        nameOut.write(rdnSet)

        write(nameOut.toByteArray())
    }

    fun writeValidity(notBefore: Date, notAfter: Date) {
        val sdf = SimpleDateFormat("yyMMddHHmmss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        val nbBytes = sdf.format(notBefore).toByteArray(StandardCharsets.US_ASCII)
        val naBytes = sdf.format(notAfter).toByteArray(StandardCharsets.US_ASCII)

        val out = DerOutputStream()
        out.write(0x17) // UTCTime
        out.writeLength(nbBytes.size)
        out.write(nbBytes)

        out.write(0x17) // UTCTime
        out.writeLength(naBytes.size)
        out.write(naBytes)

        write(out.toSequence())
    }
}

/**
 * Pure Kotlin APK Signature Scheme v2 Signer (Phases 173–174).
 * Creates and injects the APK Signing Block (ID 0x7109871a) immediately before Central Directory.
 */
object ApkV2Signer {

    const val APK_SIGNING_BLOCK_MAGIC_LO = 0x20676953204b5041L // "APK Sig "
    const val APK_SIGNING_BLOCK_MAGIC_HI = 0x3234206b636f6c42L // "Block 42"
    val APK_SIG_BLOCK_MAGIC_BYTES = "APK Sig Block 42".toByteArray(StandardCharsets.US_ASCII)

    const val SIGNATURE_SCHEME_V2_BLOCK_ID = 0x7109871a
    const val SIGNATURE_RSA_PKCS1_V1_5_WITH_SHA256 = 0x0103
    const val CHUNK_SIZE = 1048576 // 1 MB chunks

    /**
     * Signs the input APK bytes using APK Signature Scheme v2.
     */
    fun sign(apkBytes: ByteArray, config: SigningConfig? = null): ByteArray {
        val signingConfig = config ?: DebugKeyGenerator.getOrCreateDebugKey()

        // 1. Locate End of Central Directory (EOCD)
        val eocdOffset = findEocdRecord(apkBytes)
            ?: throw IllegalStateException("Invalid ZIP archive: End of Central Directory not found")

        val cdOffset = getUInt32LE(apkBytes, eocdOffset + 16).toInt()
        val cdSize = getUInt32LE(apkBytes, eocdOffset + 12).toInt()

        // Verify ZIP integrity
        if (cdOffset + cdSize != eocdOffset) {
            throw IllegalStateException("ZIP Central Directory offset mismatch")
        }

        // Section 1: ZIP entries (from byte 0 to start of Central Directory)
        val section1 = apkBytes.copyOfRange(0, cdOffset)
        // Section 3: Central Directory
        val section3 = apkBytes.copyOfRange(cdOffset, cdOffset + cdSize)
        // Section 4: EOCD (with CD offset field temporarily set to original cdOffset, which it already is)
        val section4 = apkBytes.copyOfRange(eocdOffset, apkBytes.size)

        // 2. Compute 1MB Chunked SHA-256 Digest (Phase 174)
        val topDigest = computeApkDigest(listOf(section1, section3, section4))

        // 3. Build Signed Data
        val signedData = buildSignedData(topDigest, signingConfig.certificate)

        // 4. Compute Signature over Signed Data
        val signature = Signature.getInstance("SHA256withRSA")
        signature.initSign(signingConfig.privateKey)
        signature.update(signedData)
        val signatureBytes = signature.sign()

        // 5. Build APK Signature Scheme v2 Signer Block
        val signerBlock = buildSignerBlock(signedData, signatureBytes, signingConfig.certificate.publicKey)

        val signersSeqOut = ByteArrayOutputStream()
        writeLengthPrefixedBytes(signersSeqOut, signerBlock)
        val v2ValueOut = ByteArrayOutputStream()
        writeLengthPrefixedBytes(v2ValueOut, signersSeqOut.toByteArray())

        // 6. Build ID-Value Pair (ID 0x7109871a)
        val v2Block = buildIdValuePair(SIGNATURE_SCHEME_V2_BLOCK_ID, v2ValueOut.toByteArray())

        // 7. Wrap into complete APK Signing Block
        val signingBlock = buildApkSigningBlock(listOf(v2Block))

        // 8. Update EOCD with new Central Directory Offset
        val newCdOffset = (cdOffset + signingBlock.size).toLong()
        val updatedSection4 = section4.copyOf()
        setUInt32LE(updatedSection4, 16, newCdOffset)

        // 9. Assemble final signed APK
        val finalApk = ByteArrayOutputStream(section1.size + signingBlock.size + section3.size + updatedSection4.size)
        finalApk.write(section1)
        finalApk.write(signingBlock)
        finalApk.write(section3)
        finalApk.write(updatedSection4)

        return finalApk.toByteArray()
    }

    fun signFile(inputApk: File, outputApk: File, config: SigningConfig? = null) {
        val signedBytes = sign(inputApk.readBytes(), config)
        outputApk.parentFile?.mkdirs()
        FileOutputStream(outputApk).use { it.write(signedBytes) }
    }

    /**
     * Splits data across sections into 1MB chunks and computes 2-level SHA-256 tree digest.
     */
    fun computeApkDigest(sections: List<ByteArray>): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        val chunkDigests = mutableListOf<ByteArray>()

        for (section in sections) {
            var offset = 0
            while (offset < section.size) {
                val chunkSize = minOf(CHUNK_SIZE, section.size - offset)
                // Chunk digest = SHA-256( 0xa5 || 4-byte LE length || chunkData )
                md.reset()
                md.update(0xa5.toByte())
                val lenBytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(chunkSize).array()
                md.update(lenBytes)
                md.update(section, offset, chunkSize)
                chunkDigests.add(md.digest())
                offset += chunkSize
            }
        }

        // Top digest = SHA-256( 0x5a || 4-byte LE chunkCount || concatenatedDigests )
        md.reset()
        md.update(0x5a.toByte())
        val countBytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(chunkDigests.size).array()
        md.update(countBytes)
        for (chunkDigest in chunkDigests) {
            md.update(chunkDigest)
        }
        return md.digest()
    }

    private fun buildSignedData(topDigest: ByteArray, cert: X509Certificate): ByteArray {
        val out = ByteArrayOutputStream()

        // 1. Digests list: length-prefixed sequence
        val digestsOut = ByteArrayOutputStream()
        val digestEntryOut = ByteArrayOutputStream()
        writeUInt32LE(digestEntryOut, SIGNATURE_RSA_PKCS1_V1_5_WITH_SHA256)
        writeLengthPrefixedBytes(digestEntryOut, topDigest)
        writeLengthPrefixedBytes(digestsOut, digestEntryOut.toByteArray())

        // 2. Certificates list: length-prefixed sequence
        val certsOut = ByteArrayOutputStream()
        writeLengthPrefixedBytes(certsOut, cert.encoded)

        // 3. Additional attributes: length-prefixed sequence (empty)
        val attrsOut = ByteArrayOutputStream()

        writeLengthPrefixedBytes(out, digestsOut.toByteArray())
        writeLengthPrefixedBytes(out, certsOut.toByteArray())
        writeLengthPrefixedBytes(out, attrsOut.toByteArray())

        return out.toByteArray()
    }

    private fun buildSignerBlock(signedData: ByteArray, signatureBytes: ByteArray, publicKey: PublicKey): ByteArray {
        val out = ByteArrayOutputStream()

        // 1. Signed Data (length-prefixed)
        writeLengthPrefixedBytes(out, signedData)

        // 2. Signatures list (length-prefixed)
        val sigsOut = ByteArrayOutputStream()
        val sigEntryOut = ByteArrayOutputStream()
        writeUInt32LE(sigEntryOut, SIGNATURE_RSA_PKCS1_V1_5_WITH_SHA256)
        writeLengthPrefixedBytes(sigEntryOut, signatureBytes)
        writeLengthPrefixedBytes(sigsOut, sigEntryOut.toByteArray())
        writeLengthPrefixedBytes(out, sigsOut.toByteArray())

        // 3. Public Key (length-prefixed)
        writeLengthPrefixedBytes(out, publicKey.encoded)

        return out.toByteArray()
    }

    private fun buildIdValuePair(id: Int, value: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        val pairSize = 4L + value.size
        writeUInt64LE(out, pairSize)
        writeUInt32LE(out, id)
        out.write(value)
        return out.toByteArray()
    }

    private fun buildApkSigningBlock(pairs: List<ByteArray>): ByteArray {
        var pairsSize = 0L
        for (pair in pairs) pairsSize += pair.size

        // Total block size = 8 (size_of_block) + pairsSize + 8 (size_of_block) + 16 (magic)
        // The size field in the header/footer excludes the first 8-byte size field itself:
        val blockSize = pairsSize + 8 + 16

        val out = ByteArrayOutputStream()
        writeUInt64LE(out, blockSize)
        for (pair in pairs) out.write(pair)
        writeUInt64LE(out, blockSize)
        out.write(APK_SIG_BLOCK_MAGIC_BYTES)

        return out.toByteArray()
    }

    fun findEocdRecord(bytes: ByteArray): Int? {
        val maxSearch = minOf(bytes.size, 65536 + 22)
        val start = bytes.size - 22
        for (i in start downTo bytes.size - maxSearch) {
            if (i < 0) break
            if (bytes[i] == 0x50.toByte() &&
                bytes[i + 1] == 0x4b.toByte() &&
                bytes[i + 2] == 0x05.toByte() &&
                bytes[i + 3] == 0x06.toByte()
            ) {
                return i
            }
        }
        return null
    }

    private fun writeLengthPrefixedBytes(out: ByteArrayOutputStream, bytes: ByteArray) {
        writeUInt32LE(out, bytes.size)
        out.write(bytes)
    }

    private fun writeUInt32LE(out: ByteArrayOutputStream, value: Int) {
        out.write(value and 0xFF)
        out.write((value ushr 8) and 0xFF)
        out.write((value ushr 16) and 0xFF)
        out.write((value ushr 24) and 0xFF)
    }

    private fun writeUInt64LE(out: ByteArrayOutputStream, value: Long) {
        for (i in 0..7) {
            out.write(((value ushr (i * 8)) and 0xFF).toInt())
        }
    }

    fun getUInt32LE(bytes: ByteArray, offset: Int): Long {
        return ((bytes[offset].toLong() and 0xFF)) or
                ((bytes[offset + 1].toLong() and 0xFF) shl 8) or
                ((bytes[offset + 2].toLong() and 0xFF) shl 16) or
                ((bytes[offset + 3].toLong() and 0xFF) shl 24)
    }

    fun setUInt32LE(bytes: ByteArray, offset: Int, value: Long) {
        bytes[offset] = (value and 0xFF).toByte()
        bytes[offset + 1] = ((value ushr 8) and 0xFF).toByte()
        bytes[offset + 2] = ((value ushr 16) and 0xFF).toByte()
        bytes[offset + 3] = ((value ushr 24) and 0xFF).toByte()
    }
}
