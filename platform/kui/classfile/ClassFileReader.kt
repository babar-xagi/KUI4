package kui.classfile

import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.File
import java.io.InputStream

/**
 * JVM Constant Pool Entry representations (Phase 152).
 */
sealed class CpInfo {
    data class Utf8(val value: String) : CpInfo()
    data class Integer(val value: Int) : CpInfo()
    data class Float(val value: kotlin.Float) : CpInfo()
    data class Long(val value: kotlin.Long) : CpInfo()
    data class Double(val value: kotlin.Double) : CpInfo()
    data class Class(val nameIndex: Int) : CpInfo()
    data class StringCp(val stringIndex: Int) : CpInfo()
    data class Fieldref(val classIndex: Int, val nameAndTypeIndex: Int) : CpInfo()
    data class Methodref(val classIndex: Int, val nameAndTypeIndex: Int) : CpInfo()
    data class InterfaceMethodref(val classIndex: Int, val nameAndTypeIndex: Int) : CpInfo()
    data class NameAndType(val nameIndex: Int, val descriptorIndex: Int) : CpInfo()
    data class MethodHandle(val referenceKind: Int, val referenceIndex: Int) : CpInfo()
    data class MethodType(val descriptorIndex: Int) : CpInfo()
    data class InvokeDynamic(val bootstrapMethodAttrIndex: Int, val nameAndTypeIndex: Int) : CpInfo()
    object Empty : CpInfo() // Used for slot following Long/Double
}

/**
 * JVM Field Representation (Phase 153).
 */
data class FieldInfo(
    val accessFlags: Int,
    val name: String,
    val descriptor: String,
    val attributes: Map<String, ByteArray>
)

/**
 * JVM Method Bytecode & Attributes (Phase 153).
 */
data class CodeAttribute(
    val maxStack: Int,
    val maxLocals: Int,
    val code: ByteArray,
    val exceptionTable: List<ExceptionEntry>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CodeAttribute
        return maxStack == other.maxStack && maxLocals == other.maxLocals && code.contentEquals(other.code)
    }

    override fun hashCode(): Int =
        (31 * maxStack + maxLocals) * 31 + code.contentHashCode()
}

data class ExceptionEntry(
    val startPc: Int,
    val endPc: Int,
    val handlerPc: Int,
    val catchType: Int
)

data class MethodInfo(
    val accessFlags: Int,
    val name: String,
    val descriptor: String,
    val codeAttribute: CodeAttribute?,
    val attributes: Map<String, ByteArray>
)

/**
 * Parsed JVM .class file representation (Phases 151–153).
 */
data class ClassFile(
    val minorVersion: Int,
    val majorVersion: Int,
    val constantPool: List<CpInfo?>,
    val accessFlags: Int,
    val thisClassName: String,
    val superClassName: String?,
    val interfaceNames: List<String>,
    val fields: List<FieldInfo>,
    val methods: List<MethodInfo>,
    val attributes: Map<String, ByteArray>
) {
    fun getUtf8(index: Int): String {
        val entry = constantPool.getOrNull(index)
        return (entry as? CpInfo.Utf8)?.value ?: ""
    }

    fun getClassName(classIndex: Int): String {
        val entry = constantPool.getOrNull(classIndex) as? CpInfo.Class
        return if (entry != null) getUtf8(entry.nameIndex) else ""
    }
}

/**
 * Pure Kotlin parser for standard JVM .class binaries (Phases 151–153, 165).
 */
object ClassFileReader {

    const val CLASS_MAGIC: Int = 0xCAFEBABE.toInt()

    fun read(bytes: ByteArray): ClassFile =
        read(ByteArrayInputStream(bytes))

    fun read(file: File): ClassFile =
        file.inputStream().use { read(it) }

