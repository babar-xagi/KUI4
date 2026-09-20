package kui.cli

/**
 * Semantic version representation for KUI Platform.
 */
data class Version(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val tag: String? = null
) {
    override fun toString(): String =
        if (tag != null) "$major.$minor.$patch-$tag" else "$major.$minor.$patch"
}

/**
 * Canonical KUI version constants.
 */
object KuiVersion {
    val CURRENT = Version(0, 1, 0)
    val VERSION_STRING: String = CURRENT.toString()
    val DISPLAY_NAME: String = "kui version $VERSION_STRING"
}
