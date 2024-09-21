package com.mohamedrejeb.richeditor.coil3

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.ImageData
import com.mohamedrejeb.richeditor.model.ImageLoader

@ExperimentalRichTextApi
public object Coil3ImageLoader: ImageLoader {

    @Composable
    override fun load(model: Any): ImageData {
        println("Loading image with Coil3")
        val painter = rememberAsyncImagePainter(model = model)

        var imageData by remember {
            mutableStateOf<ImageData>(
                ImageData(
                    painter = painter
                )
            )
        }

        LaunchedEffect(painter.state) {
            painter.state.collect { state ->
                when (state) {
                    is AsyncImagePainter.State.Loading -> {
                        println("Loading image")
                    }
                    is AsyncImagePainter.State.Error -> {
                        println("Error loading image")
                    }
                    is AsyncImagePainter.State.Success -> {
                        println("Image loaded")
                        imageData = ImageData(
                            painter = state.painter
                        )
                    }
                    else -> {
                        println("Unknown state")
                    }
                }
            }
        }

        return imageData
    }

}
