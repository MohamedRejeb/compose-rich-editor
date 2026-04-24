package com.mohamedrejeb.richeditor.sample.common.claude

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.FormatStrikethrough
import androidx.compose.material.icons.outlined.FormatUnderlined
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.TextDecrease
import androidx.compose.material.icons.outlined.TextIncrease
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.model.HeadingStyle
import com.mohamedrejeb.richeditor.model.RichTextState

@Composable
internal fun ClaudeFormattingPanel(
    state: RichTextState,
    openLinkDialog: MutableState<Boolean>,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier,
    ) {
        item {
            ClaudePanelButton(
                onClick = {
                    state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
                },
                isSelected = state.currentSpanStyle.fontWeight == FontWeight.Bold,
                icon = Icons.Outlined.FormatBold,
            )
        }
        item {
            ClaudePanelButton(
                onClick = {
                    state.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic))
                },
                isSelected = state.currentSpanStyle.fontStyle == FontStyle.Italic,
                icon = Icons.Outlined.FormatItalic,
            )
        }
        item {
            ClaudePanelButton(
                onClick = {
                    state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline))
                },
                isSelected = state.currentSpanStyle.textDecoration?.contains(TextDecoration.Underline) == true,
                icon = Icons.Outlined.FormatUnderlined,
            )
        }
        item {
            ClaudePanelButton(
                onClick = {
                    state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                },
                isSelected = state.currentSpanStyle.textDecoration?.contains(TextDecoration.LineThrough) == true,
                icon = Icons.Outlined.FormatStrikethrough,
            )
        }

        item { Divider() }

        item {
            ClaudePanelButton(
                onClick = {
                    val next = if (state.currentHeadingStyle == HeadingStyle.H2) {
                        HeadingStyle.Normal
                    } else {
                        HeadingStyle.H2
                    }
                    state.setHeadingStyle(next)
                },
                isSelected = state.currentHeadingStyle == HeadingStyle.H2,
                icon = Icons.Outlined.Title,
            )
        }
        item {
            ClaudePanelButton(
                onClick = { state.toggleCodeSpan() },
                isSelected = state.isCodeSpan,
                icon = Icons.Outlined.Code,
            )
        }
        item {
            ClaudePanelButton(
                onClick = { openLinkDialog.value = true },
                isSelected = state.isLink,
                icon = Icons.Outlined.Link,
            )
        }

        item { Divider() }

        item {
            ClaudePanelButton(
                onClick = { state.toggleUnorderedList() },
                isSelected = state.isUnorderedList,
                icon = Icons.AutoMirrored.Outlined.FormatListBulleted,
            )
        }
        item {
            ClaudePanelButton(
                onClick = { state.toggleOrderedList() },
                isSelected = state.isOrderedList,
                icon = Icons.Outlined.FormatListNumbered,
            )
        }
        item {
            ClaudePanelButton(
                onClick = { state.increaseListLevel() },
                enabled = state.canIncreaseListLevel,
                icon = Icons.Outlined.TextIncrease,
            )
        }
        item {
            ClaudePanelButton(
                onClick = { state.decreaseListLevel() },
                enabled = state.canDecreaseListLevel,
                icon = Icons.Outlined.TextDecrease,
            )
        }
    }
}

@Composable
private fun ClaudePanelButton(
    onClick: () -> Unit,
    icon: ImageVector,
    enabled: Boolean = true,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val tint = when {
        !enabled -> ClaudeColors.TextMuted
        isSelected -> ClaudeColors.AccentOrange
        else -> ClaudeColors.TextSecondary
    }

    Box(
        modifier = modifier
            .focusProperties { canFocus = false }
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                onClick = onClick,
                enabled = enabled,
                role = Role.Button,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = icon.name,
            tint = tint,
            modifier = Modifier
                .background(
                    color = if (isSelected) ClaudeColors.SurfaceHover else Color.Transparent,
                )
                .padding(6.dp),
        )
    }
}

@Composable
private fun Divider() {
    Box(
        Modifier
            .height(20.dp)
            .width(1.dp)
            .background(ClaudeColors.Divider),
    )
}
