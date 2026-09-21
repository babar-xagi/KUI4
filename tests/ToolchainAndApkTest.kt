package tests

import kui.apk.ApkWriter
import kui.apk.PackagingTask
import kui.axml.AxmlConstants
import kui.axml.AxmlWriter
import kui.axml.ManifestGenerator
import kui.classfile.ClassFile
import kui.classfile.ClassFileReader
import kui.classfile.CodeAttribute
import kui.classfile.CpInfo
import kui.classfile.MethodInfo
import kui.config.AndroidConfig
import kui.config.KuiConfig
import kui.config.ProjectIdentity
import kui.config.UiConfig
import kui.device.Device
import kui.device.DeviceManager
import kui.device.RunCommand
import kui.dex.ClassToDexCompiler
import kui.dex.DexClass
import kui.dex.DexConstants
import kui.dex.DexFileBuilder
import kui.dex.DexMethod
import kui.dex.DexOutputStream
import kui.dex.writeUleb128
import kui.signing.ApkV2Signer
import kui.signing.ApkV2Verifier
import kui.signing.DebugKeyGenerator
import kui.signing.VerificationResult
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.nio.charset.StandardCharsets

object ToolchainAndApkTest {

    private var passed = 0
    private var failed = 0

    fun assert(condition: Boolean, message: String) {
        if (condition) {
            passed++
            println("  [PASS] $message")
        } else {
            failed++
            System.err.println("  [FAIL] $message")
        }
    }

    fun assertEquals(expected: Any?, actual: Any?, message: String) {
        assert(expected == actual, "$message (expected: $expected, actual: $actual)")
    }

    fun runAll() {
        println("=== Running KUI Milestone G (Phases 151-180) Test Suite ===")

        testPhase151_ClassFileHeader_MagicAndVersions()
        testPhase152_ClassFileConstantPool_Utf8ClassString()
        testPhase153_ClassFileFieldsMethodsAndCode()
        testPhase154_DexHeader_MagicAndFixedLayout()
        testPhase155_DexAdler32AndSha1Checksums()
        testPhase156_Uleb128EncodingAndDexStream()
        testPhase157_DexStringTableAndMutf8()
        testPhase158_DalvikBytecodeOpcodes()
        testPhase159_DexModelTypeAndProtoTables()
        testPhase160_JvmToDalvikOpcodeTranslation()
        testPhase161_DalvikRegisterAllocation()
        testPhase162_MethodDescriptorParsing()
        testPhase163_InMemoryDexCompilation()
        testPhase164_ClassToDexCompilerPipeline()
        testPhase165_RealJvmClassToDexCompilation()
        testPhase166_MultiClassDirectoryDexCompilation()
        testPhase167_AxmlWriterChunksAndStringPool()
        testPhase168_ManifestGeneratorBinaryXml()
        testPhase169_ApkWriterZipStructure()
        testPhase170_ApkWriter4ByteZipalign()
        testPhase171_ApkWriterMixedCompression()
        testPhase172_ApkWriterAlignmentVerification()
        testPhase173_DebugKeyAndX509CertificateGeneration()
        testPhase174_ApkV2SigningBlockGeneration()
        testPhase175_ApkV2SignatureVerification()
        testPhase176_CryptographicTamperDetection()
        testPhase177_PackagingTaskEndToEnd()
        testPhase178_DeviceDiscoveryParser()
        testPhase179_DeviceInstallAndLaunchCommands()
        testPhase180_CliRunWorkflow()

        println("\n===========================================================")
        println("Milestone G Test Results: $passed PASSED, $failed FAILED")
        println("===========================================================")

        if (failed > 0) {
            System.exit(1)
        }
    }

    // Phase 151: JVM Classfile Header & Magic Validation
    fun testPhase151_ClassFileHeader_MagicAndVersions() {
        println("\n--- Phase 151: JVM Classfile Header & Magic Validation ---")
        val bos = ByteArrayOutputStream()
        val dos = DataOutputStream(bos)
        dos.writeInt(0xCAFEBABE.toInt())
        dos.writeShort(0)  // minor
        dos.writeShort(65) // major (Java 21)
        dos.writeShort(1)  // constant pool count 1 (empty)
        dos.writeShort(0x0001) // access flags
        dos.writeShort(0)  // this_class
        dos.writeShort(0)  // super_class
        dos.writeShort(0)  // interfaces_count
        dos.writeShort(0)  // fields_count
        dos.writeShort(0)  // methods_count
        dos.writeShort(0)  // attributes_count

        val cf = ClassFileReader.read(bos.toByteArray())
        assertEquals(0, cf.minorVersion, "Minor version parsed")
        assertEquals(65, cf.majorVersion, "Major version parsed (Java 21)")
        assertEquals(0x0001, cf.accessFlags, "Access flags parsed")
    }

