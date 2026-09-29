package home_screen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import common_components.AppIcons
import common_components.EditText
import common_components.HorizontalSpacer
import common_components.PillButton
import common_components.PillStyle
import common_components.VerticalSpacer
import domain.model.LanguageModel
import home_screen.HomeScreenViewModel
import theme.AppTextSelectionColors
import theme.GreenColor
import theme.MutedTextColor
import theme.PrimaryColor
import theme.ScreenColor
import theme.WarningColor
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

/**
 * Import a language list from a file or pasted text. The list may contain anything (names, extra
 * columns, JSON, XML); only the language codes are kept. Shows a live preview of what was found,
 * lets the user drop individual languages, then either selects them or saves them as a template.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImportLanguagesDialog(
    viewModel: HomeScreenViewModel,
    defaultName: String,
    onDismiss: () -> Unit,
    onSelect: (codes: List<String>) -> Unit,
    onSaveTemplate: (name: String, codes: List<String>) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf<String?>(null) }
    var fileError by remember { mutableStateOf<String?>(null) }
    var name by remember { mutableStateOf(defaultName) }
    var excluded by remember { mutableStateOf(emptySet<String>()) }

    val result = remember(text) { viewModel.parseLanguageList(text) }
    val chosen = result.matched.filter { it.langCode !in excluded }
    val chosenCodes = chosen.map { it.langCode }

    fun chooseFile() {
        val path = pickLanguageListFile() ?: return
        val file = File(path)
        if (file.extension.lowercase() in setOf("xlsx", "xls", "ods", "numbers")) {
            fileError = "Spreadsheet files can't be read directly. Save the sheet as CSV and import that."
            return
        }
        val content = viewModel.readLanguageListFile(path)
        if (content == null) {
            fileError = "Couldn't read “${file.name}”. Choose a text file under 512 KB."
            return
        }
        fileError = null
        fileName = file.name
        text = content
        excluded = emptySet()
        name = file.nameWithoutExtension.ifBlank { defaultName }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = PrimaryColor,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.width(560.dp).padding(vertical = 16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(22.dp)) {
                Text("Import languages", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                VerticalSpacer(4)
                Text(
                    "Choose a file or paste a list. Only language codes and names are picked up. " +
                            "Everything else is ignored.",
                    color = Color(0xff9fb9c1),
                    fontSize = 12.sp,
                )

                // Everything between the title and the buttons scrolls, so a long preview can never push
                // the action buttons out of a small window.
                Column(
                    modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                ) {
                    VerticalSpacer(14)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PillButton(
                            text = "Choose file…",
                            icon = AppIcons.Upload,
                            style = PillStyle.Secondary,
                            onClick = ::chooseFile
                        )
                        HorizontalSpacer(10)
                        Text(
                            text = fileName ?: ".txt, .csv, .json or .xml, or paste below",
                            color = if (fileName != null) Color.White else MutedTextColor,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    fileError?.let {
                        VerticalSpacer(6)
                        Text(it, color = WarningColor, fontSize = 11.sp)
                    }

                    VerticalSpacer(10)
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Language list",
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        if (text.isNotEmpty()) {
                            Text(
                                "Clear",
                                color = Color(0xff03b6fc),
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable {
                                        text = ""
                                        fileName = null
                                        excluded = emptySet()
                                    }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                    VerticalSpacer(4)
                    OutlinedTextField(
                        value = text,
                        onValueChange = {
                            text = it
                            fileName = null
                        },
                        modifier = Modifier.fillMaxWidth().height(110.dp),
                        textStyle = TextStyle(color = Color.White, fontSize = 12.sp),
                        placeholder = {
                            Text(
                                "fr, de, pt-BR\nvalues-es\nJapanese, 한국어",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GreenColor,
                            unfocusedBorderColor = ScreenColor,
                            unfocusedContainerColor = ScreenColor,
                            focusedContainerColor = ScreenColor,
                            cursorColor = Color.White,
                            selectionColors = AppTextSelectionColors
                        ),
                    )

                    if (text.isNotBlank()) {
                        VerticalSpacer(14)
                        ImportPreview(
                            matched = result.matched,
                            unrecognized = result.unrecognized,
                            excluded = excluded,
                            chosenCount = chosen.size,
                            onToggle = { code ->
                                excluded = if (code in excluded) excluded - code else excluded + code
                            }
                        )
                    }

                    VerticalSpacer(14)
                    Text("Template name", color = Color.White, fontSize = 12.sp)
                    VerticalSpacer(4)
                    EditText(value = name, hint = "e.g. Play Store top 20", onValueChange = { name = it })
                }

                VerticalSpacer(18)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color(0xff9fb9c1))
                    }
                    HorizontalSpacer(8)
                    PillButton(
                        text = "Select these",
                        style = PillStyle.Secondary,
                        enabled = chosen.isNotEmpty(),
                        onClick = { onSelect(chosenCodes) }
                    )
                    HorizontalSpacer(8)
                    PillButton(
                        text = "Save as template",
                        enabled = chosen.isNotEmpty() && name.isNotBlank(),
                        onClick = { onSaveTemplate(name.trim(), chosenCodes) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ImportPreview(
    matched: List<LanguageModel>,
    unrecognized: List<String>,
    excluded: Set<String>,
    chosenCount: Int,
    onToggle: (String) -> Unit,
) {
    if (matched.isEmpty()) {
        Text(
            "No languages found. Use codes like fr or pt-BR, folder names like values-fr, or names like French.",
            color = WarningColor,
            fontSize = 12.sp,
        )
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "$chosenCount ${if (chosenCount == 1) "language" else "languages"} found",
                color = Color(0xff9be870),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
            HorizontalSpacer(8)
            Text("Click a language to leave it out", color = MutedTextColor, fontSize = 11.sp)
        }
        VerticalSpacer(8)
        FlowRow(
            modifier = Modifier.fillMaxWidth().heightIn(max = 130.dp).verticalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            matched.forEach { lang ->
                LanguageChip(lang, isExcluded = lang.langCode in excluded, onClick = { onToggle(lang.langCode) })
            }
        }
    }
    if (unrecognized.isNotEmpty()) {
        VerticalSpacer(8)
        Text(
            "Not recognised (${unrecognized.size}): ${unrecognized.joinToString(", ")}",
            color = WarningColor,
            fontSize = 11.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun LanguageChip(lang: LanguageModel, isExcluded: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(if (isExcluded) Color.Transparent else ScreenColor)
            .border(1.dp, if (isExcluded) ScreenColor else GreenColor.copy(alpha = 0.5f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val decoration = if (isExcluded) TextDecoration.LineThrough else null
        Text(
            lang.langName,
            color = if (isExcluded) MutedTextColor else Color.White,
            fontSize = 11.sp,
            textDecoration = decoration
        )
        HorizontalSpacer(5)
        Text(lang.langCode, color = if (isExcluded) MutedTextColor else Color(0xff03b6fc), fontSize = 11.sp)
    }
}

/** Native Windows file picker for a language list. Returns the chosen path, or null if cancelled. */
private fun pickLanguageListFile(): String? {
    val dialog = FileDialog(null as Frame?, "Choose a language list", FileDialog.LOAD).apply {
        file = "*.txt;*.csv;*.tsv;*.json;*.xml;*.md"
        isVisible = true
    }
    val dir = dialog.directory ?: return null
    val name = dialog.file ?: return null
    return File(dir, name).path
}
