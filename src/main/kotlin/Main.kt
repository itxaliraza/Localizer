import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
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
import common_components.VerticalSpacer
import di.SharedModule
import home_screen.HomeScreenNew
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.core.context.startKoin
import theme.PrimaryColor
import theme.LightColors
import theme.ScreenColor
import java.awt.Dimension
import java.awt.Frame
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
                        frame?.extendedState =
                            if (isMaximized) Frame.MAXIMIZED_BOTH else Frame.NORMAL
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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ImageButtons(
    icon: DrawableResource = Res.drawable.ic_close,
    color: Color = Color.Gray,
    onClick: () -> Unit
) {
    var isHovered by remember { mutableStateOf(false) }
    Image(
        painter = painterResource(icon),
        contentDescription = "", colorFilter = ColorFilter.tint(Color.White),
        modifier = Modifier
            .size(25.dp)
            .background(
                color = if (isHovered) {
                    color.copy(alpha = 0.5f)
                } else {
                    Color.Transparent
                },
                shape = RoundedCornerShape(3.dp)
            )
            .padding(5.dp)
            .clickable {
                onClick.invoke()
            }
            .onPointerEvent(PointerEventType.Enter) {
                isHovered = true
            }
            .onPointerEvent(PointerEventType.Exit) {
                isHovered = false
            },

        )
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