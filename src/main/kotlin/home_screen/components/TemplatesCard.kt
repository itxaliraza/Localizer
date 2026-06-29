package home_screen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import common_components.EditText
import common_components.HorizontalSpacer
import common_components.ImageButtons
import common_components.VerticalSpacer
import data.model.LanguageTemplate
import home_screen.HomeScreenState
import home_screen.HomeScreenViewModel
import theme.GreenColor
import theme.LightPrimary
import theme.PrimaryColor
import theme.ScreenColor

/**
 * In-app language templates: save the current selection as a named, reusable set and apply it
 * later in one click. Replaces the old import/export-to-JSON flow. Self-contained — owns its
 * "Save as template" and delete-confirm dialogs; talks to [viewModel] for persistence and reports
 * user-facing outcomes through [onMessage] (rendered as a snackbar by the parent).
 */
@Composable
fun TemplatesCard(
    state: HomeScreenState,
    viewModel: HomeScreenViewModel,
    onMessage: (String) -> Unit,
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var templateToDelete by remember { mutableStateOf<LanguageTemplate?>(null) }

    val selectedCount = state.selectedLanguages.size
    val currentCodes = remember(state.selectedLanguages) {
        state.selectedLanguages.map { it.langCode }.toSet()
    }

    RectangleWithShadow(
        bgColor = PrimaryColor,
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp).wrapContentHeight()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {

            // Header: title + contextual "Save" action (enabled only when languages are selected).
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Language Templates",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Reusable sets of languages",
                        color = Color(0xff7fa6b0),
                        fontSize = 11.sp,
                    )
                }
                SavePill(
                    enabled = selectedCount > 0,
                    onClick = { showCreateDialog = true }
                )
            }

            VerticalSpacer(10)

            if (state.templates.isEmpty()) {
                EmptyTemplates(hasSelection = selectedCount > 0)
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .heightIn(max = 210.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.templates.forEach { template ->
                        TemplateRow(
                            template = template,
                            isActive = template.langCodes.toSet() == currentCodes && currentCodes.isNotEmpty(),
                            onApply = {
                                viewModel.applyTemplate(template)
                                onMessage("Applied “${template.name}” — ${template.langCodes.size} languages")
                            },
                            onDelete = { templateToDelete = template }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateTemplateDialog(
            languageCount = selectedCount,
            defaultName = "Template ${state.templates.size + 1}",
            onDismiss = { showCreateDialog = false },
            onSave = { name ->
                viewModel.createTemplate(name)
                showCreateDialog = false
                onMessage("Saved template “$name”")
            }
        )
    }

    templateToDelete?.let { target ->
        ConfirmDeleteDialog(
            templateName = target.name,
            onCancel = { templateToDelete = null },
            onConfirm = {
                viewModel.deleteTemplate(target.id)
                templateToDelete = null
                onMessage("Deleted “${target.name}”")
            }
        )
    }
}

/** Small green pill button used to trigger "save current selection as a template". */
@Composable
private fun SavePill(enabled: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (enabled) GreenColor else LightPrimary)
            .let { if (enabled) it.clickable { onClick() } else it }
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ImageButtons(
            icon = Icons.Default.Add,
            size = 18,
            tint = if (enabled) Color.White else Color(0xff7fa6b0),
            onClick = { if (enabled) onClick() }
        )
        Text(
            text = "Save",
            color = if (enabled) Color.White else Color(0xff7fa6b0),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun EmptyTemplates(hasSelection: Boolean) {
    Box(
        modifier = Modifier.fillMaxWidth()
            .background(ScreenColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (hasSelection)
                "Tap Save to store your selected languages as a reusable template."
            else
                "No templates yet. Select some languages, then tap Save to keep them for next time.",
            color = Color(0xff9fb9c1),
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun TemplateRow(
    template: LanguageTemplate,
    isActive: Boolean,
    onApply: () -> Unit,
    onDelete: () -> Unit,
) {
    val preview = remember(template) {
        val codes = template.langCodes
        val head = codes.take(6).joinToString(", ")
        if (codes.size > 6) "$head  +${codes.size - 6}" else head
    }
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(ScreenColor, RoundedCornerShape(8.dp))
            .then(
                if (isActive) Modifier.border(1.dp, GreenColor, RoundedCornerShape(8.dp))
                else Modifier
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isActive) {
                    Box(
                        modifier = Modifier.size(7.dp).clip(CircleShape).background(GreenColor)
                    )
                    HorizontalSpacer(6)
                }
                Text(
                    text = template.name,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                HorizontalSpacer(8)
                Text(
                    text = "${template.langCodes.size} langs",
                    color = Color(0xff03b6fc),
                    fontSize = 11.sp,
                )
            }
            VerticalSpacer(3)
            Text(
                text = preview.ifBlank { "no languages" },
                color = Color(0xff7fa6b0),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        HorizontalSpacer(8)
        Text(
            text = if (isActive) "Active" else "Apply",
            color = if (isActive) GreenColor else Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .let { if (isActive) it else it.clickable { onApply() } }
                .border(
                    1.dp,
                    if (isActive) Color.Transparent else GreenColor,
                    RoundedCornerShape(6.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
        HorizontalSpacer(4)
        ImageButtons(
            icon = Icons.Default.Delete,
            size = 22,
            tint = Color(0xffe07a7a),
            color = Color.Red,
            onClick = onDelete
        )
    }
}

@Composable
private fun CreateTemplateDialog(
    languageCount: Int,
    defaultName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var name by remember { mutableStateOf(defaultName) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = PrimaryColor,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.width(420.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Text(
                    text = "Save as template",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                VerticalSpacer(4)
                Text(
                    text = "Storing $languageCount selected ${if (languageCount == 1) "language" else "languages"}.",
                    color = Color(0xff9fb9c1),
                    fontSize = 12.sp,
                )
                VerticalSpacer(14)
                Text(text = "Template name", color = Color.White, fontSize = 12.sp)
                VerticalSpacer(4)
                EditText(
                    value = name,
                    hint = "e.g. Europe, RTL, Top markets…",
                    onValueChange = { name = it }
                )
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
                    TextButton(
                        enabled = name.isNotBlank() && languageCount > 0,
                        onClick = { onSave(name.trim()) },
                        colors = ButtonDefaults.buttonColors(GreenColor)
                    ) {
                        Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfirmDeleteDialog(
    templateName: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    Dialog(onDismissRequest = onCancel) {
        Surface(
            color = PrimaryColor,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.width(380.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Text(
                    text = "Delete template?",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                VerticalSpacer(8)
                Text(
                    text = "“$templateName” will be removed. This can't be undone.",
                    color = Color(0xff9fb9c1),
                    fontSize = 12.sp,
                )
                VerticalSpacer(18)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onCancel) {
                        Text("Cancel", color = Color(0xff9fb9c1))
                    }
                    HorizontalSpacer(8)
                    TextButton(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(Color(0xffe13d3d))
                    ) {
                        Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
