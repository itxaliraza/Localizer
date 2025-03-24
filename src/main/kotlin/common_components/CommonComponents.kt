package common_components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
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
        placeholder = { Text(text = hint, color = Color.White) },
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