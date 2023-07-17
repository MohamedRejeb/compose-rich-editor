# Upgrading from Compose Rich Editor 0.x to 1.x

This is a short guide to highlight the main changes when upgrading from Compose Rich Editor 0.x to 1.x and how to handle them.

## Calling `RichTextEditor` with `RichTextState`

In 0.x you would use `RichTextValue` to create the rich text state and pass it to `RichTextEditor`:

```kotlin
var richTextValue by remember { mutableStateOf(RichTextValue()) }

RichTextEditor(
    value = richTextValue,
    onValueChange = {
        richTextValue = it
    },
)
```

In 1.x `RichTextValue` is deprecated and you should use `RichTextState` instead:

```kotlin
val richTextState = rememberRichTextState()

RichTextEditor(
    state = richTextState,
)
```