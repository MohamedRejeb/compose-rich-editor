package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Regression for #423: images wider than the container should scale down
 * proportionally (aspect ratio preserved) instead of overflowing.
 *
 * Exercises the pure-math helper on [RichSpanStyle.Image.Companion]; the
 * visual integration uses the same function from the inline-content
 * `LaunchedEffect`, which reads the container width from
 * `LocalRichTextMaxImageWidth` provided by `BasicRichText`.
 */
@OptIn(ExperimentalRichTextApi::class)
class ImageClampToMaxWidthTest {

    @Test
    fun returnsInputUnchangedWhenMaxWidthUnspecified() {
        val (w, h) = RichSpanStyle.Image.clampToMaxWidth(
            width = 1500.sp,
            height = 150.sp,
            maxWidth = TextUnit.Unspecified,
        )
        assertEquals(1500.sp, w)
        assertEquals(150.sp, h)
    }

    @Test
    fun returnsInputUnchangedWhenMaxWidthZero() {
        val (w, h) = RichSpanStyle.Image.clampToMaxWidth(
            width = 1500.sp,
            height = 150.sp,
            maxWidth = 0.sp,
        )
        assertEquals(1500.sp, w)
        assertEquals(150.sp, h)
    }

    @Test
    fun returnsInputUnchangedWhenWidthAlreadyWithinMax() {
        val (w, h) = RichSpanStyle.Image.clampToMaxWidth(
            width = 300.sp,
            height = 200.sp,
            maxWidth = 400.sp,
        )
        assertEquals(300.sp, w)
        assertEquals(200.sp, h)
    }

    @Test
    fun scalesWidthAndHeightProportionallyWhenTooWide() {
        // Reporter's exact case: 1500x150 image in a 300.sp wide container.
        val (w, h) = RichSpanStyle.Image.clampToMaxWidth(
            width = 1500.sp,
            height = 150.sp,
            maxWidth = 300.sp,
        )
        assertEquals(300.sp, w)
        // 150 * (300 / 1500) = 30
        assertEquals(30.sp, h)
    }

    @Test
    fun squareImageScalesEqually() {
        val (w, h) = RichSpanStyle.Image.clampToMaxWidth(
            width = 1000.sp,
            height = 1000.sp,
            maxWidth = 250.sp,
        )
        assertEquals(250.sp, w)
        assertEquals(250.sp, h)
    }

    @Test
    fun tallImageScalesToFitWidth() {
        val (w, h) = RichSpanStyle.Image.clampToMaxWidth(
            width = 800.sp,
            height = 1200.sp,
            maxWidth = 400.sp,
        )
        assertEquals(400.sp, w)
        // 1200 * 0.5 = 600
        assertEquals(600.sp, h)
    }

    @Test
    fun exactMatchToMaxWidthIsUnchanged() {
        val (w, h) = RichSpanStyle.Image.clampToMaxWidth(
            width = 400.sp,
            height = 300.sp,
            maxWidth = 400.sp,
        )
        assertEquals(400.sp, w)
        assertEquals(300.sp, h)
    }

    @Test
    fun unspecifiedHeightStaysUnspecified() {
        val (w, h) = RichSpanStyle.Image.clampToMaxWidth(
            width = 1500.sp,
            height = TextUnit.Unspecified,
            maxWidth = 300.sp,
        )
        assertEquals(300.sp, w)
        assertEquals(TextUnit.Unspecified, h)
    }
}
