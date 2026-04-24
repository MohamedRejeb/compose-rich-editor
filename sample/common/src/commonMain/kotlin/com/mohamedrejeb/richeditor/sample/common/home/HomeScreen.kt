package com.mohamedrejeb.richeditor.sample.common.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.sample.common.components.ExampleStatus
import com.mohamedrejeb.richeditor.sample.common.components.FeatureCard
import com.mohamedrejeb.richeditor.sample.common.components.GradientHero
import com.mohamedrejeb.richeditor.sample.common.components.RealExampleCard
import com.mohamedrejeb.richeditor.sample.common.components.SampleScaffold
import com.mohamedrejeb.richeditor.sample.common.components.SectionHeader
import com.mohamedrejeb.richeditor.sample.common.ui.theme.BrandColors
import com.mohamedrejeb.richeditor.sample.common.ui.theme.SampleAccents

@Composable
fun HomeScreen(
    navigateToRichEditor: () -> Unit,
    navigateToHtmlEditor: () -> Unit,
    navigateToMarkdownEditor: () -> Unit,
    navigateToSlack: () -> Unit,
    navigateToMentions: () -> Unit,
    navigateToUndoRedo: () -> Unit,
    navigateToListsConfig: () -> Unit,
    navigateToRealExamples: () -> Unit,
) {
    SampleScaffold(
        title = "Compose Rich Editor",
        navigateBack = null,
    ) { paddingValues ->
        LazyColumn(
            contentPadding = paddingValues,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                GradientHero()
            }

            item { SectionSpacer() }

            item {
                SectionHeader(
                    eyebrow = "Editor variants",
                    title = "Pick the right surface",
                    subtitle = "Three Material flavours plus the basic primitive — same state, different chrome.",
                    accent = SampleAccents.Indigo,
                )
            }

            item {
                FeatureCard(
                    title = "Rich Text Editor",
                    description = "BasicRichTextEditor, RichTextEditor, OutlinedRichTextEditor and read-only RichText side by side.",
                    icon = Icons.Outlined.TextFields,
                    accent = SampleAccents.Indigo,
                    onClick = navigateToRichEditor,
                )
            }

            item {
                FeatureCard(
                    title = "Lists configuration",
                    description = "Tune indent, prefix alignment, ordered/unordered marker styles in real time.",
                    icon = Icons.AutoMirrored.Outlined.FormatListBulleted,
                    accent = SampleAccents.Sky,
                    onClick = navigateToListsConfig,
                )
            }

            item { SectionSpacer() }

            item {
                SectionHeader(
                    eyebrow = "Format conversion",
                    title = "Round-trip everywhere",
                    subtitle = "HTML and Markdown encode/decode against the same RichTextState.",
                    accent = SampleAccents.Coral,
                )
            }

            item {
                FeatureCard(
                    title = "HTML editor",
                    description = "Type rich text and watch HTML stream out — or paste HTML and see it parsed live.",
                    icon = Icons.Outlined.Code,
                    accent = SampleAccents.Coral,
                    onClick = navigateToHtmlEditor,
                )
            }

            item {
                FeatureCard(
                    title = "Markdown editor",
                    description = "Bidirectional Markdown ↔ Rich Text using intellij-markdown under the hood.",
                    icon = Icons.AutoMirrored.Outlined.Notes,
                    accent = SampleAccents.Amber,
                    onClick = navigateToMarkdownEditor,
                )
            }

            item { SectionSpacer() }

            item {
                SectionHeader(
                    eyebrow = "Interactivity",
                    title = "Beyond plain formatting",
                    subtitle = "Triggers, history, and stateful behaviour you can wire into product UX.",
                    accent = SampleAccents.Magenta,
                )
            }

            item {
                FeatureCard(
                    title = "Mentions & triggers",
                    description = "@-mentions, #-hashtags, /-commands with custom styling and suggestion popups.",
                    icon = Icons.Outlined.AlternateEmail,
                    accent = SampleAccents.Magenta,
                    onClick = navigateToMentions,
                )
            }

            item {
                FeatureCard(
                    title = "Undo & redo",
                    description = "Built-in history stack with keyboard shortcuts and programmatic control.",
                    icon = Icons.Outlined.History,
                    accent = SampleAccents.Teal,
                    onClick = navigateToUndoRedo,
                )
            }

            item { SectionSpacer() }

            item {
                SectionHeader(
                    eyebrow = "Real examples",
                    title = "See it in production-style apps",
                    subtitle = "Slack is live. Notion, GitHub, Discord, Medium and X are on the way.",
                    accent = SampleAccents.Emerald,
                )
            }

            item {
                RealExamplesTeaser(
                    onSlackClick = navigateToSlack,
                    onSeeAllClick = navigateToRealExamples,
                )
            }

            item {
                Spacer(Modifier.height(24.dp))
                Footer()
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SectionSpacer() {
    Spacer(Modifier.height(20.dp))
}

@Composable
private fun RealExamplesTeaser(
    onSlackClick: () -> Unit,
    onSeeAllClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        RealExampleCard(
            name = "Slack",
            tagline = "Channel messaging composer with @mentions, #channels, code spans and links.",
            monogram = "S",
            brandColor = BrandColors.Slack,
            status = ExampleStatus.Live,
            onClick = onSlackClick,
        )

        SeeAllExamplesRow(onClick = onSeeAllClick)
    }
}

@Composable
private fun SeeAllExamplesRow(onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(SampleAccents.Emerald),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Apps,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp),
            )
        }

        Spacer(Modifier.size(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Browse all examples",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Slack live · Notion, GitHub, Discord, Medium, X coming soon",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun Footer() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "com.mohamedrejeb.richeditor",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "MIT licensed · Built with Compose Multiplatform",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
