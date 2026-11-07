package com.uvos.notes.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uvos.notes.domain.model.Note
import com.uvos.notes.ui.components.TextVolumeGaugeCard
import com.uvos.notes.ui.theme.UVOSAmber
import com.uvos.notes.ui.theme.UVOSBackground
import com.uvos.notes.ui.theme.UVOSBorder
import com.uvos.notes.ui.theme.UVOSCyan
import com.uvos.notes.ui.theme.UVOSGreen
import com.uvos.notes.ui.theme.UVOSPanel
import com.uvos.notes.ui.theme.UVOSRed
import com.uvos.notes.ui.theme.UVOSSurface
import com.uvos.notes.ui.theme.UVOSTextMuted
import com.uvos.notes.ui.theme.UVOSTextPrimary
import com.uvos.notes.ui.theme.UVOSTextSecondary

enum class EditorTab {
    EDIT, PREVIEW
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkdownEditorScreen(
    note: Note,
    onSave: (Note) -> Unit,
    onDelete: (Note) -> Unit,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf(note.title) }
    var content by remember { mutableStateOf(note.content) }
    var isEncrypted by remember { mutableStateOf(note.isEncrypted) }
    var folder by remember { mutableStateOf(note.folder) }
    var currentTab by remember { mutableStateOf(EditorTab.EDIT) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val wordCount = remember(content) {
        if (content.isBlank()) 0 else content.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size
    }
    val charCount = remember(content) { content.length }

    Scaffold(
        containerColor = UVOSBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = UVOSPanel,
                    titleContentColor = UVOSTextPrimary
                ),
                navigationIcon = {
                    IconButton(onClick = {
                        val updated = note.copy(
                            title = title.ifBlank { "Без названия" },
                            content = content,
                            isEncrypted = isEncrypted,
                            folder = if (isEncrypted) "Vault" else folder
                        )
                        onSave(updated)
                        onBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = UVOSAmber
                        )
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isEncrypted) "СЕЙФ: AES-256" else "MARKDOWN",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isEncrypted) UVOSRed else UVOSAmber
                        )
                    }
                },
                actions = {
                    // Toggle Tab Button (Edit / Preview)
                    IconButton(onClick = {
                        currentTab = if (currentTab == EditorTab.EDIT) EditorTab.PREVIEW else EditorTab.EDIT
                    }) {
                        Icon(
                            imageVector = if (currentTab == EditorTab.EDIT) Icons.Default.Visibility else Icons.Default.Edit,
                            contentDescription = "Toggle Preview",
                            tint = UVOSCyan
                        )
                    }

                    // Save
                    IconButton(onClick = {
                        val updated = note.copy(
                            title = title.ifBlank { "Без названия" },
                            content = content,
                            isEncrypted = isEncrypted,
                            folder = if (isEncrypted) "Vault" else folder
                        )
                        onSave(updated)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save",
                            tint = UVOSGreen
                        )
                    }

                    // Delete
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = UVOSTextSecondary
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            // Analog M4200 Volumeter Gauge
            TextVolumeGaugeCard(
                wordCount = wordCount,
                charCount = charCount,
                maxTargetWords = 2000,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Encryption Switch & Folder Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(UVOSSurface)
                    .border(1.dp, UVOSBorder, RoundedCornerShape(6.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isEncrypted) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = "Security",
                        tint = if (isEncrypted) UVOSRed else UVOSTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (isEncrypted) "Сейф включен (AES-256-GCM)" else "Открытый Markdown (.md)",
                        color = if (isEncrypted) UVOSRed else UVOSTextSecondary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium
                    )
                }

                Switch(
                    checked = isEncrypted,
                    onCheckedChange = { isEncrypted = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = UVOSRed,
                        checkedTrackColor = UVOSRed.copy(alpha = 0.3f),
                        uncheckedThumbColor = UVOSTextMuted,
                        uncheckedTrackColor = UVOSBorder
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title input
            BasicTextField(
                value = title,
                onValueChange = { title = it },
                textStyle = TextStyle(
                    color = UVOSTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                ),
                cursorBrush = SolidColor(UVOSAmber),
                singleLine = true,
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(UVOSPanel)
                            .border(1.dp, UVOSBorder, RoundedCornerShape(6.dp))
                            .padding(12.dp)
                    ) {
                        if (title.isEmpty()) {
                            Text(
                                text = "Название заметки...",
                                color = UVOSTextMuted,
                                fontSize = 18.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        innerTextField()
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Markdown Quick Tools Bar (When in Edit tab)
            if (currentTab == EditorTab.EDIT) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    QuickToolButton("# H1") { content = insertMarkdown(content, "# ") }
                    QuickToolButton("## H2") { content = insertMarkdown(content, "## ") }
                    QuickToolButton("**B**") { content = wrapMarkdown(content, "**", "**") }
                    QuickToolButton("*I*") { content = wrapMarkdown(content, "*", "*") }
                    QuickToolButton("`Code`") { content = wrapMarkdown(content, "`", "`") }
                    QuickToolButton("```Block") { content = insertCodeBlock(content) }
                    QuickToolButton("- [ ] Task") { content = insertMarkdown(content, "- [ ] ") }
                    QuickToolButton("> Quote") { content = insertMarkdown(content, "> ") }
                    QuickToolButton("- List") { content = insertMarkdown(content, "- ") }
                    QuickToolButton("--- Line") { content = insertMarkdown(content, "\n---\n") }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Editor / Preview Body
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(UVOSPanel)
                    .border(1.dp, UVOSBorder, RoundedCornerShape(6.dp))
                    .padding(12.dp)
            ) {
                if (currentTab == EditorTab.EDIT) {
                    BasicTextField(
                        value = content,
                        onValueChange = { content = it },
                        modifier = Modifier.fillMaxSize(),
                        textStyle = TextStyle(
                            color = UVOSTextPrimary,
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        cursorBrush = SolidColor(UVOSAmber),
                        decorationBox = { innerTextField ->
                            if (content.isEmpty()) {
                                Text(
                                    text = "Начните ввод текста на Markdown...\n\nПоддерживается синтаксис:\n# Заголовок\n- [ ] Чеклист\n```kotlin\nval x = 42\n```\n> Цитата",
                                    color = UVOSTextMuted,
                                    fontSize = 14.sp,
                                    lineHeight = 22.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            innerTextField()
                        }
                    )
                } else {
                    MarkwonRenderer(
                        markdown = content,
                        modifier = Modifier.fillMaxSize(),
                        onToggleChecklist = { lineIndex, newChecked ->
                            val lines = content.lines().toMutableList()
                            if (lineIndex in lines.indices) {
                                val oldLine = lines[lineIndex]
                                val replacement = if (newChecked) "- [x] " else "- [ ] "
                                lines[lineIndex] = oldLine.replaceFirst(Regex("^[-*]\\s*\\[[ xX]\\]\\s*"), replacement)
                                content = lines.joinToString("\n")
                            }
                        }
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    text = "Удалить заметку?",
                    color = UVOSRed,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Файл заметки будет безвозвратно удален с локального накопителя.",
                    color = UVOSTextPrimary,
                    fontFamily = FontFamily.Monospace
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete(note)
                    onBack()
                }) {
                    Text("Удалить", color = UVOSRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Отмена", color = UVOSTextSecondary)
                }
            },
            containerColor = UVOSSurface
        )
    }
}

@Composable
private fun QuickToolButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF242738))
            .border(1.dp, Color(0xFF383C52), RoundedCornerShape(4.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .clickable { onClick() }
    ) {
        Text(
            text = label,
            color = UVOSAmber,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )
    }
}

private fun insertMarkdown(content: String, prefix: String): String {
    return if (content.endsWith("\n") || content.isEmpty()) {
        content + prefix
    } else {
        content + "\n" + prefix
    }
}

private fun wrapMarkdown(content: String, startTag: String, endTag: String): String {
    return "$content$startTag$endTag"
}

private fun insertCodeBlock(content: String): String {
    val block = "\n```\n\n```\n"
    return content + block
}
