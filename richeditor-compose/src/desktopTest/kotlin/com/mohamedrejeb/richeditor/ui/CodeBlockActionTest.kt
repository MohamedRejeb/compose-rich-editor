package com.mohamedrejeb.richeditor.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * The action drawn at the top end corner of each code block in the read-only text, and the copy
 * button the Material text puts there.
 */
@OptIn(ExperimentalTestApi::class, ExperimentalRichTextApi::class)
class CodeBlockActionTest {

    private val markdown = "Intro\n\n```kotlin\nval a = 1\n    val b = 2\n```\n\nBetween\n\n```\nplain\n```"

    private class FakeClipboardManager : ClipboardManager {
        var copied: AnnotatedString? = null

        override fun getText(): AnnotatedString? = copied

        override fun setText(annotatedString: AnnotatedString) {
            copied = annotatedString
        }
    }

    @Test
    fun `the action is given the code and language of each block`() = runDesktopComposeUiTest {
        val state = RichTextState().apply { setMarkdown(markdown) }
        val seen = mutableListOf<Pair<String, String?>>()
        setContent {
            BasicRichText(
                state = state,
                modifier = Modifier.fillMaxWidth(),
                codeBlockAction = { code, language ->
                    seen += code to language
                    Box(Modifier.size(20.dp).testTag(ACTION_TAG))
                },
            )
        }
        waitForIdle()

        assertEquals(
            setOf("val a = 1\n    val b = 2" to "kotlin", "plain" to null),
            seen.toSet(),
        )
        onAllNodesWithTag(ACTION_TAG).assertCountEquals(2)
    }

    @Test
    fun `each action sits at the top end corner of its block`() = runDesktopComposeUiTest {
        val state = RichTextState().apply { setMarkdown(markdown) }
        var layout: TextLayoutResult? = null
        setContent {
            Box(Modifier.width(300.dp)) {
                BasicRichText(
                    state = state,
                    modifier = Modifier.fillMaxWidth().testTag(TEXT_TAG),
                    onTextLayout = { layout = it },
                    codeBlockAction = { _, _ -> Box(Modifier.size(20.dp).testTag(ACTION_TAG)) },
                )
            }
        }
        waitForIdle()

        val lines = assertNotNull(layout)
        val text = onNodeWithTag(TEXT_TAG).getUnclippedBoundsInRoot()
        val actions = onAllNodesWithTag(ACTION_TAG)
        // Line 0 is the intro, lines 1 and 2 the first block, line 3 the text between, line 4 the second block.
        listOf(0 to 1, 1 to 4).forEach { (index, line) ->
            val bounds = actions[index].getUnclippedBoundsInRoot()
            assertEquals(text.right, bounds.right, "action $index ends at the end edge")
            assertEquals(text.top.value + lines.getLineTop(line), bounds.top.value, 1f, "action $index starts at its block's first line")
        }
    }

    @Test
    fun `text without a block has no action`() = runDesktopComposeUiTest {
        val state = RichTextState().apply { setMarkdown("just text") }
        setContent {
            BasicRichText(state = state, codeBlockAction = { _, _ -> Box(Modifier.size(20.dp).testTag(ACTION_TAG)) })
        }
        waitForIdle()

        onAllNodesWithTag(ACTION_TAG).assertCountEquals(0)
    }

    @Test
    fun `a block left out by maxLines shows no action`() = runDesktopComposeUiTest {
        val state = RichTextState().apply { setMarkdown(markdown) }
        setContent {
            BasicRichText(
                state = state,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                codeBlockAction = { _, _ -> Box(Modifier.size(20.dp).testTag(ACTION_TAG)) },
            )
        }
        waitForIdle()

        // The second block is past the third line: its action is composed but never placed.
        val actions = onAllNodesWithTag(ACTION_TAG)
        actions[0].assertIsDisplayed()
        actions[1].assertIsNotDisplayed()
    }

    @Test
    fun `the action follows the blocks when the text changes`() = runDesktopComposeUiTest {
        val state = RichTextState().apply { setMarkdown("just text") }
        setContent {
            BasicRichText(state = state, codeBlockAction = { _, _ -> Box(Modifier.size(20.dp).testTag(ACTION_TAG)) })
        }
        waitForIdle()

        state.setMarkdown(markdown)
        waitForIdle()
        onAllNodesWithTag(ACTION_TAG).assertCountEquals(2)

        state.setMarkdown("```\nonly\n```")
        waitForIdle()
        onAllNodesWithTag(ACTION_TAG).assertCountEquals(1)
    }

    @Test
    fun `the copy button copies the exact code of its block`() = runDesktopComposeUiTest {
        val state = RichTextState().apply { setMarkdown(markdown + "\n\nEnd") }
        val clipboard = FakeClipboardManager()
        setContent {
            CompositionLocalProvider(LocalClipboardManager provides clipboard) {
                RichText(state = state, modifier = Modifier.fillMaxWidth(), showCodeBlockCopyButton = true)
            }
        }
        waitForIdle()

        val buttons = onAllNodesWithContentDescription(COPY_DESCRIPTION)
        buttons.assertCountEquals(2)

        buttons[0].performClick()
        waitForIdle()
        assertEquals("val a = 1\n    val b = 2", clipboard.copied?.text)

        buttons[1].performClick()
        waitForIdle()
        assertEquals("plain", clipboard.copied?.text)
    }

    @Test
    fun `the copy button is off by default`() = runDesktopComposeUiTest {
        val state = RichTextState().apply { setMarkdown(markdown) }
        setContent { RichText(state = state) }
        waitForIdle()

        onAllNodesWithContentDescription(COPY_DESCRIPTION).assertCountEquals(0)
    }

    private companion object {
        const val ACTION_TAG = "action"
        const val TEXT_TAG = "text"
        const val COPY_DESCRIPTION = "Copy code"
    }
}
