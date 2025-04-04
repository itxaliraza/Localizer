import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.example.localizer.generated.resources.Res
import com.example.localizer.generated.resources.app_icon
import com.example.localizer.generated.resources.ic_close
import com.example.localizer.generated.resources.ic_full_screen
import com.example.localizer.generated.resources.ic_minimize
import com.example.localizer.generated.resources.ic_minus
import common_components.HorizontalSpacer
import common_components.ImageButtons
import di.SharedModule
import home_screen.HomeScreenNew
import org.jetbrains.compose.resources.painterResource
import org.koin.core.context.startKoin
import theme.LightColors
import theme.PrimaryColor
import theme.ScreenColor
import java.awt.Dimension
import java.awt.Frame
import java.awt.Rectangle
import java.awt.Toolkit
import java.awt.Window
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter


fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Fast Localizer",
        undecorated = true,
        icon = painterResource(Res.drawable.app_icon),
    ) {
        window.minimumSize = Dimension(800, 600)
        App(window) {
            exitApplication()
        }
    }
}

@Composable
@Preview
fun App(window: Window, exitApp: () -> Unit) {
    startKoin {
        modules(SharedModule)
    }
    MaterialTheme(colors = LightColors) {
        Column(modifier = Modifier.fillMaxSize().background(PrimaryColor)) {
            CustomTitleBar(window, exitApp)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ScreenColor)
            ) {
                HomeScreenNew()
            }
        }

    }
}

@Composable
fun CustomTitleBar(window: Window, exitApp: () -> Unit) {
    var isMaximized by remember { mutableStateOf(false) }
    setupWindowDragging(window)
    val frame = window as? Frame

    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(ScreenColor)
    ) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
            Row(
                modifier = Modifier.fillMaxSize().weight(1f).padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Fast Localizer",
                    color = Color.White,
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1f)
                )
                ImageButtons(
                    icon = Icons.Default.Info,
                    size = 30,
                    onClick = {

                    }
                )
                HorizontalSpacer(5)
                // Minimize Button
                ImageButtons(
                    icon = Res.drawable.ic_minus,
                    onClick = {
                        frame?.extendedState = Frame.ICONIFIED
                    }
                )
                HorizontalSpacer(5)
                ImageButtons(
                    icon = if (isMaximized) {
                        Res.drawable.ic_full_screen
                    } else {
                        Res.drawable.ic_minimize
                    },
                    onClick = {
                        isMaximized = !isMaximized
                        if (isMaximized) {
//                             frame?.extendedState=Frame.MAXIMIZED_BOTH
                            maximizeWindow(window)
                        } else {
                            restoreWindow(window)
//                            frame?.extendedState= Frame.NORMAL
                        }
                    }
                )
                HorizontalSpacer(5)
                ImageButtons(
                    icon = Res.drawable.ic_close, color = Color.Red,
                    onClick = exitApp
                )
            }
            HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.5f))
        }

    }
}


fun setupWindowDragging(window: Window) {
    var offsetX = 0
    var offsetY = 0
    var isDragging = false

    window.addMouseListener(object : MouseAdapter() {
        override fun mousePressed(e: MouseEvent) {
            if (e.y <= 40) {  // Only allow dragging if clicked within top 40px
                isDragging = true
                offsetX = e.x
                offsetY = e.y
            } else {
                isDragging = false
            }
        }

        override fun mouseReleased(e: MouseEvent) {
            isDragging = false
        }
    })

    window.addMouseMotionListener(object : MouseMotionAdapter() {
        override fun mouseDragged(e: MouseEvent) {
            if (isDragging) {
                window.setLocation(e.xOnScreen - offsetX, e.yOnScreen - offsetY)
            }
        }
    })
}

fun maximizeWindow(window: Window) {
    val screenSize = Toolkit.getDefaultToolkit().screenSize
    val insets = Toolkit.getDefaultToolkit().getScreenInsets(window.graphicsConfiguration)
    // Calculate available size (excluding taskbar)
    val availableWidth = screenSize.width - insets.left - insets.right
    val availableHeight = screenSize.height - insets.top - insets.bottom

    // Set the window bounds manually
    window.bounds = Rectangle(insets.left, insets.top, availableWidth, availableHeight)
}

fun restoreWindow(window: Window, defaultWidth: Int = 800, defaultHeight: Int = 600) {
    // Restore to default size and center it
    val screenSize = Toolkit.getDefaultToolkit().screenSize
    val x = (screenSize.width - defaultWidth) / 2
    val y = (screenSize.height - defaultHeight) / 2
    window.bounds = Rectangle(x, y, defaultWidth, defaultHeight)
}