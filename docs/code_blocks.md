# Code Formatting

## Code Spans

Code spans are used to highlight inline code within text. They are perfect for referencing:
- Variable names
- Function names
- Short code snippets
- File names

To add code spans, `RichTextState` provides `toggleCodeSpan` method:

```kotlin
// Toggle code span.
richTextState.toggleCodeSpan()
```

To get if the current selection is a code span, use `RichTextState.isCodeSpan`:

```kotlin
// Get if the current selection is a code span.
val isCodeSpan = richTextState.isCodeSpan
```

Example of how code spans appear:
Normal text with `inline code` within it.

## Code Blocks

A fenced Markdown code block or an HTML `<pre>` is imported as a code block: its lines are drawn
in a fixed-width font on a background, and coloured by the language the block names (keywords,
strings, numbers, comments, annotations). `RichText` and the editors both draw it. The colours
are presentation only, they are never written to HTML, Markdown or JSON.

Code blocks are experimental API (`@ExperimentalRichTextApi`).

### Basic usage

```kotlin
val state = rememberRichTextState()

LaunchedEffect(Unit) {
    state.setMarkdown(
        """
        Here is a function:

        ```kotlin
        fun greet() = println("Hi")
        ```
        """.trimIndent()
    )
}

RichText(state = state, modifier = Modifier.fillMaxWidth())
```

The background is as wide as the composable, so give it the width the block should have
(`fillMaxWidth()` in most layouts).

The same block in each format:

| Format | Form |
|---|---|
| Markdown | A fence of three or more backticks with the language after it. Export uses a fence longer than any backtick run in the code |
| HTML | `<pre><code class="language-kotlin">...</code></pre>`. A `<pre>` without `<code>` or without a class is a block with no language |
| JSON | One `"type": "code-block"` block per line, with `"language"` and `"start": true` on the first line of a block |

The text inside a block is kept exactly as written: indentation, tabs, blank lines, and Markdown
or HTML that would otherwise be formatting.

### Copy and paste

Copying or cutting a selection that covers code writes the code line by line, with its
indentation, as plain text and as a `<pre><code>` block in HTML.

A code block pasted into ordinary text stays whole: the text is split around it. A single line
of code pasted into a sentence joins the sentence as text. Whatever is pasted into a code block,
formatted or not, becomes plain lines of that block.

Typing shortcuts (`**bold**`, `__bold__`, `` `code` `` and the others) do nothing inside a code
block, where those marks are code.

### Copy button

A read-only `RichText` can show a copy button at the top end corner of each code block. It copies
the block's code exactly, line ends and indentation included:

```kotlin
RichText(
    state = state,
    modifier = Modifier.fillMaxWidth(),
    showCodeBlockCopyButton = true,
)
```

The Material and Material3 `RichText` both have the option, and it is off by default. For a
button of your own, or any other action, `BasicRichText` takes a slot that is given the code and
the language of each block:

```kotlin
BasicRichText(
    state = state,
    modifier = Modifier.fillMaxWidth(),
    codeBlockAction = { code, language ->
        MyCopyButton(onClick = { copyToClipboard(code) })
    },
)
```

The action is drawn over the corner of the block and takes no room of its own, so keep it about
one line tall: on a block with a long first line it covers the end of that line. The editors do
not have this option.

### Customizing

| Property on `RichTextState.config` | Type | Default | Description |
|---|---|---|---|
| `codeBlockBackgroundColor` | `Color` | translucent grey | The background drawn behind a block |
| `codeBlockFontFamily` | `FontFamily` | `FontFamily.Monospace` | The font of the code |
| `codeBlockColors` | `CodeBlockColors` | `CodeBlockColors.Default` | The style of each kind of token |

`CodeBlockColors` holds a `SpanStyle` per token kind, not just a colour, so a comment can be
italic:

```kotlin
val scheme = MaterialTheme.colorScheme

state.config.codeBlockBackgroundColor = scheme.surfaceVariant
state.config.codeBlockColors = CodeBlockColors(
    keyword = SpanStyle(color = scheme.primary),
    string = SpanStyle(color = scheme.tertiary),
    number = SpanStyle(color = scheme.secondary),
    comment = SpanStyle(color = scheme.outline, fontStyle = FontStyle.Italic),
    annotation = SpanStyle(color = scheme.secondary),
)
```

`CodeBlockColors.Default` uses mid-tone colours that stay readable on light and dark backgrounds.

### Turning code blocks off

Code blocks are one of the [editor features](features.md). An editor whose feature set leaves out
`RichTextFeature.CodeBlock` loads and pastes a code block as plain paragraphs:

```kotlin
state.config.features = RichTextFeature.All - RichTextFeature.CodeBlock
```

## Showing Code Alone

To show code that is not part of a rich text, such as a snippet or a whole file, use
`BasicCodeText`. It needs no `RichTextState`:

```kotlin
BasicCodeText(
    code = "fun greet() = println(\"Hi\")",
    language = CodeLanguage.Kotlin,
    modifier = Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
        .padding(12.dp),
)
```

