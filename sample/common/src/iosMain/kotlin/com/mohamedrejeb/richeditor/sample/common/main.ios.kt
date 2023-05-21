package com.mohamedrejeb.richeditor.sample.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeUIViewController
import com.mohamedrejeb.richeditor.sample.common.htmleditor.HtmlEditorContent
import platform.UIKit.*

@Suppress("unused", "FunctionName")
fun MainViewController(
    topSafeArea: Float,
    bottomSafeArea: Float
): UIViewController {
    return ComposeUIViewController {
        val density = LocalDensity.current

        val topSafeAreaDp = with(density) { topSafeArea.toDp() }
        val bottomSafeAreaDp = with(density) { bottomSafeArea.toDp() }
        val safeArea = PaddingValues(top = topSafeAreaDp + 10.dp, bottom = bottomSafeAreaDp)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(safeArea)
        ) {
            App()
        }
    }
}
