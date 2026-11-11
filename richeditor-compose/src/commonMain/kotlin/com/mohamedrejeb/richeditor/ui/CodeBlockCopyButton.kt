package com.mohamedrejeb.richeditor.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi

private const val CopyCodeDescription: String = "Copy code"

/** A mid-tone grey that stays visible on light and dark backgrounds. */
private val DefaultTint: Color = Color(0xFF8A8F98)

/**
 * A small button that copies [code] to the clipboard exactly as it is, line ends and
 * indentation included. It is about one line of code tall, so it fits the corner of a code
 * block: pass it as the `codeBlockAction` of [BasicRichText] or of the Material `RichText`.
 *
 * @param tint the colour of the icon.
 * @param contentDescription what accessibility services call the button. Pass a translated
 * string for other languages.
 * @param onCopied called after the code was copied, to show feedback.
 */
@ExperimentalRichTextApi
@Composable
public fun CodeBlockCopyButton(
    code: String,
    modifier: Modifier = Modifier,
    tint: Color = DefaultTint,
    contentDescription: String = CopyCodeDescription,
    onCopied: () -> Unit = {},
) {
    // The suspending LocalClipboard needs a ClipEntry, which has no common constructor.
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .padding(2.dp)
            .size(20.dp)
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClickLabel = contentDescription, role = Role.Button) {
                clipboard.setText(AnnotatedString(code))
                onCopied()
            },
    ) {
        Image(
            painter = rememberVectorPainter(CopyIcon),
            contentDescription = contentDescription,
            colorFilter = ColorFilter.tint(tint),
            modifier = Modifier.size(14.dp),
        )
    }
}

private val CopyIcon: ImageVector = ImageVector.Builder(
    name = "CopyCode",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).addPath(
    pathData = addPathNodes(
        "M16,1L4,1c-1.1,0 -2,0.9 -2,2v14h2L4,3h12L16,1zM19,5L8,5c-1.1,0 -2,0.9 -2,2v14c0,1.1 0.9,2 2,2h11" +
            "c1.1,0 2,-0.9 2,-2L21,7c0,-1.1 -0.9,-2 -2,-2zM19,21L8,21L8,7h11v14z"
    ),
    fill = SolidColor(Color.Black),
).build()
