package com.mohamedrejeb.richeditor.model

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.ui.BasicRichText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Repro for user-reported bug:
 *
 *   setHtml("<img src=X>") -> image appears.
 *   setHtml("<img src=X> some text") -> image disappears.
 *
 * The expected invariant: after any setHtml containing an `<img>`, the
 * `inlineContentMap` should converge to a single entry referenced by the
 * annotatedString, with the Placeholder dimensions matching the painter's
 * intrinsic size (possibly clamped).
 */
@OptIn(ExperimentalRichTextApi::class, ExperimentalTestApi::class)
class ImageAcrossSetHtmlTest {

    /** A painter that reports a fixed intrinsic size and draws nothing. */
    private class FakeIntrinsicPainter(
        private val w: Float,
        private val h: Float,
    ) : Painter() {
        override val intrinsicSize: Size = Size(w, h)
        override fun DrawScope.onDraw() { /* no-op */ }
    }

    private class FakeImageLoader(
        private val w: Float,
        private val h: Float,
    ) : ImageLoader {
        @Composable
        override fun load(model: Any): ImageData? {
            return ImageData(painter = FakeIntrinsicPainter(w, h))
        }
    }

    /**
     * Simulates an async image loader (e.g. Coil3) whose painter is cached
     * across `load()` calls, but whose `intrinsicSize` starts as
     * [Size.Unspecified] and flips to a real size after the first
     * recomposition.
     */
    @Stable
    private class FakeAsyncPainter : Painter() {
        var resolved by mutableStateOf(false)
        override val intrinsicSize: Size
            get() = if (resolved) Size(200f, 100f) else Size.Unspecified
        override fun DrawScope.onDraw() { /* no-op */ }
    }

    private class FakeAsyncImageLoader(
        private val painter: FakeAsyncPainter,
    ) : ImageLoader {
        @Composable
        override fun load(model: Any): ImageData? = ImageData(painter = painter)
    }

    /**
     * Models Coil3 more faithfully: every composition scope creates a fresh
     * painter (via `remember { }`). The painter starts with
     * [Size.Unspecified] and flips to specified in a launched effect
     * inside `load()`. This mimics Coil3's `rememberAsyncImagePainter`
     * where each children-scope gets a new painter that starts
     * unresolved and resolves asynchronously.
     */
    private class FreshPerScopeAsyncImageLoader : ImageLoader {
        @Composable
        override fun load(model: Any): ImageData? {
            val painter = remember { FakeAsyncPainter() }
            LaunchedEffect(painter) { painter.resolved = true }
            return ImageData(painter = painter)
        }
    }

    /**
     * Configurable intrinsic-size version of [FakeAsyncPainter] — so we can
     * simulate a large (600x400) image rather than the default 200x100.
     */
    private class FakeAsyncPainterSized(
        private val w: Float,
        private val h: Float,
    ) : Painter() {
        var resolved by mutableStateOf(false)
        override val intrinsicSize: Size
            get() = if (resolved) Size(w, h) else Size.Unspecified
        override fun DrawScope.onDraw() { /* no-op */ }
    }

    private class SizedFreshLoader(
        private val w: Float,
        private val h: Float,
    ) : ImageLoader {
        @Composable
        override fun load(model: Any): ImageData? {
            val painter = remember(model) { FakeAsyncPainterSized(w, h) }
            LaunchedEffect(painter) { painter.resolved = true }
            return ImageData(painter = painter)
        }
    }

