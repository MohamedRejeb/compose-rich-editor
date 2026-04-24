package com.mohamedrejeb.richeditor.sample.common.github

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichText

@Composable
internal fun GitHubCommentCard(
    comment: GitHubComment,
    isOriginalPost: Boolean,
    modifier: Modifier = Modifier,
) {
    val state = rememberRichTextState()

    LaunchedEffect(comment.html) {
        state.config.linkColor = GitHubColors.Link
        state.config.codeSpanColor = GitHubColors.Text
        state.config.codeSpanBackgroundColor = GitHubColors.CodeBackground
        state.config.codeSpanStrokeColor = Color.Transparent
        state.setHtml(comment.html)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, GitHubColors.Border, RoundedCornerShape(8.dp))
            .background(GitHubColors.Background),
    ) {
        CommentHeader(
            author = comment.author,
            timeAgo = comment.timeAgo,
            isOriginalPost = isOriginalPost,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            RichText(
                state = state,
                color = GitHubColors.Text,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CommentHeader(
    author: GitHubUser,
    timeAgo: String,
    isOriginalPost: Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(GitHubColors.Surface)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Avatar(author)
        Spacer(Modifier.width(10.dp))
        Text(
            text = author.handle,
            color = GitHubColors.Text,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (isOriginalPost) "opened this $timeAgo" else "commented $timeAgo",
            color = GitHubColors.TextMuted,
            fontSize = 13.sp,
        )
        Spacer(modifier = Modifier.weight(1f))
        if (isOriginalPost) {
            AuthorPill()
        }
    }
}

@Composable
internal fun Avatar(author: GitHubUser, size: Int = 28) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Color(author.avatarColor)),
    ) {
        Text(
            text = author.handle.removePrefix("@").take(1).uppercase(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size * 0.45f).sp,
        )
    }
}

@Composable
private fun AuthorPill() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .border(1.dp, GitHubColors.Border, RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            text = "Author",
            color = GitHubColors.TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
