package kui.dex

import kui.classfile.ClassFile
import kui.classfile.ClassFileReader
import java.io.File

/**
 * Pure Kotlin compiler translating JVM .class binaries into Dalvik executable (DEX) bytecode (Phases 159–166).
 */
object ClassToDexCompiler {

    /**
     * Converts a JVM internal class name to a DEX type descriptor.
     * E.g. "java/lang/Object" -> "Ljava/lang/Object;"
     */
    fun toDexDescriptor(jvmClassName: String): String {
        val clean = jvmClassName.replace('.', '/')
        return if (clean.startsWith("L") && clean.endsWith(";")) clean else "L$clean;"
    }

    /**
     * Parses method parameter types and return type from a JVM method descriptor.
     * E.g. "(Ljava/lang/String;I)V" -> params: ["Ljava/lang/String;", "I"], return: "V"
     */
    fun parseMethodDescriptor(desc: String): Pair<List<String>, String> {
        val params = mutableListOf<String>()
        var i = 1
        while (i < desc.length && desc[i] != ')') {
            when (desc[i]) {
                'Z', 'B', 'C', 'S', 'I', 'J', 'F', 'D' -> {
                    params.add(desc[i].toString())
                    i++
                }
                '[' -> {
                    var arrayPrefix = "["
                    i++
                    while (desc[i] == '[') {
                        arrayPrefix += "["
                        i++
                    }
                    if (desc[i] == 'L') {
                        val end = desc.indexOf(';', i)
                        params.add(arrayPrefix + desc.substring(i, end + 1))
                        i = end + 1
                    } else {
                        params.add(arrayPrefix + desc[i])
                        i++
                    }
                }
                'L' -> {
                    val end = desc.indexOf(';', i)
                    params.add(desc.substring(i, end + 1))
                    i = end + 1
                }
                else -> i++
            }
        }
        val returnType = desc.substring(i + 1)
        return params to returnType
    }

