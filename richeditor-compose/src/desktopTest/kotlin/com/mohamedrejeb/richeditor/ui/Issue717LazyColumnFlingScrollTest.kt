package com.mohamedrejeb.richeditor.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.model.RichTextState
import org.junit.Test
import kotlin.test.fail

/**
 * Guard rails for issue #717 (OffsetMapping out-of-bounds crash during LazyColumn
 * scroll/fling): `OffsetMapping.Identity` is only valid while
 * `annotatedString.text.length == textFieldValue.text.length`, so each test drives
 * scroll churn over editors and asserts that invariant at every layout pass. The
 * desktop patterns do not reproduce the original Android crash; they catch the
 * model-level desync if it ever manifests.
 */
class Issue717LazyColumnFlingScrollTest {

    /** Mixed paragraph types, ~168 visible chars (first variant in the report). */
    private fun shortRichHtml(seed: Int): String = """
        <h2>Section $seed</h2>
        <p>Intro paragraph with <b>bold</b> and <i>italic</i> and <u>underline</u> text.</p>
        <ol>
          <li>First numbered item</li>
          <li>Second numbered item</li>
          <li>Third numbered item</li>
        </ol>
    """.trimIndent()

    /** ~460 rendered chars (second variant in the report). */
    private fun longRichHtml(seed: Int): String = """
        <h2>Document #$seed</h2>
        <p>This editor has a moderately long mixed paragraph with several inline
        spans like <b>bold</b>, <i>italic</i>, <u>underline</u>, and
        <code>inline code</code> embedded in regular prose so paragraph lengths
        and list prefix widths exercise the offset-mapping validation path.</p>
        <ol>
          <li>Numbered item one with extra trailing words.</li>
          <li>Numbered item two also with trailing words.</li>
          <li>Item three to push past the 99/100 boundary in some seeds.</li>
        </ol>
        <ul>
          <li>Bullet alpha</li>
          <li>Bullet beta</li>
        </ul>
        <p>Closing paragraph with a final note.</p>
    """.trimIndent()

