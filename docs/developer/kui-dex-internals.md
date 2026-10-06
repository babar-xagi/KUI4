# Kotlin DEX internals

`platform/kui/classfile/ClassFileReader.kt` reads JVM class headers, constant pools, fields, methods, attributes, and method Code attributes.

`platform/kui/dex/ClassToDexCompiler.kt` converts class and method descriptors into DEX models. Translation currently handles constructor scaffolding and return instructions; arbitrary JVM instructions, fields, branching, and Kotlin runtime behavior are not supported end to end.

For simple UI4 projects, the compiler synthesizes an Android MainActivity with an Activity constructor and an onCreate method that displays literal text in a TextView. This Activity calls Android framework methods directly and does not execute the application's JVM main.

`platform/kui/dex/DexModel.kt` writes identifier tables, class definitions, encoded methods, code items, MUTF-8 strings, and the DEX map. It uses ULEB128 for variable-length values and computes the SHA-1 signature and Adler-32 checksum.

`tests/ToolchainAndApkTest.kt` verifies class parsing, DEX structure and checksums, binary AXML, ZIP alignment, APK v2 signatures, tamper rejection, and the packaging pipeline. An APK build alone does not establish full JVM bytecode compatibility.