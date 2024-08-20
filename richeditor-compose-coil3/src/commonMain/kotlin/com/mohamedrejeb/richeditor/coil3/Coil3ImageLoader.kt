package com.mohamedrejeb.richeditor.coil3

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.ImageData
import com.mohamedrejeb.richeditor.model.ImageLoader

@OptIn(ExperimentalRichTextApi::class)
public object Coil3ImageLoader: ImageLoader {

    @Composable
    override fun load(model: Any): ImageData {
        val painter = rememberAsyncImagePainter(model = model)

        return ImageData(
            painter = painter
        )
    }

}
