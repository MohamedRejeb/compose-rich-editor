# FAQ

Have a question that isn't part of the FAQ? Open an issue in our [GitHub repository][issues].

[issues]: https://github.com/MohamedRejeb/Compose-Rich-Editor/issues

## Common Questions

### How do I get development snapshots?

Add the snapshots repository to your list of repositories in `build.gradle.kts`:

```kotlin
allprojects {
    repositories {
        maven("https://s01.oss.sonatype.org/content/repositories/snapshots")
    }
}
```

Or to your dependency resolution management in `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        maven("https://s01.oss.sonatype.org/content/repositories/snapshots")
    }
}
```

Use the snapshot version:

```kotlin
implementation("com.mohamedrejeb.richeditor:richeditor-compose:1.0.0-SNAPSHOT")
```

⚠️ **Warning**: Snapshots are deployed for each new commit on `main` that passes CI. They can potentially contain breaking changes or may be unstable. Use at your own risk.

### How do I customize the appearance of links?

You can customize link appearance using the `config` property of `RichTextState`:

```kotlin
richTextState.config.linkColor = Color.Blue
richTextState.config.linkTextDecoration = TextDecoration.Underline
```

### How do I handle link clicks?

By default, links are opened by your platform's `UriHandler`. To handle links yourself:

```kotlin
val myUriHandler = remember {
    object : UriHandler {
        override fun openUri(uri: String) {
            // Your custom link handling logic
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

### How do I save/restore editor content?

You can convert the editor content to HTML or Markdown for storage:

```kotlin
// Save content
val html = richTextState.toHtml()
// or
val markdown = richTextState.toMarkdown()

// Restore content
richTextState.setHtml(savedHtml)
// or
richTextState.setMarkdown(savedMarkdown)
```
