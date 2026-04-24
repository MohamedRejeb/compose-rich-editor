# Ordered and Unordered Lists

## Table of Contents
- [Basic Usage](#basic-usage)
- [List Nesting and Levels](#list-nesting-and-levels)
- [List Style Types](#list-style-types)
  - [Ordered Lists](#ordered-lists)
  - [Unordered Lists](#unordered-lists)
- [List Indentation](#list-indentation)
- [List Prefix Alignment](#list-prefix-alignment)
- [List Marker Style](#list-marker-style)
- [Common Operations](#common-operations)
  - [Default Values](#default-values)
  - [Keyboard Shortcuts](#keyboard-shortcuts)
  - [List Behavior](#list-behavior)
  - [Visual Examples](#visual-examples)

## Basic Usage

You can add ordered and unordered lists using `RichTextState`:

```kotlin
// Toggle ordered list.
richTextState.toggleOrderedList()

// Toggle unordered list.
richTextState.toggleUnorderedList()
```

You can get if the current selection is an ordered or unordered list, using `RichTextState`:

```kotlin
// Get if the current selection is an ordered list.
val isOrderedList = richTextState.isOrderedList

// Get if the current selection is an unordered list.
val isUnorderedList = richTextState.isUnorderedList
```

## List Nesting and Levels

You can increase or decrease the nesting level of lists using `RichTextState`:

```kotlin
// Increase list level (nesting)
richTextState.increaseListLevel()

// Decrease list level (un-nesting)
richTextState.decreaseListLevel()

// Check if list level can be increased/decreased
val canIncrease = richTextState.canIncreaseListLevel
val canDecrease = richTextState.canDecreaseListLevel
```

## List Style Types

### Ordered Lists

You can customize the style of ordered lists using different formats:

```kotlin
// Set ordered list style type
richTextState.config.orderedListStyleType = OrderedListStyleType.Decimal     // 1, 2, 3, ...
richTextState.config.orderedListStyleType = OrderedListStyleType.LowerAlpha  // a, b, c, ...
richTextState.config.orderedListStyleType = OrderedListStyleType.UpperAlpha  // A, B, C, ...
richTextState.config.orderedListStyleType = OrderedListStyleType.LowerRoman  // i, ii, iii, ...
richTextState.config.orderedListStyleType = OrderedListStyleType.UpperRoman  // I, II, III, ...

// Use different styles for different nesting levels
richTextState.config.orderedListStyleType = OrderedListStyleType.Multiple(
    OrderedListStyleType.UpperAlpha,  // First level: A, B, C
    OrderedListStyleType.LowerAlpha,  // Second level: a, b, c
    OrderedListStyleType.Decimal      // Third level: 1, 2, 3
)
```

### Unordered Lists

You can customize the style of unordered lists using different bullet types:

```kotlin
// Set unordered list style type
richTextState.config.unorderedListStyleType = UnorderedListStyleType.Disc    // •
richTextState.config.unorderedListStyleType = UnorderedListStyleType.Circle  // ◦
richTextState.config.unorderedListStyleType = UnorderedListStyleType.Square  // ▪

// Use custom markers for different nesting levels
richTextState.config.unorderedListStyleType = UnorderedListStyleType.from(
    "•",  // First level
    "◦",  // Second level
    "▪"   // Third level
)
```

## List Indentation

You can control the list indentation using `RichTextState`:

```kotlin
// Change list indentation (ordered and unordered).
richTextState.config.listIndent = 20

// Change only ordered list indentation.
richTextState.config.orderedListIndent = 20

// Change only unordered list indentation.
richTextState.config.unorderedListIndent = 20
```

## List Prefix Alignment

> **Note:** This API is marked `@ExperimentalRichTextApi` and may change in a future release.

You can control where the list marker (`1.`, `10.`, `•`, ...) sits relative to the
indent "gutter" using `ListPrefixAlignment`:

```kotlin
// Default: HTML-style — the marker sits inside the gutter and ends at the content
// start, so "1." and "10." have their dots aligned vertically.
richTextState.config.listPrefixAlignment = ListPrefixAlignment.End

// Alternative: every item's marker starts at the same left edge, giving a uniform
// left edge even when item widths differ (e.g. "1." vs "10.").
richTextState.config.listPrefixAlignment = ListPrefixAlignment.Start
```

| Alignment | Layout | Example |
|---|---|---|
| `End` (default) | marker right-aligned to content start, dots align vertically | `` 1.`` / ``10.`` / ``11.`` — dots stacked |
| `Start` | marker left-aligned to the indent origin, marker left edges stack | ``1.`` / ``10. `` / ``11. `` — numbers stacked |

If the configured indent is smaller than the marker width (for example
`orderedListIndent = 0`), `End` clamps the first-line indent at `0` so the
marker stays visible. Clamping (rather than switching formulas per item) keeps
the whole list on a single layout so paragraphs with different marker widths
stay visually consistent.

## List Marker Style

> **Note:** This API is marked `@ExperimentalRichTextApi` and may change in a future release.

You can control how list markers (`•`, `1.`, etc.) inherit styles from the list item's text using `ListMarkerStyleBehavior`:

```kotlin
// Default: marker inherits typography from the item's text but drops decorations.
richTextState.config.listMarkerStyleBehavior = ListMarkerStyleBehavior.InheritFromText

// Alternative: every marker renders with the default span style regardless of content.
richTextState.config.listMarkerStyleBehavior = ListMarkerStyleBehavior.AlwaysDefault
```

With `InheritFromText` (the default), markers match the paragraph's typography so they stay visually aligned with the text. This matches Google Docs behavior:

| Attribute | Applied to marker |
|---|---|
| `color` | yes |
| `fontSize` | yes |
| `fontWeight` (bold) | yes |
| `fontStyle` (italic) | yes |
| `fontFamily` | yes |
| `letterSpacing` | yes |
| `textDecoration` (underline, strikethrough) | no |
| `background` (highlight) | no |
| `baselineShift` (super/subscript) | no |
| `shadow` | no |
| `textGeometricTransform` | no |

Use `AlwaysDefault` when you want bullets that look identical no matter how the line is formatted (for example, editors that style markers entirely through a separate theme).

## Common Operations

### Default Values
By default, the Rich Text Editor uses these configurations:
- Ordered List Style: `OrderedListStyleType.Multiple` with:
  - First level: `Decimal` (1, 2, 3, ...)
  - Second level: `LowerRoman` (i, ii, iii, ...)
  - Third level: `LowerAlpha` (a, b, c, ...)
- Unordered List Style: `UnorderedListStyleType.from` with:
  - First level: `•` (bullet)
  - Second level: `◦` (circle)
  - Third level: `▪` (square)
- List Indentation: 38
- List Prefix Alignment: `ListPrefixAlignment.End` (HTML-style, dots aligned)
- List Marker Style: `ListMarkerStyleBehavior.InheritFromText`
- Exit List on Empty Item: `true` (configurable via `richTextState.config.exitListOnEmptyItem`)

### Keyboard Shortcuts
The editor supports common keyboard shortcuts for list operations:
- `Tab`: Increase list level (indent)
- `Shift + Tab`: Decrease list level (outdent)
- `Enter` on an empty list item: Exit the list
- `Backspace` at the start of a list item: Decrease list level or exit list

### List Behavior
When working with lists:
1. Creating a new list item:
   - Press `Enter` at the end of a list item to create a new one
   - The new item inherits the same level and style as the previous one

2. Exiting a list:
   - Press `Enter` on an empty list item (configurable via `exitListOnEmptyItem`)
   - Press `Backspace` at the start of an empty list item:
     - When list level > 1: Decreases the list level by 1
     - When list level = 1: Exits the list

   > Note: The behavior of pressing `Enter` on an empty list item can be configured using `richTextState.config.exitListOnEmptyItem`. When set to `true` (default), it exits the list. When set to `false`, it creates a new list item.

3. Converting between list types:
   - You can convert between ordered and unordered lists by toggling the respective type
   - The list level and content are preserved during conversion

### Visual Examples

> Note: The following examples are representations of how lists might appear. The actual appearance in your application may vary based on your configuration settings (style type, indentation, etc.).

#### Ordered List Example
```
1. First level item
   a. Second level item
      i. Third level item
   b. Another second level
2. Back to first level
```

#### Unordered List Example
```
• First level item
  ◦ Second level item
    ▪ Third level item
  ◦ Another second level
• Back to first level
```

## Related Documentation

- For HTML list import/export, see [HTML Import and Export](html_import_export.md)
- For Markdown list import/export, see [Markdown Import and Export](markdown_import_export.md)
