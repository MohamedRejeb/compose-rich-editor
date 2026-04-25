package com.mohamedrejeb.richeditor.model

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.ui.BasicRichText
import org.junit.Test

/**
 * Reproduction attempt for issue #404:
 * "Crash when setting the maxLines with <ol> label"
 *
 * Original stack:
 *   IndexOutOfBoundsException: Index -11 out of bounds for length 10
 *     at MultiParagraph.getHorizontalPosition
 *     at RichTextState.adjustRichParagraphLayout:2672
 *     at RichTextState.onTextLayout
 *     at BasicRichText ... onTextLayout
 */
class Issue404MaxLinesOrderedListTest {

    private val problematicHtml = """
        <p><strong>Job Description:</strong></p>
        <ol>
        <li>Responsible for the design of Document AI product solutions, transforming algorithmic technology into user-friendly product features.<br>Work closely with the algorithm development team to ensure the feasibility and innovation of product solutions.<br>Conduct market research to understand user needs and integrate feedback into product solutions.<br>Write product documents, including requirement analysis, design documents, and user manuals.<br>Requirements:
        <ul>
        <li>Bachelor's degree or above in Computer Science, Artificial Intelligence, Statistics, Mathematics, Product Design, or related fields.<br>Deep understanding of product design and user experience.<br>Good cross-departmental communication skills and team spirit.<br>Experience in educational technology or intelligent scheduling system product solution design is preferred.<br>Proficient in English and Chinese, capable of writing and reading technical documents..<br>Compensation:</li>
        </ul>
        </li>
        <li>Highly competitive salary.
        <ul>
        <li>Guaranteed 13th-month pay.</li>
        <li>Transportation, communication, and meal allowances.</li>
        </ul>
        </li>
        <li>Discretionary bonus and project commission.</li>
        <li>Weekend off.
        <ul>
        <li>Beautiful office environment.</li>
        <li>Humanized management.</li>
        </ul>
        </li>
        <li>Application Method:</li>
        </ol>
        <p>Please send your resume and cover letter to [hkjobs@metaarchit.com].</p>
    """.trimIndent()

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `BasicRichText with ordered list HTML and small maxLines does not crash`() =
        runDesktopComposeUiTest(width = 320, height = 400) {
            setContent {
                val state = remember { RichTextState() }
                LaunchedEffect(Unit) {
                    state.setHtml(problematicHtml)
                }
                BasicRichText(
                    state = state,
                    maxLines = 3,
                    modifier = Modifier.width(300.dp)
                )
            }
            waitForIdle()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `BasicRichText with ordered list and tiny maxLines does not crash`() =
        runDesktopComposeUiTest(width = 320, height = 200) {
            setContent {
                val state = remember { RichTextState() }
                LaunchedEffect(Unit) {
                    state.setHtml(problematicHtml)
                }
                BasicRichText(
                    state = state,
                    maxLines = 1,
                    modifier = Modifier.width(300.dp)
                )
            }
            waitForIdle()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `BasicRichText with ordered list and maxLines larger than layout does not crash`() =
        runDesktopComposeUiTest(width = 320, height = 800) {
            setContent {
                val state = remember { RichTextState() }
                LaunchedEffect(Unit) {
                    state.setHtml(problematicHtml)
                }
                BasicRichText(
                    state = state,
                    maxLines = 10,
                    modifier = Modifier.width(300.dp)
                )
            }
            waitForIdle()
        }
}
