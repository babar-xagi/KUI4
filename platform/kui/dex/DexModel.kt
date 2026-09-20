package kui.dex

import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.zip.Adler32

/**
 * DEX File Constants and Header Offsets (Phases 154–158).
 */
object DexConstants {
    val DEX_FILE_MAGIC = byteArrayOf(0x64, 0x65, 0x78, 0x0a, 0x30, 0x33, 0x35, 0x00) // "dex\n035\0"
    const val ENDIAN_CONSTANT = 0x12345678
    const val HEADER_SIZE = 112
    const val NO_INDEX = -1

    // Dalvik Opcodes
    const val OP_NOP = 0x00
    const val OP_RETURN_VOID = 0x0e
    const val OP_RETURN = 0x0f
    const val OP_RETURN_OBJECT = 0x11
    const val OP_CONST_4 = 0x12
    const val OP_CONST_16 = 0x13
    const val OP_CONST_STRING = 0x1a
    const val OP_NEW_INSTANCE = 0x22
    const val OP_GOTO = 0x28
    const val OP_IF_EQ = 0x32
    const val OP_IF_NE = 0x33
    const val OP_IGET_OBJECT = 0x54
    const val OP_IPUT_OBJECT = 0x5b
    const val OP_SGET_OBJECT = 0x62
    const val OP_SPUT_OBJECT = 0x69
    const val OP_INVOKE_VIRTUAL = 0x6e
    const val OP_INVOKE_SUPER = 0x6f
    const val OP_INVOKE_DIRECT = 0x70
    const val OP_INVOKE_STATIC = 0x71
    const val OP_INVOKE_INTERFACE = 0x72
}

/**
 * Encodes integers in ULEB128 format (Unsigned Little-Endian Base 128).
 */
fun ByteArrayOutputStream.writeUleb128(value: Int) {
    var v = value
    while (true) {
        val byteVal = v and 0x7F
        v = v ushr 7
        if (v == 0) {
            write(byteVal)
            break
        } else {
            write(byteVal or 0x80)
        }
    }
}

/**
 * Encodes a string into Modified UTF-8 (MUTF-8) bytes per Dalvik DEX specification (Phase 157).
 * Specifically encodes characters > 0xFFFF as UTF-16 surrogate pairs with two 3-byte sequences (0xED),
 * and U+0000 as 0xC0 0x80. Dalvik / ART strictly forbids 4-byte 0xF0 start bytes in DEX strings.
 */
fun encodeMutf8(s: String): ByteArray {
    val bos = ByteArrayOutputStream()
    for (i in 0 until s.length) {
        val c = s[i].code
        if (c in 0x0001..0x007F) {
            bos.write(c)
        } else if (c == 0 || c in 0x0080..0x07FF) {
            bos.write(0xC0 or ((c ushr 6) and 0x1F))
            bos.write(0x80 or (c and 0x3F))
        } else {
            // 0x0800..0xFFFF (including UTF-16 surrogate code units 0xD800..0xDFFF)
            bos.write(0xE0 or ((c ushr 12) and 0x0F))
            bos.write(0x80 or ((c ushr 6) and 0x3F))
            bos.write(0x80 or (c and 0x3F))
        }
    }
    return bos.toByteArray()
}

/**
 * Binary Output Stream helper for writing little-endian primitives.
 */
class DexOutputStream : ByteArrayOutputStream() {

    fun writeUByte(value: Int) {
        write(value and 0xFF)
    }

    fun writeUShort(value: Int) {
        write(value and 0xFF)
        write((value ushr 8) and 0xFF)
    }

    fun writeUInt(value: Int) {
        write(value and 0xFF)
        write((value ushr 8) and 0xFF)
        write((value ushr 16) and 0xFF)
        write((value ushr 24) and 0xFF)
    }

    fun align(boundary: Int) {
        val rem = size() % boundary
        if (rem != 0) {
            val pad = boundary - rem
            for (i in 0 until pad) {
                write(0)
            }
        }
    }
}

/**
 * Symbolic reference fixups for Dalvik instructions (Phases 158–163).
 * Enables instructions to reference method IDs, type IDs, and string IDs
 * that are resolved and patched at DEX file layout time.
 */
