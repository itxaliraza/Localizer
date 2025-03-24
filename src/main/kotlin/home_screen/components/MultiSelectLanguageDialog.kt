package home_screen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import common_components.HorizontalSpacer
import common_components.VerticalSpacer
import data.util.exportLanguageCodesToJson
import data.util.importLanguageCodesFromJson
import domain.model.LanguageModel
import org.koin.compose.koinInject
import java.awt.FileDialog
import java.awt.Frame

@Composable
fun MultiSelectLanguageDialog(
    availableLanguages: List<LanguageModel>,
    selectedLanguages: List<LanguageModel>,
    onDismiss: () -> Unit,
    onConfirm: (List<LanguageModel>) -> Unit
) {
    val tempSelected: SnapshotStateList<LanguageModel> =
        remember(selectedLanguages) { selectedLanguages.toMutableStateList() }

    var showSnackbar by remember { mutableStateOf(false) }
    var searchedText by remember { mutableStateOf("") }
    var filteredLanguages by remember { mutableStateOf(availableLanguages) }

    LaunchedEffect(searchedText) {
        filteredLanguages = if (searchedText.isEmpty()) {
            availableLanguages
        } else {
            availableLanguages.filter {
                it.langName.contains(searchedText, true) || it.langCode.contains(searchedText, true)
            }
        }
    }
    Dialog(onDismissRequest = onDismiss) {

        Surface(
            modifier = Modifier.padding(16.dp).fillMaxHeight(),
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .heightIn(min = 800.dp),  // Use heightIn with min to enforce minimum height

            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Select Languages")
                    HorizontalSpacer()
                    TextButton(onClick = {
                        if (tempSelected.size >= availableLanguages.size) {
                            tempSelected.clear()
                        } else {
                            tempSelected.clear()
                            tempSelected.addAll(availableLanguages)
                        }

                    }) {
                        Text(
                            if (tempSelected.size >= availableLanguages.size)
                                "UnSelect All"
                            else
                                "Select All"
                        )
                    }
                    HorizontalSpacer()

                    TextButton(onClick = {
                        val fileDialog = FileDialog(Frame(), "Select Languages Json", FileDialog.LOAD)
                        fileDialog.isVisible = true
                        val file = fileDialog.file
                        if (file != null) {
                            val selectedFile = fileDialog.directory + file
                            println("Selected Lang file = $selectedFile")
                            val languageCodes = importLanguageCodesFromJson(selectedFile)
                            println("Selected Lang file languageCodes= $languageCodes")

                            availableLanguages.forEach {
                                if (languageCodes.contains(it.langCode) && tempSelected.contains(it).not()) {
                                    tempSelected.add(it)
                                }
                            }
                        }
                    }) {
                        Text("Import Languages")
                    }
                }
                VerticalSpacer()
                TextField(modifier = Modifier.fillMaxWidth(), value = searchedText, onValueChange = {
                    searchedText = it
                }, trailingIcon = {
                    if(searchedText.isNotEmpty()) {
                        IconButton(onClick = {
                            searchedText = ""
                        }) {
                            Icon(Icons.Filled.Close, null)
                        }
                    }
                })

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.fillMaxHeight().weight(1f)) {
                    items(filteredLanguages) { language ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable {
                                if (language in tempSelected) {
                                    tempSelected.remove(language)
                                } else {
                                    tempSelected.add(language)
                                }
                            }
                        ) {
                            Checkbox(
                                checked = language in tempSelected,
                                onCheckedChange = { isChecked ->
                                    if (isChecked) {
                                        tempSelected.add(language)
                                    } else {
                                        tempSelected.remove(language)
                                    }
                                }
                            )
                            Text(language.langName)
                        }
                    }
                }


                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 5.dp)
                ) {
                    TextButton(
                        onClick = {
                            val downloadsPath =
                                System.getProperty("user.home") + "/Downloads/languages_${System.currentTimeMillis()}.txt"
                            exportLanguageCodesToJson(downloadsPath, tempSelected.toList().map { it.langCode })
                            showSnackbar = true
                        },
                        enabled = tempSelected.size > 0
                    ) {
                        Text("Export Languages")
                    }
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = {
                                onConfirm(tempSelected.toList())
                                onDismiss()
                            }
                        ) {
                            Text("Confirm")
                        }
                    }
                }
            }

        }

        if (showSnackbar) {
            Snackbar(
                action = {
                    TextButton(onClick = { showSnackbar = false }) {
                        Text("Dismiss")
                    }
                }
            ) {
                Text("Exported successfully to Downloads")
            }
        }
    }
}