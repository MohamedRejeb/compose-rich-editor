package com.mohamedrejeb.richeditor.model

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Density
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi

@ExperimentalRichTextApi
public interface ImageLoader {

    @Composable
    public fun load(model: Any): ImageData?

}

@ExperimentalRichTextApi
public val LocalImageLoader: ProvidableCompositionLocal<ImageLoader> = staticCompositionLocalOf {
    DefaultImageLoader
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