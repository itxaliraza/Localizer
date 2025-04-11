package home_screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localizer.generated.resources.Res
import com.example.localizer.generated.resources.ic_close
import common_components.EditText
import common_components.HorizontalSpacer
import common_components.ImageButtons
import common_components.VerticalSpacer
import data.model.TranslationResult
import data.util.exportLanguageCodesToJson
import data.util.importLanguageCodesFromJson
import data.util.openDownloadsFolder
import home_screen.components.RectangleWithShadow
import home_screen.components.RoundedCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import languages_screen.LanguagesScreen
import org.koin.compose.koinInject
import theme.GreenColor
import theme.LightPrimary
import theme.PrimaryColor
import theme.ScreenColor
import java.awt.FileDialog
import java.awt.Frame

@Composable
fun HomeScreenNew(viewModel: HomeScreenViewModel = koinInject()) {
    val state by viewModel.state.collectAsState()
    var showSnackBar by remember { mutableStateOf(false) }
    var showImportGuideDialog by remember { mutableStateOf(false) }

    var showFileLoadedSnackBar by remember { mutableStateOf(false) }
    var fileLoadingStatus by remember { mutableStateOf("") }

    var canTranslateFile by remember { mutableStateOf(false) }
    LaunchedEffect(state.loadedPath, state.selectedLanguages, state.translationResult) {
        canTranslateFile = state.selectedLanguages.isNotEmpty() && (state.loadedPath.isNotBlank())
                && (state.translationResult as? TranslationResult.TranslationFailed)?.exc?.message != "Not valid file"
    }



    LaunchedEffect(Unit) {
        viewModel.oneTimeUiEvents.collectLatest {
            when (it) {
                HomeScreenOneTimeEvents.FileLoadedFail -> {
                    fileLoadingStatus = "File Loading Failed.."
                    showFileLoadedSnackBar = true
                    delay(2000)
                    showFileLoadedSnackBar = false

                }

                HomeScreenOneTimeEvents.FileLoadedSuccess -> {
                    fileLoadingStatus = "File Loading success"
                    showFileLoadedSnackBar = true
                    delay(2000)
                    showFileLoadedSnackBar = false

                }
            }
        }
    }
    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            LanguagesScreen(
                modifier = Modifier.fillMaxSize().padding(10.dp).weight(1f),
                state, viewModel
            )
            Column(modifier = Modifier.fillMaxSize().weight(0.8f).padding(10.dp)) {

                VerticalSpacer()
                RectangleWithShadow(
                    bgColor = PrimaryColor,
                    modifier = Modifier.wrapContentHeight(), onClick = {
                    }
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 11.dp)
                    ) {
                        Text(
                            text = "Enter path of Values folder",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        EditText(
                            hint = "E:\\Translator\\app\\src\\main\\res",
                            value = state.folderPath,
                            modifier = Modifier.padding(7.dp)
                        ) {
                            viewModel.updateFolderPath(it)
                        }
                        AnimatedVisibility(visible = state.folderPath.isNotEmpty()) {
                            RoundedCard(
                                modifier = Modifier.fillMaxWidth().padding(5.dp),
                                bgColor = ScreenColor,
                                onClick = {
                                    viewModel.loadFileFromPath(state.folderPath)
                                }
                            ) {
                                Text(
                                    text = "Load File",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 5.dp)
                                )
                            }
                        }
                    }
                }

                RectangleWithShadow(
                    bgColor = PrimaryColor, enableBlink = state.loadedPath.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().wrapContentHeight().padding(top = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = state.loadedPath.ifBlank { "No File Selected" },
                            color = Color.Gray,
                            fontSize = 13.sp,
                            modifier = Modifier.basicMarquee()
                        )
                        if (state.loadedPath.isNotBlank()) {
                            HorizontalSpacer()
                            ImageButtons(
                                tint = Color.Gray, size = 25,
                                icon = Res.drawable.ic_close, color = Color.Red,
                                onClick = {
                                    viewModel.clearLoadedFile()
                                }
                            )
                        }

                    }
                }
                RectangleWithShadow(
                    bgColor = PrimaryColor,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                        ) {
                            ImageButtons(
                                icon = Icons.Default.Info,
                                size = 26,
                                onClick = {
                                    showImportGuideDialog = true
                                }
                            )
                            RoundedCard(
                                clickEnable = state.folderPath.isNotBlank(),
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
                                Column {

                                    Text(
                                        text = "Import Languages",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                            .padding(horizontal = 5.dp)
                                    )
                                }

                            }
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
                        when (val result = state.translationResult) {
                            TranslationResult.TranslationCompleted -> {
                                RoundedCard(
                                    modifier = Modifier.fillMaxWidth().padding(5.dp),
                                    bgColor = ScreenColor,
                                    clickEnable = true,
                                    onClick = { openDownloadsFolder(state.folderPath) }
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
                                    modifier = Modifier.fillMaxWidth().padding(10.dp).clickable {
                                        if (canTranslateFile) {
                                            viewModel.translate()
                                        }
                                    }
                                )
                            }

                            is TranslationResult.UpdateProgress -> {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxWidth().wrapContentHeight()
                                ) {
                                    val progress = result.progress
                                    LinearProgressIndicator(
                                        progress = progress / 100f,
                                        modifier = Modifier.fillMaxWidth().height(60.dp)
                                            .padding(10.dp),
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

    if (showFileLoadedSnackBar) {
        Snackbar(
            action = {
                TextButton(onClick = { showFileLoadedSnackBar = false }) {
                    Text("Dismiss", color = Color(0xff03b6fc))
                }
            }
        ) {
            Text(fileLoadingStatus, color = Color.White)
        }
    }

    JsonGuideDialog(
        showImportGuideDialog,
        onDismiss = {
            showImportGuideDialog = false
        }
    )

}


@Composable
fun JsonGuideDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit
) {
    if (showDialog) {
        AlertDialog(
            backgroundColor = PrimaryColor,
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(Color(0xff03b6fc))
                ) {
                    Text("OK")
                }
            },
            title = {
                Text("Guide (JSON format)")
            },
            text = {
                val jsonGuide = """
                    {
                      "step1": "Tap the 'Select File' button.",
                      
                      "step2": "Choose a file that contains an array of language codes.",

                       "example": ["ar", "af", "en", "fr"],
                       
                      "note": "The file must be a valid JSON file with language codes in an array format."
                    }
                """.trimIndent()

                Box(
                    modifier = Modifier
                        .heightIn(min = 100.dp, max = 300.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = jsonGuide,
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp
                        )
                    )
                }
            }
        )
    }
}