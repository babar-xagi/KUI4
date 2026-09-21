# KUI Toolchain & Pure Kotlin Compiler Deep Dive

The KUI toolchain replaces Gradle, Android Gradle Plugin (AGP), AAPT2, D8, and `apksigner` with pure Kotlin components running on standard JDK 21. This document details the exact binary specifications, data layouts, and algorithms implemented across each stage.

---

## 1. Pure JVM Class File Reader (`ClassFileReader.kt`)

`ClassFileReader` parses standard JVM `.class` bytecode produced by `kotlinc` without relying on ASM or ByteBuddy.

### Binary Classfile Format
```
ClassFile {
    u4             magic;               // 0xCAFEBABE
    u2             minor_version;       // 0
    u2             major_version;       // 65 (Java 21)
    u2             constant_pool_count; // N
    cp_info        constant_pool[N-1];
    u2             access_flags;        // ACC_PUBLIC, ACC_FINAL, etc.
    u2             this_class;          // CP index
    u2             super_class;         // CP index
    u2             interfaces_count;
    u2             interfaces[interfaces_count];
    u2             fields_count;
    field_info     fields[fields_count];
    u2             methods_count;
    method_info    methods[methods_count];
    u2             attributes_count;
    attribute_info attributes[attributes_count];
}
```

### Key Parsing Mechanisms:
- **Constant Pool Decoding:** Handles `CONSTANT_Class (7)`, `CONSTANT_Fieldref (9)`, `CONSTANT_Methodref (10)`, `CONSTANT_String (8)`, `CONSTANT_Integer (3)`, `CONSTANT_Float (4)`, `CONSTANT_Long (5)`, `CONSTANT_Double (6)`, `CONSTANT_NameAndType (12)`, and `CONSTANT_Utf8 (1)`.
- **Code Attribute Parsing:** Decodes byte offsets, max stack, max locals, exception tables, and raw JVM bytecode instructions.

---

## 2. Pure Dalvik Executable (DEX) Compiler (`ClassToDexCompiler.kt` & `DexModel.kt`)

KUI compiles JVM bytecode directly to Android's Dalvik Executable (`classes.dex`) format (`dex\n035\0`).

### DEX Header & Section Layout (112-byte header)
```
┌─────────────────────────────────────────────────────────────┐
│ Header (112 bytes)                                          │
│  - Magic: "dex\n035\0"                                      │
│  - Adler-32 Checksum (bytes 8..11)                          │
│  - SHA-1 Signature (bytes 12..31)                           │
│  - File Size, Header Size (112), Endian Constant (0x12345678)│
│  - Offsets to StringIDs, TypeIDs, ProtoIDs, FieldIDs,       │
│    MethodIDs, ClassDefs, Data Section                       │
├─────────────────────────────────────────────────────────────┤
│ string_ids (offset -> string_data_item)                     │
├─────────────────────────────────────────────────────────────┤
│ type_ids (string_idx)                                       │
├─────────────────────────────────────────────────────────────┤
│ proto_ids (shorty_idx, return_type_idx, parameters_off)     │
├─────────────────────────────────────────────────────────────┤
│ field_ids (class_idx, type_idx, name_idx)                   │
├─────────────────────────────────────────────────────────────┤
│ method_ids (class_idx, proto_idx, name_idx)                 │
├─────────────────────────────────────────────────────────────┤
│ class_defs (class_idx, access_flags, superclass_idx, etc.)  │
├─────────────────────────────────────────────────────────────┤
│ data section (string data, type lists, code items, etc.)    │
│  - map_list (0x1000) at map_off                             │
└─────────────────────────────────────────────────────────────┘
```

### Critical ART Verifier Guarantees Implemented:
1. **Empty Section Offsets:** When `field_ids_size == 0`, `field_ids_off` must be strictly `0`. Any non-zero offset for an empty section triggers `Failure to verify dex file: Unexpected non-zero offset`.
2. **`map_list` (`0x1000`):** ART requires a sorted `map_list` record at `map_off` in the data section listing every section type, size, and file offset.
3. **Method ID Ordering in `class_data_item`:** Direct and virtual methods are encoded as ULEB128 delta indexes (`diff = mIdx - lastMethodIdx`). KUI sorts methods strictly by `mIdx` ensuring `diff >= 0`.
4. **Instance Constructor Verification:** Dalvik bytecode verifier enforces that every `<init>` constructor must invoke `super.<init>()` (`invoke-direct {this}, superDesc-><init>()V`) before returning. KUI emits this call automatically for all compiled classes.
5. **Symbolic Instruction Fixups (`DexInstructionFixup`):** Enables instructions to reference method IDs, type IDs, and string IDs before the global table is finalized. Indices are resolved and patched at layout time:
   ```kotlin
   sealed class DexInstructionFixup {
       data class MethodRef(val offsetInInstructions: Int, val classDescriptor: String, val name: String, val returnType: String, val parameterTypes: List<String>) : DexInstructionFixup()
       data class TypeRef(val offsetInInstructions: Int, val typeDescriptor: String) : DexInstructionFixup()
       data class StringRef(val offsetInInstructions: Int, val string: String) : DexInstructionFixup()
   }
   ```
