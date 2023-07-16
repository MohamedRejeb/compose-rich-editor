package com.mohamedrejeb.richeditor.sample.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeUIViewController
import com.moriatsushi.insetsx.WindowInsetsUIViewController
import platform.UIKit.*

@Suppress("unused", "FunctionName")
fun MainViewController(
    topSafeArea: Float,
    bottomSafeArea: Float
): UIViewController {
    return WindowInsetsUIViewController {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            App()
        }
    }
}
