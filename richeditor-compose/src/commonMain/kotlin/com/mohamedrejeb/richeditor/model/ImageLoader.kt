package com.mohamedrejeb.richeditor.model

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
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

@Immutable
public data class ImageData(
    val painter: Painter,
    val contentDescription: String? = "Image",
    val alignment: Alignment = Alignment.CenterStart,
    val modifier: Modifier = Modifier.fillMaxWidth()
)