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
implementation("com.mohamedrejeb.richeditor:richeditor-compose:1.2.0-SNAPSHOT")
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

### Why is the `visualTransformation` parameter on the decoration box composables deprecated?

`RichTextEditorDecorationBox` and `OutlinedRichTextEditorDecorationBox` (in `RichTextEditorDefaults`) have overloads that accept a `visualTransformation` parameter. Those overloads are deprecated: the editor renders its styled output through an `OutputTransformation` it installs on the text field itself, so the parameter is no longer applied and has no effect. Switch to the overload without `visualTransformation`.

### Can I set an `AnnotatedString` or a `VisualTransformation` on the editor?

No. The content of the editor is a rich text document, so it is set with `setHtml`, `setMarkdown`, `setText` or `setRichTextDocument`, and `richTextState.annotatedString` is a read-only view of how that document renders. The editor is built on the state based `BasicTextField`, which has no `VisualTransformation`.

To style ranges of the text for display only, for example the matches of a find-in-text feature, use [`richTextState.highlights`](highlights.md).

### How do I react to changes? There is no `onValueChange`

The editor is state based, like Compose's `BasicTextField(state)`, so there is no `value` / `onValueChange` pair. Observe the state instead:

```kotlin
LaunchedEffect(richTextState) {
    snapshotFlow { richTextState.annotatedString }
        .collect { onHtmlChanged(richTextState.toHtml()) }
}
```

This also fires for your own `setHtml` calls. If you write the value back into the editor, only do so when it differs from what the editor already holds, otherwise the two will feed each other:

```kotlin
LaunchedEffect(html) {
    if (html != richTextState.toHtml()) richTextState.setHtml(html)
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
