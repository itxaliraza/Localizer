package home_screen.components

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import theme.GreenColor
import theme.RedColor
import theme.ScreenColor

@Composable
fun RectangleWithShadow(
    modifier: Modifier = Modifier,
    elevation: Int = 5,
    radius: Int = 10,
    bgColor: Color = ScreenColor,
    align: Alignment = Alignment.Center,
    enableBlink: Boolean = false,
    blinkColor: Color = GreenColor,
    onClick: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val animatedColor = remember { Animatable(bgColor) }

    LaunchedEffect(enableBlink) {
        if (enableBlink) {
            val duration = 2000 // total duration in ms
            val interval = 200  // how fast to blink
            val times = duration / (interval * 2)
            repeat(times) {
                animatedColor.animateTo(
                    blinkColor,
                    animationSpec = tween(durationMillis = interval)
                )
                animatedColor.animateTo(bgColor, animationSpec = tween(durationMillis = interval))
            }
            // Ensure final state is bgColor
            animatedColor.snapTo(bgColor)
        }
    }

    Box(
        contentAlignment = align,
        modifier = modifier
            .clickable { onClick() }
            .background(color = animatedColor.value, shape = RoundedCornerShape(radius.dp))
            .shadow(
                elevation = elevation.dp,
                ambientColor = animatedColor.value,
                spotColor = animatedColor.value
            )
    ) {
        content()
    }
}