sealed class DexInstructionFixup {
    data class MethodRef(
        val offsetInInstructions: Int,
        val classDescriptor: String,
        val name: String,
        val returnType: String,
        val parameterTypes: List<String> = emptyList()
    ) : DexInstructionFixup()

    data class TypeRef(
        val offsetInInstructions: Int,
        val typeDescriptor: String
    ) : DexInstructionFixup()

    data class StringRef(
        val offsetInInstructions: Int,
        val string: String
    ) : DexInstructionFixup()
}

/**
 * Computes the Dalvik shorty descriptor for a method signature.
 */
fun computeShorty(returnType: String, parameterTypes: List<String>): String {
    val shorty = StringBuilder()
    shorty.append(if (returnType.startsWith("L") || returnType.startsWith("[")) 'L' else returnType.first())
    for (p in parameterTypes) {
        shorty.append(if (p.startsWith("L") || p.startsWith("[")) 'L' else p.first())
    }
    return shorty.toString()
}

/**
 * In-memory DEX method definition with Dalvik instructions (Phase 158).
 */
data class DexMethod(
    val classDescriptor: String,
    val name: String,
    val returnType: String,
    val parameterTypes: List<String>,
    val accessFlags: Int,
    val isDirect: Boolean,
    val registersSize: Int,
    val insSize: Int,
    val outsSize: Int,
    val instructions: ShortArray,
    val instructionFixups: List<DexInstructionFixup> = emptyList()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as DexMethod
        return classDescriptor == other.classDescriptor && name == other.name && returnType == other.returnType &&
                parameterTypes == other.parameterTypes && accessFlags == other.accessFlags &&
                isDirect == other.isDirect && registersSize == other.registersSize &&
                insSize == other.insSize && outsSize == other.outsSize &&
                instructions.contentEquals(other.instructions) &&
                instructionFixups == other.instructionFixups
    }

    override fun hashCode(): Int =
        31 * (31 * (classDescriptor.hashCode() + name.hashCode()) + instructions.contentHashCode()) + instructionFixups.hashCode()
}

/**
 * In-memory DEX class definition (Phase 159).
 */
data class DexClass(
    val classDescriptor: String,
    val superclassDescriptor: String = "Ljava/lang/Object;",
    val interfaceDescriptors: List<String> = emptyList(),
    val accessFlags: Int = 1, // ACC_PUBLIC
    val sourceFile: String? = null,
    val directMethods: List<DexMethod> = emptyList(),
    val virtualMethods: List<DexMethod> = emptyList()
)

/**
 * Pure Kotlin DEX file builder and serializer (Phases 154–160).
 */
class DexFileBuilder {

    private val classes = mutableListOf<DexClass>()

    fun addClass(dexClass: DexClass) {
        classes.add(dexClass)
    }

