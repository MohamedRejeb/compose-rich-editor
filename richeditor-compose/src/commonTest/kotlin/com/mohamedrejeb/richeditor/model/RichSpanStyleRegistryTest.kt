package com.mohamedrejeb.richeditor.model

import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.document.FontRunStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalRichTextApi::class)
class RichSpanStyleRegistryTest {

    private fun fontDescriptor(kind: String = "app:font"): RichSpanStyleDescriptor =
        richSpanStyleDescriptor<FontRunStyle>(
            kind = kind,
            encode = { style -> mapOf("slug" to style.slug) },
            decode = { attrs -> attrs["slug"]?.let { FontRunStyle(slug = it) } },
        )

    @Test
    fun `register and find returns the descriptor by kind`() {
        val registry = RichSpanStyleRegistry()
        val descriptor = fontDescriptor()

        registry.register(descriptor)

        assertSame(descriptor, registry.find("app:font"))
        assertNull(registry.find("app:other"))
        assertEquals(listOf("app:font"), registry.kinds())
    }

    @Test
    fun `register rejects a blank kind`() {
        val registry = RichSpanStyleRegistry()
        assertFailsWith<IllegalArgumentException> {
            registry.register(fontDescriptor(kind = " "))
        }
    }

    @Test
    fun `register rejects built-in kind names`() {
        val registry = RichSpanStyleRegistry()
        assertFailsWith<IllegalArgumentException> {
            registry.register(fontDescriptor(kind = "bold"))
        }
        assertFailsWith<IllegalArgumentException> {
            registry.register(fontDescriptor(kind = "link"))
        }
    }

    @Test
    fun `register rejects a duplicate kind unless replace is true`() {
        val registry = RichSpanStyleRegistry()
        registry.register(fontDescriptor())

        assertFailsWith<IllegalStateException> {
            registry.register(fontDescriptor())
        }

        val replacement = fontDescriptor()
        registry.register(replacement, replace = true)
        assertSame(replacement, registry.find("app:font"))
    }

    @Test
    fun `descriptor matches and encodes only its own style class`() {
        val descriptor = fontDescriptor()
        val font = FontRunStyle(slug = "amiri")

        assertTrue(descriptor.matches(font))
        assertTrue(!descriptor.matches(RichSpanStyle.Link(url = "https://example.com")))
        assertEquals(mapOf("slug" to "amiri"), descriptor.encode(font))
        assertEquals(FontRunStyle(slug = "amiri"), descriptor.decode(mapOf("slug" to "amiri")))
        assertNull(descriptor.decode(emptyMap()))
    }

    @Test
    fun `descriptor defaults to json and html formats`() {
        assertEquals(setOf(RichTextFormat.Json, RichTextFormat.Html), fontDescriptor().formats)
    }

    @Test
    fun `findForStyle returns the descriptor whose class matches`() {
        val registry = RichSpanStyleRegistry()
        val descriptor = fontDescriptor()
        registry.register(descriptor)

        assertSame(descriptor, registry.findForStyle(FontRunStyle(slug = "cairo")))
        assertNull(registry.findForStyle(RichSpanStyle.Code()))
    }

    @Test
    fun `built-in kinds are listed read-only`() {
        assertTrue("bold" in RichSpanStyleRegistry.BuiltInKinds)
        assertTrue("token" in RichSpanStyleRegistry.BuiltInKinds)
    }

    @Test
    fun `every state owns its own registry`() {
        val a = RichTextState()
        val b = RichTextState()

        assertNotSame(a.spanStyleRegistry, b.spanStyleRegistry)
        a.spanStyleRegistry.register(fontDescriptor())
        assertNull(b.spanStyleRegistry.find("app:font"))
    }
}
