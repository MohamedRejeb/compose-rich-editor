package com.mohamedrejeb.richeditor.sample.common.github

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.model.RichTextState

private data class ThreadComment(
    val data: GitHubComment,
    val isOriginalPost: Boolean,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GitHubDemoScreen(navigateBack: () -> Unit) {
    val thread = remember {
        mutableStateListOf<ThreadComment>().apply {
            add(
                ThreadComment(
                    data = GitHubComment(
                        author = sampleIssueAuthor,
                        timeAgo = "1 day ago",
                        html = sampleIssueBodyHtml,
                    ),
                    isOriginalPost = true,
                )
            )
            sampleComments.forEach { add(ThreadComment(data = it, isOriginalPost = false)) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GitHub", color = GitHubColors.Text) },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = GitHubColors.Text,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GitHubColors.Surface,
                    scrolledContainerColor = GitHubColors.Surface,
                ),
            )
        },
        containerColor = GitHubColors.Background,
        contentColor = GitHubColors.Text,
        modifier = Modifier.fillMaxSize(),
    ) { paddingValues ->
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .windowInsetsPadding(WindowInsets.ime)
                .padding(horizontal = 16.dp),
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                IssueHeader()
            }

            items(thread) { comment ->
                GitHubCommentCard(
                    comment = comment.data,
                    isOriginalPost = comment.isOriginalPost,
                )
            }

            item {
                ComposerSection(
                    onSubmit = { state ->
                        thread.add(
                            ThreadComment(
                                data = GitHubComment(
                                    author = sampleUsers[0],
                                    timeAgo = "just now",
                                    html = state.toHtml(),
                                ),
                                isOriginalPost = false,
                            )
                        )
                    },
                )
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun IssueHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = SAMPLE_ISSUE_TITLE,
                color = GitHubColors.Text,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "#$SAMPLE_ISSUE_NUMBER",
                color = GitHubColors.TextMuted,
                fontWeight = FontWeight.Light,
                fontSize = 22.sp,
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OpenStatusPill()
            Spacer(Modifier.width(10.dp))
            Text(
                text = "${sampleIssueAuthor.handle} opened this issue 1 day ago",
                color = GitHubColors.TextMuted,
                fontSize = 13.sp,
            )
        }

        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(GitHubColors.Border),
        )
    }
}

@Composable
private fun OpenStatusPill() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFF238636))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = "Open",
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun ComposerSection(
    onSubmit: (RichTextState) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
        ) {
            Avatar(sampleUsers[0])
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Add a comment",
                color = GitHubColors.Text,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
        }

        GitHubComposer(onSubmit = onSubmit)
    }
}
