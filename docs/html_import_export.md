# HTML import and export

To convert HTML to `RichTextState`, use `RichTextState.setHtml` method:

```kotlin
val html = "<p><b>Compose Rich Editor</b></p>"
richTextState.setHtml(html)
```

To convert `RichTextState` to HTML, use `RichTextState.toHtml` method:

```kotlin
val html = richTextState.toHtml()
```