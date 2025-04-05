package home_screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Snackbar
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localizer.generated.resources.Res
import com.example.localizer.generated.resources.ic_close
import common_components.HorizontalSpacer
import common_components.ImageButtons
import data.model.TranslationResult
import data.util.exportLanguageCodesToJson
import data.util.importLanguageCodesFromJson
import data.util.openDownloadsFolder
import home_screen.components.RectangleWithShadow
import home_screen.components.RoundedCard
import languages_screen.LanguagesScreen
import org.koin.compose.koinInject
import theme.GreenColor
import theme.LightPrimary
import theme.PrimaryColor
import theme.ScreenColor
import java.awt.FileDialog
import java.awt.Frame
//This screen is added for UI changes
@Composable
fun HomeScreenNew(viewModel: HomeScreenViewModel = koinInject()) {
    val state by viewModel.state.collectAsState()
    var selectedFile by remember { mutableStateOf<String?>(null) }
    var showSnackBar by remember { mutableStateOf(false) }
    var canTranslateFile by remember { mutableStateOf(false) }
    LaunchedEffect(selectedFile, state.selectedLanguages, state.translationResult) {
        canTranslateFile = state.selectedLanguages.isNotEmpty() && selectedFile != null
                && (state.translationResult as? TranslationResult.TranslationFailed)?.exc?.message != "Not valid file"
    }
    LaunchedEffect(selectedFile) {
        viewModel.extractZipFile(path = selectedFile)
    }
    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            LanguagesScreen(
                modifier = Modifier.fillMaxSize().padding(10.dp).weight(1f),
                state, viewModel
            )
            Column(modifier = Modifier.fillMaxSize().weight(0.8f).padding(10.dp)) {
                RectangleWithShadow(
                    bgColor = PrimaryColor,
                    modifier = Modifier.wrapContentHeight(), onClick = {
                        val fileDialog = FileDialog(Frame(), "Select File", FileDialog.LOAD)
                        fileDialog.setFilenameFilter { _, name ->
                            name.endsWith(".xml") || name.endsWith(".zip")
                            false
                        }
                        fileDialog.isVisible = true
                        val file = fileDialog.file
                        if (file != null) {
                            selectedFile = fileDialog.directory + file
                        }

                    }
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 11.dp)
                    ) {
                        Text(
                            text = "Select File",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "File can be .zip of values folder or strings.xml file",
                            color = Color.Gray,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                RectangleWithShadow(
                    bgColor = PrimaryColor,
                    modifier = Modifier.fillMaxWidth().wrapContentHeight().padding(top = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = selectedFile ?: "No File Selected",
                            color = Color.Gray,
                            fontSize = 13.sp,
                            modifier = Modifier.basicMarquee()
                        )
                        selectedFile?.let {
                            HorizontalSpacer()
                            ImageButtons(
                                tint = Color.Gray, size = 25,
                                icon = Res.drawable.ic_close, color = Color.Red,
                                onClick = {
                                    selectedFile = null
                                }
                            )
                        }
                    }
                }
                RectangleWithShadow(
                    bgColor = PrimaryColor,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp).wrapContentHeight()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
                    ) {
                        RoundedCard(
                            modifier = Modifier.weight(1f),
                            bgColor = ScreenColor,
                            onClick = {
                                val fileDialog = FileDialog(
                                    Frame(),
                                    "Select Languages Json",
                                    FileDialog.LOAD
                                )
                                fileDialog.isVisible = true
                                val file = fileDialog.file
                                if (file != null) {
                                    val path = fileDialog.directory + file
                                    println("Selected Lang file = $path")
                                    val languageCodes = importLanguageCodesFromJson(path)
                                    println("Selected Lang file languageCodes= $languageCodes")
                                    state.availableLanguages.forEach {
                                        if (languageCodes.contains(it.langCode) && !state.selectedLanguages.contains(
                                                it
                                            )
                                        ) {
                                            viewModel.updateSelectedLanguages(it)
                                        }
                                    }
                                }
                            }
                        ) {
                            Text(
                                text = "Import Languages",
                                color = Color.White,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 5.dp)
                            )
                        }
                        RoundedCard(
                            modifier = Modifier.weight(1f),
                            bgColor = ScreenColor,
                            clickEnable = state.selectedLanguages.isNotEmpty(),
                            onClick = {
                                val codesList =
                                    state.selectedLanguages.toList().map { it.langCode }
                                exportLanguageCodesToJson(codesList)
                                showSnackBar = true
                            }
                        ) {
                            Text(
                                text = "Export Languages",
                                color = Color.White,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 5.dp)
                            )
                        }
                    }
                }

                RectangleWithShadow(
                    bgColor = PrimaryColor,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp).wrapContentHeight()
                ) {
                    val isTranslating = state.translationResult is TranslationResult.UpdateProgress
                    RoundedCard(
                        modifier = Modifier.fillMaxWidth().padding(5.dp),
                        bgColor = ScreenColor,
                        clickEnable = canTranslateFile,
                        onClick = {
                            if (isTranslating) {
                                viewModel.cancelTranslation()
                            } else {
                                viewModel.translate()
                            }
                        }
                    ) {
                        Text(
                            text = if (isTranslating) "Stop Translation" else "Start Translation",
                            color = Color.White,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 5.dp)
                        )
                    }
                }
                RectangleWithShadow(
                    bgColor = PrimaryColor,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp).wrapContentHeight()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        AnimatedVisibility(visible = true) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp).fillMaxWidth()
                                    .clickable {
                                        viewModel.toggleParallel(!state.parallelTranslation)
                                    })
                            {
                                Text(
                                    text = "Enable Parallel Translation",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    modifier = Modifier.fillMaxWidth().weight(1f)
                                )
                                Switch(
                                    checked = state.parallelTranslation,
                                    colors = SwitchDefaults.colors(
                                        PrimaryColor,
                                        Color.Gray,
                                        uncheckedBorderColor = Color.Transparent
                                    ),
                                    onCheckedChange = {
                                        viewModel.toggleParallel(it)
                                    })

                            }
                        }
                        when (val result = state.translationResult) {
                            TranslationResult.TranslationCompleted -> {
                                RoundedCard(
                                    modifier = Modifier.fillMaxWidth().padding(5.dp),
                                    bgColor = ScreenColor,
                                    clickEnable = true,
                                    onClick = { openDownloadsFolder() }
                                ) {
                                    Text(
                                        text = "Translation Completed, Open Now",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                            is TranslationResult.TranslationFailed -> {
                                Text(
                                    text = "${result.exc.message}, Try again",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(10.dp).clickable { viewModel.translate() }
                                )
                            }
                            is TranslationResult.UpdateProgress -> {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxWidth().wrapContentHeight()
                                ) {
                                    val progress = result.progress
                                    LinearProgressIndicator(
                                        progress = progress/100f,
                                        modifier = Modifier.fillMaxWidth().height(60.dp).padding(10.dp),
                                        backgroundColor = LightPrimary,
                                        strokeCap = StrokeCap.Round,
                                        color = GreenColor
                                    )
                                    Text(
                                        text = "Translating ${result.translatingLang} (${progress} %)",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            else -> {}
                        }


                    }

                }

            }
        }
    }
    if (showSnackBar) {
        Snackbar(
            action = {
                TextButton(onClick = { showSnackBar = false }) {
                    Text("Dismiss")
                }
            }
        ) {
            Text("Exported successfully to Downloads")
        }
    }
}