import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.CanvasBasedWindow
import androidx.compose.ui.window.ComposeViewport
import com.mohamedrejeb.richeditor.sample.common.App

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport {
        Box(Modifier.fillMaxSize()) {
            App()
        }
    }
}