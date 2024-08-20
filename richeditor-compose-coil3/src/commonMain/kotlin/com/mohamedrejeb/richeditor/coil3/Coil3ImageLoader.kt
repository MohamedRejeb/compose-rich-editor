package com.mohamedrejeb.richeditor.coil3

import androidx.compose.runtime.Composable
import coil3.compose.rememberAsyncImagePainter
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.ImageData
import com.mohamedrejeb.richeditor.model.ImageLoader

@ExperimentalRichTextApi
public object Coil3ImageLoader: ImageLoader {

    @Composable
    override fun load(model: Any): ImageData {
        val painter = rememberAsyncImagePainter(model = model)

        return ImageData(
            painter = painter
        )
    }

}
