package home_screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
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
import androidx.compose.ui.window.Dialog
import com.example.localizer.generated.resources.Res
import com.example.localizer.generated.resources.ic_close
import common_components.EditText
import common_components.HorizontalSpacer
import common_components.ImageButtons
import common_components.VerticalSpacer
import data.model.TranslationResult
import data.util.openDownloadsFolder
import home_screen.components.RectangleWithShadow
import home_screen.components.RoundedCard
import home_screen.components.TemplatesCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import languages_screen.LanguagesScreen
import org.koin.compose.koinInject
import theme.GreenColor
import theme.LightPrimary
import theme.PrimaryColor
import theme.ScreenColor

@Composable
fun HomeScreenNew(viewModel: HomeScreenViewModel = koinInject()) {
    val scope= rememberCoroutineScope()
    val state by viewModel.state.collectAsState()
    var viewingModule by remember { mutableStateOf<ModuleSelection?>(null) }

    var showFileLoadedSnackBar by remember { mutableStateOf(false) }
    var fileLoadingStatus by remember { mutableStateOf("") }

    var canTranslateFile by remember { mutableStateOf(false) }
    LaunchedEffect(state.loadedPath, state.selectedLanguages, state.translationResult, state.modules) {
        canTranslateFile = state.selectedLanguages.isNotEmpty() && (state.loadedPath.isNotBlank())
                && (state.modules.isEmpty() || state.modules.any { it.selected })
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
            Column(
                modifier = Modifier.fillMaxSize().weight(0.8f).padding(10.dp)
                    .verticalScroll(rememberScrollState())
            ) {

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
                            text = "Enter res folder or project root",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        EditText(
                            hint = "D:\\AndroidProjects\\my-app  (or ...\\app\\src\\main\\res)",
                            value = state.folderPath,
                            modifier = Modifier.padding(7.dp)
                        ) {
                            viewModel.updateFolderPath(it)
                        }
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

                RectangleWithShadow(
                    bgColor = PrimaryColor, enableBlink = false,
                    modifier = Modifier.fillMaxWidth().wrapContentHeight().padding(top = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = state.loadedPath.ifBlank { "No File Selected" },
                                color = Color.White,
                                fontSize = 13.sp,
                                modifier = Modifier.basicMarquee()
                            )
                            if (state.modules.isNotEmpty()) {
                                Text(
                                    text = "${state.modules.count { it.selected }}/${state.modules.size} module(s) selected",
                                    color = Color(0xff03b6fc),
                                    fontSize = 11.sp,
                                    modifier = Modifier.basicMarquee()
                                )
                            }
                        }
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

                if (state.modules.isNotEmpty()) {
                    ModulesSelectionCard(
                        modules = state.modules,
                        onToggle = { viewModel.toggleModule(it) },
                        onToggleAll = { viewModel.toggleModule("", selectAll = true) },
                        onView = { viewingModule = it }
                    )
                }
                TemplatesCard(
                    state = state,
                    viewModel = viewModel,
                    onMessage = { message ->
                        scope.launch {
                            fileLoadingStatus = message
                            showFileLoadedSnackBar = true
                            delay(2500)
                            showFileLoadedSnackBar = false
                        }
                    }
                )

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
                                Column(
                                    modifier = Modifier.fillMaxWidth().wrapContentHeight()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    // Overall, language-level progress across all (module × language)
                                    // units, shown as a count e.g. "Languages   2/10".
                                    val totalUnits = result.totalUnits.coerceAtLeast(1)
                                    ProgressRow(
                                        label = if (result.moduleName.isNotBlank())
                                            "${result.moduleName} → ${result.translatingLang}"
                                        else
                                            "Translating ${result.translatingLang}",
                                        count = "${(result.completedUnits + 1).coerceAtMost(totalUnits)}/$totalUnits",
                                        fraction = result.completedUnits.toFloat() / totalUnits
                                    )

                                    // Per-language string-level progress for the language being translated.
                                    if (result.totalStrings > 0) {
                                        ProgressRow(
                                            label = "${result.translatingLang} strings",
                                            count = "${result.translatedStrings}/${result.totalStrings}",
                                            fraction = result.translatedStrings.toFloat() / result.totalStrings
                                        )
                                    }
                                }
                            }

                            else -> {}
                        }


                    }

                }

            }
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

    viewingModule?.let { module ->
        ModuleStringsDialog(
            module = module,
            content = viewModel.moduleStringsXml(module.resPath),
            onDismiss = { viewingModule = null }
        )
    }

}


