# RichTextState

Use `RichTextEditor` composable to create a rich text editor.

The `RichTextEditor` composable requires a `RichTextState` to manage the editor's state.

To create a `RichTextState`, use the `rememberRichTextState` function:

```kotlin
val state = rememberRichTextState()

RichTextEditor(
    state = state,
)
```

### Customizing the rich text configuration

Some of the rich text editor's features can be customized, such as the color of the links and the code blocks.

```kotlin
richTextState.setConfig(
    linkColor = Color.Blue,
    linkTextDecoration = TextDecoration.Underline,
    codeColor = Color.Yellow,
    codeBackgroundColor = Color.Transparent,
    codeStrokeColor = Color.LightGray,
)
```

### Changing the editor's selection

The editor's selection can be changed using the `RichTextState.selection` property.

```kotlin
richTextState.selection = TextRange(0, 5)
```