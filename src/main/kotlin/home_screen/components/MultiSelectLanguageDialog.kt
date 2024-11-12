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
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import common_components.HorizontalSpacer
import domain.model.LanguageModel
import org.koin.compose.koinInject

@Composable
fun MultiSelectLanguageDialog(
    availableLanguages: List<LanguageModel>,
    selectedLanguages: List<LanguageModel>,
    onDismiss: () -> Unit,
    onConfirm: (List<LanguageModel>) -> Unit
) {
    val tempSelected: SnapshotStateList<LanguageModel> =
        remember(selectedLanguages) { selectedLanguages.toMutableStateList() }

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

                    }) {
                        Text("Select Popular")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.fillMaxHeight().weight(1f)) {
                    items(availableLanguages) { language ->
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
}