    fun read(input: InputStream): ClassFile {
        val dis = DataInputStream(input)

        // 1. Magic check (Phase 151)
        val magic = dis.readInt()
        require(magic == CLASS_MAGIC) { "Invalid class magic: 0x${Integer.toHexString(magic).uppercase()}" }

        val minor = dis.readUnsignedShort()
        val major = dis.readUnsignedShort()

        // 2. Constant Pool (Phase 152)
        val cpCount = dis.readUnsignedShort()
        val cp = ArrayList<CpInfo?>(cpCount)
        cp.add(null) // CP is 1-indexed

        var i = 1
        while (i < cpCount) {
            val tag = dis.readUnsignedByte()
            when (tag) {
                1 -> { // Utf8
                    val len = dis.readUnsignedShort()
                    val b = ByteArray(len)
                    dis.readFully(b)
                    cp.add(CpInfo.Utf8(String(b, Charsets.UTF_8)))
                }
                3 -> cp.add(CpInfo.Integer(dis.readInt()))
                4 -> cp.add(CpInfo.Float(dis.readFloat()))
                5 -> { // Long (takes 2 slots)
                    cp.add(CpInfo.Long(dis.readLong()))
                    cp.add(CpInfo.Empty)
                    i++
                }
                6 -> { // Double (takes 2 slots)
                    cp.add(CpInfo.Double(dis.readDouble()))
                    cp.add(CpInfo.Empty)
                    i++
                }
                7 -> cp.add(CpInfo.Class(dis.readUnsignedShort()))
                8 -> cp.add(CpInfo.StringCp(dis.readUnsignedShort()))
                9 -> cp.add(CpInfo.Fieldref(dis.readUnsignedShort(), dis.readUnsignedShort()))
                10 -> cp.add(CpInfo.Methodref(dis.readUnsignedShort(), dis.readUnsignedShort()))
                11 -> cp.add(CpInfo.InterfaceMethodref(dis.readUnsignedShort(), dis.readUnsignedShort()))
                12 -> cp.add(CpInfo.NameAndType(dis.readUnsignedShort(), dis.readUnsignedShort()))
                15 -> cp.add(CpInfo.MethodHandle(dis.readUnsignedByte(), dis.readUnsignedShort()))
                16 -> cp.add(CpInfo.MethodType(dis.readUnsignedShort()))
                18 -> cp.add(CpInfo.InvokeDynamic(dis.readUnsignedShort(), dis.readUnsignedShort()))
                else -> throw IllegalArgumentException("Unknown CP tag: $tag at index $i")
            }
            i++
        }

        fun getUtf8Str(idx: Int): String =
            (cp.getOrNull(idx) as? CpInfo.Utf8)?.value ?: ""

        fun getClassStr(idx: Int): String {
            val cl = cp.getOrNull(idx) as? CpInfo.Class
            return if (cl != null) getUtf8Str(cl.nameIndex) else ""
        }

        // 3. Class Headers (Phase 151)
        val accessFlags = dis.readUnsignedShort()
        val thisClassIdx = dis.readUnsignedShort()
        val thisClassName = getClassStr(thisClassIdx)

        val superClassIdx = dis.readUnsignedShort()
        val superClassName = if (superClassIdx != 0) getClassStr(superClassIdx) else null

        val interfacesCount = dis.readUnsignedShort()
        val interfaces = (0 until interfacesCount).map {
            getClassStr(dis.readUnsignedShort())
        }

        // 4. Fields
        val fieldsCount = dis.readUnsignedShort()
        val fields = (0 until fieldsCount).map {
            val fFlags = dis.readUnsignedShort()
            val fName = getUtf8Str(dis.readUnsignedShort())
            val fDesc = getUtf8Str(dis.readUnsignedShort())
            val attrCount = dis.readUnsignedShort()
            val attrs = mutableMapOf<String, ByteArray>()
            for (a in 0 until attrCount) {
                val aName = getUtf8Str(dis.readUnsignedShort())
                val aLen = dis.readInt()
                val aData = ByteArray(aLen)
                dis.readFully(aData)
                attrs[aName] = aData
            }
            FieldInfo(fFlags, fName, fDesc, attrs)
        }

        // 5. Methods & Code (Phase 153)
        val methodsCount = dis.readUnsignedShort()
        val methods = (0 until methodsCount).map {
            val mFlags = dis.readUnsignedShort()
            val mName = getUtf8Str(dis.readUnsignedShort())
            val mDesc = getUtf8Str(dis.readUnsignedShort())
            val attrCount = dis.readUnsignedShort()
            val attrs = mutableMapOf<String, ByteArray>()
            var codeAttr: CodeAttribute? = null

            for (a in 0 until attrCount) {
                val aName = getUtf8Str(dis.readUnsignedShort())
                val aLen = dis.readInt()
                val aData = ByteArray(aLen)
                dis.readFully(aData)
                attrs[aName] = aData

                if (aName == "Code") {
                    val cDis = DataInputStream(ByteArrayInputStream(aData))
                    val maxStack = cDis.readUnsignedShort()
                    val maxLocals = cDis.readUnsignedShort()
                    val codeLen = cDis.readInt()
                    val codeBytes = ByteArray(codeLen)
                    cDis.readFully(codeBytes)

                    val exCount = cDis.readUnsignedShort()
                    val exTable = (0 until exCount).map {
                        ExceptionEntry(
                            startPc = cDis.readUnsignedShort(),
                            endPc = cDis.readUnsignedShort(),
                            handlerPc = cDis.readUnsignedShort(),
                            catchType = cDis.readUnsignedShort()
                        )
                    }
                    codeAttr = CodeAttribute(maxStack, maxLocals, codeBytes, exTable)
                }
            }
            MethodInfo(mFlags, mName, mDesc, codeAttr, attrs)
        }

        // 6. Class Attributes (Phase 165: kotlin.Metadata, SourceFile, etc.)
        val classAttrCount = dis.readUnsignedShort()
        val classAttrs = mutableMapOf<String, ByteArray>()
        for (a in 0 until classAttrCount) {
            val aName = getUtf8Str(dis.readUnsignedShort())
            val aLen = dis.readInt()
            val aData = ByteArray(aLen)
            dis.readFully(aData)
            classAttrs[aName] = aData
        }

        return ClassFile(
            minorVersion = minor,
            majorVersion = major,
            constantPool = cp,
            accessFlags = accessFlags,
            thisClassName = thisClassName,
            superClassName = superClassName,
            interfaceNames = interfaces,
            fields = fields,
            methods = methods,
            attributes = classAttrs
        )
    }
}
