package com.mohamedrejeb.richeditor.model

import androidx.compose.runtime.Composable
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi

@ExperimentalRichTextApi
public object DefaultImageLoader: ImageLoader {

    @Composable
    override fun load(model: Any): ImageData? = null

}