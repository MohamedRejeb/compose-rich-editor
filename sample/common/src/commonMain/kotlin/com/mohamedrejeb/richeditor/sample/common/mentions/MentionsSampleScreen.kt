package com.mohamedrejeb.richeditor.sample.common.mentions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichSpanStyle
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.model.trigger.Trigger
import com.mohamedrejeb.richeditor.sample.common.components.SampleScaffold
import com.mohamedrejeb.richeditor.sample.common.ui.theme.SampleAccents
import com.mohamedrejeb.richeditor.ui.material3.OutlinedRichTextEditor
import com.mohamedrejeb.richeditor.ui.material3.TriggerSuggestions

private data class MentionUser(val id: String, val name: String, val handle: String)
private data class SlashCommand(val id: String, val label: String, val description: String)

private val sampleUsers = listOf(
    MentionUser("u-alice", "Alice Johnson", "@alice"),
    MentionUser("u-bob", "Bob Smith", "@bob"),
    MentionUser("u-carol", "Carol Diaz", "@carol"),
    MentionUser("u-david", "David Lee", "@david"),
    MentionUser("u-mohamed", "Mohamed Rejeb", "@mohamed"),
)

private val sampleHashtags = listOf(
    "release", "rfc", "design", "bug", "good-first-issue", "discussion",
)

private val slashCommands = listOf(
    SlashCommand("heading", "/heading", "Insert a heading"),
    SlashCommand("code", "/code", "Insert a code block"),
    SlashCommand("divider", "/divider", "Insert a horizontal rule"),
    SlashCommand("date", "/date", "Insert today's date"),
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalRichTextApi::class)
@Composable
fun MentionsSampleScreen(
    navigateBack: () -> Unit,
) {
    val state = rememberRichTextState()

    LaunchedEffect(Unit) {
        state.registerTrigger(
            Trigger(
                id = "mention",
                char = '@',
                style = { SpanStyle(color = SampleAccents.Sky, fontWeight = FontWeight.Medium) },
            )
        )
        state.registerTrigger(
            Trigger(
                id = "hashtag",
                char = '#',
                style = { SpanStyle(color = SampleAccents.Magenta, fontWeight = FontWeight.Medium) },
            )
        )
        state.registerTrigger(
            Trigger(
                id = "command",
                char = '/',
                style = { SpanStyle(color = SampleAccents.Teal, fontFamily = FontFamily.Monospace) },
            )
        )
        state.setMarkdown(
            "Hi [@mohamed](trigger:mention:u-mohamed), see " +
                "[#release](trigger:hashtag:release) and " +
                "[/heading](trigger:command:heading). " +
                "Type @, # or / to try it yourself.\n"
        )
    }

    SampleScaffold(
        title = "Mentions & triggers",
        navigateBack = navigateBack,
    ) { paddingValues ->
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .windowInsetsPadding(WindowInsets.ime)
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            TriggerLegend()

            Box {
                OutlinedRichTextEditor(
                    state = state,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp),
                )

                TriggerSuggestions(
                    state = state,
                    triggerId = "mention",
                    suggestions = { query ->
                        sampleUsers.filter {
                            query.isEmpty() ||
                                it.handle.contains(query, ignoreCase = true) ||
                                it.name.contains(query, ignoreCase = true)
                        }
                    },
                    onSelect = { user ->
                        RichSpanStyle.Token(
                            triggerId = "mention",
                            id = user.id,
                            label = user.handle,
                        )
                    },
                    item = { user ->
                        Column {
                            Text(user.handle, fontWeight = FontWeight.Medium)
                            Text(user.name, style = MaterialTheme.typography.bodySmall)
                        }
                    },
                )

                TriggerSuggestions(
                    state = state,
                    triggerId = "hashtag",
                    suggestions = { query ->
                        sampleHashtags.filter { query.isEmpty() || it.contains(query, ignoreCase = true) }
                    },
                    onSelect = { tag ->
                        RichSpanStyle.Token(
                            triggerId = "hashtag",
                            id = tag,
                            label = "#$tag",
                        )
                    },
                    item = { tag ->
                        Text("#$tag")
                    },
                )

                TriggerSuggestions(
                    state = state,
                    triggerId = "command",
                    suggestions = { query ->
                        slashCommands.filter { query.isEmpty() || it.label.contains(query, ignoreCase = true) }
                    },
                    onSelect = { cmd ->
                        RichSpanStyle.Token(
                            triggerId = "command",
                            id = cmd.id,
                            label = cmd.label,
                        )
                    },
                    item = { cmd ->
                        Column {
                            Text(cmd.label, fontFamily = FontFamily.Monospace)
                            Text(cmd.description, style = MaterialTheme.typography.bodySmall)
                        }
                    },
                )
            }

            ExportPanel(
                title = "HTML",
                content = remember(state.annotatedString) { state.toHtml() },
            )

            ExportPanel(
                title = "Markdown",
                content = remember(state.annotatedString) { state.toMarkdown() },
            )

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun TriggerLegend() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
    ) {
        Text(
            text = "Active triggers",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = SampleAccents.Magenta,
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TriggerChip("@", "mention", SampleAccents.Sky)
            TriggerChip("#", "hashtag", SampleAccents.Magenta)
            TriggerChip("/", "command", SampleAccents.Teal)
        }
    }
}

@Composable
private fun TriggerChip(symbol: String, label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = symbol,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = color,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ExportPanel(
    title: String,
    content: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(6.dp))
        val scroll = rememberScrollState()
        SelectionContainer {
            Text(
                text = content,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 140.dp)
                    .verticalScroll(scroll),
            )
        }
    }
}
