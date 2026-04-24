package com.mohamedrejeb.richeditor.sample.common.github

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichSpanStyle
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.TokenClickHandler
import com.mohamedrejeb.richeditor.model.TokenHoverHandler
import com.mohamedrejeb.richeditor.model.trigger.Trigger
import kotlinx.coroutines.delay

private const val HoverDelayMillis: Long = 350L

/**
 * Registers the GitHub-sample triggers on [state]. Required on every RichText that
 * needs to render `@mention` and `#issueRef` tokens with trigger-specific colors -
 * without registration, tokens fall back to [RichTextConfig.linkColor] because
 * `AnnotatedStringExt#resolveRichSpanStyleStyle` looks up the trigger on the state.
 */
@OptIn(ExperimentalRichTextApi::class)
@Composable
internal fun RegisterGitHubTriggers(state: RichTextState) {
    LaunchedEffect(state) {
        if (state.findTriggerPublic("mention") == null) {
            state.registerTrigger(
                Trigger(
                    id = "mention",
                    char = '@',
                    style = {
                        SpanStyle(
                            color = GitHubColors.Mention,
                            fontWeight = FontWeight.Medium,
                        )
                    },
                )
            )
        }
        if (state.findTriggerPublic("issueRef") == null) {
            state.registerTrigger(
                Trigger(
                    id = "issueRef",
                    char = '#',
                    style = {
                        SpanStyle(
                            color = GitHubColors.IssueRef,
                            fontWeight = FontWeight.Medium,
                        )
                    },
                )
            )
        }
    }
}

/**
 * Public shim for the internal `RichTextState.findTrigger`. Avoids exposing the
 * library's internal-only method while giving the sample an idempotent check.
 */
@OptIn(ExperimentalRichTextApi::class)
private fun RichTextState.findTriggerPublic(id: String): Trigger? =
    triggers.firstOrNull { it.id == id }

/**
 * Wraps [content] with click + hover routing for token spans, rendering a
 * preview card at the pointer. Issue-ref clicks open the corresponding GitHub
 * URL via [LocalUriHandler]; mention clicks show a persistent card until
 * dismissed.
 *
 * The content lambda receives the click/hover handlers to pass to `RichText`.
 */
@OptIn(ExperimentalRichTextApi::class)
@Composable
internal fun GitHubTokenInteractionBox(
    modifier: Modifier = Modifier,
    content: @Composable (TokenClickHandler, TokenHoverHandler) -> Unit,
) {
    val uriHandler = LocalUriHandler.current

    // Raw hover state - updates on every token change (enter / exit / change).
    var hovered by remember { mutableStateOf<Pair<RichSpanStyle.Token, Offset>?>(null) }
    // Hover state after the delay window elapses. Null while the debounce is pending.
    var visibleHover by remember { mutableStateOf<Pair<RichSpanStyle.Token, Offset>?>(null) }
    var clicked by remember { mutableStateOf<Pair<RichSpanStyle.Token, Offset>?>(null) }

    // Debounce: clear the visible card immediately on exit; defer by HoverDelayMillis on enter/change.
    LaunchedEffect(hovered) {
        val current = hovered
        if (current == null) {
            visibleHover = null
        } else {
            delay(HoverDelayMillis)
            visibleHover = current
        }
    }

    val onTokenClick = TokenClickHandler { token, offset ->
        // Any click dismisses the hover preview so it does not linger next to the click target.
        hovered = null
        visibleHover = null
        when (token.triggerId) {
            "issueRef" -> {
                val issue = sampleIssueRefs.firstOrNull { it.id == token.id }
                if (issue != null) {
                    uriHandler.openUri(
                        "https://github.com/$GITHUB_SAMPLE_REPO_URL/issues/${issue.number}"
                    )
                }
            }
            else -> {
                clicked = token to offset
            }
        }
    }
    val onTokenHover = TokenHoverHandler { token, offset ->
        hovered = token?.let { it to offset }
    }

    Box(modifier = modifier) {
        content(onTokenClick, onTokenHover)

        val preview = visibleHover
        if (preview != null && clicked == null) {
            TokenPreviewCard(
                token = preview.first,
                anchor = preview.second,
                persistent = false,
                onDismiss = null,
            )
        }

        val c = clicked
        if (c != null) {
            TokenPreviewCard(
                token = c.first,
                anchor = c.second,
                persistent = true,
                onDismiss = { clicked = null },
            )
        }
    }
}

@OptIn(ExperimentalRichTextApi::class)
@Composable
private fun TokenPreviewCard(
    token: RichSpanStyle.Token,
    anchor: Offset,
    persistent: Boolean,
    onDismiss: (() -> Unit)?,
) {
    // Popup positions relative to the composable that owns it (the
    // GitHubTokenInteractionBox wrapping the RichText). The anchor is already in
    // that Box's local coordinate space because the RichText fills the Box.
    // Offset in px (IntOffset); nudge down 18px so the card sits below the pointer.
    val offset = IntOffset(x = anchor.x.toInt(), y = (anchor.y + 18f).toInt())

    Popup(
        alignment = Alignment.TopStart,
        offset = offset,
        onDismissRequest = onDismiss,
        properties = PopupProperties(
            focusable = persistent,
            dismissOnClickOutside = persistent,
            dismissOnBackPress = persistent,
        ),
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(GitHubColors.Surface)
                .border(1.dp, GitHubColors.Border, RoundedCornerShape(6.dp))
                .widthIn(max = 280.dp)
                .padding(12.dp),
        ) {
            Column {
                when (token.triggerId) {
                    "mention" -> {
                        val user = sampleUsers.firstOrNull { it.id == token.id }
                        if (user != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Avatar(user, size = 32)
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = user.displayName,
                                        color = GitHubColors.Text,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                    )
                                    Text(
                                        text = user.handle,
                                        color = GitHubColors.TextMuted,
                                        fontSize = 12.sp,
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "Unknown user",
                                color = GitHubColors.TextMuted,
                                fontSize = 12.sp,
                            )
                        }
                    }
                    "issueRef" -> {
                        val issue = sampleIssueRefs.firstOrNull { it.id == token.id }
                        if (issue != null) {
                            Text(
                                text = "#${issue.number}",
                                color = GitHubColors.IssueRef,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = issue.title,
                                color = GitHubColors.Text,
                                fontSize = 13.sp,
                            )
                        } else {
                            Text(
                                text = "Unknown issue",
                                color = GitHubColors.TextMuted,
                                fontSize = 12.sp,
                            )
                        }
                    }
                    else -> {
                        Text(
                            text = "${token.triggerId}: ${token.label}",
                            color = GitHubColors.Text,
                            fontSize = 13.sp,
                        )
                    }
                }

                if (persistent && onDismiss != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Close",
                        color = GitHubColors.Link,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable(onClick = onDismiss)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}
