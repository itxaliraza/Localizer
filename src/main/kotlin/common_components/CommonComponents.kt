package common_components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localizer.generated.resources.Res
import com.example.localizer.generated.resources.ic_close
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import theme.PrimaryColor
import theme.ScreenColor

@Composable
fun HorizontalSpacer(
    width: Int = 10
) {
    Spacer(modifier = Modifier.width(width.dp))
}

@Composable
fun VerticalSpacer(
    height: Int = 10
) {
    Spacer(modifier = Modifier.height(height.dp))

}

@Composable
fun EditText(
    modifier: Modifier = Modifier,
    value: String = "",
    hint: String = "Search here...",
    radius: Int = 10,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        modifier = modifier.fillMaxWidth(),
        textStyle = TextStyle(color = Color.White),
        placeholder = { Text(text = hint, color = Color.Gray) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        shape = RoundedCornerShape(radius.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PrimaryColor,
            unfocusedBorderColor = PrimaryColor,
            unfocusedContainerColor = ScreenColor,
            focusedContainerColor = ScreenColor,
            cursorColor = Color.White
        ),
        singleLine = true,
        onValueChange = onValueChange
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ImageButtons(
    icon: DrawableResource = Res.drawable.ic_close,
    color: Color = Color.Gray,
    tint: Color = Color.White,
    size: Int = 25,
    onClick: () -> Unit
) {
    var isHovered by remember { mutableStateOf(false) }
    Image(
        painter = painterResource(icon),
        contentDescription = "", colorFilter = ColorFilter.tint(tint),
        modifier = Modifier
            .size(size.dp)
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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ImageButtons(
    icon: ImageVector,
    color: Color = Color.Gray,
    tint: Color = Color.White,
    size: Int = 25,
    onClick: () -> Unit
) {
    var isHovered by remember { mutableStateOf(false) }
    Image(
        imageVector = icon,
        contentDescription = "", colorFilter = ColorFilter.tint(tint),
        modifier = Modifier
            .size(size.dp)
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