package com.mohamedrejeb.richeditor.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class Issue486DisabledColorTransformationTest {

    @Test
    fun `dims explicitly colored spans by the configured factor`() {
        val source = AnnotatedString.Builder().apply {
            append("hello ")
            pushStyle(SpanStyle(color = Color.Red))
            append("RED")
            pop()
            append(" world")
        }.toAnnotatedString()

        val transformation = DisabledTextVisualTransformation(
            delegate = VisualTransformation.None,
            disabledAlpha = 0.5f,
        )

        val result = transformation.filter(source)
        val spans = result.text.spanStyles
        assertEquals(1, spans.size)

        val dimmed = spans[0].item.color
        assertTrue(dimmed.isSpecified)
        // 8-bit color quantization (1/255) means alpha can drift by ~0.004.
        assertEquals(0.5f, dimmed.alpha, absoluteTolerance = 0.01f)
        assertEquals(Color.Red.red, dimmed.red, absoluteTolerance = 0.01f)
        assertEquals(Color.Red.green, dimmed.green, absoluteTolerance = 0.01f)
        assertEquals(Color.Red.blue, dimmed.blue, absoluteTolerance = 0.01f)
    }

    @Test
    fun `dimming compounds with existing span alpha`() {
        val translucentRed = Color.Red.copy(alpha = 0.6f)
        val source = AnnotatedString.Builder().apply {
            pushStyle(SpanStyle(color = translucentRed))
            append("RED")
            pop()
        }.toAnnotatedString()

        val transformation = DisabledTextVisualTransformation(
            delegate = VisualTransformation.None,
            disabledAlpha = 0.5f,
        )

        val dimmed = transformation.filter(source).text.spanStyles[0].item.color
        assertEquals(0.3f, dimmed.alpha, absoluteTolerance = 0.01f)
    }

    @Test
    fun `leaves spans without specified color unchanged`() {
        val source = AnnotatedString.Builder().apply {
            pushStyle(SpanStyle(background = Color.Yellow))
            append("plain")
            pop()
        }.toAnnotatedString()

        val transformation = DisabledTextVisualTransformation(
            delegate = VisualTransformation.None,
            disabledAlpha = 0.38f,
        )

        val result = transformation.filter(source)
        assertEquals(1, result.text.spanStyles.size)
        assertEquals(Color.Unspecified, result.text.spanStyles[0].item.color)
        assertEquals(Color.Yellow, result.text.spanStyles[0].item.background)
    }

    @Test
    fun `preserves text and paragraph styles`() {
        val source = AnnotatedString(
            text = "abc",
            spanStyles = emptyList(),
            paragraphStyles = listOf(
                AnnotatedString.Range(ParagraphStyle(), 0, 3)
            )
        )

        val transformation = DisabledTextVisualTransformation(
            delegate = VisualTransformation.None,
            disabledAlpha = 0.5f,
        )

        val result = transformation.filter(source)
        assertEquals("abc", result.text.text)
        assertEquals(1, result.text.paragraphStyles.size)
    }

    @Test
    fun `delegates offset mapping to the wrapped transformation`() {
        val customMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = offset + 10
            override fun transformedToOriginal(offset: Int): Int = (offset - 10).coerceAtLeast(0)
        }
        val delegate = VisualTransformation { input ->
            TransformedText(input, customMapping)
        }

        val transformation = DisabledTextVisualTransformation(
            delegate = delegate,
            disabledAlpha = 0.5f,
        )
        val result = transformation.filter(AnnotatedString("abc"))
        assertSame(customMapping, result.offsetMapping)
    }
}
