package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.highlight.CodeBlockColors
import com.mohamedrejeb.richeditor.highlight.CodeHighlighter
import com.mohamedrejeb.richeditor.highlight.CodeLanguage
import com.mohamedrejeb.richeditor.highlight.CodeToken
import com.mohamedrejeb.richeditor.paragraph.RichParagraph
import com.mohamedrejeb.richeditor.paragraph.codeBlockGroups
import com.mohamedrejeb.richeditor.paragraph.type.CodeBlock

/**
 * A code block as it sits in the rendered text: where it is and the token styles drawn over it.
 * [lastLineOffset] is an offset on the last line of the block, which [range] cannot give: its
 * end is the start of whatever follows, and an empty last line has no character of its own.
 */
internal data class RenderedCodeBlock(
    val range: TextRange,
    val lastLineOffset: Int,
    val styles: List<AnnotatedString.Range<SpanStyle>>,
)

/**
 * Tokens of the blocks rendered last, so a rebuild that leaves a block's text alone (typing
 * elsewhere, a style change) does not tokenize it again.
 */
@OptIn(ExperimentalRichTextApi::class)
internal class CodeBlockTokenCache {
    private val tokens = HashMap<Pair<String, String?>, List<CodeToken>>()
    private val used = HashSet<Pair<String, String?>>()

    fun tokens(code: String, language: String?): List<CodeToken> {
        val key = code to language
        used += key
        return tokens.getOrPut(key) {
            language?.let { CodeLanguage.fromName(it) }?.let { CodeHighlighter.tokenize(code, it) } ?: emptyList()
        }
    }

    /** Forgets every block that was not asked for since the last call. */
    fun keepOnlyUsed() {
        tokens.keys.retainAll(used)
        used.clear()
    }
}

/**
 * The code blocks of [paragraphs] located in [annotated], which must be the text built from
 * them: one ParagraphStyle range per paragraph, in order.
 */
@OptIn(ExperimentalRichTextApi::class)
internal fun renderCodeBlocks(
    paragraphs: List<RichParagraph>,
    annotated: AnnotatedString,
    colors: CodeBlockColors,
    cache: CodeBlockTokenCache,
): List<RenderedCodeBlock> {
    val ranges = annotated.paragraphStyles
    val groups = if (ranges.size == paragraphs.size) paragraphs.codeBlockGroups() else emptyList()
    val blocks = groups.map { group ->
        val start = ranges[group.first].start
        val end = ranges[group.last].end
        val chars = annotated.text.substring(start, end).toCharArray()
        // The separator that ends each line but the last is read as the line end it stands for.
        for (index in group.first until group.last) {
            val separator = ranges[index].end - 1 - start
            if (separator in chars.indices && chars[separator] == ' ') chars[separator] = '\n'
        }
        val language = (paragraphs[group.first].type as? CodeBlock)?.language
        val lastLine = ranges[group.last]
        RenderedCodeBlock(
            range = TextRange(start, end),
            lastLineOffset = if (lastLine.start == lastLine.end) lastLine.start else lastLine.end - 1,
            styles = cache.tokens(chars.concatToString(), language).map { token ->
                AnnotatedString.Range(colors.styleOf(token.kind), start + token.start, start + token.end)
            },
        )
    }
    cache.keepOnlyUsed()
    return blocks
}

/** This text with the token styles of [blocks] added after its own styles. */
internal fun AnnotatedString.withCodeBlockStyles(blocks: List<RenderedCodeBlock>): AnnotatedString {
    if (blocks.all { it.styles.isEmpty() }) return this
    return AnnotatedString.Builder(this).apply {
        blocks.forEach { block ->
            block.styles.forEach { range ->
                val start = range.start.coerceIn(0, length)
                val end = range.end.coerceIn(0, length)
                if (start < end) addStyle(range.item, start, end)
            }
        }
    }.toAnnotatedString()
}
