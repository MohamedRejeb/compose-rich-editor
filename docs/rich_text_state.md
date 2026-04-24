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

## Undo / Redo

`RichTextState` ships its own undo/redo stack that snapshots the full rich-text
tree (paragraphs, spans, list prefixes, link/image/token spans, selection, and
pending styles). It overrides `BasicTextField`'s built-in undo so rich content
never gets out of sync with plain text.

```kotlin
val state = rememberRichTextState(
    historyLimit = 100,
    coalesceWindowMs = 500L,
)

IconButton(onClick = { state.history.undo() }, enabled = state.history.canUndo) {
    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
}
IconButton(onClick = { state.history.redo() }, enabled = state.history.canRedo) {
    Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
}
```

### Keyboard shortcuts (hardware keyboard)

- Undo: `Ctrl+Z` (Windows/Linux) / `Cmd+Z` (macOS)
- Redo: `Ctrl+Shift+Z` (Windows/Linux) / `Cmd+Shift+Z` (macOS)

### Coalescing

Consecutive typing / deletion within `coalesceWindowMs` (default 500ms) collapses
into a single undo step. The following always start a new group:

- Line breaks (Enter)
- Formatting toggles (bold, color, link, list, paragraph style, rich span style)
- Structural changes (image/token insert, list level changes)
- Paste operations
- Programmatic replacements (`setHtml`, `setMarkdown`, `setConfig` - these also
  clear the stacks entirely since they typically mean "load a new document")
- Caret moves (do not push a snapshot but seal the pending group, so the next
  typed character starts a fresh undo step)

### Opt out

Pass `undoBehavior = UndoBehavior.Disabled` to any editor composable to fall
back to `BasicTextField`'s native shortcuts. You can still call
`state.history.undo()` directly from your own UI.

### Limits

`state.history.limit` caps the undo stack (default 100 entries; oldest are
evicted FIFO). `state.history.clear()` empties both stacks.

## Related Documentation

- For styling text spans, see [Span Style](span_style.md)
- For styling paragraphs, see [Paragraph Style](paragraph_style.md)
- For working with lists, see [Ordered and Unordered Lists](ordered_unordered_lists.md)
- For HTML conversion, see [HTML Import and Export](html_import_export.md)
- For Markdown conversion, see [Markdown Import and Export](markdown_import_export.md)
