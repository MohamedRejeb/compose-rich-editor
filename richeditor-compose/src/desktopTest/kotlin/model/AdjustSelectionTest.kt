package model

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.BasicRichTextEditor
import kotlinx.coroutines.delay
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class AdjustSelectionTest {
    @get:Rule
    val rule = createComposeRule()

    // Todo: Cover mode cases and add android test
    @OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
    @Test
    fun adjustSelectionTest() = runDesktopComposeUiTest {
        // Declares a mock UI to demonstrate API calls
        //
        // Replace with your own declarations to test the code in your project
        scene.setContent {
            val state = rememberRichTextState()

            var clickPosition by remember {
                mutableStateOf(Offset.Zero)
            }
            val clickPositionState by rememberUpdatedState(clickPosition)

            LaunchedEffect(Unit) {
                state.setHtml(
                    """
                        <p>fsdfdsf</p>
                        <br>
                        <p>fsdfsdfdsf aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa</p>
                        <br>
                        <p>fsdfsdfdsf</p>
                        <br>
                    """.trimIndent()
                )
            }

            Box(
                modifier = Modifier
                    .width(200.dp)
            ) {
                BasicRichTextEditor(
                    state = state,
                    onTextLayout = { textLayoutResult ->
                        val top = textLayoutResult.getLineTop(6)
                        val bottom = textLayoutResult.getLineBottom(6)
                        val height = bottom - top

                        clickPosition = Offset(
                            x = 100f,
                            y = top + height / 2f
                        )
                    },
                    modifier = Modifier
                        .testTag("editor")
                        .fillMaxWidth()
                )
            }

            LaunchedEffect(Unit) {
                delay(1000)

                scene.sendPointerEvent(
                    eventType = PointerEventType.Press,
                    position = clickPositionState,
                )
                scene.sendPointerEvent(
                    eventType = PointerEventType.Release,
                    position = clickPositionState,
                )

                delay(1000)

                scene.sendPointerEvent(
                    eventType = PointerEventType.Press,
                    position = clickPositionState,
                )
                scene.sendPointerEvent(
                    eventType = PointerEventType.Release,
                    position = clickPositionState,
                )

                delay(1000)

                assertEquals(TextRange(73), state.selection)
            }
        }
        waitForIdle()
    }

}