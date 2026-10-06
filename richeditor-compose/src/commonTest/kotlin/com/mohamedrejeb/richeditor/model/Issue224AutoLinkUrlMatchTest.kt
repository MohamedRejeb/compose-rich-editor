package com.mohamedrejeb.richeditor.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Issue 224: which typed words count as a URL for auto linking, and which part of the word
 * becomes the link. Only `http://`, `https://` and `www.` prefixes are recognized, and
 * punctuation that closes the sentence around the URL stays outside the link.
 */
class Issue224AutoLinkUrlMatchTest {

    private fun linked(word: String): String? =
        findAutoLinkUrl(word)?.let { word.substring(it.start, it.end) }

    @Test
    fun `http and https urls are recognized`() {
        assertEquals("https://example.com", linked("https://example.com"))
        assertEquals("http://example.com/a/b?c=d#e", linked("http://example.com/a/b?c=d#e"))
        assertEquals("HTTPS://Example.com", linked("HTTPS://Example.com"))
        assertEquals("http://localhost:8080", linked("http://localhost:8080"))
        assertEquals("https://example.com", findAutoLinkUrl("https://example.com")?.url)
    }

    @Test
    fun `a www address is recognized and gets an https scheme`() {
        val match = findAutoLinkUrl("www.example.com/path")

        assertEquals("https://www.example.com/path", match?.url)
        assertEquals("www.example.com/path", linked("www.example.com/path"))
        assertEquals("https://WWW.example.com", findAutoLinkUrl("WWW.example.com")?.url)
    }

    @Test
    fun `words that are not urls are left alone`() {
        val words = listOf(
            "", "hello", "example.com", "user@example.com", "file.txt", "e.g.", "http://", "https://.",
            "www.", "www.example", "www..com", "ftp://example.com", "mailto:user@example.com",
            "https:example.com", "xhttps://example.com", "http:///path", "https://-", "www.example.",
        )
        for (word in words) assertNull(findAutoLinkUrl(word), "word \"$word\"")
    }

    @Test
    fun `trailing punctuation stays outside the link`() {
        for (tail in listOf(".", ",", ";", ":", "!", "?", "...", "\"", "'", ")", "]", ">", ").", "?!")) {
            assertEquals("https://example.com/a", linked("https://example.com/a$tail"), "tail \"$tail\"")
        }
        assertEquals("www.example.com", linked("www.example.com."))
    }

    @Test
    fun `a closing parenthesis that the url opened stays in the link`() {
        assertEquals(
            "https://en.wikipedia.org/wiki/Rust_(programming_language)",
            linked("https://en.wikipedia.org/wiki/Rust_(programming_language)"),
        )
        assertEquals(
            "https://en.wikipedia.org/wiki/Rust_(programming_language)",
            linked("(https://en.wikipedia.org/wiki/Rust_(programming_language))."),
        )
    }

    @Test
    fun `opening punctuation in front of the url stays outside the link`() {
        val match = findAutoLinkUrl("(https://example.com)")

        assertEquals(1, match?.start)
        assertEquals("https://example.com", linked("(https://example.com)"))
        assertEquals("www.example.com", linked("\"www.example.com\""))
        assertEquals("https://example.com", linked("<https://example.com>"))
    }
}
