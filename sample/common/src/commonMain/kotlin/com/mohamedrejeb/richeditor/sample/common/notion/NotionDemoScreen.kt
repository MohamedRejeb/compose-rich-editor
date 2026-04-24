package com.mohamedrejeb.richeditor.sample.common.notion

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichSpanStyle
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.model.trigger.Trigger
import com.mohamedrejeb.richeditor.ui.BasicRichTextEditor
import com.mohamedrejeb.richeditor.ui.material3.TriggerSuggestions

private data class NotionMember(
    val id: String,
    val name: String,
    val handle: String,
    val role: String,
)

private val notionMembers = listOf(
    NotionMember("u-mohamed", "Mohamed Rejeb", "@mohamed", "Library maintainer"),
    NotionMember("u-alice", "Alice Johnson", "@alice", "Product"),
    NotionMember("u-bob", "Bob Smith", "@bob", "Engineering"),
    NotionMember("u-carol", "Carol Diaz", "@carol", "Design"),
    NotionMember("u-david", "David Lee", "@david", "Docs"),
    NotionMember("u-elena", "Elena Park", "@elena", "Community"),
)

private const val PAGE_COLUMN_MAX_WIDTH_DP = 720
private const val MENTION_TRIGGER_ID = "mention"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalRichTextApi::class)
@Composable
fun NotionDemoScreen(
    navigateBack: () -> Unit,
) {
    val editorState = rememberRichTextState()
    var pageTitle by remember { mutableStateOf(TextFieldValue("Project notes")) }
    var pageIcon by remember { mutableStateOf("📄") }
    var highlightedCommand by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        editorState.config.linkColor = NotionColors.Link
        editorState.config.codeSpanColor = NotionColors.CodeText
        editorState.config.codeSpanBackgroundColor = NotionColors.CodeBackground
        editorState.config.codeSpanStrokeColor = NotionColors.CodeStroke

        editorState.registerTrigger(
            Trigger(
                id = NOTION_BLOCK_TRIGGER_ID,
                char = '/',
                style = { SpanStyle(color = NotionColors.SlashAccent, fontWeight = FontWeight.Normal) },
            )
        )
        editorState.registerTrigger(
            Trigger(
                id = MENTION_TRIGGER_ID,
                char = '@',
                style = { SpanStyle(color = NotionColors.MentionText, fontWeight = FontWeight.Medium) },
            )
        )

        editorState.setMarkdown(
            "# Welcome to your Notion-style page\n\n" +
                "Press **/** on any line to transform it into a heading, list, quote, divider, " +
                "or code block.\n\n" +
                "## What you can do\n\n" +
                "- Turn a paragraph into **Heading 1**, **Heading 2**, or **Heading 3** on the fly.\n" +
                "- Toggle between bulleted and numbered lists.\n" +
                "- Drop a horizontal divider, quote a line, or wrap something in code.\n" +
                "- Mention teammates by typing **@** anywhere.\n\n" +
                "Pick any existing paragraph and hit **/** to see it transform.\n"
        )
    }

    // Clear the menu highlight whenever a fresh query starts so the first row is selected.
    LaunchedEffect(editorState.activeTriggerQuery?.query, editorState.activeTriggerQuery?.triggerId) {
        highlightedCommand = 0
    }

    val slashKeyHandler = rememberNotionSlashKeyHandler(
        state = editorState,
        highlightedIndex = highlightedCommand,
        onHighlightChange = { highlightedCommand = it },
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { NotionBreadcrumb(pageTitle.text, pageIcon) },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = NotionColors.IconTint,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Outlined.StarBorder,
                            contentDescription = "Favorite",
                            tint = NotionColors.IconMuted,
                        )
                    }
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Outlined.MoreHoriz,
                            contentDescription = "More",
                            tint = NotionColors.IconMuted,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NotionColors.Background,
                    scrolledContainerColor = NotionColors.Background,
                    titleContentColor = NotionColors.TextPrimary,
                ),
            )
        },
        containerColor = NotionColors.Background,
        contentColor = NotionColors.TextPrimary,
        modifier = Modifier.fillMaxSize(),
    ) { paddingValues ->
        PageBody(
            paddingValues = paddingValues,
            pageIcon = pageIcon,
            onIconChange = { pageIcon = it },
            pageTitle = pageTitle,
            onTitleChange = { pageTitle = it },
            editorState = editorState,
            highlightedCommand = highlightedCommand,
            onHighlightedCommandChange = { highlightedCommand = it },
            slashKeyHandler = slashKeyHandler,
        )
    }
}

