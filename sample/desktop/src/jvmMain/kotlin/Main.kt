import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.mohamedrejeb.richeditor.sample.common.App
import com.mohamedrejeb.richeditor.sample.common.HtmlEditor


fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        HtmlEditor()
    }
}
