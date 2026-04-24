package com.mohamedrejeb.richeditor.model

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.ui.BasicRichText
import org.junit.Test

/**
 * Reproduction attempts for the `adjustRichParagraphLayout` crash family:
 *   #667 — "addTextAtIndex lineIndex(2) is out of bounds [0, 2)"
 *          (real frame: getLineEnd(maxLines - 1) when maxLines > lineCount)
 *   #627 — "IndexOutOfBoundsException: index: 18, size: 0"
 *          (StateListIterator.next during iteration)
 *   #631 — "IllegalArgumentException at findParagraphByIndex via getHorizontalPosition"
 */
class AdjustRichParagraphLayoutCrashTest {

    private val listHtml = """
        <ol>
          <li>First item</li>
          <li>Second item</li>
          <li>Third item</li>
          <li>Fourth item</li>
          <li>Fifth item</li>
        </ol>
    """.trimIndent()

    private val shortHtml = "<ol><li>one</li><li>two</li></ol>"

    private val longParagraphHtml = "<p>" + "This is a fairly long line that should wrap several times in a narrow container. ".repeat(10) + "</p>"

    private val emptyTrailingHtml = """
        <ol>
        <li>item</li>
        </ol>
        <p></p>
        <br>
        <p></p>
    """.trimIndent()

    // #667: maxLines larger than actual laid-out line count while didExceedMaxLines is claimed.
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `maxLines greater than content line count with ol does not crash`() =
        runDesktopComposeUiTest(width = 800, height = 600) {
            setContent {
                val state = remember { RichTextState() }
                LaunchedEffect(Unit) { state.setHtml(shortHtml) }
                BasicRichText(
                    state = state,
                    maxLines = 5,
                    modifier = Modifier.width(400.dp)
                )
            }
            waitForIdle()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `maxLines equals line count with ol does not crash`() =
        runDesktopComposeUiTest(width = 800, height = 600) {
            setContent {
                val state = remember { RichTextState() }
                LaunchedEffect(Unit) { state.setHtml(shortHtml) }
                BasicRichText(
                    state = state,
                    maxLines = 2,
                    modifier = Modifier.width(400.dp)
                )
            }
            waitForIdle()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `maxLines one with ol forces truncation without crash`() =
        runDesktopComposeUiTest(width = 800, height = 600) {
            setContent {
                val state = remember { RichTextState() }
                LaunchedEffect(Unit) { state.setHtml(listHtml) }
                BasicRichText(
                    state = state,
                    maxLines = 1,
                    modifier = Modifier.width(400.dp)
                )
            }
            waitForIdle()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `maxLines with ellipsis overflow does not crash`() =
        runDesktopComposeUiTest(width = 800, height = 600) {
            setContent {
                val state = remember { RichTextState() }
                LaunchedEffect(Unit) { state.setHtml(listHtml) }
                BasicRichText(
                    state = state,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(400.dp)
                )
            }
            waitForIdle()
        }

    // #631: long content with maxLines aggressively truncated.
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `long wrapping paragraph with small maxLines does not crash`() =
        runDesktopComposeUiTest(width = 800, height = 600) {
            setContent {
                val state = remember { RichTextState() }
                LaunchedEffect(Unit) { state.setHtml(longParagraphHtml) }
                BasicRichText(
                    state = state,
                    maxLines = 2,
                    modifier = Modifier.width(200.dp)
                )
            }
            waitForIdle()
        }

    // #667 stress: very large maxLines with short content (maxLines >> lineCount).
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `maxLines much greater than line count does not crash`() =
        runDesktopComposeUiTest(width = 800, height = 600) {
            setContent {
                val state = remember { RichTextState() }
                LaunchedEffect(Unit) { state.setHtml(shortHtml) }
                BasicRichText(
                    state = state,
                    maxLines = 100,
                    modifier = Modifier.width(400.dp)
                )
            }
            waitForIdle()
        }

    // Empty trailing paragraph edge case.
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `empty trailing paragraphs with maxLines does not crash`() =
        runDesktopComposeUiTest(width = 800, height = 600) {
            setContent {
                val state = remember { RichTextState() }
                LaunchedEffect(Unit) { state.setHtml(emptyTrailingHtml) }
                BasicRichText(
                    state = state,
                    maxLines = 3,
                    modifier = Modifier.width(400.dp)
                )
            }
            waitForIdle()
        }

    // Dynamic content change while laid out (covers #627 concurrent-modification family).
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `setHtml swap during layout with ol and maxLines does not crash`() =
        runDesktopComposeUiTest(width = 800, height = 600) {
            setContent {
                val state = remember { RichTextState() }
                LaunchedEffect(Unit) {
                    state.setHtml(shortHtml)
                    state.setHtml(listHtml)
                    state.setHtml("<p>plain</p>")
                    state.setHtml(listHtml)
                }
                BasicRichText(
                    state = state,
                    maxLines = 2,
                    modifier = Modifier.width(400.dp)
                )
            }
            waitForIdle()
        }

    // Container height smaller than content forces clipping.
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `tiny container height with ol does not crash`() =
        runDesktopComposeUiTest(width = 800, height = 600) {
            setContent {
                val state = remember { RichTextState() }
                LaunchedEffect(Unit) { state.setHtml(listHtml) }
                BasicRichText(
                    state = state,
                    maxLines = 10,
                    modifier = Modifier
                        .width(400.dp)
                        .height(20.dp)
                )
            }
            waitForIdle()
        }
}
