# Add links

- To add links, `RichTextState` provides `addLink` method:

```kotlin
// Add link after selection.
richTextState.addLink(
    text = "Compose Rich Editor",
    url = "https://github.com/MohamedRejeb/Compose-Rich-Editor"
)
```

- To add link to the selected text, `RichTextState` provides `addLinkToSelection` method:

```kotlin
// Add link to selected text.
richTextState.addLinkToSelection(
    url = "https://kotlinlang.org/"
)
```

- To update link URL, `RichTextState` provides `updateLink` method:

```kotlin
// Update selected link URL.
richTextState.updateLink(
    url = "https://kotlinlang.org/"
)
```

- To remove links, `RichTextState` provides `removeLink` method:

```kotlin
// Remove link from selected text.
richTextState.removeLink()
```

- To get if the current selection is a link, use `RichTextState.isLink`:

```kotlin
// Get if the current selection is a link.
val isLink = richTextState.isLink
```

- To get the current link text, use `RichTextState.selectedLinkText`:

```kotlin
// Get the current link text.
val linkText = richTextState.selectedLinkText
```

- To get the current link URL, use `RichTextState.selectedLinkUrl`:

```kotlin
// Get the current link URL.
val linkUrl = richTextState.selectedLinkUrl
```

By default, links will be opened by your platform's `UriHandler`, if however you want to
handle the links on your own, you can override the composition local as such:

```kotlin
val myUriHandler by remember {
    mutableStateOf(object : UriHandler {
        override fun openUri(uri: String) {
            // Handle the clicked link however you want
        }
    })
}
CompositionLocalProvider(LocalUriHandler provides myUriHandler) {
    RichText( ... )
}
```
