import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.mohamedrejeb.richeditor.sample.common.App


fun main() = application {
    Window(
        title = "Compose Rich Editor",
        onCloseRequest = ::exitApplication
    ) {
        App()
    }
}
