package home_screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import common_components.HorizontalSpacer
import common_components.VerticalSpacer
import data.availableLanguages
import data.model.TranslationResult
import data.util.openDownloadsFolder
import home_screen.components.MultiSelectLanguageDialog
import org.koin.compose.koinInject
import java.awt.FileDialog
import java.awt.Frame


@Composable
fun HomeScreen(
    homeScreenViewModel: HomeScreenViewModel = koinInject()
) {
    val state by homeScreenViewModel.state.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var selectedFile by remember { mutableStateOf<String?>(null) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {

        LaunchedEffect(selectedFile) {
            if (selectedFile != null) {
                homeScreenViewModel.extractZipFile(path = selectedFile!!)
            }
        }

        Text("File may be zip of values folders or strings.xml")
        Spacer(modifier = Modifier.height(10.dp))

        Button(onClick = {
            val fileDialog = FileDialog(Frame(), "Select File", FileDialog.LOAD)
            fileDialog.isVisible = true
            val file = fileDialog.file
            if (file != null) {
                selectedFile = fileDialog.directory + file
            }
        }) {
            Text("Select File")
        }



        Spacer(modifier = Modifier.height(16.dp))

        selectedFile?.let { fileName ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(fileName)
                HorizontalSpacer()
                Icon(
                    modifier = Modifier.clickable {
                        selectedFile = null
                    },
                    imageVector = Icons.Filled.Clear,
                    contentDescription = null,
                )
            }
        }
        VerticalSpacer(30)
        Button(onClick = { showDialog = true }) {
            Text("Select Languages")
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 10.dp)
        ) {
            items(state.selectedLanguages) {
                Row(
                    modifier = Modifier.clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colors.secondary)
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = it.langName, color = MaterialTheme.colors.onPrimary)
                    HorizontalSpacer()
                    Icon(
                        modifier = Modifier.clickable {
                            homeScreenViewModel.removeLanguage(it)
                        },
                        imageVector = Icons.Filled.Clear,
                        contentDescription = null,
                        tint = MaterialTheme.colors.onPrimary
                    )
                }
            }
        }

        VerticalSpacer(20)

        if (showDialog) {
            MultiSelectLanguageDialog(
                availableLanguages = availableLanguages(),
                selectedLanguages = state.selectedLanguages,
                onDismiss = { showDialog = false },
                onConfirm = { selected ->
                    homeScreenViewModel.updateSelectedLanguages(selected)
                }
            )
        }

        if (state.showLoading) {
            Dialog(onDismissRequest = {
            }) {
                Box(modifier = Modifier.size(100.dp)) {
                    CircularProgressIndicator()
                }
            }
        }

        when (val translationResult = state.translationResult) {
            is TranslationResult.UpdateProgress -> {
                TranslationProgress(translationResult.progress, translationResult.translatingLang)
            }

            is TranslationResult.TranslationCompleted -> {
                TranslationCompleted()
            }

            is TranslationResult.TranslationFailed -> {
                TranslationFailed(translationResult.exc)
            }

            TranslationResult.Idle -> {

            }
        }

        if (state.translationResult is TranslationResult.UpdateProgress) {
            Button(onClick = {
                homeScreenViewModel.cancelTranslation()
            }) {
                Text(text = "Stop Translation")
            }
        } else {
            VerticalSpacer(20)
            Button(
                onClick = {
                    homeScreenViewModel.translate()
                },
                enabled = state.selectedLanguages.isNotEmpty() && selectedFile?.isNotEmpty() == true && (state.translationResult as? TranslationResult.TranslationFailed)?.exc?.message != "Not valid file"
            ) {
                Text(text = "Start Translation")
            }
            VerticalSpacer()
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable {
                homeScreenViewModel.toggleParallel(state.parallelTranslation.not())
            }) {
                Checkbox(checked = state.parallelTranslation, onCheckedChange = {
                    homeScreenViewModel.toggleParallel(it)
                })
                HorizontalSpacer()
                Text("Parallel Translation")
            }
        }


    }
}


@Composable
fun TranslationProgress(progress: Int, translatingLang: String) {
    Column(
        modifier = Modifier
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Translating $translatingLang...")
        VerticalSpacer()
        LinearProgressIndicator(progress / 100f, Modifier.fillMaxWidth())
        VerticalSpacer()
        Text("$progress% completed")
    }
}

@Composable
fun TranslationCompleted() {
    VerticalSpacer()
    Box(
        contentAlignment = Alignment.Center
    ) {
        Text("Translation Completed!")
    }
    VerticalSpacer()

    Button(onClick = { openDownloadsFolder() }) {
        Text("Go To Translated File")
    }
}

@Composable
fun TranslationFailed(exception: Exception) {
    Box(
        contentAlignment = Alignment.Center
    ) {
        Text("${exception.message}")
    }
}

