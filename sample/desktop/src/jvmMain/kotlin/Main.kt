import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.mohamedrejeb.richeditor.sample.common.App
import com.mohamedrejeb.richeditor.sample.common.htmleditor.HtmlEditorContent


fun main() = application {
    Window(
        title = "Compose Rich Editor",
        onCloseRequest = ::exitApplication
    ) {
        App()
    }
}
