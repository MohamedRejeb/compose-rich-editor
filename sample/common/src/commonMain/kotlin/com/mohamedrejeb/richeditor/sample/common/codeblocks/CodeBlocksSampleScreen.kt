package com.mohamedrejeb.richeditor.sample.common.codeblocks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.highlight.CodeBlockColors
import com.mohamedrejeb.richeditor.highlight.CodeHighlighter
import com.mohamedrejeb.richeditor.highlight.CodeLanguage
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.sample.common.components.SampleScaffold
import com.mohamedrejeb.richeditor.ui.BasicCodeText
import com.mohamedrejeb.richeditor.ui.material3.RichText

private val replyMarkdown = """
Here is a small **Kotlin** function and the same idea in *Python*:

```kotlin
/* Greets everyone
   in the list. */
@Composable
fun Greeting(names: List<String>) {
    for (name in names) {
        Text("Hello, ${'$'}name!") // one line each
    }
}
```

```python
def greet(names):
    ${"\"\"\""}Greets everyone in the list.${"\"\"\""}
    for name in names:
        print(f"Hello, {name}!")  # one line each
```

Both walk the list once.
""".trimIndent()

private const val Snippet =
    "val message = listOf(\"a fairly long line\", \"that does not fit\", \"and scrolls sideways\").joinToString(separator = \", \") { it.uppercase() }\n" +
        "println(message)"

private enum class DiffKind { Same, Added, Removed }

private class DiffLine(val kind: DiffKind, val text: String)

private const val DiffFileName = "Greeter.kt"

private val diffLines = listOf(
    DiffLine(DiffKind.Same, "class Greeter(private val names: List<String>) {"),
    DiffLine(DiffKind.Same, "    /* Greets everyone"),
    DiffLine(DiffKind.Same, "       in the list. */"),
    DiffLine(DiffKind.Removed, "    fun greet() = names.forEach { println(\"Hi \$it\") }"),
    DiffLine(DiffKind.Added, "    fun greet(greeting: String = \"Hello\") ="),
    DiffLine(DiffKind.Added, "        names.forEach { println(\"\$greeting, \$it!\") }"),
    DiffLine(DiffKind.Same, "}"),
)

private val AddedBackground = Color(0x3322C55E)
private val RemovedBackground = Color(0x33EF4444)

@OptIn(ExperimentalRichTextApi::class)
@Composable
fun CodeBlocksSampleScreen(navigateBack: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val colors = remember(scheme) {
        CodeBlockColors(
            keyword = SpanStyle(color = scheme.primary),
            string = SpanStyle(color = scheme.tertiary),
            number = SpanStyle(color = scheme.secondary),
            comment = SpanStyle(color = scheme.outline, fontStyle = FontStyle.Italic),
            annotation = SpanStyle(color = scheme.secondary),
        )
    }

    SampleScaffold(
        title = "Code blocks",
        navigateBack = navigateBack,
    ) { paddingValues ->
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(8.dp))

            SectionTitle("Markdown with code blocks", "RichText draws fenced code coloured by its language, with a copy button on each block.")
            val state = remember(scheme, colors) {
                RichTextState().apply {
                    config.codeBlockBackgroundColor = scheme.surfaceVariant
                    config.codeBlockColors = colors
                    setMarkdown(replyMarkdown)
                }
            }
            RichText(state = state, modifier = Modifier.fillMaxWidth(), showCodeBlockCopyButton = true)

            SectionTitle("Code alone", "BasicCodeText needs no state. A long line scrolls sideways.")
            BasicCodeText(
                code = Snippet,
                language = CodeLanguage.Kotlin,
                colors = colors,
                style = CodeStyle.copy(color = scheme.onSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(scheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .padding(12.dp),
            )

            SectionTitle("A diff view", "Rows drawn by the app and coloured with CodeHighlighter.tokenizeLines.")
            DiffView(colors)

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionTitle(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(top = 12.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalRichTextApi::class)
@Composable
private fun DiffView(colors: CodeBlockColors) {
    val scheme = MaterialTheme.colorScheme
    val rows = remember(colors) { diffRows(colors) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(scheme.surfaceVariant),
    ) {
        diffLines.forEachIndexed { index, line ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        when (line.kind) {
                            DiffKind.Same -> Color.Transparent
                            DiffKind.Added -> AddedBackground
                            DiffKind.Removed -> RemovedBackground
                        }
                    )
                    .padding(horizontal = 12.dp, vertical = 2.dp),
            ) {
                Text(
                    text = when (line.kind) {
                        DiffKind.Same -> " "
                        DiffKind.Added -> "+"
                        DiffKind.Removed -> "-"
                    },
                    style = CodeStyle,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.width(20.dp),
                )
                Text(text = rows[index], style = CodeStyle, color = scheme.onSurface)
            }
        }
    }
}

/**
 * The text of each diff row with its token styles. The old and the new side are tokenized apart,
 * each as the continuous code it is, so a removed line never changes how an added one is read.
 */
@OptIn(ExperimentalRichTextApi::class)
private fun diffRows(colors: CodeBlockColors): List<AnnotatedString> {
    val language = CodeLanguage.fromFileName(DiffFileName) ?: return diffLines.map { AnnotatedString(it.text) }
    val oldSide = diffLines.withIndex().filter { it.value.kind != DiffKind.Added }
    val newSide = diffLines.withIndex().filter { it.value.kind != DiffKind.Removed }
    val oldTokens = CodeHighlighter.tokenizeLines(oldSide.map { it.value.text }, language)
    val newTokens = CodeHighlighter.tokenizeLines(newSide.map { it.value.text }, language)
    val tokensByRow =
        oldSide.mapIndexed { position, row -> row.index to oldTokens[position] }.toMap() +
            newSide.mapIndexed { position, row -> row.index to newTokens[position] }.toMap()

    return diffLines.mapIndexed { index, line ->
        buildAnnotatedString {
            append(line.text)
            tokensByRow[index].orEmpty().forEach { token ->
                addStyle(colors.styleOf(token.kind), token.start, token.end)
            }
        }
    }
}

private val CodeStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp)
