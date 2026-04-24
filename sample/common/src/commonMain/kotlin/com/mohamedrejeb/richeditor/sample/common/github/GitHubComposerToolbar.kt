package com.mohamedrejeb.richeditor.sample.common.github

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.FormatStrikethrough
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.model.RichTextState

private val HeadingSpan = SpanStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold)
private val BoldSpan = SpanStyle(fontWeight = FontWeight.Bold)
private val ItalicSpan = SpanStyle(fontStyle = FontStyle.Italic)
private val StrikeSpan = SpanStyle(textDecoration = TextDecoration.LineThrough)

@Composable
internal fun GitHubComposerToolbar(
    state: RichTextState,
    onLinkClick: () -> Unit,
    onMentionTrigger: () -> Unit,
    onIssueRefTrigger: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier,
    ) {
        item {
            ToolbarButton(
                icon = Icons.Outlined.Title,
                isSelected = state.currentSpanStyle.fontSize == HeadingSpan.fontSize,
                contentDescription = "Heading",
                onClick = { state.toggleSpanStyle(HeadingSpan) },
            )
        }
        item {
            ToolbarButton(
                icon = Icons.Outlined.FormatBold,
                isSelected = state.currentSpanStyle.fontWeight == FontWeight.Bold,
                contentDescription = "Bold",
                onClick = { state.toggleSpanStyle(BoldSpan) },
            )
        }
        item {
            ToolbarButton(
                icon = Icons.Outlined.FormatItalic,
                isSelected = state.currentSpanStyle.fontStyle == FontStyle.Italic,
                contentDescription = "Italic",
                onClick = { state.toggleSpanStyle(ItalicSpan) },
            )
        }
        item {
            ToolbarButton(
                icon = Icons.Outlined.FormatStrikethrough,
                isSelected = state.currentSpanStyle.textDecoration?.contains(TextDecoration.LineThrough) == true,
                contentDescription = "Strikethrough",
                onClick = { state.toggleSpanStyle(StrikeSpan) },
            )
        }

        item { Divider() }

        item {
            ToolbarButton(
                icon = Icons.Outlined.Code,
                isSelected = state.isCodeSpan,
                contentDescription = "Code",
                onClick = { state.toggleCodeSpan() },
            )
        }
        item {
            ToolbarButton(
                icon = Icons.Outlined.Link,
                isSelected = state.isLink,
                contentDescription = "Link",
                onClick = onLinkClick,
            )
        }

        item { Divider() }

        item {
            ToolbarButton(
                icon = Icons.AutoMirrored.Outlined.FormatListBulleted,
                isSelected = state.isUnorderedList,
                contentDescription = "Bulleted list",
                onClick = { state.toggleUnorderedList() },
            )
        }
        item {
            ToolbarButton(
                icon = Icons.Outlined.FormatListNumbered,
                isSelected = state.isOrderedList,
                contentDescription = "Numbered list",
                onClick = { state.toggleOrderedList() },
            )
        }

        item { Divider() }

        item {
            ToolbarButton(
                icon = Icons.Outlined.AlternateEmail,
                isSelected = false,
                contentDescription = "Mention user",
                onClick = onMentionTrigger,
            )
        }
        item {
            ToolbarButton(
                icon = Icons.Outlined.Tag,
                isSelected = false,
                contentDescription = "Reference issue or PR",
                onClick = onIssueRefTrigger,
            )
        }
    }
}

@Composable
private fun ToolbarButton(
    icon: ImageVector,
    isSelected: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val background = when {
        isSelected -> GitHubColors.ToolbarSelected
        else -> Color.Transparent
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            // Workaround: prevent the rich editor from losing focus when a toolbar button
            // is clicked (Desktop quirk). Without this the cursor jumps and toggleSpanStyle
            // applies to the wrong place.
            .focusProperties { canFocus = false }
            .size(32.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(background)
            .clickable(onClick = onClick),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isSelected) GitHubColors.Link else GitHubColors.TextMuted,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .height(20.dp)
            .width(1.dp)
            .background(GitHubColors.Border),
    )
}
