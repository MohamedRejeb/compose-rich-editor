# RichTextState

`RichTextState` is the core component that manages the state of the Rich Text Editor. It handles:
- Text content and styling
- Selection and cursor position
- Configuration settings
- Text operations and modifications
- Import/export functionality

## Basic Usage

### Creating the State

To create a `RichTextState`, use the `rememberRichTextState` function:

```kotlin
val state = rememberRichTextState()

RichTextEditor(
    state = state,
    modifier = Modifier.fillMaxWidth()
)
```

## Configuration

### Appearance Settings

```kotlin
// Link appearance
richTextState.config.linkColor = Color.Blue
richTextState.config.linkTextDecoration = TextDecoration.Underline

// Code block appearance
richTextState.config.codeSpanColor = Color.Yellow
richTextState.config.codeSpanBackgroundColor = Color.Transparent
richTextState.config.codeSpanStrokeColor = Color.LightGray
```

### List Configuration

```kotlin
// Global list indentation
richTextState.config.listIndent = 20

// Specific list type indentation
richTextState.config.orderedListIndent = 20
richTextState.config.unorderedListIndent = 20

// List behavior
richTextState.config.exitListOnEmptyItem = true  // Exit list when pressing Enter on empty item
```

## Text Operations

### Selection Management

The editor's selection can be controlled programmatically:

```kotlin
// Set selection range
richTextState.selection = TextRange(0, 5)

// Select all text
richTextState.selection = TextRange(0, richTextState.annotatedString.text.length)

// Move cursor to end
richTextState.selection = TextRange(richTextState.annotatedString.text.length)
```

### Text Modification

The `RichTextState` provides methods to modify text while preserving styles:

```kotlin
// Insert text at specific position
richTextState.addTextAtIndex(5, "Hello")

// Insert text after current selection
richTextState.addTextAfterSelection("Hello")

// Remove text
richTextState.removeTextRange(TextRange(0, 5))
richTextState.removeSelectedText()

// Replace text
richTextState.replaceTextRange(TextRange(0, 5), "Hello")
richTextState.replaceSelectedText("Hello")
```

### Text Change Monitoring

You can monitor text changes using the `annotatedString` property:

```kotlin
LaunchedEffect(richTextState.annotatedString) {
    // Handle text changes
    println("Text changed: ${richTextState.annotatedString.text}")
}
```

## State Persistence

To save and restore the editor's state:

```kotlin
// Save state
val html = richTextState.toHtml()
// or
val markdown = richTextState.toMarkdown()

// Restore state
richTextState.setHtml(savedHtml)
// or
richTextState.setMarkdown(savedMarkdown)
```

## Related Documentation

- For styling text spans, see [Span Style](span_style.md)
- For styling paragraphs, see [Paragraph Style](paragraph_style.md)
- For working with lists, see [Ordered and Unordered Lists](ordered_unordered_lists.md)
- For HTML conversion, see [HTML Import and Export](html_import_export.md)
- For Markdown conversion, see [Markdown Import and Export](markdown_import_export.md)