| Parameter | Type | Default | Description |
|---|---|---|---|
| `code` | `String` | required | The code to show |
| `language` | `CodeLanguage?` | required | The language to colour by, or null to draw the code plain |
| `modifier` | `Modifier` | `Modifier` | Background, padding and shape come from here |
| `style` | `TextStyle` | monospace | The text style of the code |
| `colors` | `CodeBlockColors` | `CodeBlockColors.Default` | The style of each kind of token |
| `softWrap` | `Boolean` | `false` | When false, a long line scrolls sideways instead of wrapping |
| `onTextLayout` | `(TextLayoutResult) -> Unit` | `{}` | Called when the text is laid out |

`BasicCodeText` draws the text and nothing else, so it fits any design system.

For a `Text` of your own, `highlightCode` returns the coloured `AnnotatedString`:

```kotlin
Text(text = highlightCode(code, CodeLanguage.fromName("python")))
```

## Custom Layouts Such as a Diff View

A view that draws its own rows, for example a diff with a gutter and added or removed
backgrounds, needs the tokens of each line. `CodeHighlighter.tokenizeLines` returns them with
offsets relative to each line. The lines are read as one piece of code, so a comment or string
that opens on one line is still coloured on the next:

```kotlin
val language = CodeLanguage.fromFileName("src/main/kotlin/Greeter.kt") ?: return
val colors = CodeBlockColors.Default
val tokens = CodeHighlighter.tokenizeLines(lines, language)

lines.forEachIndexed { index, line ->
    val text = buildAnnotatedString {
        append(line)
        tokens[index].forEach { token ->
            addStyle(colors.styleOf(token.kind), token.start, token.end)
        }
    }
    DiffRow(text)
}
```

Lines that do not follow each other in the file, such as two hunks of a diff, are two calls.
A hunk that starts in the middle of a comment or string is coloured as code until that comment
or string ends, because the lines above it are not known.

`CodeHighlighter.tokenize(code, language)` returns the tokens of a whole piece of code.
Tokenizing is linear in the length of the code and never throws, whatever the input.

## Supported Languages

| Language | `CodeLanguage` | Names it answers to | File endings |
|---|---|---|---|
| Kotlin | `Kotlin` | `kotlin`, `kt`, `kts` | `kt`, `kts` |
| Java | `Java` | `java` | `java` |
| JavaScript | `JavaScript` | `javascript`, `js`, `jsx`, `mjs`, `cjs`, `node` | `js`, `jsx`, `mjs`, `cjs` |
| TypeScript | `TypeScript` | `typescript`, `ts`, `tsx` | `ts`, `tsx` |
| Python | `Python` | `python`, `py`, `python3` | `py` |
| Swift | `Swift` | `swift` | `swift` |
| Go | `Go` | `go`, `golang` | `go` |
| Rust | `Rust` | `rust`, `rs` | `rs` |
| C | `C` | `c`, `h` | `c`, `h` |
| C++ | `Cpp` | `cpp`, `c++`, `cc`, `cxx`, `hpp` | `cpp`, `cc`, `cxx`, `hpp`, `hh` |
| C# | `CSharp` | `csharp`, `cs`, `c#` | `cs` |
| Dart | `Dart` | `dart` | `dart` |
| Shell | `Shell` | `shell`, `sh`, `bash`, `zsh`, `shellscript` | `sh`, `bash`, `zsh` |
| SQL | `Sql` | `sql` | `sql` |
| JSON | `Json` | `json` | `json` |
| YAML | `Yaml` | `yaml`, `yml` | `yaml`, `yml` |

`CodeLanguage.fromName` takes the name of a fence or of a `language-x` class, ignoring case, and
`CodeLanguage.fromFileName` takes a file name or path. Both return null for a language the
library does not know. A code block with an unknown language is drawn as plain code and keeps
its language tag on export.

## Limitations

- A long line in a code block inside `RichText` or an editor wraps. Use `BasicCodeText` when the
  code should scroll sideways.
- There is no editor action yet to turn text into a code block or to leave one. A code block
  that was loaded can be edited, and Enter adds a line to it.
- Only fenced Markdown code is a code block. Code indented by four spaces is not.
- HTML, XML, CSS and diffs are not coloured yet.
- Markup inside an HTML `<pre>` is read as plain code. Code copied from an IDE arrives as a
  `<pre>` with coloured spans and no language, so it is pasted as a code block without colours.
- A code block inside a list item or a quote is moved to the top level.
- A block has no space of its own above or below it, and two blocks in a row touch.
- In Markdown, an empty paragraph directly after a block is not kept on a round trip.
- Text selected in a read-only `RichText` inside a `SelectionContainer` is copied with a space
  between paragraphs, so code selected that way loses its line ends. Copy and cut in the editors
  keep them.
- `ExpandableBasicRichText` colours the code but draws no background behind it.

## Related

- For the feature set that switches code blocks on and off, see [Editor features](features.md).
- For format details, see [HTML Import and Export](html_import_export.md),
  [Markdown Import and Export](markdown_import_export.md) and
  [JSON Import and Export](json_import_export.md).
- For the block model behind the JSON format, see [Rich Text Document](rich_text_document.md).
