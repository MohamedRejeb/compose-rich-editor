import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.mohamedrejeb.richeditor.sample.common.App


fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}
