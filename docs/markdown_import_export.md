# Markdown import and export

To convert Markdown to `RichTextState`, use `RichTextState.setHtml` method:

```kotlin
val markdown = "**Compose** *Rich* Editor"
richTextState.setMarkdown(markdown)
```

To convert `RichTextState` to Markdown, use `RichTextState.toHtml` method:

```kotlin
val markdown = richTextState.toMarkdown()
```