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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.mohamedrejeb.richeditor.ui.material3.OutlinedRichTextEditor
import com.mohamedrejeb.richeditor.ui.material3.TriggerSuggestions

/** One sample user / tag / command, keyed by stable id. */
private data class MentionUser(val id: String, val name: String, val handle: String)
private data class HashtagSuggestion(val slug: String)
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
                style = { SpanStyle(color = Color(0xFF1E88E5), fontWeight = FontWeight.Medium) },
            )
        )
        state.registerTrigger(
            Trigger(
                id = "hashtag",
                char = '#',
                style = { SpanStyle(color = Color(0xFF8E24AA), fontWeight = FontWeight.Medium) },
            )
        )
        state.registerTrigger(
            Trigger(
                id = "command",
                char = '/',
                style = { SpanStyle(color = Color(0xFF00897B), fontFamily = FontFamily.Monospace) },
            )
        )
        // Pre-populate one token of each kind so the styling is visible without typing.
        state.setMarkdown(
            "Hi [@mohamed](trigger:mention:u-mohamed), see " +
                "[#release](trigger:hashtag:release) and " +
                "[/heading](trigger:command:heading). " +
                "Type @, # or / to try it yourself.\n"
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mentions / Triggers Demo") },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        modifier = Modifier.fillMaxSize(),
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .windowInsetsPadding(WindowInsets.ime)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box {
                OutlinedRichTextEditor(
                    state = state,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                )

                // Popup for each trigger — only renders when its triggerId is active.
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
        }
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
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(6.dp))
        val scroll = rememberScrollState()
        Text(
            text = content,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 140.dp)
                .verticalScroll(scroll),
        )
    }
}
