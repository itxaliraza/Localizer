package theme

import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.lightColors
import androidx.compose.ui.graphics.Color



val PrimaryColor = Color(0xff134655)
val ScreenColor = Color(0xFF16404D)
val RedColor = Color(0xffe13d3d)
val LightPrimary = Color(0xff28515e)
val GreenColor = Color(0xb363e125)
val WarningColor = Color(0xffffb74d)
val MutedTextColor = Color(0xff7fa6b0)
val AccentBlue = Color(0xff03b6fc)

// The default selection highlight is derived from PrimaryColor, which is almost the same shade as the
// text-field background, so selected text was invisible. A translucent bright blue reads clearly on the
// dark teal fields while keeping the white text legible.
val AppTextSelectionColors = TextSelectionColors(
    handleColor = AccentBlue,
    backgroundColor = AccentBlue.copy(alpha = 0.45f)
)

val LightColors = lightColors(
    primary = PrimaryColor,
    onPrimary = Color.White,
    secondary = LightPrimary,
    onSecondary = Color.White
)
