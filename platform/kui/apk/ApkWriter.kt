package kui.apk

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.CRC32
import java.util.zip.Deflater

/**
 * Entry to be packaged into an APK (Phases 169–171).
 */
data class ApkEntry(
    val name: String,
    val data: ByteArray,
    val compress: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ApkEntry
        return name == other.name && compress == other.compress && data.contentEquals(other.data)
    }

    override fun hashCode(): Int =
        (31 * name.hashCode() + compress.hashCode()) * 31 + data.contentHashCode()
}

/**
 * Inspection metadata for an entry in an APK.
 */
data class ApkEntryInfo(
    val name: String,
    val compressionMethod: Int,
    val compressedSize: Long,
    val uncompressedSize: Long,
    val localHeaderOffset: Long,
    val dataOffset: Long,
    val isAligned4: Boolean
)

/**
 * Pure Kotlin ZIP and APK Writer with 4-byte data alignment (zipalign) (Phases 169–172).
 * Operates without external zip, aapt2, or zipalign binaries.
 */
class ApkWriter {

    private val entries = mutableListOf<ApkEntry>()

    /**
     * Adds a file entry to the APK.
     * By default, binary XML and DEX files are STORED uncompressed and 4-byte aligned.
     */
    fun addEntry(name: String, data: ByteArray, compress: Boolean = false): ApkWriter {
        entries.add(ApkEntry(name, data, compress))
        return this
    }

    /**
     * Builds the complete binary APK byte array with 4-byte zipalign.
     */
    fun build(): ByteArray {
        val out = ByteArrayOutputStream()

        data class WrittenEntry(
            val entry: ApkEntry,
            val localHeaderOffset: Int,
            val compressionMethod: Int,
            val crc32: Long,
            val compressedBytes: ByteArray,
            val extraField: ByteArray
        )

        val writtenEntries = mutableListOf<WrittenEntry>()

        for (item in entries) {
            val nameBytes = item.name.toByteArray(StandardCharsets.UTF_8)
            val currentOffset = out.size()

            val crc = CRC32()
            crc.update(item.data)
            val crcValue = crc.value

            val (compressionMethod, compressedData) = if (item.compress) {
                val deflater = Deflater(Deflater.DEFAULT_COMPRESSION, true) // nowrap = true
                deflater.setInput(item.data)
                deflater.finish()
                val compBuf = ByteArray(1024)
                val compOut = ByteArrayOutputStream()
                while (!deflater.finished()) {
                    val count = deflater.deflate(compBuf)
                    compOut.write(compBuf, 0, count)
                }
                deflater.end()
                8 to compOut.toByteArray()
            } else {
                0 to item.data
            }

            // 4-byte alignment calculation (Phase 170):
            // The data starts at: currentOffset + 30 (header) + nameBytes.size + extraBytes.size
            // If STORED, data offset MUST be a multiple of 4.
            val baseHeaderSize = 30 + nameBytes.size
            val extraBytes = if (compressionMethod == 0) {
                val unalignedOffset = currentOffset + baseHeaderSize
                val remainder = unalignedOffset % 4
                val paddingNeeded = if (remainder != 0) 4 - remainder else 0
                ByteArray(paddingNeeded)
            } else {
                ByteArray(0)
            }

            // Write Local File Header (30 bytes)
            writeUInt32(out, 0x04034b50) // Local file header signature
            writeUInt16(out, 20)         // Version needed to extract (2.0)
            writeUInt16(out, 0)          // General purpose bit flag
            writeUInt16(out, compressionMethod) // Compression method (0 = STORED, 8 = DEFLATED)
            writeUInt16(out, 0)          // Last mod file time
            writeUInt16(out, 0x5421)     // Last mod file date (2022-01-01)
            writeUInt32(out, crcValue)   // CRC-32
            writeUInt32(out, compressedData.size.toLong()) // Compressed size
            writeUInt32(out, item.data.size.toLong())       // Uncompressed size
            writeUInt16(out, nameBytes.size)               // File name length
            writeUInt16(out, extraBytes.size)              // Extra field length
            out.write(nameBytes)                           // File name
            out.write(extraBytes)                          // Extra field (padding)
            out.write(compressedData)                      // File data

            writtenEntries.add(
                WrittenEntry(
                    entry = item,
                    localHeaderOffset = currentOffset,
                    compressionMethod = compressionMethod,
                    crc32 = crcValue,
                    compressedBytes = compressedData,
                    extraField = extraBytes
                )
            )
        }

        // Central Directory
        val cdOffset = out.size()
        for (w in writtenEntries) {
            val nameBytes = w.entry.name.toByteArray(StandardCharsets.UTF_8)
            writeUInt32(out, 0x02014b50) // Central directory header signature
            writeUInt16(out, 20)         // Version made by
            writeUInt16(out, 20)         // Version needed
            writeUInt16(out, 0)          // Bit flag
            writeUInt16(out, w.compressionMethod) // Compression method
            writeUInt16(out, 0)          // Last mod time
            writeUInt16(out, 0x5421)     // Last mod date
            writeUInt32(out, w.crc32)    // CRC-32
            writeUInt32(out, w.compressedBytes.size.toLong()) // Compressed size
            writeUInt32(out, w.entry.data.size.toLong())       // Uncompressed size
            writeUInt16(out, nameBytes.size)                  // File name length
            writeUInt16(out, w.extraField.size)               // Extra field length
            writeUInt16(out, 0)          // File comment length
            writeUInt16(out, 0)          // Disk number start
            writeUInt16(out, 0)          // Internal file attributes
            writeUInt32(out, 0)          // External file attributes
            writeUInt32(out, w.localHeaderOffset.toLong()) // Relative offset of local header
            out.write(nameBytes)
            out.write(w.extraField)
        }
        val cdSize = out.size() - cdOffset

        // End of Central Directory (EOCD)
        writeUInt32(out, 0x06054b50) // EOCD signature
        writeUInt16(out, 0)          // Number of this disk
        writeUInt16(out, 0)          // Disk where central directory starts
        writeUInt16(out, writtenEntries.size) // Number of central directory records on this disk
        writeUInt16(out, writtenEntries.size) // Total number of central directory records
        writeUInt32(out, cdSize.toLong())     // Size of central directory
        writeUInt32(out, cdOffset.toLong())   // Offset of start of central directory
        writeUInt16(out, 0)          // Comment length

        return out.toByteArray()
    }

