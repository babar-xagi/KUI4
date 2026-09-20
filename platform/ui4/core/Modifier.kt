package ui4.core

/**
 * Immutable modifier chain for styling and layout constraints (Phases 086–089).
 */
interface Modifier {

    fun <R> foldIn(initial: R, operation: (R, Element) -> R): R

    fun all(predicate: (Element) -> Boolean): Boolean =
        foldIn(true) { acc, elem -> acc && predicate(elem) }

    fun any(predicate: (Element) -> Boolean): Boolean =
        foldIn(false) { acc, elem -> acc || predicate(elem) }

    infix fun then(other: Modifier): Modifier =
        if (other === Modifier) this else CombinedModifier(this, other)

    interface Element : Modifier {
        override fun <R> foldIn(initial: R, operation: (R, Element) -> R): R =
            operation(initial, this)
    }

    companion object : Modifier {
        override fun <R> foldIn(initial: R, operation: (R, Element) -> R): R = initial
        override fun toString(): String = "Modifier"
    }
}

/**
 * Node combining two modifier chains preserving order.
 */
class CombinedModifier(
    val outer: Modifier,
    val inner: Modifier
) : Modifier {
    override fun <R> foldIn(initial: R, operation: (R, Modifier.Element) -> R): R =
        inner.foldIn(outer.foldIn(initial, operation), operation)

    override fun toString(): String =
        "[" + foldIn(StringBuilder()) { sb, elem ->
            if (sb.isNotEmpty()) sb.append(", ")
            sb.append(elem.toString())
        }.toString() + "]"
}

// -------------------------------------------------------------
// Concrete Modifier Elements (Phases 076, 077, 078, 087, 088, 089)
// -------------------------------------------------------------

/**
 * Padding inset modifier (Phase 087).
 */
data class PaddingModifier(val insets: Insets) : Modifier.Element {
    override fun toString(): String = "Padding($insets)"
}

/**
 * Background fill modifier (Phase 088).
 */
data class BackgroundModifier(val color: Color) : Modifier.Element {
    override fun toString(): String = "Background(${color.toHexString()})"
}

/**
 * Explicit sizing modifier (Phase 089).
 */
data class SizeModifier(val width: Float? = null, val height: Float? = null) : Modifier.Element {
    override fun toString(): String = "Size(w=$width, h=$height)"
}

/**
 * Fill available width modifier (Phase 076, 089).
 */
data class FillWidthModifier(val fraction: Float = 1.0f) : Modifier.Element {
    init { require(fraction in 0f..1f) { "fraction must be in 0..1" } }
    override fun toString(): String = "FillMaxWidth($fraction)"
}

/**
 * Fill available height modifier (Phase 077, 089).
 */
data class FillHeightModifier(val fraction: Float = 1.0f) : Modifier.Element {
    init { require(fraction in 0f..1f) { "fraction must be in 0..1" } }
    override fun toString(): String = "FillMaxHeight($fraction)"
}

/**
 * Proportional flex weight modifier (Phase 078).
 */
data class WeightModifier(val weight: Float) : Modifier.Element {
    init { require(weight > 0f) { "weight must be positive: $weight" } }
    override fun toString(): String = "Weight($weight)"
}

/**
 * Layer opacity modifier (Phase 099).
 */
data class AlphaModifier(val alpha: Float) : Modifier.Element {
    init { require(alpha in 0f..1f) { "alpha must be in 0..1: $alpha" } }
    override fun toString(): String = "Alpha($alpha)"
}

/**
 * Rounded corner radius modifier (Phase 098).
 */
data class RoundedModifier(val radius: Float) : Modifier.Element {
    init { require(radius >= 0f) { "radius must be non-negative: $radius" } }
    override fun toString(): String = "Rounded($radius)"
}

/**
 * Clipping boundary modifier (Phase 097).
 */
data class ClipModifier(val clip: Boolean = true) : Modifier.Element {
    override fun toString(): String = "Clip($clip)"
}

/**
 * Enabled state modifier (Phase 111).
 */
data class EnabledModifier(val enabled: Boolean) : Modifier.Element {
    override fun toString(): String = "Enabled($enabled)"
}

// -------------------------------------------------------------
// Fluent Modifier Extensions
// -------------------------------------------------------------

