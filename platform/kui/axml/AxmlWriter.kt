package kui.axml

import java.io.ByteArrayOutputStream

/**
 * Android Binary XML (AXML) Chunk Types and Constants (Phase 167).
 */
object AxmlConstants {
    const val RES_XML_TYPE = 0x0003
    const val RES_STRING_POOL_TYPE = 0x0001
    const val RES_XML_RESOURCE_MAP_TYPE = 0x0180
    const val RES_XML_START_NAMESPACE_TYPE = 0x0100
    const val RES_XML_END_NAMESPACE_TYPE = 0x0101
    const val RES_XML_START_ELEMENT_TYPE = 0x0102
    const val RES_XML_END_ELEMENT_TYPE = 0x0103

    // TypedValue Types
    const val TYPE_NULL = 0x00
    const val TYPE_REFERENCE = 0x01
    const val TYPE_STRING = 0x03
    const val TYPE_INT_DEC = 0x10
    const val TYPE_INT_BOOLEAN = 0x12

    // Standard Android Attribute Resource IDs
    const val ATTR_MIN_SDK_VERSION = 0x0101020c
    const val ATTR_TARGET_SDK_VERSION = 0x01010270
    const val ATTR_VERSION_CODE = 0x0101021b
    const val ATTR_VERSION_NAME = 0x0101021c
    const val ATTR_NAME = 0x01010003
    const val ATTR_LABEL = 0x01010001
    const val ATTR_EXPORTED = 0x01010010
}

/**
 * AXML Attribute Definition.
 */
data class AxmlAttribute(
    val uriIndex: Int,
    val nameIndex: Int,
    val valueStringIndex: Int,
    val type: Int,
    val data: Int
)

/**
 * Binary Output Stream helper for AXML Chunk Serializer.
 */
class AxmlOutputStream : ByteArrayOutputStream() {

    fun writeByte(v: Int) {
        write(v and 0xFF)
    }

    fun writeShort(v: Int) {
        write(v and 0xFF)
        write((v ushr 8) and 0xFF)
    }

    fun writeInt(v: Int) {
        write(v and 0xFF)
        write((v ushr 8) and 0xFF)
        write((v ushr 16) and 0xFF)
        write((v ushr 24) and 0xFF)
    }

    fun align4() {
        val rem = size() % 4
        if (rem != 0) {
            for (i in 0 until (4 - rem)) write(0)
        }
    }
}

/**
 * Pure Kotlin Binary XML (AXML) Writer (Phase 167).
 */
class AxmlWriter {

    private val strings = mutableListOf<String>()
    private val stringIndices = mutableMapOf<String, Int>()

    fun getStringIndex(s: String): Int =
        stringIndices.getOrPut(s) {
            val idx = strings.size
            strings.add(s)
            idx
        }

    /**
     * Builds binary AXML byte array given root element definition.
     */
    fun build(generator: (AxmlWriter, AxmlOutputStream) -> Unit): ByteArray {
        val bodyOut = AxmlOutputStream()
        generator(this, bodyOut)
        val bodyBytes = bodyOut.toByteArray()

        // 1. Build String Pool Chunk
        val stringPoolOut = AxmlOutputStream()
        val stringDataOut = AxmlOutputStream()
        val stringOffsets = IntArray(strings.size)

        for (i in strings.indices) {
            stringOffsets[i] = stringDataOut.size()
            val s = strings[i]
            val utf16 = s.toCharArray()
            stringDataOut.writeShort(utf16.size)
            for (ch in utf16) {
                stringDataOut.writeShort(ch.code)
            }
            stringDataOut.writeShort(0) // Null terminator
        }
        stringDataOut.align4()

        val stringPoolHeaderSize = 28
        val stringPoolTotalSize = stringPoolHeaderSize + (strings.size * 4) + stringDataOut.size()

        stringPoolOut.writeShort(AxmlConstants.RES_STRING_POOL_TYPE)
        stringPoolOut.writeShort(stringPoolHeaderSize)
        stringPoolOut.writeInt(stringPoolTotalSize)
        stringPoolOut.writeInt(strings.size) // string_count
        stringPoolOut.writeInt(0)            // style_count
        stringPoolOut.writeInt(0)            // flags (0 = UTF-16)
        stringPoolOut.writeInt(stringPoolHeaderSize + (strings.size * 4)) // strings_start
        stringPoolOut.writeInt(0)            // styles_start

        for (off in stringOffsets) {
            stringPoolOut.writeInt(off)
        }
        stringPoolOut.write(stringDataOut.toByteArray())
        val stringPoolBytes = stringPoolOut.toByteArray()

        // 2. Assemble Final AXML Chunk
        val totalFileSize = 8 + stringPoolBytes.size + bodyBytes.size
        val finalOut = AxmlOutputStream()
        finalOut.writeShort(AxmlConstants.RES_XML_TYPE)
        finalOut.writeShort(8) // header_size
        finalOut.writeInt(totalFileSize)
        finalOut.write(stringPoolBytes)
        finalOut.write(bodyBytes)

        return finalOut.toByteArray()
    }
}
