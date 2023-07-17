# Code Blocks

To add code blocks, `RichTextState` provides `toggleCode` method:

```kotlin
// Toggle code block.
richTextState.toggleCode()
```

To get if the current selection is a code block, use `RichTextState.isCode`:

```kotlin
// Get if the current selection is a code block.
val isCode = richTextState.isCode
```