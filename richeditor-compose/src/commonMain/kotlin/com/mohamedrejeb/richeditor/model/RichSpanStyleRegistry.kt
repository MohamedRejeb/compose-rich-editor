package com.mohamedrejeb.richeditor.model

import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi

/**
 * Registry of [RichSpanStyleDescriptor]s keyed by kind, consulted by the serialization
 * carriers (JSON, HTML) when they meet a custom [RichSpanStyle]. Each [RichTextState] owns
 * one via [RichTextState.spanStyleRegistry]; the library's built-in styles are always
 * available and their kinds are listed in [BuiltInKinds].
 *
 * Iteration order is insertion order.
 */
@ExperimentalRichTextApi
public class RichSpanStyleRegistry {

    private val byKind: MutableMap<String, RichSpanStyleDescriptor> = LinkedHashMap()

    /**
     * Registers [descriptor]. Throws if its kind is blank or collides with a built-in kind.
     * By default, throws if a descriptor is already registered under the same kind; pass
     * [replace] = true to overwrite.
     */
    public fun register(descriptor: RichSpanStyleDescriptor, replace: Boolean = false) {
        val kind = descriptor.kind
        require(kind.isNotBlank()) { "Descriptor kind must be non-blank" }
        require(kind !in BuiltInKinds) {
            "Kind '$kind' is reserved by the library; use a namespaced kind like \"app:font\""
        }
        if (!replace && byKind.containsKey(kind)) {
            throw IllegalStateException(
                "A descriptor for kind '$kind' is already registered; pass replace = true to overwrite"
            )
        }
        byKind[kind] = descriptor
    }

    /** Returns the descriptor registered under [kind], or null. */
    public fun find(kind: String): RichSpanStyleDescriptor? = byKind[kind]

    /** All registered kinds in insertion order. */
    public fun kinds(): List<String> = byKind.keys.toList()

    /** Returns the descriptor registered under [kind] when it opts into [format], or null. */
    public fun find(kind: String, format: RichTextFormat): RichSpanStyleDescriptor? =
        byKind[kind]?.takeIf { format in it.formats }

    /** Returns the first registered descriptor whose [RichSpanStyleDescriptor.matches] accepts [style]. */
    public fun findForStyle(style: RichSpanStyle): RichSpanStyleDescriptor? =
        byKind.values.firstOrNull { it.matches(style) }

    /** Returns the first matching descriptor that opts into [format], or null. */
    public fun findForStyle(style: RichSpanStyle, format: RichTextFormat): RichSpanStyleDescriptor? =
        findForStyle(style)?.takeIf { format in it.formats }

    /** Copies every registration from [other] into this registry, replacing on collision. */
    internal fun copyFrom(other: RichSpanStyleRegistry) {
        other.byKind.values.forEach { register(it, replace = true) }
    }

    public companion object {
        /**
         * The kinds the library itself serializes. Registration under these names is
         * rejected; future library versions may claim more un-namespaced names, so custom
         * kinds should carry a namespace separator (for example `"app:font"`).
         */
        public val BuiltInKinds: Set<String> = setOf(
            "bold", "italic", "underline", "strike", "code", "link", "color", "highlight",
            "font-size", "font-weight", "letter-spacing", "baseline-shift", "shadow",
            "image", "token",
        )
    }
}
