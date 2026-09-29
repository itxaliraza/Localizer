package common_components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import theme.GreenColor
import theme.LightPrimary
import theme.MutedTextColor
import theme.PrimaryColor
import theme.RedColor

enum class PillStyle {
    /** Solid green: the main action in a group (Save, Save as template, Download). */
    Primary,

    /** Muted teal: secondary actions next to a primary one (Import, Select these). */
    Secondary,

    /** Soft green that fills in on hover: repeated per-row actions (Apply). */
    Accent,
}

/** Rounded pill button shared by the templates card and dialogs; brightens on hover. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PillButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    style: PillStyle = PillStyle.Primary,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    var hovered by remember { mutableStateOf(false) }
    val active = enabled && hovered
    val (background, content) = when {
        !enabled -> LightPrimary.copy(alpha = 0.6f) to MutedTextColor
        style == PillStyle.Primary -> (if (active) Color(0xff63e125) else GreenColor) to Color.White
        style == PillStyle.Secondary -> (if (active) Color(0xff35687a) else LightPrimary) to Color.White
        else -> (if (active) GreenColor else GreenColor.copy(alpha = 0.16f)) to
                (if (active) Color.White else Color(0xff9be870))
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(background)
            .onPointerEvent(PointerEventType.Enter) { hovered = true }
            .onPointerEvent(PointerEventType.Exit) { hovered = false }
            .let { if (enabled) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(15.dp))
            HorizontalSpacer(5)
        }
        Text(text = text, color = content, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

/**
 * Square icon button that stays quiet until hovered, then tints to [hoverColor] on a soft matching
 * background. Shows [tooltip] on hover. Used for destructive/secondary row actions such as delete.
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun IconActionButton(
    icon: ImageVector,
    tooltip: String,
    hoverColor: Color = RedColor,
    onClick: () -> Unit,
) {
    var hovered by remember { mutableStateOf(false) }
    TooltipArea(
        tooltip = { Tooltip(tooltip) },
        delayMillis = 400,
        tooltipPlacement = TooltipPlacement.CursorPoint(offset = DpOffset(0.dp, 18.dp))
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (hovered) hoverColor.copy(alpha = 0.15f) else Color.Transparent)
                .onPointerEvent(PointerEventType.Enter) { hovered = true }
                .onPointerEvent(PointerEventType.Exit) { hovered = false }
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = tooltip,
                tint = if (hovered) hoverColor else MutedTextColor,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun Tooltip(text: String) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 11.sp,
        modifier = Modifier
            .background(PrimaryColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}
