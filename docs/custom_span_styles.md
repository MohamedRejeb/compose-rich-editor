# Custom span styles

Any class implementing `RichSpanStyle` can style ranges in the editor: apply it with `addRichSpan`, remove it with `removeRichSpan`, and carry app-domain payloads (for example a font identity) on the instance. This page covers making such a style a first-class citizen of persistence: registering a descriptor so the style survives the [document model](rich_text_document.md), [JSON](json_import_export.md), and [HTML](html_import_export.md), with each format individually opt-in.

All API on this page is marked `@ExperimentalRichTextApi`.

## Defining a style

```kotlin
class FontRunStyle(
    val slug: String,                 // identity
    val fontFamily: FontFamily?,      // render resource, excluded from equals
) : RichSpanStyle {
    override fun getSpanStyle(config: RichTextConfig): SpanStyle =
        SpanStyle(fontFamily = fontFamily)
    override fun equals(other: Any?): Boolean =
        other is FontRunStyle && slug == other.slug
    override fun hashCode(): Int = slug.hashCode()
}
```

The style's own `equals` drives editing semantics: adjacent runs whose styles compare equal coalesce, and applying a same-class style over an overlapping range replaces it in the overlap (last write wins). Keep render resources out of `equals` so differently-resolved instances of the same logical style still merge.

## Registering a descriptor

A descriptor gives the style a stable `kind`, converts it to and from a string attribute map, and picks the formats that carry it:

```kotlin
val fontDescriptor = richSpanStyleDescriptor<FontRunStyle>(
    kind = "app:font",
    formats = setOf(RichTextFormat.Json, RichTextFormat.Html),  // the default
    encode = { style -> mapOf("slug" to style.slug) },
    decode = { attrs -> attrs["slug"]?.let { FontRunStyle(it, fontRepository.resolve(it)) } },
)

val state = rememberRichTextState()
state.spanStyleRegistry.register(fontDescriptor)
```

Each `RichTextState` owns its registry; register once per state. The `decode` factory is where external resources resolve (the example resolves fonts from a repository), and returning `null` declines the attributes so the mark is dropped instead of failing the load.

Kinds must be unique per registry and must not collide with the built-in kinds listed in `RichSpanStyleRegistry.BuiltInKinds`. Names without a namespace separator are reserved for future library use, so prefix yours (`"app:font"`, `"myapp:highlighter"`).

## What each format does

| Format | Carrier | Without registration |
|---|---|---|
| Document model | `RichTextSpanMark.Custom` carries the instance itself; no descriptor needed | Always works |
| JSON | `{"k": "app:font", "r": [0, 4], "attrs": {"slug": "amiri"}}` | Kind decodes as `Unknown`, editor drops it on load |
| HTML | `<span data-richeditor-kind="app:font" data-richeditor-attrs="slug=amiri">` | Span renders as plain text |

A format left out of `formats` simply drops the mark on export, so a style can be persisted in JSON and HTML but intentionally excluded from another format.

Error semantics differ by format on import. JSON treats a throwing decoder as an app bug and surfaces it as `MalformedRichTextJsonException`, while foreign data degrades: unregistered kinds and attrs shapes this version cannot read fall back to `Unknown`. HTML is lenient throughout (foreign clipboard content must never fail a paste), so unregistered kinds, opted-out formats, and failing decoders all degrade to plain text.

Because the HTML carrier also runs through `insertHtml` and the rich clipboard, copying and pasting between two editors that register the same descriptor preserves custom spans.

## Async resources

Style lambdas are only re-evaluated on content changes. If a resource a style depends on arrives late (an async-loaded font), call `state.invalidateStyles()` once it is available; it re-runs styling without touching the content, the selection, or the undo history.

## Limitations

- Markdown is not carried yet: markdown has no attribute syntax, so custom spans are dropped by `toMarkdown`. A future opt-in carrier may use inline HTML.
- The attribute map is `Map<String, String>`; encode richer payloads into strings yourself.
- Descriptors are consulted at export/import time only. Applying, editing, and rendering a custom style never requires registration.

## Related

- [Document model](rich_text_document.md)
- [JSON import/export](json_import_export.md)
- [HTML import/export](html_import_export.md)
