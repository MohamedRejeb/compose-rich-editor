# Highlights

`RichTextState.highlights` draws styles over ranges of the text without making
them part of the document. It is meant for temporary marks such as the matches
of a find-in-text feature and the match the user is currently on. The editor
(`BasicRichTextEditor`, `RichTextEditor`, `OutlinedRichTextEditor`) and the
read-only views (`BasicRichText`, `RichText`, `ExpandableRichText`) all render
them.

The API is marked `@ExperimentalRichTextApi` for now so it can evolve before
it stabilizes.

## Basic Usage

A highlight is a `TextRange` and a `SpanStyle`. Assign a list to show
highlights and an empty list to clear them:

```kotlin
@OptIn(ExperimentalRichTextApi::class)
fun highlightFirstWord(state: RichTextState) {
    state.highlights = listOf(
        RichTextHighlight(
            range = TextRange(0, 5),
            style = SpanStyle(background = Color.Yellow),
        ),
    )
}

@OptIn(ExperimentalRichTextApi::class)
fun clearHighlights(state: RichTextState) {
    state.highlights = emptyList()
}
```

`highlights` is snapshot state, so the text is redrawn as soon as the list is
assigned. The list is replaced as a whole: there is no add or remove method.

## Reference

| API | Description |
|---|---|
| `RichTextState.highlights: List<RichTextHighlight>` | The highlights drawn over the text. Empty by default. |
| `RichTextHighlight(range, style)` | One highlight. `range` holds offsets in `state.annotatedString.text`, `style` is drawn on top of the text's own styles. |

## Ranges

Ranges are offsets in the current text of `state.annotatedString`, the same
offsets `state.selection` uses. In that text:

- paragraphs are separated by one character (a space),
- list items include their marker, for example `"• "` or `"1. "`.

Searching `state.annotatedString.text` gives ranges that can be used as they
are.

Highlights do not follow edits. A range keeps its offsets when the user types,
so recompute the list when the text changes. A range that reaches past the end
of the text is drawn up to the end, and a range that lies outside the text is
not drawn, so a list that is briefly out of date is harmless.

## Find in text

The example below highlights every match of a query, marks the focused match
with a stronger style, and recomputes both when the text or the query changes:

```kotlin
@OptIn(ExperimentalRichTextApi::class)
@Composable
fun FindInText(state: RichTextState) {
    var query by remember { mutableStateOf("") }
    var focusedMatch by remember { mutableStateOf(0) }

    val text = state.annotatedString.text
    val matches = remember(text, query) { findMatches(text, query) }

    LaunchedEffect(state, matches, focusedMatch) {
        state.highlights = matches.mapIndexed { index, range ->
            RichTextHighlight(
                range = range,
                style = if (index == focusedMatch) FocusedMatchStyle else MatchStyle,
            )
        }
    }
    DisposableEffect(state) {
        onDispose { state.highlights = emptyList() }
    }

    Column {
        Row {
            BasicTextField(
                value = query,
                onValueChange = {
                    query = it
                    focusedMatch = 0
                },
            )
            Button(
                enabled = matches.isNotEmpty(),
                onClick = { focusedMatch = (focusedMatch + 1) % matches.size },
            ) {
                Text("Next")
            }
        }
        RichTextEditor(state = state)
    }
}

private val MatchStyle = SpanStyle(background = Color(0xFFFFF59D))
private val FocusedMatchStyle = SpanStyle(background = Color(0xFFFF9800), color = Color.Black)

private fun findMatches(text: String, query: String): List<TextRange> {
    if (query.isEmpty()) return emptyList()
    val matches = mutableListOf<TextRange>()
    var index = text.indexOf(query, ignoreCase = true)
    while (index >= 0) {
        matches += TextRange(index, index + query.length)
        index = text.indexOf(query, index + query.length, ignoreCase = true)
    }
    return matches
}
```

## Overlaps

Highlights are drawn after the text's own styles, so a highlight background
replaces a background the text already has, and a highlight color replaces the
text color. Properties the highlight style leaves unset keep the text's value.

When two highlights overlap, the one later in the list wins.

While the user has a selection, a highlight's background is not drawn under
the selected part, so the selection stays visible. The other properties of the
style are kept.

## What highlights are not

Highlights are presentation only. They are:

- not exported by `toHtml`, `toMarkdown`, `toText` or `toRichTextDocument`,
- not copied or cut to the clipboard,
- not recorded in the undo history, and not changed by undo and redo,
- not reflected in `currentSpanStyle` or applied to text typed inside them,
- not kept by `RichTextState.copy()` or by `rememberRichTextState`'s saver.

To change the document itself, use [Span Style](span_style.md).

## Limitations

- A highlight takes a `SpanStyle` only. Paragraph styles and custom drawing
  are not supported.
- Highlights are not gated by `RichTextConfig.features`, since they are not
  formatting.
- The paragraph separator is a space in the text, so a query that contains a
  space can match across the end of one paragraph and the start of the next.

## Related

- For styles that are part of the document, see [Span Style](span_style.md).
- For spans with custom drawing, see [Custom span styles](custom_span_styles.md).
- For the state API and text change monitoring, see
  [RichTextState](rich_text_state.md).
