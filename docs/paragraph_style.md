## Styling Paragraphs

To style paragraphs, `RichTextState` provides `toggleParagraphStyle` method:

```kotlin
// Toggle a paragraph style.
richTextState.toggleParagraphStyle(ParagraphStyle(textAlign = TextAlign.Center))

// Add a paragraph style.
richTextState.addParagraphStyle(ParagraphStyle(textAlign = TextAlign.Center))

// Remove a paragraph style.
richTextState.removeParagraphStyle(ParagraphStyle(textAlign = TextAlign.Center))
```

To get the current paragraph style of the selection, use `RichTextState.currentParagraphStyle`:

```kotlin
// Get the current paragraph style.
val currentParagraphStyle = richTextState.currentParagraphStyle
val isCentered = currentParagraphStyle.textAlign = TextAlign.Center
val isLeft = currentParagraphStyle.textAlign = TextAlign.Left
val isRight = currentParagraphStyle.textAlign = TextAlign.Right
```