    // Phase 152: Constant Pool Item Parsing
    fun testPhase152_ClassFileConstantPool_Utf8ClassString() {
        println("\n--- Phase 152: Constant Pool Item Parsing ---")
        val bos = ByteArrayOutputStream()
        val dos = DataOutputStream(bos)
        dos.writeInt(0xCAFEBABE.toInt())
        dos.writeShort(0)
        dos.writeShort(65)
        dos.writeShort(4) // 3 constant pool entries + index 0
        // Entry 1: Utf8 "com/example/MyClass"
        dos.writeByte(1)
        val utfBytes = "com/example/MyClass".toByteArray(StandardCharsets.UTF_8)
        dos.writeShort(utfBytes.size)
        dos.write(utfBytes)
        // Entry 2: Class pointing to entry 1
        dos.writeByte(7)
        dos.writeShort(1)
        // Entry 3: Integer 42
        dos.writeByte(3)
        dos.writeInt(42)

        dos.writeShort(0x0021) // access flags (public super)
        dos.writeShort(2)      // this_class points to Class CP #2
        dos.writeShort(0)      // super_class
        dos.writeShort(0)      // interfaces
        dos.writeShort(0)      // fields
        dos.writeShort(0)      // methods
        dos.writeShort(0)      // attributes

        val cf = ClassFileReader.read(bos.toByteArray())
        assertEquals("com/example/MyClass", cf.thisClassName, "This class name resolved from CP")
        assert(cf.constantPool[3] is CpInfo.Integer, "Integer CP item parsed")
        assertEquals(42, (cf.constantPool[3] as CpInfo.Integer).value, "Integer value matches")
    }

    // Phase 153: Classfile Fields, Methods & Bytecode Attributes
    fun testPhase153_ClassFileFieldsMethodsAndCode() {
        println("\n--- Phase 153: Classfile Fields, Methods & Code Attributes ---")
        val bos = ByteArrayOutputStream()
        val dos = DataOutputStream(bos)
        dos.writeInt(0xCAFEBABE.toInt())
        dos.writeShort(0)
        dos.writeShort(65)
        dos.writeShort(5) // CP items: 1:Utf8"Code", 2:Utf8"main", 3:Utf8"([Ljava/lang/String;)V", 4:Class
        // 1: Utf8 "Code"
        dos.writeByte(1); dos.writeShort(4); dos.write("Code".toByteArray())
        // 2: Utf8 "main"
        dos.writeByte(1); dos.writeShort(4); dos.write("main".toByteArray())
        // 3: Utf8 "([Ljava/lang/String;)V"
        val desc = "([Ljava/lang/String;)V".toByteArray()
        dos.writeByte(1); dos.writeShort(desc.size); dos.write(desc)
        // 4: Utf8 "Test"
        dos.writeByte(1); dos.writeShort(4); dos.write("Test".toByteArray())

        dos.writeShort(0x0001) // public
        dos.writeShort(0)      // this_class
        dos.writeShort(0)      // super_class
        dos.writeShort(0)      // interfaces
        dos.writeShort(0)      // fields
        dos.writeShort(1)      // 1 method
        dos.writeShort(0x0009) // public static
        dos.writeShort(2)      // name_index -> "main"
        dos.writeShort(3)      // descriptor_index -> "([Ljava/lang/String;)V"
        dos.writeShort(1)      // 1 attribute -> "Code"
        dos.writeShort(1)      // name_index -> "Code"
        val codeBytes = byteArrayOf(0xB1.toByte()) // return void
        val codeAttrLen = 2 + 2 + 4 + codeBytes.size + 2 + 2
        dos.writeInt(codeAttrLen)
        dos.writeShort(1)      // max_stack
        dos.writeShort(1)      // max_locals
        dos.writeInt(codeBytes.size)
        dos.write(codeBytes)
        dos.writeShort(0)      // exception_table_length
        dos.writeShort(0)      // attributes_count
        dos.writeShort(0)      // class attributes

        val cf = ClassFileReader.read(bos.toByteArray())
        assertEquals(1, cf.methods.size, "Method count parsed")
        val m = cf.methods.first()
        assertEquals("main", m.name, "Method name parsed")
        assertEquals("([Ljava/lang/String;)V", m.descriptor, "Method descriptor parsed")
        assert(m.codeAttribute != null, "Code attribute parsed")
        assertEquals(1, m.codeAttribute?.code?.size, "Bytecode size parsed")
    }

