# Span Styling

Span styles allow you to format individual characters or ranges of text within a paragraph. You can control:
- Font weight (bold)
- Font style (italic)
- Text decoration (underline, strikethrough)
- Text color
- Background color
- Font size
- Font family
- Letter spacing
- And more

## Basic Usage

### Applying Styles

The `RichTextState` provides several methods to manage span styles:

```kotlin
// Toggle a style (add if not present, remove if present)
richTextState.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))

// Add a style (overwrites existing value)
richTextState.addSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))

// Add a style to specific range
richTextState.addSpanStyle(
    spanStyle = SpanStyle(fontWeight = FontWeight.Bold),
    textRange = TextRange(0, 5)
)

// Remove a style
richTextState.removeSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))

// Remove a style from specific range
richTextState.removeSpanStyle(
    spanStyle = SpanStyle(fontWeight = FontWeight.Bold),
    textRange = TextRange(0, 5)
)
```

### Checking Current Styles

To get the current span style of the selection:

```kotlin
val currentSpanStyle = richTextState.currentSpanStyle

// Check common style properties
val isBold = currentSpanStyle.fontWeight == FontWeight.Bold
val isItalic = currentSpanStyle.fontStyle == FontStyle.Italic
val isUnderlined = currentSpanStyle.textDecoration == TextDecoration.Underline

// Check style at specific range
val rangeStyle = richTextState.getSpanStyle(TextRange(0, 5))
val isRangeBold = rangeStyle.fontWeight == FontWeight.Bold
```

## Supported Properties

### Text Formatting

```kotlin
// Font weight
richTextState.addSpanStyle(SpanStyle(
    fontWeight = FontWeight.Bold  // or FontWeight.W500, etc.
))

// Font style
richTextState.addSpanStyle(SpanStyle(
    fontStyle = FontStyle.Italic
))

// Text decoration
richTextState.addSpanStyle(SpanStyle(
    textDecoration = TextDecoration.Underline
    // or TextDecoration.LineThrough
    // or TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough))
))
```

### Colors and Size

```kotlin
// Text color
richTextState.addSpanStyle(SpanStyle(
    color = Color.Blue
))

// Background color
richTextState.addSpanStyle(SpanStyle(
    background = Color.Yellow
))

// Font size
richTextState.addSpanStyle(SpanStyle(
    fontSize = 18.sp
))
```

### Advanced Properties

```kotlin
// Font family
richTextState.addSpanStyle(SpanStyle(
    fontFamily = FontFamily.Monospace
))

// Letter spacing
richTextState.addSpanStyle(SpanStyle(
    letterSpacing = 2.sp
))

// Baseline shift (subscript/superscript)
richTextState.addSpanStyle(SpanStyle(
    baselineShift = BaselineShift.Superscript
    // or BaselineShift.Subscript
))
```

### Combining Styles

You can combine multiple style properties in a single SpanStyle:

```kotlin
richTextState.addSpanStyle(SpanStyle(
    fontWeight = FontWeight.Bold,
    fontStyle = FontStyle.Italic,
    color = Color.Blue,
    fontSize = 16.sp,
    background = Color.Yellow.copy(alpha = 0.3f)
))
```

## Visual Examples

Here's how different span styles might appear:

```
Normal text
**Bold text**
*Italic text*
__Underlined text__
~~Strikethrough text~~
`Code text`
```

## Related Documentation

- For paragraph styling, see [Paragraph Style](paragraph_style.md)
- For HTML conversion, see [HTML Import and Export](html_import_export.md)
- For Markdown conversion, see [Markdown Import and Export](markdown_import_export.md)