    /**
     * Writes the APK archive to the given file.
     */
    fun writeTo(file: File) {
        file.parentFile?.mkdirs()
        FileOutputStream(file).use { it.write(build()) }
    }

    companion object {
        private fun writeUInt16(out: ByteArrayOutputStream, value: Int) {
            out.write(value and 0xFF)
            out.write((value ushr 8) and 0xFF)
        }

        private fun writeUInt32(out: ByteArrayOutputStream, value: Long) {
            out.write((value and 0xFF).toInt())
            out.write(((value ushr 8) and 0xFF).toInt())
            out.write(((value ushr 16) and 0xFF).toInt())
            out.write(((value ushr 24) and 0xFF).toInt())
        }

        /**
         * Convenience helper to build an APK from standard Android components.
         */
        fun buildApk(
            manifestBytes: ByteArray,
            dexBytes: ByteArray,
            assets: Map<String, ByteArray> = emptyMap(),
            outputFile: File? = null
        ): ByteArray {
            val writer = ApkWriter()
            // AndroidManifest.xml must be STORED and uncompressed
            writer.addEntry("AndroidManifest.xml", manifestBytes, compress = false)
            // classes.dex must be STORED and 4-byte aligned for fast memory-mapping
            writer.addEntry("classes.dex", dexBytes, compress = false)

            for ((assetPath, assetData) in assets) {
                val cleanPath = assetPath.trimStart('/')
                val fullPath = if (cleanPath.startsWith("assets/")) cleanPath else "assets/$cleanPath"
                writer.addEntry(fullPath, assetData, compress = false)
            }

            val apkBytes = writer.build()
            if (outputFile != null) {
                outputFile.parentFile?.mkdirs()
                outputFile.writeBytes(apkBytes)
            }
            return apkBytes
        }

        /**
         * Inspects and lists all entries from an APK byte array.
         */
        fun listEntries(apkBytes: ByteArray): List<ApkEntryInfo> {
            val result = mutableListOf<ApkEntryInfo>()
            var pos = 0
            while (pos + 30 <= apkBytes.size) {
                val sig = readUInt32(apkBytes, pos)
                if (sig != 0x04034b50L) break // Reached central directory or unknown

                val method = readUInt16(apkBytes, pos + 8)
                val compSize = readUInt32(apkBytes, pos + 18)
                val uncompSize = readUInt32(apkBytes, pos + 22)
                val nameLen = readUInt16(apkBytes, pos + 26)
                val extraLen = readUInt16(apkBytes, pos + 28)

                val name = String(apkBytes, pos + 30, nameLen, StandardCharsets.UTF_8)
                val dataOffset = (pos + 30 + nameLen + extraLen).toLong()
                val isAligned = (dataOffset % 4) == 0L

                result.add(
                    ApkEntryInfo(
                        name = name,
                        compressionMethod = method,
                        compressedSize = compSize,
                        uncompressedSize = uncompSize,
                        localHeaderOffset = pos.toLong(),
                        dataOffset = dataOffset,
                        isAligned4 = isAligned
                    )
                )

                pos = dataOffset.toInt() + compSize.toInt()
            }
            return result
        }

        /**
         * Verifies that all uncompressed (STORED) entries are 4-byte aligned (Phase 172).
         */
        fun verifyAlignment(apkBytes: ByteArray): Boolean {
            val entries = listEntries(apkBytes)
            if (entries.isEmpty()) return false
            for (e in entries) {
                if (e.compressionMethod == 0 && !e.isAligned4) {
                    return false
                }
            }
            return true
        }

        private fun readUInt16(bytes: ByteArray, offset: Int): Int {
            return (bytes[offset].toInt() and 0xFF) or
                    ((bytes[offset + 1].toInt() and 0xFF) shl 8)
        }

        private fun readUInt32(bytes: ByteArray, offset: Int): Long {
            return ((bytes[offset].toLong() and 0xFF)) or
                    ((bytes[offset + 1].toLong() and 0xFF) shl 8) or
                    ((bytes[offset + 2].toLong() and 0xFF) shl 16) or
                    ((bytes[offset + 3].toLong() and 0xFF) shl 24)
        }
    }
}
