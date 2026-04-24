package com.mohamedrejeb.richeditor.sample.common.examples

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.sample.common.components.ExampleStatus
import com.mohamedrejeb.richeditor.sample.common.components.RealExampleCard
import com.mohamedrejeb.richeditor.sample.common.components.SampleScaffold
import com.mohamedrejeb.richeditor.sample.common.ui.theme.BrandColors

private data class RealExample(
    val name: String,
    val tagline: String,
    val monogram: String,
    val brandColor: Color,
    val status: ExampleStatus,
)

private val examples = listOf(
    RealExample(
        name = "Slack",
        tagline = "Channel composer with mentions, channels, links and code spans.",
        monogram = "S",
        brandColor = BrandColors.Slack,
        status = ExampleStatus.Live,
    ),
    RealExample(
        name = "Notion",
        tagline = "Block editor with slash commands, headings, lists, quote, divider and mentions.",
        monogram = "N",
        brandColor = BrandColors.Notion,
        status = ExampleStatus.Live,
    ),
    RealExample(
        name = "GitHub",
        tagline = "Issue thread with composer: headings, code blocks, mentions, links and lists.",
        monogram = "G",
        brandColor = BrandColors.GitHub,
        status = ExampleStatus.Live,
    ),
    RealExample(
        name = "Discord",
        tagline = "Chat input with markdown, emojis, mentions and rich link previews.",
        monogram = "D",
        brandColor = BrandColors.Discord,
        status = ExampleStatus.ComingSoon,
    ),
    RealExample(
        name = "Medium",
        tagline = "Long-form article editor with inline toolbar and pull-quotes.",
        monogram = "M",
        brandColor = BrandColors.Medium,
        status = ExampleStatus.ComingSoon,
    ),
    RealExample(
        name = "X",
        tagline = "Compact composer with mentions, hashtags and character limits.",
        monogram = "X",
        brandColor = BrandColors.X,
        status = ExampleStatus.ComingSoon,
    ),
    RealExample(
        name = "Linear",
        tagline = "Issue editor with markdown, mentions and code highlighting.",
        monogram = "L",
        brandColor = BrandColors.Linear,
        status = ExampleStatus.ComingSoon,
    ),
)

@Composable
fun RealExamplesScreen(
    navigateBack: () -> Unit,
    navigateToSlack: () -> Unit,
    navigateToGithub: () -> Unit,
    navigateToNotion: () -> Unit,
) {
    var pendingComingSoon by remember { mutableStateOf<RealExample?>(null) }

    SampleScaffold(
        title = "Real-world examples",
        navigateBack = navigateBack,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            Header()

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 260.dp),
                contentPadding = PaddingValues(20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(examples, key = { it.name }) { example ->
                    RealExampleCard(
                        name = example.name,
                        tagline = example.tagline,
                        monogram = example.monogram,
                        brandColor = example.brandColor,
                        status = example.status,
                        onClick = {
                            when (example.status) {
                                ExampleStatus.Live -> when (example.name) {
                                    "Slack" -> navigateToSlack()
                                    "GitHub" -> navigateToGithub()
                                    "Notion" -> navigateToNotion()
                                }
                                ExampleStatus.ComingSoon -> pendingComingSoon = example
                            }
                        },
                    )
                }
            }
        }

        pendingComingSoon?.let { example ->
            ComingSoonDialog(
                example = example,
                onDismiss = { pendingComingSoon = null },
            )
        }
    }
}

@Composable
private fun Header() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp),
    ) {
        Text(
            text = "What can you build with it?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "These showcase how Compose Rich Editor maps onto familiar product UX. " +
                "Slack, GitHub and Notion are fully functional today; the others are scaffolds we're " +
                "building out - open an issue or PR if you want to help.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ComingSoonDialog(
    example: RealExample,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it")
            }
        },
        title = {
            Text(
                text = "${example.name} demo coming soon",
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Text(
                text = "We're working on a ${example.name}-style sample to highlight more of " +
                    "the library's surface area. Star the repo to be notified when it lands.",
            )
        },
        shape = RoundedCornerShape(20.dp),
    )
}

