package com.mohamedrejeb.richeditor.sample.common.claude

import com.mohamedrejeb.richeditor.model.RichTextState
import kotlinx.coroutines.delay

internal const val MENTION_TRIGGER_ID = "mention"
internal const val SLASH_TRIGGER_ID = "slash"

internal data class ClaudeContact(
    val id: String,
    val name: String,
    val handle: String,
    val description: String,
)

internal data class ClaudeSlashCommand(
    val id: String,
    val keyword: String,
    val description: String,
)

internal val claudeMentionContacts = listOf(
    ClaudeContact("c-claude", "Claude", "@claude", "Anthropic's assistant"),
    ClaudeContact("c-haiku", "Haiku", "@haiku", "Fastest model in the family"),
    ClaudeContact("c-sonnet", "Sonnet", "@sonnet", "Best for coding"),
    ClaudeContact("c-opus", "Opus", "@opus", "Deepest reasoning"),
    ClaudeContact("c-mohamed", "Mohamed Rejeb", "@mohamed", "Library maintainer"),
    ClaudeContact("c-team", "Team", "@team", "Group of collaborators"),
)

internal val claudeSlashCommands = listOf(
    ClaudeSlashCommand("cmd-summarize", "summarize", "Summarize the current conversation"),
    ClaudeSlashCommand("cmd-explain", "explain", "Explain a concept or piece of code"),
    ClaudeSlashCommand("cmd-translate", "translate", "Translate text into another language"),
    ClaudeSlashCommand("cmd-rewrite", "rewrite", "Rewrite text in a different tone"),
    ClaudeSlashCommand("cmd-brainstorm", "brainstorm", "Brainstorm ideas around a topic"),
    ClaudeSlashCommand("cmd-code", "code", "Write a code snippet for a task"),
    ClaudeSlashCommand("cmd-clear", "clear", "Clear the current chat"),
)

/**
 * Hand-written canned replies, picked round-robin per send. Each one shows
 * different markdown features (headings, lists, bold/italic, code, links)
 * so the streaming demo highlights what `setMarkdown` round-trips through
 * the editor's tree model.
 */
internal val claudeCannedReplies: List<String> = listOf(
    """
        Happy to help. Here is a quick rundown of what **Compose Rich Editor** can do today:

        - Bold, *italic*, ~~strikethrough~~, and `inline code` styles
        - Ordered, unordered, and nested lists
        - Headings from H1 through H6 with first-class `headingStyle`
        - Inline links and images that round-trip through HTML and Markdown

        Let me know which one you'd like to dig into next.
    """.trimIndent(),

    """
        ## Streaming markdown, simply

        You have two reasonable approaches:

        1. **Replace per chunk.** Buffer the tokens as they arrive, then call `state.setMarkdown(buffered)` on every update. Easiest path, works great for short replies.
        2. **Append per chunk.** Call `state.insertMarkdownAfterSelection(token)` on each chunk. Lower allocation, but partial markdown like `**Hel` will render literally until the closing pair arrives.

        For most chat UIs the first approach is plenty fast.
    """.trimIndent(),

    """
        Here's a tiny Kotlin example you can drop in:

        ```kotlin
        val state = rememberRichTextState()

        LaunchedEffect(prompt) {
            val chunks = response.split(" ")
            val buffer = StringBuilder()
            for (chunk in chunks) {
                buffer.append(chunk).append(' ')
                state.setMarkdown(buffer.toString())
                delay(35)
            }
        }
        ```

        That's the whole streaming loop - the editor handles the rest.
    """.trimIndent(),

    """
        # Quick note on triggers

        `RichTextState` exposes `registerTrigger(...)` so you can wire `@`, `#`, or `/` to any popup you like. The library tracks the active query for you via `state.activeTriggerQuery`, and you commit a selection with `RichSpanStyle.Token`.

        > Tip: pair triggers with `TriggerSuggestions` to get a Material-styled popup out of the box, or roll your own if you want a Notion-style menu.

        Try typing **@** or **/** in the composer below to see it light up.
    """.trimIndent(),

    """
        ## What good streaming looks like

        Streaming feels great when:

        1. The buffer flushes on **word boundaries**, not character by character.
        2. Code blocks render as soon as the opening fence arrives.
        3. Links resolve atomically once the closing parenthesis lands.

        For this demo I'm flushing every 1-2 words with a small delay, which keeps the UI lively without thrashing the parser.
    """.trimIndent(),

    """
        Of course. Here are a few directions you could take this sample further:

        - Hook it up to the [Anthropic API](https://docs.anthropic.com) and stream real responses
        - Add a **stop generating** affordance that cancels the in-flight coroutine
        - Track per-message tokens used so power users can watch their budget
        - Render code blocks with syntax highlighting via a custom `RichSpanStyle`

        Pick one and I can help you sketch it out.
    """.trimIndent(),
)

/**
 * Splits markdown into small chunks suitable for the streaming demo. Splits
 * on whitespace, keeping the trailing space on each chunk so the buffer can
 * be rebuilt by simple concatenation. Markdown formatting markers stay attached
 * to the words they surround, which keeps `**bold**` and `` `code` `` arriving
 * as single units in most cases.
 */
internal fun markdownStreamChunks(markdown: String): List<String> =
    markdown.split(Regex("(?<=\\s)"))
        .filter { it.isNotEmpty() }

/**
 * Streams [markdown] into [state] using `setMarkdown` per chunk. Cancellable
 * - if the surrounding `LaunchedEffect` is restarted the coroutine cancels,
 * leaving the partial state in place.
 */
internal suspend fun streamMarkdownInto(
    state: RichTextState,
    markdown: String,
    delayMs: Long = 35L,
) {
    val chunks = markdownStreamChunks(markdown)
    val buffer = StringBuilder()
    for (chunk in chunks) {
        buffer.append(chunk)
        state.setMarkdown(buffer.toString())
        delay(delayMs)
    }
    if (state.toMarkdown() != markdown) {
        state.setMarkdown(markdown)
    }
}
