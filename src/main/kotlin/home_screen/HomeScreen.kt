package home_screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import common_components.HorizontalSpacer
import common_components.VerticalSpacer
import data.model.TranslationResult
import data.util.openDownloadsFolder
import domain.model.LanguageModel
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
            items(state.selectedLanguagesList) {
                Row(
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colors.secondary)
                        .padding(horizontal = 10.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically
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
                availableLanguages = availableLanguagesList,
                selectedLanguages = state.selectedLanguagesList,
                onDismiss = { showDialog = false },
                onConfirm = { selected ->
                    homeScreenViewModel.updateSelectedLanguages(selected)
//                    selectedLanguages = selected
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
                enabled = state.selectedLanguagesList.isNotEmpty() && selectedFile?.isNotEmpty() == true && (state.translationResult as? TranslationResult.TranslationFailed)?.exc?.message != "Not valid file"
            ) {
                Text(text = "Start Translation")
            }
            VerticalSpacer()
            Row (verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable {
                homeScreenViewModel.toggleParallel(state.parallelTranslation.not())
            }){
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


val availableLanguagesList: List<LanguageModel> by lazy {
    listOf(
        LanguageModel("Afrikaans", "af"),
        LanguageModel("Albanian", "sq"),
        LanguageModel("Amharic", "am"),
        LanguageModel("Arabic", "ar"),
        LanguageModel("Armenian", "hy"),
        LanguageModel("Azerbaijani", "az"),
        LanguageModel("Basque", "eu"),
        LanguageModel("Belarusian", "be"),
        LanguageModel("Bengali", "bn"),
        LanguageModel("Bosnian", "bs"),
        LanguageModel("Bulgarian", "bg"),
        LanguageModel("Catalan", "ca"),
        LanguageModel("Cebuano", "ceb"),
        LanguageModel("Chinese (Simplified)", "zh-CN"),
        LanguageModel("Chinese (Traditional)", "zh-TW"),
        LanguageModel("Corsican", "co"),
        LanguageModel("Croatian", "hr"),
        LanguageModel("Czech", "cs"),
        LanguageModel("Danish", "da"),
        LanguageModel("Dutch", "nl"),
//        LanguageModel("English", "en"),
        LanguageModel("Esperanto", "eo"),
        LanguageModel("Estonian", "et"),
        LanguageModel("Finnish", "fi"),
        LanguageModel("French", "fr"),
        LanguageModel("Frisian", "fy"),
        LanguageModel("Galician", "gl"),
        LanguageModel("Georgian", "ka"),
        LanguageModel("German", "de"),
        LanguageModel("Greek", "el"),
        LanguageModel("Gujarati", "gu"),
        LanguageModel("Haitian Creole", "ht"),
        LanguageModel("Hausa", "ha"),
        LanguageModel("Hawaiian", "haw"),
        LanguageModel("Hebrew he", "he"),
        LanguageModel("Hebrew iw", "iw"),
        LanguageModel("Hindi", "hi"),
        LanguageModel("Hmong", "hmn"),
        LanguageModel("Hungarian", "hu"),
        LanguageModel("Icelandic", "is"),
        LanguageModel("Igbo", "ig"),
        LanguageModel("Indonesian id", "id"),
        LanguageModel("Indonesian In", "in"),
        LanguageModel("Irish", "ga"),
        LanguageModel("Italian", "it"),
        LanguageModel("Japanese", "ja"),
        LanguageModel("Javanese", "jw"),
        LanguageModel("Kannada", "kn"),
        LanguageModel("Kazakh", "kk"),
        LanguageModel("Khmer", "km"),
        LanguageModel("Kinyarwanda", "rw"),
        LanguageModel("Korean", "ko"),
        LanguageModel("Kurdish", "ku"),
        LanguageModel("Kyrgyz", "ky"),
        LanguageModel("Lao", "lo"),
        LanguageModel("Latin", "la"),
        LanguageModel("Latvian", "lv"),
        LanguageModel("Lithuanian", "lt"),
        LanguageModel("Luxembourgish", "lb"),
        LanguageModel("Macedonian", "mk"),
        LanguageModel("Malagasy", "mg"),
        LanguageModel("Malay", "ms"),
        LanguageModel("Malayalam", "ml"),
        LanguageModel("Maltese", "mt"),
        LanguageModel("Maori", "mi"),
        LanguageModel("Marathi", "mr"),
        LanguageModel("Mongolian", "mn"),
        LanguageModel("Myanmar (Burmese)", "my"),
        LanguageModel("Nepali", "ne"),
        LanguageModel("Norwegian", "no"),
        LanguageModel("Nyanja (Chichewa)", "ny"),
        LanguageModel("Odia (Oriya)", "or"),
        LanguageModel("Oromo", "om"),
        LanguageModel("Pashto", "ps"),
        LanguageModel("Persian", "fa"),
        LanguageModel("Polish", "pl"),
        LanguageModel("Portuguese", "pt"),
        LanguageModel("Punjabi", "pa"),
        LanguageModel("Romanian", "ro"),
        LanguageModel("Russian", "ru"),
        LanguageModel("Samoan", "sm"),
        LanguageModel("Scots Gaelic", "gd"),
        LanguageModel("Serbian", "sr"),
        LanguageModel("Sesotho", "st"),
        LanguageModel("Shona", "sn"),
        LanguageModel("Sindhi", "sd"),
        LanguageModel("Sinhala (Sinhalese)", "si"),
        LanguageModel("Slovak", "sk"),
        LanguageModel("Slovenian", "sl"),
        LanguageModel("Somali", "so"),
        LanguageModel("Spanish", "es"),
        LanguageModel("Sundanese", "su"),
        LanguageModel("Swahili", "sw"),
        LanguageModel("Swedish", "sv"),
        LanguageModel("Tagalog (Filipino)", "tl"),
        LanguageModel("Tajik", "tg"),
        LanguageModel("Tamil", "ta"),
        LanguageModel("Tatar", "tt"),
        LanguageModel("Telugu", "te"),
        LanguageModel("Thai", "th"),
        LanguageModel("Turkish", "tr"),
        LanguageModel("Turkmen", "tk"),
        LanguageModel("Ukrainian", "uk"),
        LanguageModel("Urdu", "ur"),
        LanguageModel("Uyghur", "ug"),
        LanguageModel("Uzbek", "uz"),
        LanguageModel("Vietnamese", "vi"),
        LanguageModel("Welsh", "cy"),
        LanguageModel("Xhosa", "xh"),
        LanguageModel("Yiddish yi", "yi"),
        LanguageModel("Yiddish ji", "ji"),
        LanguageModel("Yoruba", "yo"),
        LanguageModel("Zulu", "zu")
    )
}