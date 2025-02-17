# Code Formatting

## Code Spans

Code spans are used to highlight inline code within text. They are perfect for referencing:
- Variable names
- Function names
- Short code snippets
- File names

To add code spans, `RichTextState` provides `toggleCodeSpan` method:

```kotlin
// Toggle code span.
richTextState.toggleCodeSpan()
```

To get if the current selection is a code span, use `RichTextState.isCodeSpan`:

```kotlin
// Get if the current selection is a code span.
val isCodeSpan = richTextState.isCodeSpan
```

Example of how code spans appear:
Normal text with `inline code` within it.

## Code Blocks

Multiline code blocks are planned for a future release. They will support:
- Syntax highlighting
- Multiple lines of code
- Language specification
- Copy to clipboard functionality

Stay tuned for updates!
