# Ordered and Unordered Lists

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

You can control the list indentation using `RichTextState`:

```kotlin
// Change list indentation (ordered and unordered).
richTextState.config.listIndent = 20

// Change only ordered list indentation.
richTextState.config.orderedListIndent = 20

// Change only unordered list indentation.
richTextState.config.unorderedListIndent = 20
```