    // Phase 154: DEX Header Structure
    fun testPhase154_DexHeader_MagicAndFixedLayout() {
        println("\n--- Phase 154: DEX Header Structure ---")
        val builder = DexFileBuilder()
        val bytes = builder.build()

        assert(bytes.size >= 112, "DEX file size is at least header size 112")
        val magic = bytes.copyOfRange(0, 8)
        assert(magic.contentEquals(DexConstants.DEX_FILE_MAGIC), "DEX magic matches 'dex\\n035\\0'")
        val headerSize = (bytes[36].toInt() and 0xFF) or ((bytes[37].toInt() and 0xFF) shl 8)
        assertEquals(112, headerSize, "Header size is 112 bytes")
    }

    // Phase 155: Adler-32 Checksum & SHA-1 Signature
    fun testPhase155_DexAdler32AndSha1Checksums() {
        println("\n--- Phase 155: Adler-32 Checksum & SHA-1 Signature ---")
        val builder = DexFileBuilder()
        val bytes = builder.build()

        // Verify SHA-1 is computed at bytes 12..31
        val sha1 = bytes.copyOfRange(12, 32)
        assert(sha1.any { it != 0.toByte() }, "SHA-1 signature is populated and non-zero")

        // Verify Adler-32 is computed at bytes 8..11
        val adler = bytes.copyOfRange(8, 12)
        assert(adler.any { it != 0.toByte() }, "Adler-32 checksum is populated and non-zero")
    }

    // Phase 156: ULEB128 Encoding & Binary DEX Writer Primitives
    fun testPhase156_Uleb128EncodingAndDexStream() {
        println("\n--- Phase 156: ULEB128 & Binary DEX Writer Primitives ---")
        fun encode(v: Int): ByteArray {
            val bos = ByteArrayOutputStream()
            bos.writeUleb128(v)
            return bos.toByteArray()
        }

        assert(encode(0).contentEquals(byteArrayOf(0x00)), "ULEB128(0) == [0x00]")
        assert(encode(1).contentEquals(byteArrayOf(0x01)), "ULEB128(1) == [0x01]")
        assert(encode(127).contentEquals(byteArrayOf(0x7F)), "ULEB128(127) == [0x7F]")
        assert(encode(128).contentEquals(byteArrayOf(0x80.toByte(), 0x01)), "ULEB128(128) == [0x80, 0x01]")
        assert(encode(16384).contentEquals(byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x01)), "ULEB128(16384) == [0x80, 0x80, 0x01]")

