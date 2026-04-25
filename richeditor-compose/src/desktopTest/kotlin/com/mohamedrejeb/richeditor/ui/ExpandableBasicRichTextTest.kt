package com.mohamedrejeb.richeditor.ui

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichTextState
import org.junit.Test

@OptIn(ExperimentalTestApi::class, ExperimentalRichTextApi::class)
class ExpandableBasicRichTextTest {

    private val longContent = "alpha bravo charlie delta echo foxtrot golf hotel india juliet " +
        "kilo lima mike november oscar papa quebec romeo sierra tango uniform victor whiskey " +
        "xray yankee zulu and a few more words to make sure we overflow the three line budget " +
        "we set in the test."

    private val shortContent = "fits in three lines."

    private val singleLongWord = "x".repeat(400)

    @Test
    fun `short text does not show See more`() =
        runDesktopComposeUiTest(width = 400, height = 600) {
            setContent {
                val state = remember {
                    RichTextState().apply { setText(shortContent) }
                }
                ExpandableBasicRichText(
                    state = state,
                    expanded = false,
                    onExpandedChange = {},
                    collapsedMaxLines = 3,
                    modifier = Modifier.width(200.dp),
                )
            }
            waitForIdle()
            onAllNodes(hasText("See more", substring = true)).assertCountEquals(0)
        }

    @Test
    fun `long text collapsed shows See more`() =
        runDesktopComposeUiTest(width = 400, height = 600) {
            setContent {
                val state = remember {
                    RichTextState().apply { setText(longContent) }
                }
                ExpandableBasicRichText(
                    state = state,
                    expanded = false,
                    onExpandedChange = {},
                    collapsedMaxLines = 3,
                    modifier = Modifier.width(200.dp),
                )
            }
            waitForIdle()
            onAllNodes(hasText("See more", substring = true)).assertCountEquals(1)
        }

    @Test
    fun `expanded shows See less and not See more`() =
        runDesktopComposeUiTest(width = 400, height = 600) {
            setContent {
                val state = remember {
                    RichTextState().apply { setText(longContent) }
                }
                ExpandableBasicRichText(
                    state = state,
                    expanded = true,
                    onExpandedChange = {},
                    collapsedMaxLines = 3,
                    modifier = Modifier.width(200.dp),
                )
            }
            waitForIdle()
            onAllNodes(hasText("See less", substring = true)).assertCountEquals(1)
            onAllNodes(hasText("See more", substring = true)).assertCountEquals(0)
        }

    @Test
    fun `expanded does not truncate the body content`() =
        runDesktopComposeUiTest(width = 400, height = 600) {
            setContent {
                val state = remember {
                    RichTextState().apply { setText(longContent) }
                }
                ExpandableBasicRichText(
                    state = state,
                    expanded = true,
                    onExpandedChange = {},
                    collapsedMaxLines = 3,
                    modifier = Modifier.width(200.dp),
                )
            }
            waitForIdle()
            // The trailing word of the content must be present when expanded.
            onAllNodes(hasText("test.", substring = true)).assertCountEquals(1)
        }

    @Test
    fun `toggling expanded after collapse re-renders without crashing`() =
        runDesktopComposeUiTest(width = 400, height = 600) {
            setContent {
                val state = remember {
                    RichTextState().apply { setText(longContent) }
                }
                var expanded by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    // Flip to expanded then back to collapsed in successive frames; this exercises
                    // the NeedsMeasure -> Truncated -> NeedsMeasure phase transitions.
                    expanded = true
                    expanded = false
                }
                ExpandableBasicRichText(
                    state = state,
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    collapsedMaxLines = 3,
                    modifier = Modifier.width(200.dp),
                )
            }
            waitForIdle()
            onAllNodes(hasText("See more", substring = true)).assertCountEquals(1)
        }

    @Test
    fun `single very long word does not crash and shows See more`() =
        runDesktopComposeUiTest(width = 400, height = 600) {
            setContent {
                val state = remember {
                    RichTextState().apply { setText(singleLongWord) }
                }
                ExpandableBasicRichText(
                    state = state,
                    expanded = false,
                    onExpandedChange = {},
                    collapsedMaxLines = 3,
                    modifier = Modifier.width(200.dp),
                )
            }
            waitForIdle()
            // Cut must fall back to char-boundary; the affordance still renders.
            onAllNodes(hasText("See more", substring = true)).assertCountEquals(1)
        }
}
