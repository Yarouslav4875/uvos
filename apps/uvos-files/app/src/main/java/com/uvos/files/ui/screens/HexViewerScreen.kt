package com.uvos.files.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uvos.files.ui.theme.HexAsciiColor
import com.uvos.files.ui.theme.HexByteColor
import com.uvos.files.ui.theme.HexOffsetColor
import com.uvos.files.ui.theme.IndustrialAccent
import com.uvos.files.ui.theme.IndustrialTextPrimary
import com.uvos.files.ui.theme.IndustrialTextSecondary
import com.uvos.files.ui.theme.LampGreen
import com.uvos.files.ui.theme.UvosBg
import com.uvos.files.ui.theme.UvosBorder
import com.uvos.files.ui.theme.UvosCard
import com.uvos.files.ui.theme.UvosPanel
import com.uvos.files.ui.viewmodel.HexEditorViewModel

@Composable
fun HexViewerScreen(
    viewModel: HexEditorViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    var editDialogOffset by remember { mutableStateOf<Long?>(null) }
    var editByteInput by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(UvosBg)
            .padding(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(UvosPanel, RoundedCornerShape(12.dp))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Close", tint = IndustrialTextPrimary)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.fileItem?.name ?: "Hex Viewer",
                    color = IndustrialTextPrimary,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Offset: 0x%08X / 0x%08X (${state.totalSize} B)".format(state.currentOffset, state.totalSize),
                    color = IndustrialTextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            if (state.modifiedBytes.isNotEmpty()) {
                Button(
                    onClick = { viewModel.saveModifications() },
                    colors = ButtonDefaults.buttonColors(containerColor = IndustrialAccent),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save (${state.modifiedBytes.size})", fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Table Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(UvosCard, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text(
                text = "OFFSET  ",
                color = HexOffsetColor,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "00 01 02 03 04 05 06 07  08 09 0A 0B 0C 0D 0E 0F",
                color = HexByteColor,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "ASCII",
                color = HexAsciiColor,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        // Virtualized Hex rows
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(UvosPanel, RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                .padding(8.dp)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = IndustrialAccent
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.lines, key = { it.offset }) { line ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    editDialogOffset = line.offset
                                    editByteInput = "%02X".format(line.bytes.firstOrNull() ?: 0)
                                }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = line.offsetHex,
                                color = HexOffsetColor,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "${line.hexGroup1}  ${line.hexGroup2}",
                                color = HexByteColor,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = line.asciiString,
                                color = HexAsciiColor,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Paging Footer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(UvosPanel, RoundedCornerShape(12.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.previousPage() },
                enabled = state.currentOffset > 0
            ) {
                Icon(Icons.Default.ChevronLeft, "Prev", tint = IndustrialTextPrimary)
            }

            Text(
                text = "Block: ${state.currentOffset} - ${minOf(state.currentOffset + state.pageSize, state.totalSize)}",
                color = IndustrialTextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )

            IconButton(
                onClick = { viewModel.nextPage() },
                enabled = state.currentOffset + state.pageSize < state.totalSize
            ) {
                Icon(Icons.Default.ChevronRight, "Next", tint = IndustrialTextPrimary)
            }
        }

        // Status / Error message
        state.statusMessage?.let {
            Text(
                text = it,
                color = LampGreen,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }

    // Byte Edit Dialog
    editDialogOffset?.let { offset ->
        AlertDialog(
            onDismissRequest = { editDialogOffset = null },
            title = {
                Text(
                    "Изменить байт (0x%08X)".format(offset),
                    fontFamily = FontFamily.Monospace,
                    color = IndustrialTextPrimary
                )
            },
            text = {
                OutlinedTextField(
                    value = editByteInput,
                    onValueChange = { if (it.length <= 2) editByteInput = it.uppercase() },
                    label = { Text("HEX значение (00..FF)") }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val byteVal = editByteInput.toIntOrNull(16)?.toByte()
                        if (byteVal != null) {
                            viewModel.editByte(offset, byteVal)
                        }
                        editDialogOffset = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IndustrialAccent)
                ) {
                    Text("Применить")
                }
            },
            dismissButton = {
                TextButton(onClick = { editDialogOffset = null }) {
                    Text("Отмена", color = IndustrialTextSecondary)
                }
            },
            containerColor = UvosCard
        )
    }
}