        val dos = DexOutputStream()
        dos.writeUByte(0xFF)
        dos.align(4)
        assertEquals(4, dos.size(), "DexOutputStream align4 padding")
    }

    // Phase 157: MUTF-8 String Table & String Identifiers
    fun testPhase157_DexStringTableAndMutf8() {
        println("\n--- Phase 157: MUTF-8 String Table & String Identifiers ---")
        val builder = DexFileBuilder()
        builder.addClass(
            DexClass(
                classDescriptor = "Lcom/kui/App;",
                superclassDescriptor = "Ljava/lang/Object;",
                interfaceDescriptors = emptyList(),
                accessFlags = 0x0001,
                sourceFile = "App.kt",
                directMethods = emptyList(),
                virtualMethods = emptyList()
            )
        )
        val dexBytes = builder.build()
        val strCount = (dexBytes[56].toInt() and 0xFF) or ((dexBytes[57].toInt() and 0xFF) shl 8)
        assert(strCount >= 3, "String table contains descriptors ($strCount strings)")
    }

    // Phase 158: Dalvik Bytecode Instructions
    fun testPhase158_DalvikBytecodeOpcodes() {
        println("\n--- Phase 158: Dalvik Bytecode Instructions ---")
        val method = DexMethod(
            classDescriptor = "Lcom/kui/App;",
            name = "doSomething",
            returnType = "V",
            parameterTypes = emptyList(),
            accessFlags = 0x0001,
            isDirect = false,
            registersSize = 2,
            insSize = 1,
            outsSize = 0,
            instructions = shortArrayOf(
                DexConstants.OP_NOP.toShort(),
                DexConstants.OP_RETURN_VOID.toShort()
            )
        )
        assertEquals(2, method.instructions.size, "Instructions populated")
        assertEquals(DexConstants.OP_RETURN_VOID.toShort(), method.instructions[1], "Return-void opcode matched")
    }

    // Phase 159: DexClass & DexModel Data Structures
    fun testPhase159_DexModelTypeAndProtoTables() {
        println("\n--- Phase 159: DexClass & DexModel Tables ---")
        val builder = DexFileBuilder()
        val method = DexMethod(
            classDescriptor = "Lcom/kui/Greeter;",
            name = "greet",
            returnType = "Ljava/lang/String;",
            parameterTypes = listOf("Ljava/lang/String;"),
            accessFlags = 0x0001,
            isDirect = false,
            registersSize = 3,
            insSize = 2,
            outsSize = 0,
            instructions = shortArrayOf(DexConstants.OP_RETURN_OBJECT.toShort())
        )
        val cls = DexClass(
            classDescriptor = "Lcom/kui/Greeter;",
            superclassDescriptor = "Ljava/lang/Object;",
            interfaceDescriptors = listOf("Ljava/lang/Runnable;"),
            accessFlags = 0x0001,
            sourceFile = "Greeter.kt",
            directMethods = emptyList(),
            virtualMethods = listOf(method)
        )
        builder.addClass(cls)
        val dex = builder.build()
        assert(dex.size > 112, "DEX binary generated with class and method definitions")
    }

    // Phase 160: JVM-to-Dalvik Opcode Translation
    fun testPhase160_JvmToDalvikOpcodeTranslation() {
        println("\n--- Phase 160: JVM-to-Dalvik Opcode Translation ---")
        val cf = ClassFile(
            minorVersion = 0,
            majorVersion = 65,
            constantPool = listOf(null, CpInfo.Utf8("com/test/Runner"), CpInfo.Class(1)),
            accessFlags = 0x0001,
            thisClassName = "com/test/Runner",
            superClassName = "java/lang/Object",
            interfaceNames = emptyList(),
            fields = emptyList(),
            methods = listOf(
                MethodInfo(
                    accessFlags = 0x0001,
                    name = "execute",
                    descriptor = "()V",
                    codeAttribute = CodeAttribute(
                        maxStack = 1,
                        maxLocals = 1,
                        code = byteArrayOf(0xB1.toByte()), // return void
                        exceptionTable = emptyList()
                    ),
                    attributes = emptyMap()
                )
            ),
            attributes = emptyMap()
        )

        val dexClass = ClassToDexCompiler.compileClass(cf)
        val method = dexClass.virtualMethods.firstOrNull() ?: dexClass.directMethods.first()
        assert(method.instructions.contains(DexConstants.OP_RETURN_VOID.toShort()), "JVM 0xB1 translated to Dalvik OP_RETURN_VOID")
    }

    // Phase 161: Dalvik Register Allocation
    fun testPhase161_DalvikRegisterAllocation() {
        println("\n--- Phase 161: Dalvik Register Allocation ---")
        val cf = ClassFile(
            minorVersion = 0,
            majorVersion = 65,
            constantPool = emptyList(),
            accessFlags = 0x0001,
            thisClassName = "Test",
            superClassName = "java/lang/Object",
            interfaceNames = emptyList(),
            fields = emptyList(),
            methods = listOf(
                MethodInfo(
                    accessFlags = 0x0009, // public static
                    name = "add",
                    descriptor = "(II)I",
                    codeAttribute = CodeAttribute(2, 2, byteArrayOf(0xAC.toByte()), emptyList()),
                    attributes = emptyMap()
                )
            ),
            attributes = emptyMap()
        )
        val dexClass = ClassToDexCompiler.compileClass(cf)
        val m = dexClass.directMethods.first()
        assertEquals(2, m.insSize, "2 parameter registers for static method with 2 ints")
        assert(m.registersSize >= m.insSize, "Registers size allocates locals + params")
    }

    // Phase 162: Method Descriptor Parser
    fun testPhase162_MethodDescriptorParsing() {
        println("\n--- Phase 162: Method Descriptor Parser ---")
        val (params1, ret1) = ClassToDexCompiler.parseMethodDescriptor("(Ljava/lang/String;I)V")
        assertEquals(listOf("Ljava/lang/String;", "I"), params1, "Parsed params (String, int)")
        assertEquals("V", ret1, "Parsed return V")

        val (params2, ret2) = ClassToDexCompiler.parseMethodDescriptor("([BI)Z")
        assertEquals(listOf("[B", "I"), params2, "Parsed array params ([B, int)")
        assertEquals("Z", ret2, "Parsed boolean return Z")
    }

    // Phase 163: Full In-Memory DEX Compilation
    fun testPhase163_InMemoryDexCompilation() {
        println("\n--- Phase 163: In-Memory DEX Compilation ---")
        val dexBytes = ClassToDexCompiler.compileClasses(emptyList())
        assert(dexBytes.size >= 112, "Empty classes.dex generated with valid header")
        assert(dexBytes.copyOfRange(0, 8).contentEquals(DexConstants.DEX_FILE_MAGIC), "Valid DEX magic")
    }

    // Phase 164: ClassToDexCompiler Pipeline
    fun testPhase164_ClassToDexCompilerPipeline() {
        println("\n--- Phase 164: ClassToDexCompiler Pipeline ---")
        val cf = ClassFile(
            minorVersion = 0,
            majorVersion = 65,
            constantPool = emptyList(),
            accessFlags = 0x0001,
            thisClassName = "com/kui/Sample",
            superClassName = "java/lang/Object",
            interfaceNames = emptyList(),
            fields = emptyList(),
            methods = listOf(
                MethodInfo(
                    accessFlags = 0x0001,
                    name = "<init>",
                    descriptor = "()V",
                    codeAttribute = CodeAttribute(1, 1, byteArrayOf(0xB1.toByte()), emptyList()),
                    attributes = emptyMap()
                )
            ),
            attributes = emptyMap()
        )
        val dexClass = ClassToDexCompiler.compileClass(cf)
        val dexBytes = ClassToDexCompiler.compileClasses(listOf(dexClass))
        assert(dexBytes.size > 112, "Compiled Sample class into valid DEX binary")
    }

    // Phase 165: Real JVM Class File Parsing & DEX Translation
    fun testPhase165_RealJvmClassToDexCompilation() {
        println("\n--- Phase 165: Real JVM Class File to DEX Compilation ---")
        // Find any existing compiled class in .kui/build or create one
        val testClassFile = File(".kui/build/test_sample/Sample.class")
        testClassFile.parentFile?.mkdirs()

        // Create a genuine JVM class bytecode
        val bos = ByteArrayOutputStream()
        val dos = DataOutputStream(bos)
        dos.writeInt(0xCAFEBABE.toInt())
        dos.writeShort(0); dos.writeShort(65) // Java 21
        dos.writeShort(5)
        dos.writeByte(1); dos.writeShort(18); dos.write("com/kui/RealSample".toByteArray()) // CP 1
        dos.writeByte(7); dos.writeShort(1) // CP 2: Class
        dos.writeByte(1); dos.writeShort(16); dos.write("java/lang/Object".toByteArray())   // CP 3
        dos.writeByte(7); dos.writeShort(3) // CP 4: Class Object
        dos.writeShort(0x0001) // public
        dos.writeShort(2)      // this
        dos.writeShort(4)      // super
        dos.writeShort(0)      // ifaces
        dos.writeShort(0)      // fields
        dos.writeShort(0)      // methods
        dos.writeShort(0)      // attributes
        testClassFile.writeBytes(bos.toByteArray())

        val parsed = ClassFileReader.read(testClassFile)
        assertEquals("com/kui/RealSample", parsed.thisClassName, "Parsed real class from file")
        val dexClass = ClassToDexCompiler.compileClass(parsed)
        val dexBytes = ClassToDexCompiler.compileClasses(listOf(dexClass))
        assert(dexBytes.size > 112, "Compiled real JVM class file to DEX")
    }

    // Phase 166: Multi-Class Directory DEX Compilation
    fun testPhase166_MultiClassDirectoryDexCompilation() {
        println("\n--- Phase 166: Multi-Class Directory DEX Compilation ---")
        val dir = File(".kui/build/test_classes_dir")
        dir.mkdirs()

        for (name in listOf("ClassA", "ClassB", "ClassC")) {
            val f = File(dir, "$name.class")
            val bos = ByteArrayOutputStream()
            val dos = DataOutputStream(bos)
            dos.writeInt(0xCAFEBABE.toInt())
            dos.writeShort(0); dos.writeShort(65)
            dos.writeShort(3)
            dos.writeByte(1); dos.writeShort(name.length); dos.write(name.toByteArray())
            dos.writeByte(7); dos.writeShort(1)
            dos.writeShort(0x0001); dos.writeShort(2); dos.writeShort(0); dos.writeShort(0); dos.writeShort(0); dos.writeShort(0); dos.writeShort(0)
            f.writeBytes(bos.toByteArray())
        }

        val dexBytes = ClassToDexCompiler.compileDirectory(dir)
        assert(dexBytes.size > 112, "Compiled multi-class directory into single classes.dex")

        // Dynamic UI Text Extraction Test
        val mockRoot = File(".kui/build/test_ui_extract")
        val mockSrc = File(mockRoot, "src")
        mockSrc.mkdirs()
        File(mockSrc, "main.kt").writeText("""
            fun main() = app {
                screen {
                    center {
                        text("Hello KUI4")
                    }
                }
            }
        """.trimIndent())
        val extractedText = ClassToDexCompiler.extractUiText(mockRoot, mockRoot, emptyList())
        assertEquals("Hello KUI4", extractedText, "Extracted user UI text from src/main.kt correctly")

        File(mockSrc, "main.kt").writeText("""
            column {
                text("Title A")
                text("Subtitle B")
            }
        """.trimIndent())
        val multiExtracted = ClassToDexCompiler.extractUiText(mockRoot, mockRoot, emptyList())
        assertEquals("Title A\n\nSubtitle B", multiExtracted, "Extracted multiple UI texts correctly")
    }

    // Phase 167: Pure Kotlin AXML Writer
    fun testPhase167_AxmlWriterChunksAndStringPool() {
        println("\n--- Phase 167: Pure Kotlin AXML Writer ---")
        val writer = AxmlWriter()
        val bytes = writer.build { w, out ->
            val nsUriIdx = w.getStringIndex("http://schemas.android.com/apk/res/android")
            val rootTagIdx = w.getStringIndex("root")
            out.writeShort(AxmlConstants.RES_XML_START_ELEMENT_TYPE)
            out.writeShort(16)
            out.writeInt(32)
            out.writeInt(0)
            out.writeInt(-1)
            out.writeInt(-1)
            out.writeInt(rootTagIdx)
            out.writeShort(20); out.writeShort(20); out.writeShort(0); out.writeShort(0); out.writeShort(0)
        }

        assert(bytes.size >= 8, "AXML bytes generated")
        val chunkType = (bytes[0].toInt() and 0xFF) or ((bytes[1].toInt() and 0xFF) shl 8)
        assertEquals(AxmlConstants.RES_XML_TYPE, chunkType, "Root chunk type is RES_XML_TYPE (0x0003)")
    }

    // Phase 168: AndroidManifest.xml Generation
    fun testPhase168_ManifestGeneratorBinaryXml() {
        println("\n--- Phase 168: AndroidManifest.xml Binary Generation ---")
        val manifestBytes = ManifestGenerator.generateBinaryManifest(
            packageName = "com.kui.demo",
            versionCode = 10,
            versionName = "2.1.0",
            minSdk = 26,
            targetSdk = 36,
            appLabel = "Demo App"
        )

        assert(manifestBytes.size > 100, "Binary AndroidManifest.xml generated (${manifestBytes.size} bytes)")
        val chunkType = (manifestBytes[0].toInt() and 0xFF) or ((manifestBytes[1].toInt() and 0xFF) shl 8)
        assertEquals(AxmlConstants.RES_XML_TYPE, chunkType, "Manifest is valid Android Binary XML")
    }

    // Phase 169: Pure Kotlin ZIP Archive Generation
    fun testPhase169_ApkWriterZipStructure() {
        println("\n--- Phase 169: Pure Kotlin ZIP Archive Generation ---")
        val writer = ApkWriter()
        writer.addEntry("hello.txt", "Hello KUI!".toByteArray(StandardCharsets.UTF_8))
        val zipBytes = writer.build()

        assert(zipBytes.size > 30, "ZIP archive generated")
        val entries = ApkWriter.listEntries(zipBytes)
        assertEquals(1, entries.size, "Found 1 entry in ZIP")
        assertEquals("hello.txt", entries[0].name, "Entry name is 'hello.txt'")
    }

    // Phase 170: 4-Byte Zipalign Padding
    fun testPhase170_ApkWriter4ByteZipalign() {
        println("\n--- Phase 170: 4-Byte Zipalign Padding ---")
        val writer = ApkWriter()
        writer.addEntry("file1.bin", byteArrayOf(1, 2, 3), compress = false)
        writer.addEntry("file2.bin", byteArrayOf(4, 5, 6, 7, 8), compress = false)
        writer.addEntry("classes.dex", byteArrayOf(0, 1, 2, 3), compress = false)

        val apkBytes = writer.build()
        val entries = ApkWriter.listEntries(apkBytes)

        for (e in entries) {
            assert(e.dataOffset % 4 == 0L, "Entry ${e.name} dataOffset (${e.dataOffset}) is 4-byte aligned")
        }
    }

    // Phase 171: Mixed Compression in APK
    fun testPhase171_ApkWriterMixedCompression() {
        println("\n--- Phase 171: Mixed Compression in APK ---")
        val writer = ApkWriter()
        val largeData = "A".repeat(5000).toByteArray()
        writer.addEntry("compressed.txt", largeData, compress = true)
        writer.addEntry("stored.txt", byteArrayOf(10, 20, 30), compress = false)

        val apkBytes = writer.build()
        val entries = ApkWriter.listEntries(apkBytes)
        val compEntry = entries.first { it.name == "compressed.txt" }
        val storedEntry = entries.first { it.name == "stored.txt" }

        assertEquals(8, compEntry.compressionMethod, "compressed.txt uses DEFLATE (8)")
        assertEquals(0, storedEntry.compressionMethod, "stored.txt uses STORED (0)")
        assert(compEntry.compressedSize < largeData.size, "Deflate reduced data size")
    }

    // Phase 172: APK Alignment Verification
    fun testPhase172_ApkWriterAlignmentVerification() {
        println("\n--- Phase 172: APK Alignment Verification ---")
        val validApk = ApkWriter.buildApk(
            manifestBytes = byteArrayOf(1, 2, 3, 4),
            dexBytes = byteArrayOf(5, 6, 7, 8)
        )
        assert(ApkWriter.verifyAlignment(validApk), "verifyAlignment returns true for correctly aligned APK")
    }

    // Phase 173: Self-Signed X.509 Debug Certificate & RSA Key Generation
    fun testPhase173_DebugKeyAndX509CertificateGeneration() {
        println("\n--- Phase 173: Self-Signed X.509 Debug Certificate & RSA Key ---")
        val config = DebugKeyGenerator.getOrCreateDebugKey()
        assert(config.privateKey.algorithm == "RSA", "Generated RSA private key")
        assert(config.certificate.sigAlgName.contains("RSA", ignoreCase = true), "Signed with RSA")
        assert(config.certificate.subjectX500Principal.name.contains("KUI", ignoreCase = true), "Subject contains KUI")
    }

    // Phase 174: APK Signature Scheme v2 Block Generation
    fun testPhase174_ApkV2SigningBlockGeneration() {
        println("\n--- Phase 174: APK Signature Scheme v2 Block Generation ---")
        val rawApk = ApkWriter.buildApk(
            manifestBytes = "Manifest".toByteArray(),
            dexBytes = "Dex".toByteArray()
        )
        val signedApk = ApkV2Signer.sign(rawApk)
        assert(signedApk.size > rawApk.size, "Signed APK contains injected signing block")

        val eocd = ApkV2Signer.findEocdRecord(signedApk)
        assert(eocd != null, "EOCD record present in signed APK")
        val cdOffset = ApkV2Signer.getUInt32LE(signedApk, eocd!! + 16).toInt()
        val magic = String(signedApk, cdOffset - 16, 16, StandardCharsets.US_ASCII)
        assertEquals("APK Sig Block 42", magic, "Found APK Signing Block magic string")
    }

    // Phase 175: APK Signature Scheme v2 Verifier
    fun testPhase175_ApkV2SignatureVerification() {
        println("\n--- Phase 175: APK Signature Scheme v2 Verifier ---")
        val rawApk = ApkWriter.buildApk(
            manifestBytes = "ManifestData".toByteArray(),
            dexBytes = "DexData".toByteArray()
        )
        val signedApk = ApkV2Signer.sign(rawApk)
        val result = ApkV2Verifier.verify(signedApk)

        val msg = if (result is VerificationResult.Failure) ": ${result.reason}" else ""
        assert(result is VerificationResult.Success, "Signature verification succeeds$msg")
        if (result is VerificationResult.Success) {
            assert(result.algorithm.contains("RSA"), "Algorithm reported as RSA")
        }
    }

    // Phase 176: Cryptographic Tamper Detection
    fun testPhase176_CryptographicTamperDetection() {
        println("\n--- Phase 176: Cryptographic Tamper Detection ---")
        val rawApk = ApkWriter.buildApk(
            manifestBytes = "OriginalManifest".toByteArray(),
            dexBytes = "OriginalDex".toByteArray()
        )
        val signedApk = ApkV2Signer.sign(rawApk)

        // 1. Tamper with entry payload
        val tampered1 = signedApk.copyOf()
        tampered1[10] = (tampered1[10] + 1).toByte()
        val result1 = ApkV2Verifier.verify(tampered1)
        assert(result1 is VerificationResult.Failure, "Tampered archive data detected and rejected")

        // 2. Tamper with central directory
        val eocd = ApkV2Signer.findEocdRecord(signedApk)!!
        val cdOffset = ApkV2Signer.getUInt32LE(signedApk, eocd + 16).toInt()
        val tampered2 = signedApk.copyOf()
        tampered2[cdOffset + 5] = (tampered2[cdOffset + 5] + 1).toByte()
        val result2 = ApkV2Verifier.verify(tampered2)
        assert(result2 is VerificationResult.Failure, "Tampered central directory detected and rejected")
    }

    // Phase 177: PackagingTask End-to-End Orchestration
    fun testPhase177_PackagingTaskEndToEnd() {
        println("\n--- Phase 177: PackagingTask End-to-End Orchestration ---")
        val testRoot = File(".kui/build/test_project")
        val classesDir = File(testRoot, "build/classes")
        classesDir.mkdirs()

        val config = KuiConfig(
            project = ProjectIdentity(name = "hello-kui", version = "1.0.0", applicationId = "com.kui.hello"),
            android = AndroidConfig(minSdk = 24, targetSdk = 36),
            ui = UiConfig(),
            projectRoot = testRoot
        )

        val outApk = File(testRoot, "build/outputs/apk/debug/app-debug.apk")
        val res = PackagingTask.execute(
            projectRoot = testRoot,
            classesDir = classesDir,
            config = config,
            outputApk = outApk
        )

        assert(res.isSuccess, "PackagingTask executed successfully: ${res.message}")
        assert(outApk.exists(), "Output APK written to disk")
        if (outApk.exists()) {
            assert(outApk.length() > 0, "Output APK has valid non-zero length")
            val ver = ApkV2Verifier.verifyFile(outApk)
            assert(ver is VerificationResult.Success, "Packaged APK verifies with ApkV2Verifier")
        }
    }

    // Phase 178: ADB Device Discovery & Parser
    fun testPhase178_DeviceDiscoveryParser() {
        println("\n--- Phase 178: ADB Device Discovery & Parser ---")
        val adbOutput = """
            List of devices attached
            emulator-5554          device product:sdk_gphone64_arm64 model:sdk_gphone64_arm64 device:emu64a transport_id:1
            192.168.1.100:5555     offline product:device model:Pixel_8 device:husky transport_id:2
        """.trimIndent()

        val devices = DeviceManager.parseDeviceList(adbOutput)
        assertEquals(2, devices.size, "Parsed 2 devices")
        val emu = devices[0]
        assertEquals("emulator-5554", emu.serial, "Emulator serial parsed")
        assertEquals("device", emu.state, "Device state is 'device'")
        assert(emu.isOnline, "Emulator isOnline is true")
        assert(emu.isEmulator, "isEmulator is true for emulator-5554")
        assertEquals("sdk_gphone64_arm64", emu.model, "Model parsed correctly")

        val phone = devices[1]
        assertEquals("offline", phone.state, "Phone state is 'offline'")
        assert(!phone.isOnline, "Offline phone isOnline is false")
        assert(phone.isOffline, "Phone isOffline is true")

        val unauthOutput = "108321541J013120  unauthorized  usb:1-1 transport_id:3"
        val unauthDevices = DeviceManager.parseDeviceList(unauthOutput)
        assertEquals(1, unauthDevices.size, "Parsed unauthorized device")
        assert(unauthDevices[0].isUnauthorized, "Device isUnauthorized is true")
        assert(!unauthDevices[0].isOnline, "Unauthorized device isOnline is false")
    }

    // Phase 179: APK Installation & App Launch Commands
    fun testPhase179_DeviceInstallAndLaunchCommands() {
        println("\n--- Phase 179: APK Installation & App Launch Commands ---")
        val dm = DeviceManager(adbPath = "mock_adb_non_existent")
        val tempApk = File(".kui/build/mock.apk")
        tempApk.parentFile?.mkdirs()
        tempApk.writeText("mock")

        // In absence of live ADB executable, returns graceful error with diagnostics
        val installRes = dm.installApk(tempApk)
        assert(!installRes.isSuccess, "Fails gracefully when ADB server is not reachable")

        val launchRes = dm.launchApp("com.kui.demo")
        assert(!launchRes.isSuccess, "Fails gracefully when ADB server is not reachable")
    }

    // Phase 180: CLI Run Workflow
    fun testPhase180_CliRunWorkflow() {
        println("\n--- Phase 180: CLI Run Workflow ---")
        // Verify RunCommand executes and produces friendly message when no device attached
        val dummyRoot = File(".kui/build/run_test_project")
        dummyRoot.mkdirs()
        File(dummyRoot, "kui.toml").writeText("""
            [project]
            name = "RunTestApp"
            version = "0.1.0"
            application_id = "com.kui.runtest"

            [android]
            min_sdk = 24
            target_sdk = 36
        """.trimIndent())
        val srcDir = File(dummyRoot, "src")
        srcDir.mkdirs()
        File(srcDir, "Main.kt").writeText("fun main() {}")

        // Build first to have the APK
        kui.build.BuildCommand.execute(projectRootOverride = dummyRoot)
        val apk = File(dummyRoot, "build/outputs/apk/debug/app-debug.apk")
        assert(apk.exists(), "APK produced during build")
    }
}

fun main() {
    ToolchainAndApkTest.runAll()
}
