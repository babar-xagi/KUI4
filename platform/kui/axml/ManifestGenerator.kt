package kui.axml

import kui.config.KuiConfig
import java.io.File

/**
 * Generates binary AndroidManifest.xml from KUI project configuration (Phase 168).
 */
object ManifestGenerator {

    /**
     * Generates Android binary XML bytes for AndroidManifest.xml.
     */
    fun generateBinaryManifest(
        packageName: String,
        versionCode: Int = 1,
        versionName: String = "1.0.0",
        minSdk: Int = 24,
        targetSdk: Int = 36,
        appLabel: String = "KUI App"
    ): ByteArray {
        val writer = AxmlWriter()

        return writer.build { w, out ->
            val nsUri = "http://schemas.android.com/apk/res/android"
            val nsPrefix = "android"

            val nsUriIdx = w.getStringIndex(nsUri)
            val nsPrefixIdx = w.getStringIndex(nsPrefix)
            val emptyNsIdx = -1

            // Pre-register tag strings
            val manifestIdx = w.getStringIndex("manifest")
            val usesSdkIdx = w.getStringIndex("uses-sdk")
            val applicationIdx = w.getStringIndex("application")
            val activityIdx = w.getStringIndex("activity")
            val intentFilterIdx = w.getStringIndex("intent-filter")
            val actionIdx = w.getStringIndex("action")
            val categoryIdx = w.getStringIndex("category")

            // Pre-register attribute strings
            val packageIdx = w.getStringIndex("package")
            val versionCodeIdx = w.getStringIndex("versionCode")
            val versionNameIdx = w.getStringIndex("versionName")
            val minSdkIdx = w.getStringIndex("minSdkVersion")
            val targetSdkIdx = w.getStringIndex("targetSdkVersion")
            val labelIdx = w.getStringIndex("label")
            val nameIdx = w.getStringIndex("name")
            val exportedIdx = w.getStringIndex("exported")

            // Values
            val pkgValIdx = w.getStringIndex(packageName)
            val verNameValIdx = w.getStringIndex(versionName)
            val labelValIdx = w.getStringIndex(appLabel)
            val actNameValIdx = w.getStringIndex(".MainActivity")
            val mainActionValIdx = w.getStringIndex("android.intent.action.MAIN")
            val launcherCatValIdx = w.getStringIndex("android.intent.category.LAUNCHER")

            fun writeStartNamespace() {
                out.writeShort(AxmlConstants.RES_XML_START_NAMESPACE_TYPE)
                out.writeShort(16) // header_size
                out.writeInt(24)   // chunk_size
                out.writeInt(0)    // line_number
                out.writeInt(-1)   // comment
                out.writeInt(nsPrefixIdx)
                out.writeInt(nsUriIdx)
            }

            fun writeEndNamespace() {
                out.writeShort(AxmlConstants.RES_XML_END_NAMESPACE_TYPE)
                out.writeShort(16)
                out.writeInt(24)
                out.writeInt(0)
                out.writeInt(-1)
                out.writeInt(nsPrefixIdx)
                out.writeInt(nsUriIdx)
            }

            fun writeStartElement(
                nsIdx: Int,
                nameIdx: Int,
                attrs: List<AxmlAttribute> = emptyList()
            ) {
                val headerSize = 16
                val attrStart = 20
                val attrSize = 20
                val totalSize = headerSize + 16 + (attrs.size * attrSize)

                out.writeShort(AxmlConstants.RES_XML_START_ELEMENT_TYPE)
                out.writeShort(headerSize)
                out.writeInt(totalSize)
                out.writeInt(0)  // line_number
                out.writeInt(-1) // comment
                out.writeInt(nsIdx)
                out.writeInt(nameIdx)
                out.writeShort(attrStart)
                out.writeShort(attrSize)
                out.writeShort(attrs.size)
                out.writeShort(0) // idIndex
                out.writeShort(0) // classIndex
                out.writeShort(0) // styleIndex

                for (attr in attrs) {
                    out.writeInt(attr.uriIndex)
                    out.writeInt(attr.nameIndex)
                    out.writeInt(attr.valueStringIndex)
                    out.writeShort(8) // typedValue.size
                    out.writeShort(0) // res0
                    out.writeByte(attr.type)
                    out.writeByte(0) // data type padding
                    out.writeShort(0)
                    out.writeInt(attr.data)
                }
            }

            fun writeEndElement(nsIdx: Int, nameIdx: Int) {
                out.writeShort(AxmlConstants.RES_XML_END_ELEMENT_TYPE)
                out.writeShort(16)
                out.writeInt(24)
                out.writeInt(0)
                out.writeInt(-1)
                out.writeInt(nsIdx)
                out.writeInt(nameIdx)
            }

            // 1. Start Namespace
            writeStartNamespace()

            // 2. <manifest package="..." android:versionCode="..." android:versionName="...">
            writeStartElement(
                nsIdx = emptyNsIdx,
                nameIdx = manifestIdx,
                attrs = listOf(
                    AxmlAttribute(emptyNsIdx, packageIdx, pkgValIdx, AxmlConstants.TYPE_STRING, pkgValIdx),
                    AxmlAttribute(nsUriIdx, versionCodeIdx, -1, AxmlConstants.TYPE_INT_DEC, versionCode),
                    AxmlAttribute(nsUriIdx, versionNameIdx, verNameValIdx, AxmlConstants.TYPE_STRING, verNameValIdx)
                )
            )

            // 3. <uses-sdk android:minSdkVersion="..." android:targetSdkVersion="..."/>
            writeStartElement(
                nsIdx = emptyNsIdx,
                nameIdx = usesSdkIdx,
                attrs = listOf(
                    AxmlAttribute(nsUriIdx, minSdkIdx, -1, AxmlConstants.TYPE_INT_DEC, minSdk),
                    AxmlAttribute(nsUriIdx, targetSdkIdx, -1, AxmlConstants.TYPE_INT_DEC, targetSdk)
                )
            )
            writeEndElement(emptyNsIdx, usesSdkIdx)

            // 4. <application android:label="...">
            writeStartElement(
                nsIdx = emptyNsIdx,
                nameIdx = applicationIdx,
                attrs = listOf(
                    AxmlAttribute(nsUriIdx, labelIdx, labelValIdx, AxmlConstants.TYPE_STRING, labelValIdx)
                )
            )

            // 5. <activity android:name=".MainActivity" android:exported="true">
            writeStartElement(
                nsIdx = emptyNsIdx,
                nameIdx = activityIdx,
                attrs = listOf(
                    AxmlAttribute(nsUriIdx, nameIdx, actNameValIdx, AxmlConstants.TYPE_STRING, actNameValIdx),
                    AxmlAttribute(nsUriIdx, exportedIdx, -1, AxmlConstants.TYPE_INT_BOOLEAN, 1)
                )
            )

            // 6. <intent-filter>
            writeStartElement(emptyNsIdx, intentFilterIdx)

            // <action android:name="android.intent.action.MAIN"/>
            writeStartElement(
                nsIdx = emptyNsIdx,
                nameIdx = actionIdx,
                attrs = listOf(
                    AxmlAttribute(nsUriIdx, nameIdx, mainActionValIdx, AxmlConstants.TYPE_STRING, mainActionValIdx)
                )
            )
            writeEndElement(emptyNsIdx, actionIdx)

            // <category android:name="android.intent.category.LAUNCHER"/>
            writeStartElement(
                nsIdx = emptyNsIdx,
                nameIdx = categoryIdx,
                attrs = listOf(
                    AxmlAttribute(nsUriIdx, nameIdx, launcherCatValIdx, AxmlConstants.TYPE_STRING, launcherCatValIdx)
                )
            )
            writeEndElement(emptyNsIdx, categoryIdx)

            // </intent-filter>
            writeEndElement(emptyNsIdx, intentFilterIdx)

            // </activity>
            writeEndElement(emptyNsIdx, activityIdx)

            // </application>
            writeEndElement(emptyNsIdx, applicationIdx)

            // </manifest>
            writeEndElement(emptyNsIdx, manifestIdx)

            // End Namespace
            writeEndNamespace()
        }
    }

    fun generateToFile(config: KuiConfig, outputFile: File): File {
        val axmlBytes = generateBinaryManifest(
            packageName = config.project.applicationId ?: "com.example.${config.project.name.lowercase().replace('-', '_')}",
            versionCode = 1,
            versionName = config.project.version,
            minSdk = config.android.minSdk,
            targetSdk = config.android.targetSdk,
            appLabel = config.project.name
        )
        outputFile.parentFile?.mkdirs()
        outputFile.writeBytes(axmlBytes)
        return outputFile
    }
}