@OptIn(ExperimentalRichTextApi::class)
@Composable
private fun PageBody(
    paddingValues: PaddingValues,
    pageIcon: String,
    onIconChange: (String) -> Unit,
    pageTitle: TextFieldValue,
    onTitleChange: (TextFieldValue) -> Unit,
    editorState: RichTextState,
    highlightedCommand: Int,
    onHighlightedCommandChange: (Int) -> Unit,
    slashKeyHandler: (KeyEvent) -> Boolean,
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .windowInsetsPadding(WindowInsets.ime),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = PAGE_COLUMN_MAX_WIDTH_DP.dp)
                    .padding(horizontal = 24.dp),
            ) {
                PageIconButton(icon = pageIcon, onChange = onIconChange)
                Spacer(Modifier.height(12.dp))
                PageTitleField(title = pageTitle, onChange = onTitleChange)
                Spacer(Modifier.height(16.dp))

                Box {
                    BasicRichTextEditor(
                        state = editorState,
                        textStyle = TextStyle(
                            color = NotionColors.TextPrimary,
                            fontSize = 16.sp,
                            lineHeight = 26.sp,
                        ),
                        cursorBrush = SolidColor(NotionColors.TextPrimary),
                        decorationBox = { innerTextField ->
                            EditorSurface(isEmpty = editorState.annotatedString.text.isEmpty()) {
                                innerTextField()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 320.dp)
                            .onPreviewKeyEvent(slashKeyHandler),
                    )

                    NotionSlashMenu(
                        state = editorState,
                        highlightedIndex = highlightedCommand,
                        onHighlightChange = onHighlightedCommandChange,
                        onCommit = { command -> commitNotionCommand(editorState, command) },
                    )

                    TriggerSuggestions(
                        state = editorState,
                        triggerId = MENTION_TRIGGER_ID,
                        suggestions = { query ->
                            notionMembers.filter {
                                query.isEmpty() ||
                                    it.handle.contains(query, ignoreCase = true) ||
                                    it.name.contains(query, ignoreCase = true)
                            }
                        },
                        onSelect = { member ->
                            RichSpanStyle.Token(
                                triggerId = MENTION_TRIGGER_ID,
                                id = member.id,
                                label = member.handle,
                            )
                        },
                        containerColor = NotionColors.MenuSurface,
                        contentColor = NotionColors.TextPrimary,
                        highlightColor = NotionColors.MenuHighlight,
                        item = { member ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                MemberAvatar(member.name)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = member.name,
                                        color = NotionColors.TextPrimary,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.sp,
                                    )
                                    Text(
                                        text = "${member.handle} · ${member.role}",
                                        color = NotionColors.TextMuted,
                                        fontSize = 11.sp,
                                    )
                                }
                            }
                        },
                    )
                }
            }

            Spacer(Modifier.height(40.dp))
            TipCard()
            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
private fun NotionBreadcrumb(title: String, icon: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = icon,
            fontSize = 16.sp,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title.ifBlank { "Untitled" },
            color = NotionColors.TextPrimary,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun PageIconButton(icon: String, onChange: (String) -> Unit) {
    val cycle = remember { listOf("📄", "📘", "📝", "✨", "🚀", "💡", "📊", "🗂️") }
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(8.dp))
            .focusProperties { canFocus = false }
            .clickable {
                val next = (cycle.indexOf(icon) + 1).mod(cycle.size)
                onChange(cycle[next])
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = icon,
            fontSize = 54.sp,
        )
    }
}

@Composable
private fun PageTitleField(
    title: TextFieldValue,
    onChange: (TextFieldValue) -> Unit,
) {
    val textStyle = TextStyle(
        color = NotionColors.TextPrimary,
        fontSize = 40.sp,
        lineHeight = 48.sp,
        fontWeight = FontWeight.Bold,
    )
    Box(modifier = Modifier.fillMaxWidth()) {
        BasicTextField(
            value = title,
            onValueChange = onChange,
            textStyle = textStyle,
            cursorBrush = SolidColor(NotionColors.TextPrimary),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (title.text.isEmpty()) {
            Text(
                text = "Untitled",
                color = NotionColors.PlaceholderText,
                fontSize = 40.sp,
                lineHeight = 48.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun EditorSurface(
    isEmpty: Boolean,
    innerTextField: @Composable () -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        innerTextField()
        if (isEmpty) {
            Text(
                text = "Press '/' for commands, or just start writing",
                color = NotionColors.PlaceholderText,
                fontSize = 16.sp,
            )
        }
    }
}

@Composable
private fun MemberAvatar(name: String) {
    val initial = name.firstOrNull()?.uppercase() ?: "?"
    val palette = remember(name) {
        val hues = listOf(
            Color(0xFFEB5757), Color(0xFF2F80ED), Color(0xFF219653),
            Color(0xFF9B51E0), Color(0xFFF2994A), Color(0xFF0B6E99),
        )
        hues[(name.hashCode().rem(hues.size) + hues.size).rem(hues.size)]
    }
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(palette.copy(alpha = 0.9f), palette.copy(alpha = 0.6f)),
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun TipCard() {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = PAGE_COLUMN_MAX_WIDTH_DP.dp)
            .padding(horizontal = 24.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(NotionColors.SidebarSurface)
            .border(1.dp, NotionColors.Border, RoundedCornerShape(8.dp))
            .padding(16.dp),
    ) {
        Text(
            text = "Tips",
            color = NotionColors.TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
        TipRow("/", "Opens the block menu for the current line.")
        TipRow("@", "Mention a teammate from the member list.")
        TipRow("↑ ↓", "Navigate the menu. Enter commits, Escape cancels.")
    }
}

@Composable
private fun TipRow(symbol: String, description: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(22.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White)
                .border(1.dp, NotionColors.Border, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = symbol,
                color = NotionColors.TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = description,
            color = NotionColors.TextPrimary,
            fontSize = 13.sp,
        )
    }
}

