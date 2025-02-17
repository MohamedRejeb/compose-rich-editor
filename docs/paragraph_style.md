# Paragraph Styling

The Rich Text Editor provides comprehensive support for paragraph styling, allowing you to control:
- Text alignment
- Line spacing
- Paragraph spacing
- Text direction
- Text indentation

## Basic Usage

### Applying Styles

To style paragraphs, `RichTextState` provides several methods:

```kotlin
// Toggle a paragraph style (adds if not present, removes if present)
richTextState.toggleParagraphStyle(ParagraphStyle(textAlign = TextAlign.Center))

// Add a paragraph style (overwrites existing value)
richTextState.addParagraphStyle(ParagraphStyle(textAlign = TextAlign.Center))

// Remove a paragraph style (restores default value)
richTextState.removeParagraphStyle(ParagraphStyle(textAlign = TextAlign.Center))
```

### Checking Current Styles

To get the current paragraph style of the selection:

```kotlin
// Get the current paragraph style
val currentParagraphStyle = richTextState.currentParagraphStyle

// Check text alignment
val isCentered = currentParagraphStyle.textAlign == TextAlign.Center
val isLeft = currentParagraphStyle.textAlign == TextAlign.Left
val isRight = currentParagraphStyle.textAlign == TextAlign.Right
val isJustified = currentParagraphStyle.textAlign == TextAlign.Justify
```

## Supported Properties

### Text Alignment

```kotlin
// Center alignment
richTextState.addParagraphStyle(ParagraphStyle(
    textAlign = TextAlign.Center
))

// Left alignment
richTextState.addParagraphStyle(ParagraphStyle(
    textAlign = TextAlign.Left
))

// Right alignment
richTextState.addParagraphStyle(ParagraphStyle(
    textAlign = TextAlign.Right
))

// Justified alignment
richTextState.addParagraphStyle(ParagraphStyle(
    textAlign = TextAlign.Justify
))
```

### Line Spacing

```kotlin
// Set line spacing
richTextState.addParagraphStyle(ParagraphStyle(
    lineHeight = 1.5.em  // 1.5 times the font size
))
```

### Text Direction

```kotlin
// Right-to-left text direction
richTextState.addParagraphStyle(ParagraphStyle(
    textDirection = TextDirection.Rtl
))

// Left-to-right text direction
richTextState.addParagraphStyle(ParagraphStyle(
    textDirection = TextDirection.Ltr
))
```

### Text Indentation

```kotlin
// Set text indentation
richTextState.addParagraphStyle(ParagraphStyle(
    textIndent = TextIndent(
        firstLine = 20.sp,    // First line indent
        restLine = 10.sp      // Rest of lines indent
    )
))
```

## Related Documentation

- For HTML paragraph style import/export, see [HTML Import and Export](html_import_export.md)
- For Markdown paragraph style import/export, see [Markdown Import and Export](markdown_import_export.md)