    private fun RichTextState.assertOffsetMappingInvariant(label: String) {
        val asLen = annotatedString.text.length
        val tfvLen = textFieldValue.text.length
        if (asLen != tfvLen) {
            fail(
                "Offset mapping invariant violated $label: " +
                    "annotatedString.length=$asLen, textFieldValue.text.length=$tfvLen " +
                    "(diff=${tfvLen - asLen}). OffsetMapping.Identity will throw."
            )
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `scroll through LazyColumn of BasicRichTextEditor does not crash with short content`() =
        runDesktopComposeUiTest(width = 480, height = 360) {
            val editorCount = 12
            val states = List(editorCount) { i ->
                RichTextState().apply { setHtml(shortRichHtml(i)) }
            }

            setContent {
                val listState = rememberLazyListState()

                LaunchedEffect(Unit) {
                    repeat(6) {
                        listState.scrollToItem(editorCount - 1)
                        listState.scrollToItem(0)
                        listState.scrollToItem(editorCount / 2)
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(states) { state ->
                        BasicRichTextEditor(
                            state = state,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            onTextLayout = {
                                state.assertOffsetMappingInvariant("during onTextLayout")
                            },
                        )
                    }
                }
            }

            waitForIdle()

            states.forEachIndexed { i, state ->
                state.assertOffsetMappingInvariant("after scroll, editor $i")
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `scroll through LazyColumn of BasicRichTextEditor does not crash with long content`() =
        runDesktopComposeUiTest(width = 480, height = 360) {
            val editorCount = 8
            val states = List(editorCount) { i ->
                RichTextState().apply { setHtml(longRichHtml(i)) }
            }

            setContent {
                val listState = rememberLazyListState()

                LaunchedEffect(Unit) {
                    repeat(8) { iter ->
                        listState.scrollToItem(editorCount - 1)
                        listState.scrollToItem(0)
                        listState.scrollToItem(iter % editorCount)
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(states) { state ->
                        BasicRichTextEditor(
                            state = state,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            onTextLayout = {
                                state.assertOffsetMappingInvariant("during onTextLayout (long)")
                            },
                        )
                    }
                }
            }

            waitForIdle()

            states.forEachIndexed { i, state ->
                state.assertOffsetMappingInvariant("after scroll, editor $i")
            }
        }

    /**
     * `dispatchRawDelta` exercises the same measure path as a real fling
     * without animation latency.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `dispatchRawDelta fling through rich editors does not crash`() =
        runDesktopComposeUiTest(width = 480, height = 360) {
            val editorCount = 10
            val states = List(editorCount) { i ->
                RichTextState().apply { setHtml(longRichHtml(i)) }
            }

            setContent {
                val listState = rememberLazyListState()

                LaunchedEffect(Unit) {
                    val flingProfile = listOf(
                        80f, 160f, 240f, 320f, 280f, 220f, 160f, 100f, 60f, 30f,
                        -30f, -60f, -100f, -160f, -220f, -280f, -320f, -240f, -160f, -80f,
                    )
                    repeat(4) {
                        flingProfile.forEach { delta ->
                            listState.dispatchRawDelta(delta)
                        }
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(states) { state ->
                        BasicRichTextEditor(
                            state = state,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            onTextLayout = {
                                state.assertOffsetMappingInvariant("during dispatchRawDelta")
                            },
                        )
                    }
                }
            }

            waitForIdle()

            states.forEachIndexed { i, state ->
                state.assertOffsetMappingInvariant("after fling, editor $i")
            }
        }

    /**
     * Items leaving and re-entering composition re-run `setHtml`, which recomputes
     * both `annotatedString` and `textFieldValue` during scroll churn.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `LazyColumn with re-mounted RichTextState items survives scroll churn`() =
        runDesktopComposeUiTest(width = 480, height = 360) {
            val editorCount = 12

            setContent {
                val listState = rememberLazyListState()

                LaunchedEffect(Unit) {
                    repeat(5) {
                        listState.scrollToItem(editorCount - 1)
                        listState.scrollToItem(0)
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(
                        count = editorCount,
                        key = { it },
                    ) { index ->
                        val state = remember(index) {
                            RichTextState().apply { setHtml(longRichHtml(index)) }
                        }
                        BasicRichTextEditor(
                            state = state,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            onTextLayout = {
                                state.assertOffsetMappingInvariant("during re-mount churn")
                            },
                        )
                    }
                }
            }

            waitForIdle()
        }

    /**
     * Intermixes content mutations with scrolling so a scroll-driven measure pass can
     * land between the `annotatedString` and `textFieldValue` writes.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `mutate content while scrolling does not crash and keeps invariant`() =
        runDesktopComposeUiTest(width = 480, height = 360) {
            val editorCount = 8
            val states = List(editorCount) { i ->
                RichTextState().apply { setHtml(shortRichHtml(i)) }
            }

            setContent {
                val listState = rememberLazyListState()

                LaunchedEffect(Unit) {
                    repeat(4) { iter ->
                        // Mutate state of every editor before each scroll pass.
                        states.forEachIndexed { i, state ->
                            state.setHtml(
                                if ((iter + i) % 2 == 0) shortRichHtml(i) else longRichHtml(i)
                            )
                        }
                        listState.scrollToItem(editorCount - 1)
                        listState.scrollToItem(0)
                        listState.scrollToItem(editorCount / 2)
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(states) { state ->
                        BasicRichTextEditor(
                            state = state,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            onTextLayout = {
                                state.assertOffsetMappingInvariant("during scroll+mutate")
                            },
                        )
                    }
                }
            }

            waitForIdle()

            states.forEachIndexed { i, state ->
                state.assertOffsetMappingInvariant("after scroll+mutate, editor $i")
            }
        }
}
