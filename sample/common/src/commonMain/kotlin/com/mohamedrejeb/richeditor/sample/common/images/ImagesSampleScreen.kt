package com.mohamedrejeb.richeditor.sample.common.images

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.coil3.Coil3ImageLoader
import com.mohamedrejeb.richeditor.model.LocalImageLoader
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.sample.common.components.CopyButton
import com.mohamedrejeb.richeditor.sample.common.components.SampleScaffold
import com.mohamedrejeb.richeditor.sample.common.ui.theme.SampleAccents
import com.mohamedrejeb.richeditor.ui.material3.RichText

private data class ImagePreset(
    val label: String,
    val url: String,
    val width: Int,
    val height: Int,
    val note: String,
)

private val imagePresets = listOf(
    ImagePreset(
        label = "Landscape · 600×400",
        url = "https://picsum.photos/id/1018/600/400",
        width = 600,
        height = 400,
        note = "Renders at requested size - well within container.",
    ),
    ImagePreset(
        label = "Portrait · 400×600",
        url = "https://picsum.photos/id/1025/400/600",
        width = 400,
        height = 600,
        note = "Tall image, fits comfortably.",
    ),
    ImagePreset(
        label = "Oversized · 1600×900",
        url = "https://picsum.photos/id/1043/1600/900",
        width = 1600,
        height = 900,
        note = "Bigger than the editor - clamped to container width (#423).",
    ),
)

private const val SeedHtml = """
<p>Inline images live as <b>RichSpanStyle.Image</b> spans. They render through
<b>RichText</b> - the editor surfaces don't display images yet, so this screen uses the read-only
view for the preview while toolbar actions append more <code>&lt;img&gt;</code> tags below.</p>
<p><img src="https://picsum.photos/id/1015/600/400" width="600" height="400" /></p>
"""

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalRichTextApi::class)
@Composable
fun ImagesSampleScreen(navigateBack: () -> Unit) {
    val state = rememberRichTextState()
    var html by remember { mutableStateOf(SeedHtml.trimIndent()) }
    var customUrl by remember { mutableStateOf("") }
    var customWidth by remember { mutableStateOf(400f) }
    var customHeight by remember { mutableStateOf(260f) }

    LaunchedEffect(html) {
        state.setHtml(html)
    }

    // Provide Coil3 as the image loader so picsum URLs resolve via the network stack already wired
    // up in the sample app. Outside of CompositionLocalProvider, the library falls back to
    // DefaultImageLoader (which only knows how to render Painter-backed models).
    CompositionLocalProvider(LocalImageLoader provides Coil3ImageLoader) {
        SampleScaffold(
            title = "Images",
            navigateBack = navigateBack,
        ) { paddingValues ->
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .windowInsetsPadding(WindowInsets.ime)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Spacer(Modifier.height(4.dp))

                IntroCard()

                PresetCard(
                    onInsert = { preset ->
                        html = appendImage(
                            html = html,
                            url = preset.url,
                            width = preset.width,
                            height = preset.height,
                        )
                    },
                    onReset = { html = SeedHtml.trimIndent() },
                )

                CustomCard(
                    url = customUrl,
                    onUrlChange = { customUrl = it },
                    width = customWidth,
                    onWidthChange = { customWidth = it },
                    height = customHeight,
                    onHeightChange = { customHeight = it },
                    onInsert = {
                        if (customUrl.isNotBlank()) {
                            html = appendImage(
                                html = html,
                                url = customUrl.trim(),
                                width = customWidth.toInt(),
                                height = customHeight.toInt(),
                            )
                        }
                    },
                )

                PreviewCard(state = state)

                ExportCard(
                    title = "HTML source",
                    accent = SampleAccents.Coral,
                    content = remember(state.annotatedString) { state.toHtml() },
                )

                ExportCard(
                    title = "Markdown source",
                    accent = SampleAccents.Amber,
                    content = remember(state.annotatedString) { state.toMarkdown() },
                )

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

private fun appendImage(html: String, url: String, width: Int, height: Int): String {
    val tag = "<p><img src=\"$url\" width=\"$width\" height=\"$height\" /></p>"
    return html.trimEnd() + "\n" + tag
}

@Composable
private fun IntroCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
    ) {
        Text(
            text = "How it works",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = SampleAccents.Violet,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Images are inserted via setHtml/setMarkdown and rendered with RichText. " +
                "The editor surfaces don't display images yet, so this demo focuses on the read-only view. " +
                "Try the oversized preset to see the container-width clamp from #423 in action.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PresetCard(
    onInsert: (ImagePreset) -> Unit,
    onReset: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(16.dp),
    ) {
        Text(
            text = "Quick insert",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = SampleAccents.Violet,
        )
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            imagePresets.forEach { preset ->
                AssistChip(
                    onClick = { onInsert(preset) },
                    label = { Text(preset.label) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Image,
                            contentDescription = null,
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                )
            }
            AssistChip(
                onClick = onReset,
                label = { Text("Reset") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.RestartAlt,
                        contentDescription = null,
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            )
        }
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            imagePresets.forEach { preset ->
                Text(
                    text = "• ${preset.label} - ${preset.note}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CustomCard(
    url: String,
    onUrlChange: (String) -> Unit,
    width: Float,
    onWidthChange: (Float) -> Unit,
    height: Float,
    onHeightChange: (Float) -> Unit,
    onInsert: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(16.dp),
    ) {
        Text(
            text = "Custom URL",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = SampleAccents.Violet,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = url,
            onValueChange = onUrlChange,
            label = { Text("https://…") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))

        Text(
            text = "Width - ${width.toInt()} px",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Slider(
            value = width,
            onValueChange = onWidthChange,
            valueRange = 60f..2000f,
        )

        Text(
            text = "Height - ${height.toInt()} px",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Slider(
            value = height,
            onValueChange = onHeightChange,
            valueRange = 60f..2000f,
        )

        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            TextButton(
                onClick = onInsert,
                enabled = url.isNotBlank(),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                )
                Spacer(Modifier.width(6.dp))
                Text("Insert")
            }
        }
    }
}

@Composable
private fun PreviewCard(state: com.mohamedrejeb.richeditor.model.RichTextState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
    ) {
        Text(
            text = "Preview",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = SampleAccents.Indigo,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Rendered with RichText (read-only). Container width feeds the clamp logic.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        RichText(
            state = state,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ExportCard(
    title: String,
    accent: Color,
    content: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(12.dp),
    ) {
        Row(
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = accent,
                modifier = Modifier.weight(1f),
            )
            CopyButton(content = content)
        }
        Spacer(Modifier.height(6.dp))
        val scroll = rememberScrollState()
        SelectionContainer {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 160.dp)
                    .verticalScroll(scroll),
            ) {
                Text(
                    text = content,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