fun Modifier.padding(all: Float): Modifier =
    then(PaddingModifier(Insets.all(all)))

fun Modifier.padding(horizontal: Float = 0f, vertical: Float = 0f): Modifier =
    then(PaddingModifier(Insets.symmetric(horizontal, vertical)))

fun Modifier.padding(left: Float = 0f, top: Float = 0f, right: Float = 0f, bottom: Float = 0f): Modifier =
    then(PaddingModifier(Insets(left, top, right, bottom)))

fun Modifier.padding(insets: Insets): Modifier =
    then(PaddingModifier(insets))

fun Modifier.background(color: Color): Modifier =
    then(BackgroundModifier(color))

fun Modifier.size(size: Float): Modifier =
    then(SizeModifier(width = size, height = size))

fun Modifier.size(width: Float, height: Float): Modifier =
    then(SizeModifier(width = width, height = height))

fun Modifier.width(width: Float): Modifier =
    then(SizeModifier(width = width, height = null))

fun Modifier.height(height: Float): Modifier =
    then(SizeModifier(width = null, height = height))

fun Modifier.fillMaxWidth(fraction: Float = 1.0f): Modifier =
    then(FillWidthModifier(fraction))

fun Modifier.fillMaxHeight(fraction: Float = 1.0f): Modifier =
    then(FillHeightModifier(fraction))

fun Modifier.fillMaxSize(fraction: Float = 1.0f): Modifier =
    then(FillWidthModifier(fraction)).then(FillHeightModifier(fraction))

fun Modifier.weight(weight: Float): Modifier =
    then(WeightModifier(weight))

fun Modifier.alpha(alpha: Float): Modifier =
    then(AlphaModifier(alpha))

fun Modifier.rounded(radius: Float): Modifier =
    then(RoundedModifier(radius))

fun Modifier.clip(clip: Boolean = true): Modifier =
    then(ClipModifier(clip))

fun Modifier.enabled(enabled: Boolean): Modifier =
    then(EnabledModifier(enabled))

// -------------------------------------------------------------
// Modifier Inspection Helpers
// -------------------------------------------------------------

fun Modifier.findPadding(): Insets? =
    foldIn<Insets?>(null) { acc, elem ->
        if (elem is PaddingModifier) {
            val curr = elem.insets
            if (acc == null) curr else Insets(
                left = acc.left + curr.left,
                top = acc.top + curr.top,
                right = acc.right + curr.right,
                bottom = acc.bottom + curr.bottom
            )
        } else acc
    }

fun Modifier.findBackground(): Color? =
    foldIn<Color?>(null) { acc, elem ->
        if (elem is BackgroundModifier) elem.color else acc
    }

fun Modifier.findWeight(): Float? =
    foldIn<Float?>(null) { acc, elem ->
        if (elem is WeightModifier) elem.weight else acc
    }

fun Modifier.findSize(): SizeModifier? =
    foldIn<SizeModifier?>(null) { acc, elem ->
        if (elem is SizeModifier) {
            if (acc == null) elem else SizeModifier(
                width = elem.width ?: acc.width,
                height = elem.height ?: acc.height
            )
        } else acc
    }

fun Modifier.findFillWidth(): Float? =
    foldIn<Float?>(null) { acc, elem ->
        if (elem is FillWidthModifier) elem.fraction else acc
    }

fun Modifier.findFillHeight(): Float? =
    foldIn<Float?>(null) { acc, elem ->
        if (elem is FillHeightModifier) elem.fraction else acc
    }

fun Modifier.findAlpha(): Float? =
    foldIn<Float?>(null) { acc, elem ->
        if (elem is AlphaModifier) {
            if (acc == null) elem.alpha else acc * elem.alpha
        } else acc
    }

fun Modifier.findRadius(): Float? =
    foldIn<Float?>(null) { acc, elem ->
        if (elem is RoundedModifier) elem.radius else acc
    }

fun Modifier.findClip(): Boolean? =
    foldIn<Boolean?>(null) { acc, elem ->
        if (elem is ClipModifier) elem.clip else acc
    }

fun Modifier.findEnabled(): Boolean? =
    foldIn<Boolean?>(null) { acc, elem ->
        if (elem is EnabledModifier) elem.enabled else acc
    }

