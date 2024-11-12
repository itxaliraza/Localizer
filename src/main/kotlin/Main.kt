import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.application
import di.SharedModule
import home_screen.HomeScreen
import org.koin.core.context.startKoin

val LightPrimary = Color(0xFF6200EE)
val LightPrimaryVariant = Color(0xFF3700B3)
val LightSecondary = Color(0xFF03DAC6)

val DarkPrimary = Color(0xFFBB86FC)
val DarkPrimaryVariant = Color(0xFF3700B3)
val DarkSecondary = Color(0xFF03DAC6)

val LightColors = lightColors(
    primary = LightPrimary,
    onPrimary = Color.White,
    secondary = LightSecondary,
    onSecondary = Color.Black
)
@Composable
@Preview
fun App() {
    var text by remember { mutableStateOf("Hello, World!") }
    startKoin {
        modules(SharedModule)
    }
    MaterialTheme(colors = LightColors) {
        HomeScreen()
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}



