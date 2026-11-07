package com.uvos.notes.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.uvos.notes.ui.components.TextVolumeGauge
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotesListScreen(
    viewModel: NotesViewModel,
    onNoteClick: (Note) -> Unit,
    onCreateNote: (isEncrypted: Boolean) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = UVOSBackground,
        floatingActionButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Quick New Encrypted Note
                FloatingActionButton(
                    onClick = { onCreateNote(true) },
                    containerColor = UVOSRed,
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(4.dp),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "New Vault Note",
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Quick New Plain Markdown Note
                FloatingActionButton(
                    onClick = { onCreateNote(false) },
                    containerColor = UVOSAmber,
                    contentColor = UVOSBackground,
                    elevation = FloatingActionButtonDefaults.elevation(4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Markdown Note"
                        )
                        Text(
                            text = ".MD ЗАМЕТКА",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Header: Title & Retro Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "UVOS // NOTES",
                        color = UVOSAmber,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "100% OFFLINE  •  AES-256 VAULT",
                        color = UVOSTextMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF1B2A1E))
                        .border(1.dp, UVOSGreen, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "AIR-GAPPED",
                        color = UVOSGreen,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Search Bar (FTS4)
            SearchBar(
                query = state.searchQuery,
                onQueryChange = { viewModel.onSearchQueryChanged(it) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Folder Filter Chips
            FolderChipsRow(
                folders = listOf("Все", "General", "Work", "Vault"),
                selected = state.selectedFolder ?: "Все",
                onSelect = { viewModel.onSelectFolder(if (it == "Все") null else it) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Note List
            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = UVOSAmber)
                }
            } else if (state.notes.isEmpty()) {
                EmptyNotesView(hasSearch = state.searchQuery.isNotBlank())
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(state.notes, key = { it.id + it.filePath }) { note ->
                        NoteCard(
                            note = note,
                            onClick = {
                                if (note.isEncrypted) {
                                    viewModel.unlockNote(note) { unlockedNote ->
                                        onNoteClick(unlockedNote)
                                    }
                                } else {
                                    onNoteClick(note)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        textStyle = TextStyle(
            color = UVOSTextPrimary,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace
        ),
        cursorBrush = SolidColor(UVOSAmber),
        singleLine = true,
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(UVOSPanel)
                    .border(1.dp, UVOSBorder, RoundedCornerShape(6.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = UVOSTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text = "FTS4 Полнотекстовый поиск...",
                            color = UVOSTextMuted,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    innerTextField()
                }
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = { onQueryChange("") },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = UVOSTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun FolderChipsRow(
    folders: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (folder in folders) {
            val isSelected = selected.equals(folder, ignoreCase = true)
            val isVault = folder.equals("Vault", ignoreCase = true) || folder.equals("Сейф", ignoreCase = true)

            val chipBg = when {
                isSelected && isVault -> UVOSRed
                isSelected -> UVOSAmber
                else -> UVOSSurface
            }
            val textColor = when {
                isSelected -> UVOSBackground
                isVault -> UVOSRed
                else -> UVOSTextSecondary
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(chipBg)
                    .border(
                        1.dp,
                        if (isSelected) chipBg else UVOSBorder,
                        RoundedCornerShape(4.dp)
                    )
                    .clickable { onSelect(folder) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isVault) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Vault",
                        tint = textColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = folder.uppercase(),
                    color = textColor,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun NoteCard(
    note: Note,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    val dateStr = dateFormat.format(Date(note.modifiedAt))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(UVOSSurface)
            .border(
                1.dp,
                if (note.isEncrypted) UVOSRed.copy(alpha = 0.5f) else UVOSBorder,
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (note.isEncrypted) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Encrypted",
                        tint = UVOSRed,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = note.title,
                    color = UVOSTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (note.isEncrypted) UVOSRed.copy(alpha = 0.2f) else UVOSPanel)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (note.isEncrypted) "VAULT" else note.folder.uppercase(),
                    color = if (note.isEncrypted) UVOSRed else UVOSCyan,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Snippet
        val previewSnippet = when {
            note.isEncrypted -> "[Зашифровано аппаратным ключом AES-256-GCM]"
            note.snippet != null -> note.snippet
            else -> note.content.take(120).replace("\n", " ")
        }

        Text(
            text = previewSnippet,
            color = if (note.isEncrypted) UVOSRed.copy(alpha = 0.8f) else UVOSTextSecondary,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 2,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = dateStr,
                color = UVOSTextMuted,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!note.isEncrypted) {
                    Text(
                        text = "${note.wordCount} сл. / ${note.charCount} симв.",
                        color = UVOSTextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    text = if (note.isEncrypted) ".uvsec" else ".md",
                    color = if (note.isEncrypted) UVOSRed else UVOSAmber,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun EmptyNotesView(hasSearch: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 80.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = if (hasSearch) Icons.Default.Search else Icons.Default.Description,
                contentDescription = null,
                tint = UVOSTextMuted,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = if (hasSearch) "НЕТ СОВПАДЕНИЙ В FTS4" else "НЕТ ЗАМЕТОК",
                color = UVOSTextSecondary,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                text = if (hasSearch) "Попробуйте изменить поисковый запрос" else "Нажмите кнопку ниже для создания .md файла",
                color = UVOSTextMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
        }
    }
}
