package com.mohamedrejeb.richeditor.model

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.TextUnit
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi

public interface ImageLoader {

    @OptIn(ExperimentalRichTextApi::class)
    @Composable
    public fun load(model: Any): ImageData?

}

@OptIn(ExperimentalRichTextApi::class)
public val LocalImageLoader: ProvidableCompositionLocal<ImageLoader> = staticCompositionLocalOf {
    DefaultImageLoader
}

/**
 * The container width available to images inside a [com.mohamedrejeb.richeditor.ui.BasicRichText].
 *
 * Populated by `BasicRichText` via `Modifier.onSizeChanged` so that images wider
 * than the container can be scaled down proportionally instead of overflowing.
 * When unspecified (default), images render at their intrinsic size.
 */
internal val LocalRichTextMaxImageWidthProvider: ProvidableCompositionLocal<RichTextMaxImageWidthProvider> =
    staticCompositionLocalOf { RichTextMaxImageWidthProvider() }

internal class RichTextMaxImageWidthProvider {
    var maxWidth by mutableStateOf(TextUnit.Unspecified)
}

@ExperimentalRichTextApi
@Immutable
public class ImageData(
    public val painter: Painter,
    public val contentDescription: String? = null,
    public val alignment: Alignment = Alignment.Center,
    public val contentScale: ContentScale = ContentScale.Fit,
    public val modifier: Modifier = Modifier.fillMaxWidth()
)