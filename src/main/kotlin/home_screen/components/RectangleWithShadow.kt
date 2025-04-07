package home_screen.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.onClick
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import theme.ScreenColor

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RectangleWithShadow(
    modifier: Modifier = Modifier,
    elevation: Int = 5,
    radius: Int = 10,
    bgColor: Color = ScreenColor,
    align: Alignment = Alignment.Center,
    onClick: () -> Unit = {},
    content: @Composable () -> Unit
) {
    Box(
        contentAlignment = align,
        modifier = modifier.onClick {
            onClick()
        }.background(color = bgColor, shape = RoundedCornerShape(radius.dp))
            .shadow(
                elevation = elevation.dp,
                ambientColor = bgColor,
                spotColor = bgColor
            )
    ) {
        content()
    }
}