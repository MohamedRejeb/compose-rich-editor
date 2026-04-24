# Mentions & Triggers

The Rich Text Editor has first-class support for **trigger-driven token insertion**,
the pattern behind `@mentions`, `#hashtags`, and `/commands` in tools like Slack,
GitHub, and Notion.

A **trigger** is a single character (`@`, `#`, `/`, ...) that activates a query
session while the user types. A suggestions popup lists candidates; on commit the
typed range is replaced with an atomic **token** span that behaves as a single
unit for editing (backspace deletes the whole token, selections snap to its edges,
typing adjacent to it creates a sibling span).

> **Note:** The trigger APIs are marked `@ExperimentalRichTextApi` and may change
> in a future release.

## Quick Start

```kotlin
@OptIn(ExperimentalRichTextApi::class)
@Composable
fun MentionsSample() {
    val state = rememberRichTextState()

    LaunchedEffect(Unit) {
        state.registerTrigger(
            Trigger(
                id = "mention",
                char = '@',
                style = { SpanStyle(color = Color(0xFF0969DA), fontWeight = FontWeight.Medium) },
            )
        )
    }

    Box {
        OutlinedRichTextEditor(state = state)

        TriggerSuggestions(
            state = state,
            triggerId = "mention",
            suggestions = { query ->
                users.filter { it.handle.contains(query, ignoreCase = true) }
            },
            onSelect = { user ->
                RichSpanStyle.Token(
                    triggerId = "mention",
                    id = user.id,
                    label = user.handle, // must start with '@'
                )
            },
            item = { user ->
                Column {
                    Text(user.handle, fontWeight = FontWeight.Medium)
                    Text(user.name, style = MaterialTheme.typography.bodySmall)
                }
            },
        )
    }
}
```

## Registering Triggers

Register one or more triggers on the state. Each trigger has a unique [`id`],
a unique [`char`], and an optional [`style`] that is re-evaluated whenever
`RichTextConfig` changes so token colors follow the editor theme.

```kotlin
// @mention
state.registerTrigger(
    Trigger(
        id = "mention",
        char = '@',
        style = { SpanStyle(color = it.linkColor, fontWeight = FontWeight.Medium) },
    )
)

// #hashtag
state.registerTrigger(
    Trigger(
        id = "hashtag",
        char = '#',
        style = { SpanStyle(color = Color.Magenta) },
    )
)

// /command
state.registerTrigger(
    Trigger(
        id = "command",
        char = '/',
        style = { SpanStyle(color = Color(0xFF009688), fontFamily = FontFamily.Monospace) },
    )
)
```

### Trigger options

| Property | Default | Description |
|---|---|---|
| `id` | required | Stable identifier. Used as the map key for tokens and for HTML/Markdown round-trip. Must not contain `:`. |
| `char` | required | The character that activates this trigger. Must be unique per state. |
| `style` | `{ SpanStyle(color = it.linkColor) }` | Visual style applied to committed tokens. Receives the live `RichTextConfig`. |
| `drawStyle` | `null` | Optional custom draw pass beneath the token (e.g. to draw a pill background). |
| `stopChars` | `{' ', '\n', '\t'}` | Characters that cancel an in-progress query. |
| `requireWordBoundary` | `true` | When true, `foo@bar` does not activate — the char before `@` must be whitespace, a paragraph boundary, or nothing. |
| `maxQueryLength` | `50` | Cap on query characters after the trigger char before detection gives up. |

### Unregistering

```kotlin
state.unregisterTrigger("mention")
```

If the currently active query belongs to the removed trigger, it is cleared.

## Observing the Active Query

`state.activeTriggerQuery` is a `TriggerQuery?` that updates after every edit
and selection change. When non-null it describes the in-progress query:

```kotlin
data class TriggerQuery(
    val triggerId: String,    // which trigger is active
    val query: String,        // characters typed after the trigger char
    val range: TextRange,     // the range that will be replaced on commit
    val caretRect: Rect?,     // caret position in editor-local coords (for popup anchoring)
)
```

