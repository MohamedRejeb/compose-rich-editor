# Paragraph Styling

The Rich Text Editor provides comprehensive support for paragraph styling, allowing you to control:
- Text alignment
- Line spacing
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

#### The same line height for every line

Each paragraph of the document is laid out on its own. With a plain `lineHeight` in the text style, Compose trims the extra space above the first line and below the last line of every paragraph. A paragraph of one line then keeps its natural height, the gap between two paragraphs is smaller than the gap between two lines of a wrapped paragraph, and an empty line from a `<br>` is shorter than the `lineHeight` you set.

To give every line the full `lineHeight`, including single-line paragraphs and empty lines, turn the trimming off in the text style you pass to the editor or to `RichText`:

```kotlin
RichTextEditor(
    state = richTextState,
    textStyle = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
    ),
)
```

The Material 3 typography styles already set this, so an editor using `MaterialTheme.typography` text styles has uniform lines without it.

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
