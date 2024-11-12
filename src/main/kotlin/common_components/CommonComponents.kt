package common_components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import domain.model.LanguageModel

@Composable
fun HorizontalSpacer(
    width:Int=10
) {
    Spacer(modifier = Modifier.width(width.dp))
}

@Composable
fun VerticalSpacer(
    height:Int=10
) {
    Spacer(modifier = Modifier.height(height.dp))

}