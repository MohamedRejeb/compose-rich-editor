# Getting Started

## Artifacts

Compose Rich Editor has only one artifact, for now, published to `mavenCentral()`:

* `com.mohamedrejeb.richeditor:richeditor-compose`: The default artifact which includes all the functionalities of the library.

My goal is to move the core functionalities of the library to a separate artifact and keep the default artifact as a wrapper around the core artifact,
so I can support other targets like Android Views and Compose HTML.

## RichTextState

[`RichTextState`](rich_text_state.md) is a class that manages the state of the editor.

The `RichTextEditor` composable requires a `RichTextState` to manage the editor's state.

To create a `RichTextState`, use the `rememberRichTextState` function:

```kotlin
val state = rememberRichTextState()

RichTextEditor(
    state = state,
)
```

Check out [the full documentation](rich_text_state.md) for more info.

## Styling Spans

To style spans, `RichTextState` provides `toggleSpanStyle` method:

```kotlin
// Toggle a span style.
richTextState.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
```

To get the current span style of the selection, use `RichTextState.currentSpanStyle`:

```kotlin
// Get the current span style.
val currentSpanStyle = richTextState.currentSpanStyle
val isBold = currentSpanStyle.fontWeight == FontWeight.Bold
```

Check out [the full documentation](span_style.md) for more info.

## Styling Paragraphs

To style paragraphs, `RichTextState` provides `toggleParagraphStyle` method:

```kotlin
// Toggle a paragraph style.
richTextState.toggleParagraphStyle(ParagraphStyle(textAlign = TextAlign.Center))
```

To get the current paragraph style of the selection, use `RichTextState.currentParagraphStyle`:

```kotlin
// Get the current paragraph style.
val currentParagraphStyle = richTextState.currentParagraphStyle
val isCentered = currentParagraphStyle.textAlign == TextAlign.Center
```

Check out [the full documentation](paragraph_style.md) for more info.

## Supported Styling Formats

The styling formats supported by Compose Rich Editor:

### Text Formatting
* Bold
* Italic
* Underline
* Strikethrough
* Text color
* Background color
* Font size
* Any custom style using `SpanStyle`

### Paragraph Formatting
* Text Align
* Any custom style using `ParagraphStyle`

### Lists and Blocks
* Ordered List
* Unordered List
* Code Blocks

### Links
* Hyperlinks

There are some styling formats that are not supported yet, but I'm planning to add them in the future:

* Images
* Blockquotes
* Multiline Code Blocks
* Checkbox
