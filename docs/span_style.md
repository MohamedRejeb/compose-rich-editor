# Styling Spans

To style spans, `RichTextState` provides `toggleSpanStyle` method:

```kotlin
// Toggle a span style.
richTextState.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))

// Add a span style.
richTextState.addSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))

// Remove a span style.
richTextState.removeSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
```

To get the current span style of the selection, use `RichTextState.currentSpanStyle`:

```kotlin
// Get the current span style.
val currentSpanStyle = richTextState.currentSpanStyle
val isBold = currentSpanStyle.fontWeight = FontWeight.Bold
val isItalic = currentSpanStyle.fontStyle = FontStyle.Italic
val isUnderline = currentSpanStyle.textDecoration = TextDecoration.Underline
```