    @Test
    fun imageStillPresentAfterSecondSetHtmlWithAddedText() = runDesktopComposeUiTest(
        width = 800,
        height = 600,
    ) {
        var state: RichTextState by mutableStateOf(RichTextState()) // replaced in-effect below
        setContent {
            state = remember { RichTextState() }
            CompositionLocalProvider(
                LocalImageLoader provides FakeImageLoader(w = 200f, h = 100f),
            ) {
                LaunchedEffect(Unit) {
                    state.setHtml("""<p><img src="http://x/a.png"/></p>""")
                }
                BasicRichText(
                    state = state,
                    modifier = Modifier.width(400.dp),
                )
            }
        }
        waitForIdle()

        // After first setHtml + LaunchedEffect(s): map should contain one entry
        // matching the annotatedString's inline marker.
        assertEquals(
            1,
            state.inlineContentMap.size,
            "after first setHtml, inlineContentMap should have the image entry",
        )
        val markerIdsAfterFirst = state.annotatedString
            .getStringAnnotations(
                tag = "androidx.compose.foundation.text.inlineContent",
                start = 0,
                end = state.annotatedString.length,
            )
            .map { it.item }
            .toSet()
        assertEquals(markerIdsAfterFirst, state.inlineContentMap.keys)

        // Change the html - same img + trailing text.
        runOnUiThread {
            state.setHtml("""<p><img src="http://x/a.png"/> some text</p>""")
        }
        waitForIdle()

        val markerIdsAfterSecond = state.annotatedString
            .getStringAnnotations(
                tag = "androidx.compose.foundation.text.inlineContent",
                start = 0,
                end = state.annotatedString.length,
            )
            .map { it.item }
            .toSet()

        assertEquals(
            1,
            markerIdsAfterSecond.size,
            "annotatedString should still reference exactly one inline image marker",
        )
        assertTrue(
            markerIdsAfterSecond.all { it in state.inlineContentMap.keys },
            "every inline marker must exist in the map. markers=$markerIdsAfterSecond, keys=${state.inlineContentMap.keys}",
        )
        assertEquals(
            markerIdsAfterSecond,
            state.inlineContentMap.keys,
            "map should contain exactly the keys referenced by the annotatedString (no leftover, no missing)",
        )

        // The Image span's width must have been resolved from the painter's
        // intrinsic size after the LaunchedEffect ran. If it's still 0, the
        // Placeholder is zero-sized and the image is not visible - which is
        // exactly the reported "image disappears" symptom.
        val imageSpan = findImageSpan(state)
        assertNotEquals(
            0f,
            imageSpan.width.value,
            "Image.width must be resolved to the painter's intrinsic size after setHtml; still 0 means the LaunchedEffect didn't run or didn't update",
        )
    }

    private fun findImageSpan(state: RichTextState): RichSpanStyle.Image {
        for (paragraph in state.richParagraphList) {
            for (child in paragraph.children) {
                val result = collectImage(child)
                if (result != null) return result
            }
        }
        error("no Image span found in state")
    }

    private fun collectImage(span: RichSpan): RichSpanStyle.Image? {
        if (span.richSpanStyle is RichSpanStyle.Image) return span.richSpanStyle as RichSpanStyle.Image
        for (c in span.children) {
            val r = collectImage(c)
            if (r != null) return r
        }
        return null
    }

    /**
     * Closer to the user-reported scenario: each composition scope gets a
     * fresh painter (like Coil3) that starts Unspecified and resolves
     * asynchronously. The image dimensions must still end up non-zero after
     * the second setHtml.
     */
    @Test
    fun imageResolvesAcrossSecondSetHtmlWithFreshAsyncPainters() = runDesktopComposeUiTest(
        width = 800,
        height = 600,
    ) {
        var state: RichTextState by mutableStateOf(RichTextState())
        setContent {
            state = remember { RichTextState() }
            CompositionLocalProvider(
                LocalImageLoader provides FreshPerScopeAsyncImageLoader(),
            ) {
                LaunchedEffect(Unit) {
                    state.setHtml("""<p><img src="http://x/a.png"/></p>""")
                }
                BasicRichText(
                    state = state,
                    modifier = Modifier.width(400.dp),
                )
            }
        }
        waitForIdle()

        val imageSpanAfterFirst = findImageSpan(state)
        assertNotEquals(
            0f,
            imageSpanAfterFirst.width.value,
            "After first setHtml + async painter resolution, Image.width must be set. " +
                "Got ${imageSpanAfterFirst.width.value}",
        )

        runOnUiThread {
            state.setHtml("""<p><img src="http://x/a.png"/> some text</p>""")
        }
        waitForIdle()

        val imageSpanAfterSecond = findImageSpan(state)
        assertNotEquals(
            0f,
            imageSpanAfterSecond.width.value,
            "After second setHtml, Image.width must resolve again. " +
                "Got ${imageSpanAfterSecond.width.value} - image is currently invisible.",
        )
    }

