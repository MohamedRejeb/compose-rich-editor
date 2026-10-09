package com.mohamedrejeb.richeditor.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.highlight.CodeBlockColors
import com.mohamedrejeb.richeditor.highlight.CodeLanguage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class, ExperimentalRichTextApi::class)
class BasicCodeTextTest {

    private val longLine = "val a = " + "\"word \" + ".repeat(40) + "1"

    @Test
    fun `the code is laid out with its token styles`() = runDesktopComposeUiTest {
        var layout: TextLayoutResult? = null
        setContent {
            BasicCodeText(code = "val a = 1", language = CodeLanguage.Kotlin, onTextLayout = { layout = it })
        }
        waitForIdle()

        val text = assertNotNull(layout).layoutInput.text
        assertEquals("val a = 1", text.text)
        assertEquals(
            listOf(
                AnnotatedString.Range(CodeBlockColors.Default.keyword, 0, 3),
                AnnotatedString.Range(CodeBlockColors.Default.number, 8, 9),
            ),
            text.spanStyles,
        )
    }

    @Test
    fun `a long line stays on one line by default`() = runDesktopComposeUiTest {
        var layout: TextLayoutResult? = null
        setContent {
            Box(Modifier.width(120.dp)) {
                BasicCodeText(code = longLine, language = CodeLanguage.Kotlin, onTextLayout = { layout = it })
            }
        }
        waitForIdle()

        val result = assertNotNull(layout)
        assertEquals(1, result.lineCount)
        // Laid out wider than the 120.dp parent and not clipped: the line scrolls sideways.
        assertTrue(result.size.width > 120, "the line keeps its full width, was ${result.size.width}")
        assertTrue(!result.didOverflowWidth, "the line is not cut off")
    }

    @Test
    fun `a long line wraps when softWrap is on`() = runDesktopComposeUiTest {
        var layout: TextLayoutResult? = null
        setContent {
            Box(Modifier.width(120.dp)) {
                BasicCodeText(code = longLine, language = CodeLanguage.Kotlin, softWrap = true, onTextLayout = { layout = it })
            }
        }
        waitForIdle()

        assertTrue(assertNotNull(layout).lineCount > 1)
    }
}
