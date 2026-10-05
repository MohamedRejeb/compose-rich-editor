# Editor features

An editor rarely supports everything the library can represent. A comment box may offer bold, italic and underline and nothing else, and content it cannot render or persist (colors, links, lists, images pasted from a browser) can break it. `RichTextConfig.features` declares the set of formatting kinds an editor supports. Everything outside the set is stripped from content entering the editor and ignored by the formatting mutators, so content is held to the set from the moment it is declared.

!!! note
    `RichTextFeature` and `RichTextConfig.features` are marked `@ExperimentalRichTextApi`.

## Basic usage

```kotlin
val state = rememberRichTextState()

LaunchedEffect(state) {
    state.config.features = setOf(
        RichTextFeature.Bold,
        RichTextFeature.Italic,
        RichTextFeature.Underline,
    )
}
```

The default is `RichTextFeature.All`, which allows everything. An empty set makes a plain text editor: every paste is a plain text paste and every formatting call is a no-op.

```kotlin
state.config.features = emptySet()
```

The toolbar can read the same set to decide which buttons to show:

```kotlin
if (RichTextFeature.Link in state.config.features) {
    LinkButton(onClick = { state.addLinkToSelection(url) })
}
```

## Features

| Feature | Covers |
|---|---|
| `Bold` | Font weight 700, `<b>` and `<strong>`, `**text**` |
| `Italic` | Italic font style |
| `Underline` | Underline text decoration |
| `Strikethrough` | Line-through text decoration |
| `TextColor` | Text color |
| `Highlight` | Background color behind the text |
| `FontSize` | An explicit font size |
| `FontWeight` | Any explicit font weight other than 700 |
| `LetterSpacing` | Letter spacing |
| `BaselineShift` | Subscript and superscript |
| `Shadow` | Text shadow |
| `Link` | Links (`RichSpanStyle.Link`) |
| `CodeSpan` | Inline code (`RichSpanStyle.Code`) |
| `Image` | Inline images (`RichSpanStyle.Image`) |
| `Token` | Mention-like tokens (`RichSpanStyle.Token`) |
| `CustomSpanStyle` | Every app-defined `RichSpanStyle` |
| `OrderedList` | Ordered list items |
| `UnorderedList` | Unordered list items |
| `Heading` | Heading levels 1 to 6 |
| `ParagraphStyle` | Paragraph alignment, direction, first-line indent and line height |

Each feature corresponds to one `RichTextSpanMark` or one block attribute of the [document model](rich_text_document.md). `RichTextFeature.All` is computed from the enum, so a library version that adds a feature keeps it allowed for editors on the default and excluded for editors with an explicit set.

## What the set applies to

**Content entering the editor.** `setHtml`, `setMarkdown`, `setRichTextDocument`, `insertHtml`, `insertHtmlAfterSelection`, `insertMarkdown`, `insertMarkdownAfterSelection` and every clipboard paste keep only the allowed features:

- A disallowed span mark is removed and its text kept. A link becomes its text, a code span becomes plain text, a token keeps its label.
- A disallowed list item or heading becomes a plain paragraph with the same text.
- A disallowed paragraph style (alignment, direction, indent, line height) is reset.
- A disallowed image is removed together with its placeholder character. A paragraph that held only an image is dropped, and a paste that held only images inserts nothing.

**Formatting calls.** `toggleSpanStyle` and `addSpanStyle` apply only the fields of allowed features, so a `SpanStyle(fontWeight = Bold, color = Red)` under a bold-only set applies bold. `addRichSpan`, `addLinkToSelection`, `addLinkToTextRange`, `addCodeSpan`, `setHeadingStyle`, `addParagraphStyle`, `addOrderedList` and `addUnorderedList` are no-ops for a disallowed feature. The toggles never switch a disallowed feature on, but still switch it off where it is already applied. `addLink(text, url)` inserts the plain text. `insertToken` cancels the active trigger query. Every `remove*` call keeps working, so a toolbar can always clear formatting that was allowed when it was applied.

**Typing shortcuts.** `- `, `* ` or a number followed by `. ` at the start of a paragraph convert it to a list only when that list kind is allowed. To keep lists and turn off only these shortcuts, set `config.listTypingShortcutsEnabled = false`. The heading shortcut (`# ` at a paragraph start) and the inline shortcuts (`**bold**`, `*italic*`, `` `code` ``, `~~strike~~`) follow the same rule: a mark whose feature is not allowed stays as text. Both are off by default, see `config.headingTypingShortcutsEnabled` and `config.inlineTypingShortcutsEnabled`.

**Paste with no features.** With an empty set the clipboard's HTML is ignored and the pasted text flows through the plain text path: it inherits the style at the caret, exactly like typed text, and keeps its paragraph breaks. This is the same path `richClipboardEnabled = false` uses for paste.

## Relation to `richClipboardEnabled`

`richClipboardEnabled` controls copy and paste together. Set to `false`, copy writes plain text and paste inserts plain text, whatever the feature set. The feature set only affects what comes in, so an editor can keep rich copy while accepting a restricted or plain paste.

## Changing the set at runtime

The set applies to content that enters after the change. Existing content is not re-filtered: narrowing the set on an editor that already holds a link keeps the link, and `removeLink()` still removes it.

## Limitations

- A heading's own bold is part of the heading. When `Heading` is disallowed, `<h1>Title</h1>` becomes a plain paragraph with no bold, and an explicit `<b>` inside a heading is indistinguishable from the heading's bold and goes with it. Marks a heading does not carry by itself (italic, color, underline) stay, each subject to its own feature.
- Under an empty set a paste inherits the caret's style like typed text. Under a non-empty set a paste keeps its own allowed styles and does not inherit the caret's, exactly as a rich paste does today.
- `SpanStyle` fields no feature governs (font family, font synthesis, feature settings, geometric transform, locale, draw style) pass through the formatting calls unchanged. The HTML and Markdown parsers never produce them.
- A `SpanStyle` built with a `brush` is governed by `TextColor`: the brush is kept when text color is allowed and dropped otherwise.

## Related

- [RichTextState](rich_text_state.md)
- [Document model](rich_text_document.md)
- [Span Style](span_style.md)
- [Custom span styles](custom_span_styles.md)