/**
 * One labeled progress line: a left-aligned [label], a right-aligned [count] (e.g. "2/10"),
 * and a full-width rounded progress bar underneath. Used for both the overall language-level
 * bar and the per-language string-level bar so they share identical alignment.
 */
@Composable
private fun ProgressRow(label: String, count: String, fraction: Float) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f, fill = false)
            )
            HorizontalSpacer(8)
            Text(
                text = count,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        VerticalSpacer(6)
        LinearProgressIndicator(
            progress = fraction.coerceIn(0f, 1f),
            modifier = Modifier.fillMaxWidth().height(10.dp),
            backgroundColor = LightPrimary,
            strokeCap = StrokeCap.Round,
            color = GreenColor
        )
    }
}


@Composable
private fun ModulesSelectionCard(
    modules: List<ModuleSelection>,
    onToggle: (resPath: String) -> Unit,
    onToggleAll: () -> Unit,
    onView: (ModuleSelection) -> Unit,
) {
    RectangleWithShadow(
        bgColor = PrimaryColor,
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp).wrapContentHeight()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Modules to translate",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                val allSelected = modules.all { it.selected }
                Text(
                    text = if (allSelected) "Unselect all" else "Select all",
                    color = Color(0xff03b6fc),
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { onToggleAll() }
                )
            }
            VerticalSpacer()
            Column(
                modifier = Modifier.fillMaxWidth()
                    .heightIn(max = 220.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                modules.forEach { module ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clickable { onToggle(module.resPath) }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = module.selected,
                            onCheckedChange = { onToggle(module.resPath) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = GreenColor,
                                uncheckedColor = Color.LightGray,
                                checkmarkColor = Color.White
                            )
                        )
                        Text(
                            text = module.name,
                            color = Color.White,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f).padding(start = 4.dp).basicMarquee()
                        )
                        Text(
                            text = "${module.stringCount} strings",
                            color = Color(0xff03b6fc),
                            fontSize = 12.sp,
                        )
                        HorizontalSpacer()
                        Text(
                            text = "View",
                            color = GreenColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { onView(module) }
                                .padding(horizontal = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModuleStringsDialog(
    module: ModuleSelection,
    content: String,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = PrimaryColor,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.width(720.dp).height(560.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text(
                    text = "${module.name} • values/strings.xml (${module.stringCount} strings)",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
                VerticalSpacer()

                // weight(1f) bounds the scroll area to the remaining height inside the fixed-size
                // dialog, so the content never grows the window as you scroll.
                val vScroll = rememberScrollState()
                val hScroll = rememberScrollState()
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    SelectionContainer(
                        modifier = Modifier.fillMaxSize()
                            .verticalScroll(vScroll)
                            .horizontalScroll(hScroll)
                            .padding(end = 12.dp, bottom = 12.dp)
                    ) {
                        Text(
                            text = content.ifBlank { "(empty or no strings.xml found)" },
                            color = Color.White,
                            softWrap = false,
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        )
                    }
                    VerticalScrollbar(
                        adapter = rememberScrollbarAdapter(vScroll),
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                    )
                    HorizontalScrollbar(
                        adapter = rememberScrollbarAdapter(hScroll),
                        modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth()
                            .padding(end = 12.dp)
                    )
                }

                VerticalSpacer()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(Color(0xff03b6fc))
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}