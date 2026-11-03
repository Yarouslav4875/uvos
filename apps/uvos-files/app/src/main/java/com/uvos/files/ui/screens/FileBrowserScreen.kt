package com.uvos.files.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uvos.files.domain.model.FileItem
import com.uvos.files.domain.model.FileType
import com.uvos.files.ui.components.BreadcrumbBar
import com.uvos.files.ui.components.DiskStorageGaugeView
import com.uvos.files.ui.theme.GaugeNeedleRed
import com.uvos.files.ui.theme.UvosBg
import com.uvos.files.ui.theme.UvosSurface
import com.uvos.files.ui.viewmodel.FilesViewModel

@Composable
fun FileBrowserScreen(
    viewModel: FilesViewModel,
    onOpenFileHex: (FileItem) -> Unit,
    onOpenEditText: (FileItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val leftState by viewModel.leftPaneState.collectAsState()
    val partitions by viewModel.storagePartitions.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(UvosBg)
            .padding(8.dp)
    ) {
        // Шапка с аналоговыми приборами свободного места M4200
        if (partitions.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                partitions.take(3).forEach { part ->
                    val usedPerc = if (part.totalBytes > 0) part.usedBytes.toFloat() / part.totalBytes else 0f
                    val freeText = formatSize(part.freeBytes)
                    val totalText = formatSize(part.totalBytes)
                    DiskStorageGaugeView(
                        title = part.label,
                        usedPercentage = usedPerc,
                        freeSpaceText = freeText,
                        totalSpaceText = totalText,
                        isMounted = part.isMounted,
                        modifier = Modifier
                            .weight(1f)
                            .height(130.dp)
                            .padding(4.dp)
                    )
                }
            }
        }

        // Хлебные крошки (Текущий путь)
        BreadcrumbBar(
            currentPath = leftState.currentPath,
            isRootActive = viewModel.isRootPath(leftState.currentPath),
            onNavigate = { path ->
                viewModel.navigateTo(isLeftPane = true, path = path)
            },
            onNavigateUp = {
                val parent = leftState.currentPath.substringBeforeLast("/", "/")
                viewModel.navigateTo(isLeftPane = true, path = if (parent.isEmpty()) "/" else parent)
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        )

        // Список файлов
        if (leftState.isLoading) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = GaugeNeedleRed)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(leftState.files, key = { it.absolutePath }) { file ->
                    FileListItemRow(
                        file = file,
                        onClick = {
                            if (file.isDirectory) {
                                viewModel.navigateTo(isLeftPane = true, path = file.absolutePath)
                            } else {
                                val isTxt = file.name.endsWith(".txt") ||
                                        file.name.endsWith(".json") ||
                                        file.name.endsWith(".xml") ||
                                        file.name.endsWith(".conf") ||
                                        file.name.endsWith(".rc") ||
                                        file.name.endsWith(".prop")
                                if (isTxt) {
                                    onOpenEditText(file)
                                } else {
                                    onOpenFileHex(file)
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FileListItemRow(
    file: FileItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .background(UvosSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (file.isDirectory) "[DIR]" else "[FILE]",
            color = if (file.isDirectory) GaugeNeedleRed else Color(0xFFE8E4D8),
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(54.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            val permStr = file.permissions?.toFormattedString() ?: "rw-r--r--"
            Text(
                text = "$permStr  |  ${formatSize(file.sizeBytes)}",
                color = Color.Gray,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}
