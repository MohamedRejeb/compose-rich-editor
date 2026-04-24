package com.mohamedrejeb.richeditor.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichSpanStyle
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.trigger.Trigger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@OptIn(ExperimentalTestApi::class, ExperimentalRichTextApi::class)
class TokenClickGestureTest {

    @Test
    fun tap_on_token_fires_onTokenClick_with_token() = runDesktopComposeUiTest(width = 400, height = 200) {
        var clicked: RichSpanStyle.Token? = null
        val state = RichTextState()
        state.registerTrigger(Trigger(id = "mention", char = '@'))
        state.setHtml("hello <span data-trigger=\"mention\" data-id=\"u1\">@bob</span> !")

        val layout = mutableStateOf<TextLayoutResult?>(null)

        setContent {
            BasicRichText(
                state = state,
                modifier = Modifier.fillMaxSize(),
                onTextLayout = { layout.value = it },
                onTokenClick = { token, _ -> clicked = token },
            )
        }

        waitUntil { layout.value != null }
        val tl = assertNotNull(layout.value)
        val tokenStartBox = tl.getBoundingBox(6)
        val tokenEndBox = tl.getBoundingBox(9)
        val center = Offset(
            x = (tokenStartBox.left + tokenEndBox.right) / 2f,
            y = (tokenStartBox.top + tokenStartBox.bottom) / 2f,
        )

        onRoot().performMouseInput { click(position = center) }
        waitForIdle()

        assertEquals("mention", clicked?.triggerId)
        assertEquals("u1", clicked?.id)
        assertEquals("@bob", clicked?.label)
    }

    @Test
    fun tap_outside_token_does_not_fire_handler() = runDesktopComposeUiTest(width = 400, height = 200) {
        var fired = false
        val state = RichTextState()
        state.registerTrigger(Trigger(id = "mention", char = '@'))
        state.setHtml("hello <span data-trigger=\"mention\" data-id=\"u1\">@bob</span> !")

        setContent {
            BasicRichText(
                state = state,
                modifier = Modifier.fillMaxSize(),
                onTokenClick = { _, _ -> fired = true },
            )
        }

        onRoot().performMouseInput { click(position = Offset(5f, 10f)) }
        waitForIdle()

        assertEquals(false, fired)
    }
}
