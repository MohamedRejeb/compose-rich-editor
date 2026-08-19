package com.mohamedrejeb.richeditor.model

import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi

/** A serialization format a registered custom [RichSpanStyle] can opt into. */
@ExperimentalRichTextApi
public enum class RichTextFormat {
    Json,
    Html,
}

/**
 * Describes how an app-defined [RichSpanStyle] is persisted: a stable [kind] identifier plus
 * an attribute map the style converts to and from. The formats in [formats] carry the mark;
 * formats left out simply drop it.
 *
 * Create instances with [richSpanStyleDescriptor] and register them on
 * [RichTextState.spanStyleRegistry].
 */
@ExperimentalRichTextApi
public interface RichSpanStyleDescriptor {
    /**
     * Stable identifier written to the serialized form, for example `"app:font"`. Kinds
     * without a namespace separator are reserved by the library
     * (see [RichSpanStyleRegistry.BuiltInKinds]).
     */
    public val kind: String

    /** The formats that carry this style. Defaults to JSON and HTML. */
    public val formats: Set<RichTextFormat>

    /** Whether [style] is an instance this descriptor serializes. */
    public fun matches(style: RichSpanStyle): Boolean

    /** Converts a matching [style] to its attribute map. Only called when [matches] is true. */
    public fun encode(style: RichSpanStyle): Map<String, String>

    /**
     * Reconstructs a style from [attributes], or returns null when the attributes are not
     * usable (the mark is then dropped instead of failing the whole load).
     */
    public fun decode(attributes: Map<String, String>): RichSpanStyle?
}

/**
 * Builds a [RichSpanStyleDescriptor] for style class [T] from typed [encode] and [decode]
 * lambdas.
 */
@ExperimentalRichTextApi
public inline fun <reified T : RichSpanStyle> richSpanStyleDescriptor(
    kind: String,
    formats: Set<RichTextFormat> = setOf(RichTextFormat.Json, RichTextFormat.Html),
    noinline encode: (T) -> Map<String, String>,
    noinline decode: (Map<String, String>) -> T?,
): RichSpanStyleDescriptor =
    TypedRichSpanStyleDescriptor(
        kind = kind,
        formats = formats,
        isInstance = { it is T },
        encodeTyped = { encode(it as T) },
        decodeTyped = decode,
    )

@ExperimentalRichTextApi
@PublishedApi
internal class TypedRichSpanStyleDescriptor(
    override val kind: String,
    override val formats: Set<RichTextFormat>,
    private val isInstance: (RichSpanStyle) -> Boolean,
    private val encodeTyped: (RichSpanStyle) -> Map<String, String>,
    private val decodeTyped: (Map<String, String>) -> RichSpanStyle?,
) : RichSpanStyleDescriptor {

    override fun matches(style: RichSpanStyle): Boolean = isInstance(style)

    override fun encode(style: RichSpanStyle): Map<String, String> {
        require(matches(style)) { "Descriptor '$kind' cannot encode ${style::class.simpleName}" }
        return encodeTyped(style)
    }

    override fun decode(attributes: Map<String, String>): RichSpanStyle? = decodeTyped(attributes)
}