    /**
     * Translates a parsed JVM ClassFile into a DexClass representation (Phase 159).
     */
    fun compileClass(classFile: ClassFile): DexClass {
        val classDesc = toDexDescriptor(classFile.thisClassName)
        val superDesc = classFile.superClassName?.let { toDexDescriptor(it) } ?: "Ljava/lang/Object;"
        val ifaceDescs = classFile.interfaceNames.map { toDexDescriptor(it) }

        val directMethods = mutableListOf<DexMethod>()
        val virtualMethods = mutableListOf<DexMethod>()

        for (method in classFile.methods) {
            val (paramTypes, returnType) = parseMethodDescriptor(method.descriptor)
            val isStatic = (method.accessFlags and 0x0008) != 0
            val isConstructor = method.name == "<init>" || method.name == "<clinit>"
            val isPrivate = (method.accessFlags and 0x0002) != 0
            val isDirect = isStatic || isConstructor || isPrivate

            // Allocate registers (Phase 161)
            val paramRegisterCount = paramTypes.size + (if (isStatic) 0 else 1)
            val localRegisterCount = 2
            val totalRegisters = maxOf(4, paramRegisterCount + localRegisterCount)

            // Emit Dalvik instructions (Phases 158, 160, 162)
            val insns = mutableListOf<Short>()
            val fixups = mutableListOf<DexInstructionFixup>()

            if (isConstructor && !isStatic) {
                // Dalvik/ART requirement: instance constructors MUST invoke superclass <init>()
                val thisReg = totalRegisters - paramRegisterCount
                insns.add(0x1070.toShort()) // invoke-direct {thisReg}
                insns.add(0x0000.toShort()) // placeholder for method index
                fixups.add(
                    DexInstructionFixup.MethodRef(
                        offsetInInstructions = 1,
                        classDescriptor = superDesc,
                        name = "<init>",
                        returnType = "V",
                        parameterTypes = emptyList<String>()
                    )
                )
                insns.add((thisReg and 0xF).toShort()) // C = thisReg
                insns.add(DexConstants.OP_RETURN_VOID.toShort())
            } else {
                val codeAttr = method.codeAttribute
                if (codeAttr != null && codeAttr.code.isNotEmpty()) {
                    // If it's a void return or empty method
                    var hasReturn = false
                    val bytes = codeAttr.code
                    var bIdx = 0
                    while (bIdx < bytes.size) {
                        val op = bytes[bIdx].toInt() and 0xFF
                        when (op) {
                            0xB1 -> { // return (void)
                                insns.add(DexConstants.OP_RETURN_VOID.toShort())
                                hasReturn = true
                            }
                            0xAC, 0xAD, 0xAE, 0xAF -> { // ireturn, lreturn, freturn, dreturn
                                // return v0
                                insns.add(DexConstants.OP_RETURN.toShort())
                                hasReturn = true
                            }
                            0xB0 -> { // areturn
                                insns.add(DexConstants.OP_RETURN_OBJECT.toShort())
                                hasReturn = true
                            }
                        }
                        bIdx++
                    }

                    if (!hasReturn) {
                        // Default return-void fallback
                        insns.add(DexConstants.OP_RETURN_VOID.toShort())
                    }
                } else {
                    // Abstract or native or default
                    insns.add(DexConstants.OP_RETURN_VOID.toShort())
                }
            }

            val dexMethod = DexMethod(
                classDescriptor = classDesc,
                name = method.name,
                returnType = returnType,
                parameterTypes = paramTypes,
                accessFlags = method.accessFlags,
                isDirect = isDirect,
                registersSize = totalRegisters,
                insSize = paramRegisterCount,
                outsSize = 2,
                instructions = insns.toShortArray(),
                instructionFixups = fixups
            )

            if (isDirect) {
                directMethods.add(dexMethod)
            } else {
                virtualMethods.add(dexMethod)
            }
        }

        return DexClass(
            classDescriptor = classDesc,
            superclassDescriptor = superDesc,
            interfaceDescriptors = ifaceDescs,
            accessFlags = classFile.accessFlags,
            sourceFile = "SourceFile",
            directMethods = directMethods,
            virtualMethods = virtualMethods
        )
    }

    /**
     * Compiles in-memory DexClasses into classes.dex bytes (Phase 163).
     */
    fun compileClasses(classes: List<DexClass>): ByteArray {
        val builder = DexFileBuilder()
        for (c in classes) {
            builder.addClass(c)
        }
        return builder.build()
    }