    /**
     * Builds and validates a complete binary classes.dex byte array.
     */
    fun build(): ByteArray {
        // 1. Collect all unique strings
        val strings = sortedSetOf<String>()
        // Mandatory types and descriptors
        strings.add("V")
        strings.add("Ljava/lang/Object;")

        for (c in classes) {
            strings.add(c.classDescriptor)
            strings.add(c.superclassDescriptor)
            c.sourceFile?.let { strings.add(it) }
            for (iface in c.interfaceDescriptors) strings.add(iface)

            for (m in c.directMethods + c.virtualMethods) {
                strings.add(m.name)
                strings.add(m.returnType)
                for (p in m.parameterTypes) strings.add(p)
                strings.add(computeShorty(m.returnType, m.parameterTypes))

                for (fixup in m.instructionFixups) {
                    when (fixup) {
                        is DexInstructionFixup.MethodRef -> {
                            strings.add(fixup.classDescriptor)
                            strings.add(fixup.name)
                            strings.add(fixup.returnType)
                            for (p in fixup.parameterTypes) strings.add(p)
                            strings.add(computeShorty(fixup.returnType, fixup.parameterTypes))
                        }
                        is DexInstructionFixup.TypeRef -> {
                            strings.add(fixup.typeDescriptor)
                        }
                        is DexInstructionFixup.StringRef -> {
                            strings.add(fixup.string)
                        }
                    }
                }
            }
        }

        val stringList = strings.toList()
        val stringMap = stringList.mapIndexed { idx, s -> s to idx }.toMap()

        // 2. Collect unique types
        val types = sortedSetOf<String>()
        types.add("V")
        types.add("Ljava/lang/Object;")
        for (c in classes) {
            types.add(c.classDescriptor)
            types.add(c.superclassDescriptor)
            for (iface in c.interfaceDescriptors) types.add(iface)
            for (m in c.directMethods + c.virtualMethods) {
                types.add(m.returnType)
                for (p in m.parameterTypes) types.add(p)
                for (fixup in m.instructionFixups) {
                    when (fixup) {
                        is DexInstructionFixup.MethodRef -> {
                            types.add(fixup.classDescriptor)
                            types.add(fixup.returnType)
                            for (p in fixup.parameterTypes) types.add(p)
                        }
                        is DexInstructionFixup.TypeRef -> {
                            types.add(fixup.typeDescriptor)
                        }
                        else -> {}
                    }
                }
            }
        }
        val typeList = types.toList()
        val typeMap = typeList.mapIndexed { idx, t -> t to idx }.toMap()

        // 3. Collect unique method prototypes
        data class ProtoKey(val shorty: String, val returnType: String, val params: List<String>) : Comparable<ProtoKey> {
            override fun compareTo(other: ProtoKey): Int {
                val r = returnType.compareTo(other.returnType)
                if (r != 0) return r
                return params.joinToString(",").compareTo(other.params.joinToString(","))
            }
        }

        val protos = sortedSetOf<ProtoKey>()
        for (c in classes) {
            for (m in c.directMethods + c.virtualMethods) {
                protos.add(ProtoKey(computeShorty(m.returnType, m.parameterTypes), m.returnType, m.parameterTypes))
                for (fixup in m.instructionFixups) {
                    if (fixup is DexInstructionFixup.MethodRef) {
                        protos.add(ProtoKey(computeShorty(fixup.returnType, fixup.parameterTypes), fixup.returnType, fixup.parameterTypes))
                    }
                }
            }
        }
        val protoList = protos.toList()
        val protoMap = protoList.mapIndexed { idx, pr -> pr to idx }.toMap()

        // 4. Collect unique method IDs
        data class MethodIdKey(val classDesc: String, val name: String, val proto: ProtoKey) : Comparable<MethodIdKey> {
            override fun compareTo(other: MethodIdKey): Int {
                val c = classDesc.compareTo(other.classDesc)
                if (c != 0) return c
                val n = name.compareTo(other.name)
                if (n != 0) return n
                return proto.compareTo(other.proto)
            }
        }

        val methodIds = sortedSetOf<MethodIdKey>()
        for (c in classes) {
            for (m in c.directMethods + c.virtualMethods) {
                val proto = ProtoKey(computeShorty(m.returnType, m.parameterTypes), m.returnType, m.parameterTypes)
                methodIds.add(MethodIdKey(m.classDescriptor, m.name, proto))
                for (fixup in m.instructionFixups) {
                    if (fixup is DexInstructionFixup.MethodRef) {
                        val fProto = ProtoKey(computeShorty(fixup.returnType, fixup.parameterTypes), fixup.returnType, fixup.parameterTypes)
                        methodIds.add(MethodIdKey(fixup.classDescriptor, fixup.name, fProto))
                    }
                }
            }
        }
        val methodIdList = methodIds.toList()
        val methodIdMap = methodIdList.mapIndexed { idx, m -> m to idx }.toMap()

        // 5. Layout offsets calculation
        val headerSize = DexConstants.HEADER_SIZE
        val stringIdsSize = stringList.size
        val stringIdsOff = headerSize

        val typeIdsSize = typeList.size
        val typeIdsOff = stringIdsOff + (stringIdsSize * 4)

        val protoIdsSize = protoList.size
        val protoIdsOff = typeIdsOff + (typeIdsSize * 4)

        val fieldIdsSize = 0
        val fieldIdsOff = protoIdsOff + (protoIdsSize * 12)

        val methodIdsSize = methodIdList.size
        val methodIdsOff = fieldIdsOff + (fieldIdsSize * 8)

        val classDefsSize = classes.size
        val classDefsOff = methodIdsOff + (methodIdsSize * 8)

        val dataStart = classDefsOff + (classDefsSize * 32)

        // 6. Write Data Section (Strings data, Type lists, Code items, Class data)
        val dataOut = DexOutputStream()
        val stringDataOffsets = IntArray(stringList.size)

        for (i in stringList.indices) {
            dataOut.align(1)
            stringDataOffsets[i] = dataStart + dataOut.size()
            val s = stringList[i]
            val mutf8Bytes = encodeMutf8(s)
            dataOut.writeUleb128(s.length)
            dataOut.write(mutf8Bytes)
            dataOut.write(0) // Null-terminator
        }

        val protoParamsOffsets = IntArray(protoList.size)
        for (i in protoList.indices) {
            val params = protoList[i].params
            if (params.isEmpty()) {
                protoParamsOffsets[i] = 0
            } else {
                dataOut.align(4)
                protoParamsOffsets[i] = dataStart + dataOut.size()
                dataOut.writeUInt(params.size)
                for (p in params) {
                    dataOut.writeUShort(typeMap[p] ?: 0)
                }
            }
        }

        // Write Code Items
        data class CodeItemRef(val codeOffset: Int)
        val methodCodeOffsets = mutableMapOf<DexMethod, Int>()

        for (c in classes) {
            for (m in c.directMethods + c.virtualMethods) {
                dataOut.align(4)
                val codeOff = dataStart + dataOut.size()
                methodCodeOffsets[m] = codeOff

                dataOut.writeUShort(m.registersSize)
                dataOut.writeUShort(m.insSize)
                dataOut.writeUShort(m.outsSize)
                dataOut.writeUShort(0) // tries_size
                dataOut.writeUInt(0)   // debug_info_off
                dataOut.writeUInt(m.instructions.size)

                // Apply instruction fixups (symbolic method, type, string resolution)
                val patchedInsns = m.instructions.copyOf()
                for (fixup in m.instructionFixups) {
                    when (fixup) {
                        is DexInstructionFixup.MethodRef -> {
                            val fProto = ProtoKey(computeShorty(fixup.returnType, fixup.parameterTypes), fixup.returnType, fixup.parameterTypes)
                            val key = MethodIdKey(fixup.classDescriptor, fixup.name, fProto)
                            val idx = methodIdMap[key] ?: error("Method ID not found: $key")
                            patchedInsns[fixup.offsetInInstructions] = idx.toShort()
                        }
                        is DexInstructionFixup.TypeRef -> {
                            val idx = typeMap[fixup.typeDescriptor] ?: error("Type ID not found: ${fixup.typeDescriptor}")
                            patchedInsns[fixup.offsetInInstructions] = idx.toShort()
                        }
                        is DexInstructionFixup.StringRef -> {
                            val idx = stringMap[fixup.string] ?: error("String ID not found: ${fixup.string}")
                            patchedInsns[fixup.offsetInInstructions] = idx.toShort()
                        }
                    }
                }

                for (insn in patchedInsns) {
                    dataOut.writeUShort(insn.toInt() and 0xFFFF)
                }
            }
        }

        // Write Class Data Items
        val classDataOffsets = IntArray(classes.size)
        for (i in classes.indices) {
            val c = classes[i]
            dataOut.align(1)
            classDataOffsets[i] = dataStart + dataOut.size()

            dataOut.writeUleb128(0) // static_fields_size
            dataOut.writeUleb128(0) // instance_fields_size
            dataOut.writeUleb128(c.directMethods.size)
            dataOut.writeUleb128(c.virtualMethods.size)

            val sortedDirect = c.directMethods.sortedBy { m ->
                val proto = ProtoKey(computeShorty(m.returnType, m.parameterTypes), m.returnType, m.parameterTypes)
                methodIdMap[MethodIdKey(m.classDescriptor, m.name, proto)] ?: 0
            }

            var lastMethodIdx = 0
            for (m in sortedDirect) {
                val proto = ProtoKey(computeShorty(m.returnType, m.parameterTypes), m.returnType, m.parameterTypes)
                val mIdx = methodIdMap[MethodIdKey(m.classDescriptor, m.name, proto)] ?: 0
                val diff = mIdx - lastMethodIdx
                lastMethodIdx = mIdx

                dataOut.writeUleb128(diff)
                dataOut.writeUleb128(m.accessFlags)
                dataOut.writeUleb128(methodCodeOffsets[m] ?: 0)
            }

            val sortedVirtual = c.virtualMethods.sortedBy { m ->
                val proto = ProtoKey(computeShorty(m.returnType, m.parameterTypes), m.returnType, m.parameterTypes)
                methodIdMap[MethodIdKey(m.classDescriptor, m.name, proto)] ?: 0
            }

            lastMethodIdx = 0
            for (m in sortedVirtual) {
                val proto = ProtoKey(computeShorty(m.returnType, m.parameterTypes), m.returnType, m.parameterTypes)
                val mIdx = methodIdMap[MethodIdKey(m.classDescriptor, m.name, proto)] ?: 0
                val diff = mIdx - lastMethodIdx
                lastMethodIdx = mIdx

                dataOut.writeUleb128(diff)
                dataOut.writeUleb128(m.accessFlags)
                dataOut.writeUleb128(methodCodeOffsets[m] ?: 0)
            }
        }

        // 6b. Build and Append map_list (0x1000)
        dataOut.align(4)
        val mapOff = dataStart + dataOut.size()

        data class MapItem(val type: Int, val size: Int, val offset: Int)
        val mapItems = mutableListOf<MapItem>()

        mapItems.add(MapItem(0x0000, 1, 0)) // TYPE_HEADER_ITEM
        if (stringIdsSize > 0) mapItems.add(MapItem(0x0001, stringIdsSize, stringIdsOff)) // TYPE_STRING_ID_ITEM
        if (typeIdsSize > 0) mapItems.add(MapItem(0x0002, typeIdsSize, typeIdsOff)) // TYPE_TYPE_ID_ITEM
        if (protoIdsSize > 0) mapItems.add(MapItem(0x0003, protoIdsSize, protoIdsOff)) // TYPE_PROTO_ID_ITEM
        if (methodIdsSize > 0) mapItems.add(MapItem(0x0005, methodIdsSize, methodIdsOff)) // TYPE_METHOD_ID_ITEM
        if (classDefsSize > 0) mapItems.add(MapItem(0x0006, classDefsSize, classDefsOff)) // TYPE_CLASS_DEF_ITEM

        // Data section items
        if (stringList.isNotEmpty()) {
            mapItems.add(MapItem(0x2002, stringList.size, stringDataOffsets[0])) // TYPE_STRING_DATA_ITEM
        }
        val nonZeroProtoParams = protoParamsOffsets.filter { it != 0 }
        if (nonZeroProtoParams.isNotEmpty()) {
            mapItems.add(MapItem(0x1001, nonZeroProtoParams.size, nonZeroProtoParams.minOrNull() ?: 0)) // TYPE_TYPE_LIST
        }
        if (methodCodeOffsets.isNotEmpty()) {
            mapItems.add(MapItem(0x2001, methodCodeOffsets.size, methodCodeOffsets.values.minOrNull() ?: 0)) // TYPE_CODE_ITEM
        }
        if (classDataOffsets.isNotEmpty()) {
            mapItems.add(MapItem(0x2000, classDataOffsets.size, classDataOffsets[0])) // TYPE_CLASS_DATA_ITEM
        }
        mapItems.add(MapItem(0x1000, 1, mapOff)) // TYPE_MAP_LIST

        mapItems.sortBy { it.offset }

        dataOut.writeUInt(mapItems.size)
        for (item in mapItems) {
            dataOut.writeUShort(item.type)
            dataOut.writeUShort(0)
            dataOut.writeUInt(item.size)
            dataOut.writeUInt(item.offset)
        }

        val dataBytes = dataOut.toByteArray()
        val totalFileSize = dataStart + dataBytes.size

        // 7. Assemble Complete Binary Output
        val out = DexOutputStream()

        // Placeholder Header (112 bytes)
        out.write(DexConstants.DEX_FILE_MAGIC) // 8 bytes
        out.writeUInt(0) // Checksum (bytes 8..11) placeholder
        out.write(ByteArray(20)) // SHA-1 signature (bytes 12..31) placeholder
        out.writeUInt(totalFileSize) // file_size
        out.writeUInt(DexConstants.HEADER_SIZE) // header_size
        out.writeUInt(DexConstants.ENDIAN_CONSTANT) // endian_tag
        out.writeUInt(0) // link_size
        out.writeUInt(0) // link_off
        out.writeUInt(mapOff) // map_off (points to map_list in data section)
        out.writeUInt(stringIdsSize)
        out.writeUInt(if (stringIdsSize == 0) 0 else stringIdsOff)
        out.writeUInt(typeIdsSize)
        out.writeUInt(if (typeIdsSize == 0) 0 else typeIdsOff)
        out.writeUInt(protoIdsSize)
        out.writeUInt(if (protoIdsSize == 0) 0 else protoIdsOff)
        out.writeUInt(fieldIdsSize)
        out.writeUInt(if (fieldIdsSize == 0) 0 else fieldIdsOff)
        out.writeUInt(methodIdsSize)
        out.writeUInt(if (methodIdsSize == 0) 0 else methodIdsOff)
        out.writeUInt(classDefsSize)
        out.writeUInt(if (classDefsSize == 0) 0 else classDefsOff)
        out.writeUInt(dataBytes.size) // data_size
        out.writeUInt(if (dataBytes.isEmpty()) 0 else dataStart) // data_off

        // Write String IDs
        for (off in stringDataOffsets) {
            out.writeUInt(off)
        }

        // Write Type IDs
        for (t in typeList) {
            out.writeUInt(stringMap[t] ?: 0)
        }

        // Write Proto IDs (12 bytes each)
        for (i in protoList.indices) {
            val pr = protoList[i]
            out.writeUInt(stringMap[pr.shorty] ?: 0)
            out.writeUInt(typeMap[pr.returnType] ?: 0)
            out.writeUInt(protoParamsOffsets[i])
        }

        // Write Method IDs (8 bytes each)
        for (m in methodIdList) {
            out.writeUShort(typeMap[m.classDesc] ?: 0)
            out.writeUShort(protoMap[m.proto] ?: 0)
            out.writeUInt(stringMap[m.name] ?: 0)
        }

        // Write Class Defs (32 bytes each)
        for (i in classes.indices) {
            val c = classes[i]
            out.writeUInt(typeMap[c.classDescriptor] ?: 0)
            out.writeUInt(c.accessFlags)
            out.writeUInt(typeMap[c.superclassDescriptor] ?: 0)
            out.writeUInt(0) // interfaces_off
            out.writeUInt(if (c.sourceFile != null) (stringMap[c.sourceFile] ?: DexConstants.NO_INDEX) else DexConstants.NO_INDEX)
            out.writeUInt(0) // annotations_off
            out.writeUInt(classDataOffsets[i])
            out.writeUInt(0) // static_values_off
        }

        // Write Data Section
        out.write(dataBytes)

        val raw = out.toByteArray()

        // 8. Compute and Patch SHA-1 Signature (bytes 12..31)
        val md = MessageDigest.getInstance("SHA-1")
        md.update(raw, 32, raw.size - 32)
        val sha1 = md.digest()
        System.arraycopy(sha1, 0, raw, 12, 20)

        // 9. Compute and Patch Adler-32 Checksum (bytes 8..11)
        val adler = Adler32()
        adler.update(raw, 12, raw.size - 12)
        val checksum = adler.value.toInt()
        raw[8] = (checksum and 0xFF).toByte()
        raw[9] = ((checksum ushr 8) and 0xFF).toByte()
        raw[10] = ((checksum ushr 16) and 0xFF).toByte()
        raw[11] = ((checksum ushr 24) and 0xFF).toByte()

        return raw
    }
}
