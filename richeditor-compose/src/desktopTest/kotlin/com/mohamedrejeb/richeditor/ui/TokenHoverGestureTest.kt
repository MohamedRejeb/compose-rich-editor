package com.mohamedrejeb.richeditor.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class, ExperimentalRichTextApi::class)
class TokenHoverGestureTest {

    @Test
    fun hover_enter_and_exit_fires_onTokenHover_with_token_then_null() = runDesktopComposeUiTest(width = 400, height = 200) {
        val calls = mutableListOf<RichSpanStyle.Token?>()
        val state = RichTextState()
        state.registerTrigger(Trigger(id = "mention", char = '@'))
        state.setHtml("hi <span data-trigger=\"mention\" data-id=\"u1\">@bob</span> !")

        val layout = mutableStateOf<TextLayoutResult?>(null)
        setContent {
            BasicRichText(
                state = state,
                modifier = Modifier.fillMaxSize(),
                onTextLayout = { layout.value = it },
                onTokenHover = { token, _ -> calls += token },
            )
        }

        waitUntil { layout.value != null }
        val tl = assertNotNull(layout.value)
        val tokenBox = tl.getBoundingBox(3) // '@' of "@bob"
        val tokenCenter = Offset(
            x = (tokenBox.left + tokenBox.right) / 2f,
            y = (tokenBox.top + tokenBox.bottom) / 2f,
        )

        onRoot().performMouseInput {
            moveTo(Offset(1f, 1f))
            moveTo(tokenCenter)
            moveTo(Offset(1f, 1f))
        }
        waitForIdle()

        assertTrue(calls.isNotEmpty(), "expected at least one hover callback")
        val enter = calls.firstOrNull { it != null }
        assertNotNull(enter, "expected an enter callback with a non-null token")
        assertEquals("u1", enter.id)
        assertNull(calls.last(), "expected last hover callback to be null (pointer left the token)")
    }

    @Test
    fun hover_does_not_fire_repeatedly_while_on_the_same_token() = runDesktopComposeUiTest(width = 400, height = 200) {
        var nonNullCount = 0
        val state = RichTextState()
        state.registerTrigger(Trigger(id = "mention", char = '@'))
        state.setHtml("hi <span data-trigger=\"mention\" data-id=\"u1\">@bob</span> !")

        val layout = mutableStateOf<TextLayoutResult?>(null)
        setContent {
            BasicRichText(
                state = state,
                modifier = Modifier.fillMaxSize(),
                onTextLayout = { layout.value = it },
                onTokenHover = { token, _ -> if (token != null) nonNullCount++ },
            )
        }

        waitUntil { layout.value != null }
        val tl = assertNotNull(layout.value)
        val box = tl.getBoundingBox(3)
        val center = Offset((box.left + box.right) / 2f, (box.top + box.bottom) / 2f)

        onRoot().performMouseInput {
            moveTo(center)
            moveTo(Offset(center.x + 1f, center.y))
            moveTo(Offset(center.x + 2f, center.y))
        }
        waitForIdle()

        assertEquals(1, nonNullCount)
    }
}