6. **Modified UTF-8 (`encodeMutf8`):** ART's DEX string validator rejects standard 4-byte UTF-8 sequences (starting with `0xF0`, such as emojis `👋` and `🚀`). KUI encodes code points > `0xFFFF` into UTF-16 surrogate pairs, where each surrogate is represented as a 3-byte sequence (`0xED...`) per the official Dalvik DEX specification.

---

## 3. Pure Kotlin Binary Android XML (`AxmlWriter.kt` & `ManifestGenerator.kt`)

Android OS `PackageParser` will reject plain text XML files. KUI generates genuine compiled binary XML (`AndroidManifest.xml`).

### Binary AXML Chunk Hierarchy:
1. **`RES_XML_TYPE` (`0x0003`)**: Root chunk containing the total file size.
2. **`RES_STRING_POOL_TYPE` (`0x0001`)**: Pool containing all tag names, attribute names, and string literals.
3. **`RES_XML_RESOURCE_MAP_TYPE` (`0x0180`)**: Mandatory mapping table mapping `android:*` attribute name indices to Android OS system resource IDs (e.g. `0x0101021b` for `minSdkVersion`, `0x01010003` for `name`).
4. **`RES_XML_START_NAMESPACE_TYPE` (`0x0100`)**: Defines `xmlns:android="http://schemas.android.com/apk/res/android"`.
5. **`RES_XML_START_ELEMENT_TYPE` (`0x0102`)**: Elements (`manifest`, `application`, `activity`, `intent-filter`, `action`, `category`).
   - Every attribute is encoded as a 20-byte struct:
     - `ns_idx` (4 bytes)
     - `name_idx` (4 bytes)
     - `raw_value_idx` (4 bytes)
     - `typed_value_size` (2 bytes = 8)
     - `typed_value_res0` (1 byte = 0)
     - `typed_value_type` (1 byte = `TYPE_STRING`, `TYPE_INT_DEC`, `TYPE_INT_BOOLEAN`)
     - `typed_value_data` (4 bytes = parsed integer, boolean, or string index)
6. **`RES_XML_END_ELEMENT_TYPE` (`0x0103`)** & **`RES_XML_END_NAMESPACE_TYPE` (`0x0101`)**.

---

## 4. 4-Byte Zipaligned APK Packaging (`ApkWriter.kt`)

Android's dynamic linker and ART runtime memory-map (`mmap`) `classes.dex` and uncompressed native libraries directly from the APK archive.

### 4-Byte Alignment Rule:
Every uncompressed entry's file payload offset must satisfy:
$$\text{data\_offset} \pmod 4 == 0$$

`ApkWriter` calculates the local file header length:
$$\text{header\_len} = 30 + \text{name\_len} + \text{extra\_len}$$
If `(current_file_offset + header_len) % 4 != 0`, `ApkWriter` adds 1 to 3 bytes of zero padding into the ZIP `extra_field` to achieve alignment.

---

## 5. APK Signature Scheme v2 Engine (`ApkV2Signer.kt`)

Unlike legacy JAR signing (v1), APK Signature Scheme v2 signs the entire binary file.

### Signed APK File Layout:
```
┌─────────────────────────────────────────────────────────────┐
│ Section 1: ZIP Entries                                      │
│  (classes.dex, AndroidManifest.xml, assets, etc.)           │
├─────────────────────────────────────────────────────────────┤
│ Section 2: APK Signing Block                                │
│  - Size of block (8 bytes)                                  │
│  - ID-Value Pair (ID = 0x7109871a, APK Signature Scheme v2) │
│    - Signer: Certificate, SHA-256 with RSA Signature        │
│  - Size of block (8 bytes)                                  │
│  - Magic: "APK Sig Block 42" (16 bytes)                     │
├─────────────────────────────────────────────────────────────┤
│ Section 3: ZIP Central Directory                            │
├─────────────────────────────────────────────────────────────┤
│ Section 4: ZIP End of Central Directory (EOCD)              │
│  - Central directory offset points to Section 3             │
└─────────────────────────────────────────────────────────────┘
```

### Signature Generation Algorithm:
1. **Chunk Hashing:** The APK (excluding the signing block) is split into 1MB chunks across Section 1, Section 3, and Section 4. Each chunk is hashed with SHA-256.
2. **Root Digest:** The concatenation of all chunk hashes is hashed with SHA-256 to form the root digest.
3. **Asymmetric Signing:** The root digest is signed with a 2048-bit RSA private key using `SHA256withRSA`.
4. **Persistent Debug Keystore:** Keys are saved to `~/.kui/debug.pk8` and `~/.kui/debug.crt`. Subsequent builds use the same persistent key, preventing `INSTALL_FAILED_UPDATE_INCOMPATIBLE`.

---

## 6. ADB Device Discovery & Deployment (`DeviceManager.kt`)

`DeviceManager` communicates with the local Android Debug Bridge daemon:
1. **Discovery:** Runs `adb devices -l` and parses device serials, models, states (`device`, `offline`, `unauthorized`), and transport IDs.
2. **Installation:** Executes `adb install -r -d -t <apk-path>`:
   - `-r`: Replace existing application without losing user data.
   - `-d`: Allow version code downgrade.
   - `-t`: Allow test packages.
3. **Execution:** Executes `adb shell am start -n <package>/.MainActivity` to bring the activity to the top foreground.
