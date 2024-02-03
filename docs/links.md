# Add links

To add links, `RichTextState` provides `addLink` method:

```kotlin
// Add link after selection.
richTextState.addLink(
    text = "Compose Rich Editor",
    url = "https://github.com/MohamedRejeb/Compose-Rich-Editor"
)
```

To get if the current selection is a link, use `RichTextState.isLink`:

```kotlin
// Get if the current selection is a link.
val isLink = richTextState.isLink
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
