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
// Change link color and text decoration.
richTextState.config.linkColor = Color.Blue
richTextState.config.linkTextDecoration = TextDecoration.Underline

// Change code block colors.
richTextState.config.codeSpanColor = Color.Yellow
richTextState.config.codeSpanBackgroundColor = Color.Transparent
richTextState.config.codeSpanStrokeColor = Color.LightGray

// Change list indentation (ordered and unordered).
richTextState.config.listIndent = 20

// Change only ordered list indentation.
richTextState.config.orderedListIndent = 20

// Change only unordered list indentation.
richTextState.config.unorderedListIndent = 20
```

### Changing the editor's selection

The editor's selection can be changed using the `RichTextState.selection` property.

```kotlin
richTextState.selection = TextRange(0, 5)
```