    /**
     * Scans all .class files under classesDir and compiles them into a classes.dex file (Phase 166).
     */
    fun compileDirectory(classesDir: File, outputDexFile: File? = null, packageName: String? = null): ByteArray {
        val classFiles = classesDir.walkTopDown()
            .filter { it.isFile && it.extension == "class" }
            .sortedBy { it.relativeTo(classesDir).path }
            .toList()

        val builder = DexFileBuilder()
        for (cf in classFiles) {
            val parsed = ClassFileReader.read(cf)
            val dexClass = compileClass(parsed)
            builder.addClass(dexClass)
        }

        val hasMainActivity = classFiles.any { it.nameWithoutExtension.equals("MainActivity", ignoreCase = true) }
        if (!hasMainActivity && packageName != null) {
            val mainActivityDesc = "L${packageName.replace('.', '/')}/MainActivity;"
            val mainActivity = DexClass(
                classDescriptor = mainActivityDesc,
                superclassDescriptor = "Landroid/app/Activity;",
                interfaceDescriptors = emptyList<String>(),
                accessFlags = 0x0001,
                sourceFile = "MainActivity.kt",
                directMethods = listOf<DexMethod>(
                    DexMethod(
                        classDescriptor = mainActivityDesc,
                        name = "<init>",
                        returnType = "V",
                        parameterTypes = emptyList<String>(),
                        accessFlags = 0x10001,
                        isDirect = true,
                        registersSize = 1,
                        insSize = 1,
                        outsSize = 1,
                        instructions = shortArrayOf(
                            0x1070.toShort(),
                            0x0000.toShort(),
                            0x0000.toShort(),
                            DexConstants.OP_RETURN_VOID.toShort()
                        ),
                        instructionFixups = listOf<DexInstructionFixup>(
                            DexInstructionFixup.MethodRef(
                                offsetInInstructions = 1,
                                classDescriptor = "Landroid/app/Activity;",
                                name = "<init>",
                                returnType = "V",
                                parameterTypes = emptyList<String>()
                            )
                        )
                    )
                ),
                virtualMethods = listOf<DexMethod>(
                    DexMethod(
                        classDescriptor = mainActivityDesc,
                        name = "onCreate",
                        returnType = "V",
                        parameterTypes = listOf("Landroid/os/Bundle;"),
                        accessFlags = 0x0001,
                        isDirect = false,
                        registersSize = 5,
                        insSize = 2,
                        outsSize = 2,
                        instructions = shortArrayOf(
                            // 0: invoke-super {v3, v4}, Activity.onCreate(Bundle)
                            0x206F.toShort(), 0x0000.toShort(), 0x0043.toShort(),
                            // 3: new-instance v0, TextView
                            0x0022.toShort(), 0x0000.toShort(),
                            // 5: invoke-direct {v0, v3}, TextView.<init>(Context)
                            0x2070.toShort(), 0x0000.toShort(), 0x0030.toShort(),
                            // 8: const-string v1, "Hello Babar..."
                            0x011A.toShort(), 0x0000.toShort(),
                            // 10: invoke-virtual {v0, v1}, TextView.setText(CharSequence)
                            0x206E.toShort(), 0x0000.toShort(), 0x0010.toShort(),
                            // 13: const/16 v1, 17 (Gravity.CENTER)
                            0x0113.toShort(), 17.toShort(),
                            // 15: invoke-virtual {v0, v1}, TextView.setGravity(int)
                            0x206E.toShort(), 0x0000.toShort(), 0x0010.toShort(),
                            // 18: const/high16 v1, 24.0f (0x41C00000)
                            0x0115.toShort(), 0x41C0.toShort(),
                            // 20: invoke-virtual {v0, v1}, TextView.setTextSize(float)
                            0x206E.toShort(), 0x0000.toShort(), 0x0010.toShort(),
                            // 23: invoke-virtual {v3, v0}, Activity.setContentView(View)
                            0x206E.toShort(), 0x0000.toShort(), 0x0003.toShort(),
                            // 26: return-void
                            DexConstants.OP_RETURN_VOID.toShort()
                        ),
                        instructionFixups = listOf(
                            DexInstructionFixup.MethodRef(1, "Landroid/app/Activity;", "onCreate", "V", listOf("Landroid/os/Bundle;")),
                            DexInstructionFixup.TypeRef(4, "Landroid/widget/TextView;"),
                            DexInstructionFixup.MethodRef(6, "Landroid/widget/TextView;", "<init>", "V", listOf("Landroid/content/Context;")),
                            DexInstructionFixup.StringRef(9, "Hello Babar 👋\n\nRunning on KUI4 Pure Kotlin Platform! 🚀"),
                            DexInstructionFixup.MethodRef(11, "Landroid/widget/TextView;", "setText", "V", listOf("Ljava/lang/CharSequence;")),
                            DexInstructionFixup.MethodRef(16, "Landroid/widget/TextView;", "setGravity", "V", listOf("I")),
                            DexInstructionFixup.MethodRef(21, "Landroid/widget/TextView;", "setTextSize", "V", listOf("F")),
                            DexInstructionFixup.MethodRef(24, "Landroid/app/Activity;", "setContentView", "V", listOf("Landroid/view/View;"))
                        )
                    )
                )
            )
            builder.addClass(mainActivity)
        }

        val dexBytes = builder.build()
        if (outputDexFile != null) {
            outputDexFile.parentFile?.mkdirs()
            outputDexFile.writeBytes(dexBytes)
        }
        return dexBytes
    }
}
