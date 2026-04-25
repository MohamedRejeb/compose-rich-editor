package com.mohamedrejeb.richeditor.ui.material3

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.ImageLoader
import com.mohamedrejeb.richeditor.model.LocalImageLoader
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.ui.ExpandableBasicRichText

/**
 * Material3 wrapper around [ExpandableBasicRichText] that pulls [LocalTextStyle] /
 * [LocalContentColor] / [MaterialTheme.colorScheme.primary] for default styling, mirroring how the
 * sibling [RichText] composable wraps [com.mohamedrejeb.richeditor.ui.BasicRichText].
 *
 * @see ExpandableBasicRichText for the underlying composable, the v1 limitations note, and
 * documentation on each parameter.
 */
@ExperimentalRichTextApi
@Composable
public fun ExpandableRichText(
    state: RichTextState,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    collapsedMaxLines: Int = 3,
    seeMoreLabel: String = "… See more",
    seeLessLabel: String = " See less",
    seeMoreColor: Color = MaterialTheme.colorScheme.primary,
    softWrap: Boolean = true,
    inlineContent: Map<String, InlineTextContent> = mapOf(),
    imageLoader: ImageLoader = LocalImageLoader.current,
) {
    val textColor = color.takeOrElse {
        style.color.takeOrElse {
            LocalContentColor.current
        }
    }
    val mergedStyle = style.merge(TextStyle(color = textColor))

    ExpandableBasicRichText(
        state = state,
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier,
        style = mergedStyle,
        collapsedMaxLines = collapsedMaxLines,
        seeMoreLabel = seeMoreLabel,
        seeLessLabel = seeLessLabel,
        seeMoreStyle = SpanStyle(
            color = seeMoreColor,
            textDecoration = TextDecoration.Underline,
        ),
        softWrap = softWrap,
        inlineContent = inlineContent,
        imageLoader = imageLoader,
    )
}
