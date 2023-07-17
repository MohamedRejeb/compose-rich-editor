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