# JSON import/export

The `richeditor-compose-json` module serializes editor content to a stable, versioned JSON format built on the [document model](rich_text_document.md). Unlike HTML or Markdown, the JSON format is lossless for everything the editor supports, canonical (the same content always produces the same string), and safe to store, compare, and migrate.

## Installation

```kotlin
dependencies {
    implementation("com.mohamedrejeb.richeditor:richeditor-compose-json:1.0.0")
}
```

Use the same version as `richeditor-compose`.

## Basic usage

```kotlin
val state = rememberRichTextState()

// Export
val json: String = state.toJson()

// Import
state.setJson(json)
```

Both are thin wrappers over `RichTextDocumentCodec`, which you can use directly when working with documents instead of states:

```kotlin
val document = RichTextDocumentCodec.decodeFromString(json)
val encoded = RichTextDocumentCodec.encodeToString(document)
```

## Format

The envelope is `{"v": 1, "blocks": [...]}` where `v` is the schema version. Example:

```json
{
  "v": 1,
  "blocks": [
    {"id": "b0", "type": "heading", "level": 1, "text": "Title", "spans": []},
    {"id": "b1", "type": "list-item", "ordered": true, "indent": 0, "text": "One", "spans": []},
    {
      "id": "b2",
      "type": "paragraph",
      "align": "center",
      "text": "Hello bold link",
      "spans": [
        {"k": "bold", "r": [6, 9]},
        {"k": "link", "r": [11, 14], "url": "https://example.com"}
      ]
    }
  ]
}
```

Block fields:

| Field | When present | Meaning |
|---|---|---|
| `id` | always | `"b" + index`; accepted and ignored on import |
| `type` | always | `paragraph`, `heading`, or `list-item` |
| `level` | headings | Heading level 1 to 6 |
| `ordered`, `indent`, `start` | list items | Ordered flag, 0-based nesting, numbering restart |
| `align`, `dir`, `lineHeight`, `textIndent` | when set | Paragraph style |
| `br` | line-break paragraphs | Paragraph created by `<br>` |
| `text`, `spans` | always | Content and styling marks |

Each mark is `{"k": kind, "r": [first, last], ...}` with an inclusive range. Kinds: `bold`, `italic`, `underline`, `strike`, `code`, `link` (`url`), `color` and `highlight` (`argb` as 8-char uppercase hex), `font-size` and `letter-spacing` (`value`, `unit`), `font-weight` (`value`), `baseline-shift` (`value`), `shadow` (`argb`, `x`, `y`, `blur`), `image` (`url`, `width`, `height`, `alt`), `token` (`trigger`, `id`, `label`).

## Stability guarantees

- **Versioned**: every document carries `"v"`. Documents from newer schema versions are rejected with `UnsupportedRichTextJsonVersionException` instead of being decoded lossily.
- **Canonical**: the same content always encodes to the same string, so JSON strings can be compared for equality in tests and caches.
- **Idempotent round-trip**: `toJson` after `setJson` returns the identical string for any document made of editor-representable content (everything in the tables above except `Unknown` marks).
- **Unknown preservation at the codec level**: mark kinds this version does not understand decode as `RichTextSpanMark.Unknown` and `RichTextDocumentCodec` re-encodes them verbatim, so passing documents through the codec is lossless. Loading into a `RichTextState` is different: the editor cannot represent unknown marks, so `setJson` followed by `toJson` drops them. To keep forward-compatible data intact, store the original JSON and treat the editor as a consumer, or merge edited output with the preserved marks yourself.
- **Custom marks are not serialized**: `RichTextSpanMark.Custom` carries an in-memory `RichSpanStyle` instance the JSON format cannot represent, so the codec skips it on encode. Persist custom payloads in your own model.
- **Strict validation**: structurally invalid input (non-finite numbers, out-of-range font weights, malformed `argb`, negative indents) fails fast with `MalformedRichTextJsonException` rather than producing a document that cannot be re-encoded. `start` may be negative, matching the HTML `start` attribute.
- **v2 compatible**: the envelope, block fields, and the core mark kinds (`bold` through `highlight`) match the upcoming richeditor v2 document format, so stored v1 documents remain readable there.

## Error handling

```kotlin
try {
    state.setJson(json)
} catch (e: MalformedRichTextJsonException) {
    // Structurally invalid input
} catch (e: UnsupportedRichTextJsonVersionException) {
    // Written by a newer library version (e.documentVersion)
}
```

Both extend `IllegalArgumentException`.

## Related

- [Document model](rich_text_document.md)
- [HTML import/export](html_import_export.md)
- [Markdown import/export](markdown_import_export.md)