    /**
     * Directly reproduces the user-reported bug: the html-source editor
     * (`HtmlToRichText` sample) calls `setHtml` on every keystroke, so each
     * edit creates a fresh Image span with `width=0`. If nothing else
     * helps, the Placeholder flashes through 0x0 between keystrokes and
     * the image visibly disappears.
     *
     * The dimensions cache populated by the first render keeps subsequent
     * fresh Images for the same src starting at the previous resolved
     * size, so the Placeholder never flashes.
     */
    @Test
    fun keystrokeLikeRepeatedSetHtmlKeepsImageVisible() = runDesktopComposeUiTest(
        width = 800,
        height = 600,
    ) {
        var html by mutableStateOf("""<img src="http://x/a.png">""")
        var state: RichTextState by mutableStateOf(RichTextState())

        setContent {
            state = remember { RichTextState() }
            CompositionLocalProvider(
                LocalImageLoader provides FreshPerScopeAsyncImageLoader(),
            ) {
                // Mimic HtmlToRichText's keystroke-driven behavior.
                LaunchedEffect(html) {
                    state.setHtml(html)
                }
                BasicRichText(
                    state = state,
                    modifier = Modifier.width(400.dp),
                )
            }
        }
        waitForIdle()

        // After initial render + async painter resolution, the image dims
        // should be populated in the cache.
        val widthAfterInitial = findImageSpan(state).width.value
        assertNotEquals(
            0f,
            widthAfterInitial,
            "first render must resolve dims. Got $widthAfterInitial",
        )

        // Now simulate the user typing. Each assignment triggers another
        // setHtml through LaunchedEffect.
        val keystrokes = listOf(
            """<img src="http://x/a.png"> """,
            """<img src="http://x/a.png"> s""",
            """<img src="http://x/a.png"> so""",
            """<img src="http://x/a.png"> som""",
            """<img src="http://x/a.png"> some""",
            """<img src="http://x/a.png"> some """,
            """<img src="http://x/a.png"> some t""",
            """<img src="http://x/a.png"> some te""",
            """<img src="http://x/a.png"> some tex""",
            """<img src="http://x/a.png"> some text""",
        )

        for (edit in keystrokes) {
            runOnUiThread { html = edit }
            waitForIdle()

            val imageSpan = findImageSpan(state)
            assertNotEquals(
                0f,
                imageSpan.width.value,
                "After keystroke-style edit to '$edit', Image.width dropped to 0. " +
                    "The Placeholder flashed through a 0x0 state - image invisible.",
            )
        }
    }

