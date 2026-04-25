# Images

The Rich Text Editor supports **inline images** via the `RichSpanStyle.Image`
span, with pluggable loading through the `ImageLoader` interface. Images render
inside the editor's text flow as atomic inline content, round-trip through HTML
(`<img>`), and are automatically clamped to the editor's container width so
oversized sources don't overflow the layout.

> **Note:** The image APIs are marked `@ExperimentalRichTextApi` and may change
> in a future release. Images currently render in the read-only `RichText` view;
> full editor-surface rendering is tracked for future work.

## Loading Images

`RichSpanStyle.Image.model` is an opaque `Any` — a URL, a `Painter`, a resource
id, or any type your [`ImageLoader`](#image-loaders) understands. The library
resolves it through the `ImageLoader` supplied via `LocalImageLoader`.

### Default loader

Out of the box, `DefaultImageLoader` returns `null` for every model — it has no
knowledge of networking or asset pipelines. You **must** provide an
`ImageLoader` to see images rendered, even for local resources.

### Coil3 integration (recommended)

The `richeditor-compose-coil3` artifact ships a drop-in `Coil3ImageLoader` that
uses Coil's `rememberAsyncImagePainter`:

```kotlin
implementation("com.mohamedrejeb.richeditor:richeditor-compose-coil3:1.0.0-rc13")
```

Provide it via `CompositionLocalProvider`, usually at the root of your editor
screen:

```kotlin
CompositionLocalProvider(LocalImageLoader provides Coil3ImageLoader) {
    RichText(
        state = state,
        modifier = Modifier.fillMaxWidth(),
    )
}
```

Any `<img src="...">` tag imported via `setHtml` now resolves through Coil's
network stack. Configure Coil itself (network clients, disk cache, interceptors)
via your app's `SingletonImageLoader`.

## Inserting Images

### From HTML

The simplest path is to import HTML containing `<img>` tags:

```kotlin
val html = """
    <p>Header paragraph.</p>
    <p><img src="https://picsum.photos/id/1015/600/400" width="600" height="400" /></p>
    <p>Footer paragraph.</p>
"""
state.setHtml(html)
```

The parser reads `width` / `height` attributes for the initial placeholder size.
If they're missing, the image starts at `0×0` and expands once the painter
resolves its intrinsic size.

### Programmatic

You can construct `RichSpanStyle.Image` directly if you're composing content
outside HTML:

```kotlin
val image = RichSpanStyle.Image(
    model = "https://picsum.photos/id/1015/600/400",
    width = 600.sp,
    height = 400.sp,
    contentDescription = "Landscape photo",
)
```

At least one of `width` / `height` must be specified; both must be finite and
non-negative.

## Container-Width Clamping

When an image's intrinsic width exceeds the `RichText` container width, it is
**scaled down proportionally** so it never overflows. The editor captures its
layout width via `Modifier.onSizeChanged` and clamps images on the next frame.

This means you can safely load wide source images (e.g. a `1600×900` hero)
inside a narrow column — they'll render at the column's width with correct
aspect ratio.

If the clamp finishes before the painter reports its intrinsic size, the
Placeholder temporarily reserves space at the HTML-attribute or caller-supplied
dimensions. A process-wide cache of resolved dimensions per model prevents the
"big-then-small" flicker on subsequent renders of the same URL.

## Image Loaders

### The interface

```kotlin
interface ImageLoader {
    @Composable
    fun load(model: Any): ImageData?
}

class ImageData(
    val painter: Painter,
    val contentDescription: String? = null,
    val alignment: Alignment = Alignment.Center,
    val contentScale: ContentScale = ContentScale.Fit,
    val modifier: Modifier = Modifier.fillMaxWidth(),
)
```

Return `null` while the image is still loading or failed — the Placeholder stays
at its reserved size and the layout doesn't jump when your loader later returns
a painter.

### Custom loaders

Implement `ImageLoader` to integrate any image stack (Fresco, Glide wrapper,
your own network layer, resource-based assets):

```kotlin
@OptIn(ExperimentalRichTextApi::class)
object MyResourceImageLoader : ImageLoader {
    @Composable
    override fun load(model: Any): ImageData? {
        val id = model as? Int ?: return null
        return ImageData(
            painter = painterResource(id),
            contentScale = ContentScale.Fit,
        )
    }
}
```

Then plug it in via the CompositionLocal:

```kotlin
CompositionLocalProvider(LocalImageLoader provides MyResourceImageLoader) {
    RichText(state = state)
}
```

You can also pass the loader directly to a single surface:

```kotlin
RichText(
    state = state,
    imageLoader = Coil3ImageLoader,
)
```

The composable parameter takes precedence over the `CompositionLocal`.

## HTML Round-Trip

Images import and export through `<img>`:

| Attribute | Handled |
|---|---|
| `src`  | ✅ becomes `model` |
| `width`  | ✅ initial placeholder width |
| `height`  | ✅ initial placeholder height |
| `alt`  | ✅ becomes `contentDescription` |

```kotlin
state.setHtml("""<p><img src="https://.../hero.png" width="600" height="400" alt="Hero" /></p>""")
val roundTripped = state.toHtml()
```

## Markdown Round-Trip

Standard Markdown image syntax is supported on import and export:

```
![alt text](https://.../hero.png)
```

Markdown has no native width/height syntax, so programmatic sizes are lost in a
Markdown round-trip. Prefer HTML if you need to preserve explicit dimensions.

## Notes

- Images are **atomic**: backspace deletes the whole image span, typing adjacent
  to an image creates a sibling text span instead of merging.
- Images render in `RichText` / `BasicRichText` (the read-only surfaces).
  Editor-surface rendering is a work in progress — expect images to appear as
  placeholders inside `RichTextEditor` until that lands.
- `DefaultImageLoader` returns `null` for every model, so configure
  `LocalImageLoader` (typically with `Coil3ImageLoader`) before expecting
  images to appear.

## Sample

See the [Images sample][images-sample] for a live demo that exercises the
container-width clamp with preset and custom images.

[images-sample]: https://github.com/MohamedRejeb/Compose-Rich-Editor/blob/main/sample/common/src/commonMain/kotlin/com/mohamedrejeb/richeditor/sample/common/images/ImagesSampleScreen.kt
