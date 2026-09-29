package about_screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.localizer.generated.resources.Res
import com.example.localizer.generated.resources.app_icon
import common_components.AppIcons
import common_components.HorizontalSpacer
import common_components.PillButton
import common_components.PillStyle
import common_components.VerticalSpacer
import common_components.openUrl
import domain.model.LatestRelease
import org.jetbrains.compose.resources.painterResource
import theme.GreenColor
import theme.MutedTextColor
import theme.PrimaryColor
import theme.ScreenColor
import theme.WarningColor

/** Opened from the title-bar info icon: version, quick how-to, links, and an update check. */
@Composable
fun AboutDialog(viewModel: AboutViewModel, onDismiss: () -> Unit) {
    val updateState by viewModel.updateState.collectAsState()
    LaunchedEffect(Unit) {
        if (updateState is UpdateState.Idle) viewModel.checkForUpdates()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(color = PrimaryColor, shape = RoundedCornerShape(12.dp), modifier = Modifier.width(480.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(22.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(Res.drawable.app_icon),
                        contentDescription = null,
                        modifier = Modifier.size(52.dp).clip(RoundedCornerShape(12.dp))
                    )
                    HorizontalSpacer(14)
                    Column {
                        Text("Fast Localizer", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Version ${viewModel.currentVersion}", color = MutedTextColor, fontSize = 12.sp)
                    }
                }

                VerticalSpacer(14)
                Text(
                    "Translates your Android app's strings.xml into 240+ languages with Google Translate, " +
                            "keeping placeholders, markup and your existing translations intact.",
                    color = Color(0xff9fb9c1),
                    fontSize = 12.sp,
                )

                VerticalSpacer(16)
                Text("How to use", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                VerticalSpacer(6)
                listOf(
                    "Paste your project root or res folder path and click Load File.",
                    "Pick languages, or apply or import a template.",
                    "Click Start Translation. The files are written into each module's res folder.",
                ).forEachIndexed { index, step -> HowToStep(index + 1, step) }

                VerticalSpacer(16)
                UpdateBox(
                    state = updateState,
                    currentVersion = viewModel.currentVersion,
                    onRetry = viewModel::checkForUpdates,
                    onDownload = { openUrl(viewModel.downloadUrl(it)) },
                    onViewRelease = { openUrl(it.pageUrl) }
                )

                VerticalSpacer(16)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LinkText("GitHub") { openUrl(viewModel.repoUrl) }
                    HorizontalSpacer(16)
                    LinkText("All releases") { openUrl(viewModel.releasesUrl) }
                    HorizontalSpacer(16)
                    LinkText("Report an issue") { openUrl(viewModel.issuesUrl) }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) {
                        Text("Close", color = Color(0xff9fb9c1))
                    }
                }
            }
        }
    }
}

@Composable
private fun HowToStep(number: Int, text: String) {
    Row(modifier = Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier.size(18.dp).clip(RoundedCornerShape(9.dp)).background(ScreenColor),
            contentAlignment = Alignment.Center
        ) {
            Text("$number", color = Color(0xff9be870), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        HorizontalSpacer(8)
        Text(text, color = Color(0xffc9dbe0), fontSize = 12.sp)
    }
}

@Composable
private fun UpdateBox(
    state: UpdateState,
    currentVersion: String,
    onRetry: () -> Unit,
    onDownload: (LatestRelease) -> Unit,
    onViewRelease: (LatestRelease) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(ScreenColor, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (state) {
            UpdateState.Idle, UpdateState.Checking -> {
                CircularProgressIndicator(color = GreenColor, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                HorizontalSpacer(10)
                Text("Checking for updates…", color = Color(0xffc9dbe0), fontSize = 12.sp)
            }

            is UpdateState.UpToDate -> {
                Icon(Icons.Default.CheckCircle, null, tint = Color(0xff9be870), modifier = Modifier.size(18.dp))
                HorizontalSpacer(10)
                Text(
                    "You're on the latest version ($currentVersion).",
                    color = Color(0xffc9dbe0),
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
                LinkText("Check again", onClick = onRetry)
            }

            is UpdateState.Available -> {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Version ${state.release.version} is available",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("You have $currentVersion.", color = MutedTextColor, fontSize = 11.sp)
                }
                LinkText("What's new") { onViewRelease(state.release) }
                HorizontalSpacer(10)
                PillButton(text = "Download", icon = AppIcons.OpenInNew, onClick = { onDownload(state.release) })
            }

            is UpdateState.Failed -> {
                Icon(Icons.Default.Warning, null, tint = WarningColor, modifier = Modifier.size(18.dp))
                HorizontalSpacer(10)
                Column(modifier = Modifier.weight(1f)) {
                    Text("Couldn't check for updates", color = Color.White, fontSize = 12.sp)
                    Text(
                        state.message,
                        color = MutedTextColor,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                PillButton(text = "Retry", style = PillStyle.Secondary, onClick = onRetry)
            }
        }
    }
}

@Composable
private fun LinkText(text: String, onClick: () -> Unit) {
    Text(
        text,
        color = Color(0xff03b6fc),
        fontSize = 12.sp,
        modifier = Modifier.clip(RoundedCornerShape(4.dp)).clickable(onClick = onClick).padding(2.dp)
    )
}
