# Links

The Rich Text Editor provides comprehensive support for hyperlinks, allowing you to:
- Add links to new or existing text
- Turn typed or pasted URLs into links automatically
- Update link URLs
- Remove links
- Customize link appearance
- Handle link clicks

## Adding Links

### New Text with Link

To add a new text with a link, use the `addLink` method:

```kotlin
// Add link after selection
richTextState.addLink(
    text = "Compose Rich Editor",
    url = "https://github.com/MohamedRejeb/Compose-Rich-Editor"
)
```

### Converting Text to Link

To convert selected text into a link, use the `addLinkToSelection` method:

```kotlin
// Add link to selected text
richTextState.addLinkToSelection(
    url = "https://kotlinlang.org/"
)
```

## Automatic Links

The editor turns a URL into a link on its own, the way Google Docs, Slack and Notion do:

- **Typing.** A word that starts with `http://`, `https://` or `www.` becomes a link when a space or Enter is typed after it. Nothing is linked while the URL is still being typed.
- **Pasting.** A paste whose whole text is such a URL is inserted as a link. A paste that contains a URL among other text stays plain text.

| Typed text | Linked part | Link URL |
|---|---|---|
| `https://example.com/a?b=c` | all of it | `https://example.com/a?b=c` |
| `www.example.com` | all of it | `https://www.example.com` |
| `(see https://example.com).` | `https://example.com` | `https://example.com` |
| `https://en.wikipedia.org/wiki/Rust_(programming_language)` | all of it | the same |
| `example.com`, `user@example.com` | nothing | |

Punctuation that closes the sentence after the URL (`.`, `,`, `;`, `:`, `!`, `?`, quotes and closing brackets) stays outside the link. A closing parenthesis is kept when the URL itself opened it.

The link is added as its own undo step after the edit, so one undo removes the link and keeps the typed or pasted text.

To keep typed and pasted URLs as plain text, turn it off in the config:

```kotlin
richTextState.config.autoLinkEnabled = false
```

### Limitations

- Bare domains (`example.com`) and email addresses are not linked.
- Text that is already a link, a code span or a token is left as it is, so typing inside or right after an existing link never changes that link.
- Only text typed or pasted into the editor is checked. Content set with `setHtml`, `setMarkdown`, `setText` or the insert functions is not scanned for URLs.
- Nothing is linked while `RichTextFeature.Link` is left out of `config.features` (see [Editor features](features.md)).
- `autoLinkEnabled` is marked `@ExperimentalRichTextApi`.

## Managing Links

### Updating Links

To update an existing link's URL:

```kotlin
// Update selected link URL
richTextState.updateLink(
    url = "https://kotlinlang.org/"
)
```

### Removing Links

To remove a link while keeping the text:

```kotlin
// Remove link from selected text
richTextState.removeLink()
```

## Link Information

### Checking Link Status

To check if the current selection is a link:

```kotlin
val isLink = richTextState.isLink
```

### Getting Link Details

To get the current link's text and URL:

```kotlin
// Get link text and URL
val linkText = richTextState.selectedLinkText
val linkUrl = richTextState.selectedLinkUrl
```

## Customizing Links

### Link Appearance

You can customize how links appear in the editor:

```kotlin
richTextState.config.linkColor = Color.Blue
richTextState.config.linkTextDecoration = TextDecoration.Underline
```

## Handling Link Clicks

By default, links are opened by your platform's `UriHandler`. To customize link handling:

```kotlin
val myUriHandler = remember {
    object : UriHandler {
        override fun openUri(uri: String) {
            // Custom link handling logic
            // For example: open in specific browser, validate URL, etc.
        }
    }
}

CompositionLocalProvider(LocalUriHandler provides myUriHandler) {
    RichText(
        state = richTextState,
        modifier = Modifier.fillMaxWidth()
    )
}
```