    /**
     * Repro for: in the Images sample, adding a new `<img>` via setHtml
     * makes the previously-rendered images jump back to their HTML-attr
     * size (wider than the container). Resizing the window clamps them
     * again.
     *
     * Scenario:
     *  1. setHtml(`<img width="600">`). Container is 400sp wide. LaunchedEffect
     *     clamps Image.width to 400 on the initial render.
     *  2. setHtml(`<img width="600"><img width="1600">`). Both images are
     *     FRESH instances with width=600 / 1600 from the HTML. The first
     *     image's width should still end up at 400 after the second render
     *     settles; otherwise it visibly grows from 400 back to 600.
     */
    @Test
    fun addingSecondImageDoesNotReEnlargeFirstImage() = runDesktopComposeUiTest(
        width = 800,
        height = 600,
    ) {
        var html by mutableStateOf(
            """<p><img src="http://x/a.png" width="600" height="400"/></p>"""
        )
        var state: RichTextState by mutableStateOf(RichTextState())

        setContent {
            state = remember { RichTextState() }
            CompositionLocalProvider(
                // Intrinsic 600x400 — matches the HTML-attr width the sample
                // uses for the Landscape preset. The container is 400sp wide
                // so a correctly-clamped Image.width must end up <= 400.
                LocalImageLoader provides SizedFreshLoader(w = 600f, h = 400f),
            ) {
                LaunchedEffect(html) {
                    state.setHtml(html)
                }
                BasicRichText(
                    state = state,
                    modifier = Modifier.width(400.dp),
                )
            }
        }
        waitForIdle()

        val firstImage = findImageSpan(state)
        val clampedWidth = firstImage.width.value
        assertTrue(
            clampedWidth <= 401f,
            "first render must clamp image to the 400sp container. " +
                "Got ${clampedWidth}sp - Image.width should be <= 400.",
        )

        // Now add a second image - the exact action the user takes in the
        // Images sample when clicking a preset.
        runOnUiThread {
            html = """<p><img src="http://x/a.png" width="600" height="400"/></p>""" +
                """<p><img src="http://y/b.png" width="1600" height="900"/></p>"""
        }
        waitForIdle()

        val images = collectAllImages(state)
        assertEquals(2, images.size, "should have two image spans now")
        val firstImageAfter = images[0]
        assertTrue(
            firstImageAfter.width.value <= 401f,
            "after adding a second image, the first image must stay clamped. " +
                "Got ${firstImageAfter.width.value}sp - it grew back to its HTML-attr size.",
        )
        val secondImageAfter = images[1]
        assertTrue(
            secondImageAfter.width.value <= 401f,
            "the newly-added image must also be clamped to the container. " +
                "Got ${secondImageAfter.width.value}sp.",
        )
    }

    private fun collectAllImages(state: RichTextState): List<RichSpanStyle.Image> {
        val result = mutableListOf<RichSpanStyle.Image>()
        for (paragraph in state.richParagraphList) {
            for (child in paragraph.children) {
                collectImagesRecursive(child, result)
            }
        }
        return result
    }

    private fun collectImagesRecursive(span: RichSpan, into: MutableList<RichSpanStyle.Image>) {
        val style = span.richSpanStyle
        if (style is RichSpanStyle.Image) into.add(style)
        for (c in span.children) collectImagesRecursive(c, into)
    }

    /**
     * Sanity check: a painter whose `intrinsicSize` is initially
     * [Size.Unspecified] and becomes specified later must still end up with
     * a resolved Image.width.
     */
    @Test
    fun imageResolvesWhenIntrinsicSizeBecomesAvailableLater() = runDesktopComposeUiTest(
        width = 800,
        height = 600,
    ) {
        val painter = FakeAsyncPainter() // starts Unspecified
        var state: RichTextState by mutableStateOf(RichTextState())

        setContent {
            state = remember { RichTextState() }
            CompositionLocalProvider(
                LocalImageLoader provides FakeAsyncImageLoader(painter),
            ) {
                LaunchedEffect(Unit) {
                    state.setHtml("""<p><img src="http://x/a.png"/> some text</p>""")
                }
                BasicRichText(
                    state = state,
                    modifier = Modifier.width(400.dp),
                )
            }
        }
        waitForIdle()

        // Now flip the painter to resolved; composition should observe it
        // and the Image.width should settle to the intrinsic size.
        runOnUiThread { painter.resolved = true }
        waitForIdle()

        val imageSpan = findImageSpan(state)
        assertNotEquals(
            0f,
            imageSpan.width.value,
            "Image.width must become non-zero once the painter's intrinsicSize resolves. " +
                "Currently ${imageSpan.width.value}sp - the LaunchedEffect exited early on " +
                "first run (intrinsicSize was Unspecified) and never re-ran when it resolved.",
        )
    }
}
