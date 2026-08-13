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
```

`setRichTextDocument` clears undo history and moves the selection to the end, matching `setHtml`.

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

The document is canonical: adjacent runs with equal styling merge into a single mark, marks are deterministically ordered, and heading or list visuals never appear as marks (they are carried by `headingLevel` and the block type).

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

To observe content changes, read the document inside a snapshot observer:

```kotlin
LaunchedEffect(state) {
    snapshotFlow { state.toRichTextDocument() }
        .distinctUntilChanged()
        .collect { document -> viewModel.onContentChanged(document) }
}
```

## Limitations

- `RichTextConfig` (link color, list markers, indent widths, and so on) is presentation, not content, and is not part of the document.
- Ordered and unordered list marker styles are config-level and not captured; only `ordered`, `indent`, and `startNumber` are.
- `RichSpanStyle.Code` visual parameters (corner radius, stroke, padding) are not captured; a `CodeSpan` mark decodes with defaults.
- Inline images whose `model` is not a `String` cannot be represented and are dropped from the snapshot.
- `Unknown` marks survive `RichTextDocumentCodec` round-trips but are not applied to a `RichTextState` when a document is loaded, so loading and re-saving through the editor drops them.
- A range snapshot (`toRichTextDocument(range)`) preserves the visible numbering of ordered list items via `startNumber`; clipboard copies intentionally restart numbering at 1.

## Related

- [RichTextState](rich_text_state.md)
- [JSON import/export](json_import_export.md)
- [HTML import/export](html_import_export.md)
