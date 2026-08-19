package com.mohamedrejeb.richeditor.json

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.document.RichTextSpanMark
import com.mohamedrejeb.richeditor.model.RichSpanStyle
import com.mohamedrejeb.richeditor.model.RichSpanStyleDescriptor
import com.mohamedrejeb.richeditor.model.RichTextConfig
import com.mohamedrejeb.richeditor.model.RichTextFormat
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.richSpanStyleDescriptor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@OptIn(ExperimentalRichTextApi::class)
internal class TestFontStyle(
    val slug: String,
) : RichSpanStyle {
    override fun getSpanStyle(config: RichTextConfig): SpanStyle = SpanStyle()

    override fun equals(other: Any?): Boolean = other is TestFontStyle && slug == other.slug
    override fun hashCode(): Int = slug.hashCode()
}

@OptIn(ExperimentalRichTextApi::class)
class RichTextJsonCustomStylesTest {

    private fun fontDescriptor(
        formats: Set<RichTextFormat> = setOf(RichTextFormat.Json, RichTextFormat.Html),
    ): RichSpanStyleDescriptor =
        richSpanStyleDescriptor<TestFontStyle>(
            kind = "app:font",
            formats = formats,
            encode = { style -> mapOf("slug" to style.slug) },
            decode = { attrs -> attrs["slug"]?.let { TestFontStyle(slug = it) } },
        )

    private fun stateWithFont(): RichTextState {
        val state = RichTextState()
        state.spanStyleRegistry.register(fontDescriptor())
        state.setText("Hello world")
        state.addRichSpan(TestFontStyle(slug = "amiri"), TextRange(0, 5))
        return state
    }

    @Test
    fun `registered custom style round-trips through toJson and setJson`() {
        val state = stateWithFont()

        val json = state.toJson()
        assertTrue("app:font" in json)

        val loaded = RichTextState()
        loaded.spanStyleRegistry.register(fontDescriptor())
        loaded.setJson(json)

        assertEquals(state.toRichTextDocument(), loaded.toRichTextDocument())
        assertEquals(json, loaded.toJson())
    }

    @Test
    fun `custom style with json format opted out is skipped on encode`() {
        val state = RichTextState()
        state.spanStyleRegistry.register(fontDescriptor(formats = setOf(RichTextFormat.Html)))
        state.setText("Hello world")
        state.addRichSpan(TestFontStyle(slug = "amiri"), TextRange(0, 5))

        assertTrue("app:font" !in state.toJson())
    }

    @Test
    fun `custom kind json without registration degrades to no styling`() {
        val json =
            """{"v":1,"blocks":[{"type":"paragraph","text":"Hello","spans":[{"k":"app:font","r":[0,4],"attrs":{"slug":"amiri"}}]}]}"""
        val state = RichTextState()
        state.setJson(json)

        assertEquals("Hello", state.toText())
        assertTrue(
            state.toRichTextDocument().blocks.single().spans
                .none { it is RichTextSpanMark.Custom },
        )
    }

    @Test
    fun `custom kind json with registration restores the style`() {
        val json =
            """{"v":1,"blocks":[{"type":"paragraph","text":"Hello","spans":[{"k":"app:font","r":[0,4],"attrs":{"slug":"amiri"}}]}]}"""
        val state = RichTextState()
        state.spanStyleRegistry.register(fontDescriptor())
        state.setJson(json)

        val custom = state.toRichTextDocument().blocks.single().spans
            .filterIsInstance<RichTextSpanMark.Custom>()
            .single()
        assertEquals(0..4, custom.range)
        assertEquals(TestFontStyle(slug = "amiri"), custom.style)
    }

    @Test
    fun `decoder returning null drops the mark without failing the load`() {
        val json =
            """{"v":1,"blocks":[{"type":"paragraph","text":"Hello","spans":[{"k":"app:font","r":[0,4],"attrs":{}}]}]}"""
        val state = RichTextState()
        state.spanStyleRegistry.register(fontDescriptor())
        state.setJson(json)

        assertEquals("Hello", state.toText())
    }

    @Test
    fun `decoder exceptions surface as MalformedRichTextJsonException`() {
        val json =
            """{"v":1,"blocks":[{"type":"paragraph","text":"Hello","spans":[{"k":"app:boom","r":[0,4],"attrs":{}}]}]}"""
        val state = RichTextState()
        state.spanStyleRegistry.register(
            richSpanStyleDescriptor<TestFontStyle>(
                kind = "app:boom",
                encode = { emptyMap() },
                decode = { error("decoder bug") },
            ),
        )

        assertFailsWith<MalformedRichTextJsonException> { state.setJson(json) }
    }

    @Test
    fun `non-primitive attr values degrade the mark instead of failing the load`() {
        val json =
            """{"v":1,"blocks":[{"type":"paragraph","text":"Hello","spans":[{"k":"app:font","r":[0,4],"attrs":{"slug":{"nested":true}}}]}]}"""
        val state = RichTextState()
        state.spanStyleRegistry.register(fontDescriptor())
        state.setJson(json)

        assertEquals("Hello", state.toText())
        assertTrue(
            state.toRichTextDocument().blocks.single().spans
                .none { it is RichTextSpanMark.Custom },
        )
    }
}