Most apps do not read this directly — [`TriggerSuggestions`](#trigger-suggestions-material-3)
wires it up for you. You can also build a fully custom popup against it.

## Trigger Suggestions (Material 3)

`TriggerSuggestions` is a Material 3 popup that renders suggestions for a single
trigger while its query is active. It handles keyboard navigation, caret anchoring,
and commit:

| Key | Action |
|---|---|
| `↓` / `↑` | Move the highlighted row |
| `Enter` | Commit the highlighted row |
| `Esc` | Cancel the active query (leaves typed text in place) |
| Click | Commit the clicked row |

### API

```kotlin
@Composable
fun <T> TriggerSuggestions(
    state: RichTextState,
    triggerId: String,
    suggestions: (query: String) -> List<T>,
    onSelect: (T) -> RichSpanStyle.Token,
    modifier: Modifier = Modifier,
    verticalOffset: Dp = 4.dp,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    highlightColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    shape: Shape = RoundedCornerShape(8.dp),
    maxVisibleItems: Int = 5,
    item: @Composable (T) -> Unit,
)
```

Place `TriggerSuggestions` inside a `Box` alongside the editor so its popup is
parented to the same layout. Render one `TriggerSuggestions` per registered
trigger — each one checks `activeTriggerQuery.triggerId` and shows nothing when
its trigger is not active.

```kotlin
Box {
    OutlinedRichTextEditor(state = state)

    TriggerSuggestions(state = state, triggerId = "mention", ...)
    TriggerSuggestions(state = state, triggerId = "hashtag", ...)
    TriggerSuggestions(state = state, triggerId = "command", ...)
}
```

### onSelect contract

`onSelect` must return a `RichSpanStyle.Token` whose `triggerId` matches the
popup's `triggerId` and whose `label` **starts with the trigger's character**:

```kotlin
onSelect = { user ->
    RichSpanStyle.Token(
        triggerId = "mention",
        id = user.id,              // stable id for serialization
        label = "@" + user.handle, // MUST begin with '@'
    )
}
```

## Committing a Token Programmatically

You can bypass the popup and commit a token directly:

```kotlin
state.insertToken(
    triggerId = "mention",
    id = "u123",
    label = "@mohamed",
)
```

Preconditions (enforced at runtime):

- a matching query must be active (`state.activeTriggerQuery?.triggerId == triggerId`);
- the trigger must be registered on the state;
- `label` must start with the trigger's character.

On commit, the query range (trigger char + typed chars) is replaced atomically
with the token span followed by a trailing space.

## Cancelling a Query

```kotlin
state.cancelActiveTrigger()
```

Dismisses the current query without inserting a token. The typed text is left
in place (so `@moh` stays as plain text) and detection is suppressed until the
cursor leaves the typed range.

## Handling Token Clicks and Hover

Tokens in **read-only** `RichText` (and `BasicRichText`) surfaces can be wired
to click and hover handlers to drive things like profile popovers, hashtag
navigation, or command dispatch.

### onTokenClick

```kotlin
RichText(
    state = state,
    onTokenClick = { token, tapOffset ->
        when (token.triggerId) {
            "mention" -> openProfile(token.id)
            "hashtag" -> navigateToTag(token.id)
            "command" -> runCommand(token.id)
        }
    },
)
```

`tapOffset` is in the rich-text composable's local coordinates — useful for
anchoring a popover at the tap point.

### onTokenHover

Fires on **enter** (token becomes non-null), **exit** (token becomes null), and
**change** (pointer moves between adjacent tokens). Does not fire on every
pointer-move event while staying over the same token:

```kotlin
RichText(
    state = state,
    onTokenHover = { token, pointerOffset ->
        hoveredToken = token
    },
)
```

Typical use: drive a GitHub-style `@user` / `#issue` preview popup.

### Screen-wide defaults

`LocalTokenClickHandler` and `LocalTokenHoverHandler` let you provide a default
for every `RichText` on a screen, without wiring the parameter each time:

```kotlin
CompositionLocalProvider(
    LocalTokenClickHandler provides TokenClickHandler { token, _ ->
        openReferenceSheet(token)
    },
) {
    // every RichText here inherits the handler
    RichText(state = commentState)
    RichText(state = replyState)
}
```

Per-composable `onTokenClick` / `onTokenHover` take precedence over the
`CompositionLocal`.

## Serialization

Tokens round-trip through both HTML and Markdown, preserving `triggerId` and `id`
so server-side rendering stays consistent.

### HTML

Committed tokens are serialized as `<span>` elements carrying `data-trigger-id`
and `data-token-id` attributes. On `setHtml`, unknown trigger ids render as
plain text — so make sure to `registerTrigger(...)` **before** loading content
that contains tokens.

### Markdown

Tokens serialize to a link-shaped form that survives other Markdown renderers
as a readable label:

```
[@mohamed](trigger:mention:u-mohamed)
[#release](trigger:hashtag:release)
[/heading](trigger:command:heading)
```

Format: `[label](trigger:<triggerId>:<tokenId>)`. Because `:` is the separator,
trigger ids and token ids must not contain `:` (enforced by `Trigger`'s
constructor and `insertToken`).

## Sample

See the [Mentions & Triggers sample][mentions-sample] for a full working demo
with `@mentions`, `#hashtags`, and `/commands` in one editor.

[mentions-sample]: https://github.com/MohamedRejeb/Compose-Rich-Editor/blob/main/sample/common/src/commonMain/kotlin/com/mohamedrejeb/richeditor/sample/common/mentions/MentionsSampleScreen.kt
