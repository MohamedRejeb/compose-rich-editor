# Document model

`RichTextDocument` is an immutable, structural snapshot of the editor content. Two states with the same visible content and styling produce equal documents, no matter how the editor represented them internally. This makes it the right value for unit test assertions, persistence, diffing, and change observation, and it is the foundation of the [JSON import/export](json_import_export.md) format.

## Basic usage

```kotlin
val state = rememberRichTextState()

// Read an immutable snapshot of the content.
val document: RichTextDocument = state.toRichTextDocument()

// Snapshot only a range.
val selectionDocument = state.toRichTextDocument(state.selection)

// Replace the editor content from a document.
state.setRichTextDocument(document)

// Optionally pass the selection to apply after the load.
state.setRichTextDocument(document, selection = TextRange(0, 5))
```

`setRichTextDocument` clears undo history, matching `setHtml`. Without an explicit `selection` the caret moves to the end.

## Structure

A document is a list of blocks, one per paragraph:

| `RichTextBlock` field | Type | Meaning |
|---|---|---|
| `text` | `String` | Flattened paragraph text. List prefixes are excluded. Each inline image occupies one U+FFFC placeholder character. |
| `type` | `RichTextBlockType` | `Paragraph` or `ListItem(ordered, indent, startNumber)` |
| `spans` | `List<RichTextSpanMark>` | Styling marks over inclusive character ranges of `text` |
| `headingLevel` | `Int` | 0 for normal text, 1 to 6 for headings |
| `textAlign` | `TextAlign` | Paragraph alignment, `Unspecified` when not set |
| `textDirection` | `TextDirection` | `Ltr`, `Rtl`, `Content`, or `Unspecified` |
| `lineHeight` | `TextUnit` | Line height, `Unspecified` when not set |
| `textIndent` | `TextIndent?` | First and rest line indent, `null` when not set |
| `isLineBreak` | `Boolean` | True for paragraphs created by a `<br>` |

Styling is expressed as marks with inclusive ranges:

| Mark | Payload | Produced by |
|---|---|---|
| `Bold` | none | `FontWeight.Bold` |
| `Italic` | none | `FontStyle.Italic` |
| `Underline` | none | `TextDecoration.Underline` |
| `Strikethrough` | none | `TextDecoration.LineThrough` |
| `CodeSpan` | none | `RichSpanStyle.Code` |
| `Link` | `url` | `RichSpanStyle.Link` |
| `TextColor` | `argb` | `SpanStyle.color` |
| `Highlight` | `argb` | `SpanStyle.background` |
| `FontSize` | `size: TextUnit` | `SpanStyle.fontSize` |
| `FontWeight` | `weight: Int` | Non-bold font weights |
| `LetterSpacing` | `size: TextUnit` | `SpanStyle.letterSpacing` |
| `BaselineShift` | `multiplier: Float` | Subscript and superscript |
| `Shadow` | `argb`, `offsetX`, `offsetY`, `blurRadius` | `SpanStyle.shadow` |
| `Image` | `url`, `width`, `height`, `description` | Inline images |
| `Token` | `trigger`, `id`, `label` | Mentions and triggers |
| `Unknown` | `kind`, `rawJson` | Forward compatibility (see JSON docs) |
| `Custom` | `style: RichSpanStyle` | App-defined rich span styles, carried by instance |

The document is canonical: adjacent runs with equal styling merge into a single mark, marks are deterministically ordered, and heading or list visuals never appear as marks (they are carried by `headingLevel` and the block type).

## Character space

Mark ranges are inclusive character ranges over the owning block's `text`, in the same UTF-16 units as `annotatedString.text`. Each block is one paragraph, and `toText()` joins paragraphs with `\n`, so for content without list prefixes the block texts joined with `\n` are exactly `toText()` and a mark's global offset is its block-local offset plus the lengths of the preceding blocks and one separator each. List prefixes ("1. ", "• ") exist only in the rendered text, never in block text or mark ranges.

## Custom span styles

An app-defined `RichSpanStyle` (any class implementing the interface that is not one of the built-ins) survives the document as `RichTextSpanMark.Custom`, carrying the exact instance that was applied in the editor. Loading a document applies the carried instances back, so custom payloads (for example an app-domain font identity) round-trip without any registration:

```kotlin
class FontRunStyle(
    val slug: String,
    val fontFamily: FontFamily?, // render resource, excluded from equals
) : RichSpanStyle { /* ... */ }

state.addRichSpan(FontRunStyle("amiri", family), TextRange(0, 5))
val custom = state.toRichTextDocument().blocks.first().spans
    .filterIsInstance<RichTextSpanMark.Custom>()
    .first()
// custom.style is the same FontRunStyle instance
```

The style's own `equals` drives the document semantics: adjacent runs whose styles compare equal coalesce into one `Custom` mark, and applying a same-class style over an overlapping range replaces it in the overlap (last write wins). Keep render resources out of `equals` so differently-resolved instances of the same logical style still merge.

If a custom style resolves an external resource lazily (an async-loaded font, for example), call `state.invalidateStyles()` once the resource is available: style lambdas are only re-evaluated on content changes, and `invalidateStyles` re-runs them without touching the content, the selection, or the undo history.

To persist custom styles through JSON and HTML as well, register a descriptor on `state.spanStyleRegistry`; see [Custom span styles](custom_span_styles.md).

When several rich-span marks cover the same characters on load, one wins per segment in this order: `Image`, `Token`, `Link`, `CodeSpan`, `Custom`.

## Testing your editor

The document's value equality makes editor logic testable in plain unit tests, without composition:

```kotlin
@Test
fun `toolbar bold button bolds the selection`() {
    val state = RichTextState()
    state.setText("Hello world")
    state.selection = TextRange(0, 5)
    state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))

    assertEquals(
        RichTextDocument(
            blocks = listOf(
                RichTextBlock(
                    text = "Hello world",
                    spans = listOf(RichTextSpanMark.Bold(range = 0..4)),
                ),
            ),
        ),
        state.toRichTextDocument(),
    )
}
```

`toRichTextDocument()` is a plain conversion, like `toHtml()`. To observe content changes, observe `annotatedString` (which updates on every edit) and convert:

```kotlin
LaunchedEffect(state) {
    snapshotFlow { state.annotatedString }
        .map { state.toRichTextDocument() }
        .distinctUntilChanged()
        .collect { document -> viewModel.onContentChanged(document) }
}
```

## Limitations

- `RichTextConfig` (link color, list markers, indent widths, and so on) is presentation, not content, and is not part of the document.
- Ordered and unordered list marker styles are config-level and not captured; only `ordered`, `indent`, and `startNumber` are.
- Inline images whose `model` is not a `String` cannot be represented and are dropped from the snapshot.
- `Unknown` marks are not applied to a `RichTextState` when a document is loaded, so loading and re-saving JSON through the editor drops them (see the JSON docs).
- `Custom` marks serialize only when a descriptor is registered on the state's `spanStyleRegistry`; without one the JSON and HTML codecs skip them. See [Custom span styles](custom_span_styles.md).
- A range snapshot (`toRichTextDocument(range)`) preserves the visible numbering of ordered list items via `startNumber`; clipboard copies intentionally restart numbering at 1.

## Related

- [RichTextState](rich_text_state.md)
- [JSON import/export](json_import_export.md)
- [HTML import/export](html_import_export.md)
