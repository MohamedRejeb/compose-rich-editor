# Links

The Rich Text Editor provides comprehensive support for hyperlinks, allowing you to:
- Add links to new or existing text
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
