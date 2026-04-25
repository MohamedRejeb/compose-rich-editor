# Expandable Text

`ExpandableBasicRichText` (and its Material3 wrapper `ExpandableRichText`)
renders a read-only `RichTextState` with an inline **See more / See less**
toggle, LinkedIn / X / Reddit style. When the content overflows the configured
line budget, the last visible line ends with a clickable `… See more` label;
tapping it expands the content and shows a trailing `See less` toggle on the
same baseline as the last word.

The composable is marked `@ExperimentalRichTextApi` for now so the API can
evolve before it stabilizes.

## Basic Usage

The expanded state is hoisted, so the caller owns a `Boolean` and updates it
from `onExpandedChange`:

```kotlin
@OptIn(ExperimentalRichTextApi::class)
@Composable
fun PostBody(state: RichTextState) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    ExpandableRichText(
        state = state,
        expanded = expanded,
        onExpandedChange = { expanded = it },
        collapsedMaxLines = 3,
    )
}
```

When the content fits within `collapsedMaxLines`, the affordance is not
rendered at all; nothing visually distinguishes the composable from a plain
`RichText` until the content actually overflows.

### Tapping the toggle

The labels are wired through Compose's `LinkAnnotation`, so taps fire
`onExpandedChange` with no extra `pointerInput` plumbing on the caller side.
Both the leading `…` and the label text are part of the link target.

## Customizing labels

Pass plain strings for `seeMoreLabel` and `seeLessLabel`. The defaults include
the leading horizontal ellipsis on `seeMoreLabel` and a leading space on
`seeLessLabel` so they sit naturally next to the surrounding content:

```kotlin
ExpandableRichText(
    state = state,
    expanded = expanded,
    onExpandedChange = { expanded = it },
    collapsedMaxLines = 4,
    seeMoreLabel = "… read more",
    seeLessLabel = " hide",
)
```

For localization, supply the strings from your resource layer:

```kotlin
ExpandableRichText(
    state = state,
    expanded = expanded,
    onExpandedChange = { expanded = it },
    seeMoreLabel = stringResource(R.string.expandable_see_more),
    seeLessLabel = stringResource(R.string.expandable_see_less),
)
```

## Customizing the toggle styling

The Material3 wrapper exposes a single `seeMoreColor` parameter that defaults
to `MaterialTheme.colorScheme.primary`. The label is underlined by default to
match the standard hyperlink affordance:

```kotlin
ExpandableRichText(
    state = state,
    expanded = expanded,
    onExpandedChange = { expanded = it },
    seeMoreColor = Color(0xFF1F6FEB),
)
```

For full control over the `SpanStyle` (different focus / hover / pressed
styling, custom font weight, etc.), drop down to `ExpandableBasicRichText`:

```kotlin
ExpandableBasicRichText(
    state = state,
    expanded = expanded,
    onExpandedChange = { expanded = it },
    seeMoreStyle = SpanStyle(
        color = Color(0xFF1F6FEB),
        fontWeight = FontWeight.SemiBold,
        textDecoration = TextDecoration.None,
    ),
)
```

## Material3 vs Foundation

| Composable | Lives in | Default style source |
|---|---|---|
| `ExpandableRichText` | `com.mohamedrejeb.richeditor.ui.material3` | `LocalTextStyle`, `LocalContentColor`, `MaterialTheme.colorScheme.primary` |
| `ExpandableBasicRichText` | `com.mohamedrejeb.richeditor.ui` | None: caller provides `style` and `seeMoreStyle` |

Use `ExpandableRichText` when your screen is already inside a `MaterialTheme`.
Drop to `ExpandableBasicRichText` if you do not depend on Material3, or if you
need to override the `SpanStyle` directly.

## Parameters

| Name | Required | Default | Notes |
|---|---|---|---|
| `state` | yes | - | The `RichTextState` to display. |
| `expanded` | yes | - | Hoisted boolean. The composable does not own this state. |
| `onExpandedChange` | yes | - | Called with `true` when `See more` is tapped, `false` for `See less`. |
| `modifier` | no | `Modifier` | Applied to the underlying `BasicText`. |
| `style` | no | `TextStyle.Default` (foundation) / `LocalTextStyle.current` (Material3) | Base text style. |
| `collapsedMaxLines` | no | `3` | Must be `>= 1`. |
| `seeMoreLabel` | no | `"… See more"` | Inline label appended to the last visible line when collapsed. |
| `seeLessLabel` | no | `" See less"` | Inline label appended at the end of the content when expanded. |
| `seeMoreStyle` (foundation) | no | underlined, color inherited | `SpanStyle` applied to both labels via `TextLinkStyles`. |
| `seeMoreColor` (Material3) | no | `MaterialTheme.colorScheme.primary` | Forwarded into a `SpanStyle` with underline. |
| `softWrap` | no | `true` | Standard `BasicText` softWrap. |
| `inlineContent` | no | `mapOf()` | Inline content map merged with the state's own. |
| `imageLoader` | no | `LocalImageLoader.current` | For inline images carried by the state. |

## v1 limitations

`ExpandableBasicRichText` flows through Compose's `BasicText` rather than
`BasicRichText`. The trade-off this enables is the inline `See less` toggle
sharing a baseline with the trailing word. The price:

- ✅ Preserved: bold, italic, color, underline, font size, font family,
  hyperlinks (`LinkAnnotation`), inline images and inline content.
- ⚠️ Not rendered in v1: code-span pill backgrounds, list-bullet glyphs,
  paragraph backgrounds, code-block stroke, mention/token pointer
  interactions, or any other paragraph-level decoration that
  `BasicRichText`'s overlay modifiers draw.

If your content needs paragraph-level decoration, use `BasicRichText` /
`RichText` with manual `maxLines` and a separate `See more` button below
until v2.

## How truncation works

When the content does overflow, the composable runs a small two-pass layout:

1. Render the full text with `maxLines = collapsedMaxLines`.
2. The `onTextLayout` callback observes `hasVisualOverflow == true` and
   captures the visible end of the last line plus the actual width Compose
   used.
3. A `TextMeasurer` walks the cut position back to the previous word boundary
   (with a character-boundary fallback for inputs that have no whitespace),
   re-measuring `prefix + suffix` until it fits within `collapsedMaxLines` at
   the captured width.
4. The composable swaps its rendered text to `prefix + "… See more"`, where
   the suffix carries a `LinkAnnotation.Clickable` that toggles `expanded`.

When `expanded == true`, the text is `content + " See less"` with
`maxLines = Int.MAX_VALUE`, so the full content is shown and the trailing
toggle stays inline with the last word.

## Related

- For the non-expanding read-only equivalent, see
  [BasicRichText / RichText](rich_text_state.md).
- For inline formatting that survives truncation, see
  [Span Style](span_style.md) and [Links](links.md).
- For a runnable demo, the `expandable` sample in the desktop sample app
  shows three cards: long plain text, short text, and span-styled text.
