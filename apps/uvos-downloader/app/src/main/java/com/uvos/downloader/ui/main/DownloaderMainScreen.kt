package com.uvos.downloader.ui.main

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uvos.downloader.domain.model.DownloadStatus
import com.uvos.downloader.domain.model.DownloadTask
import com.uvos.downloader.ui.gauge.ChunkProgressStripView
import com.uvos.downloader.ui.gauge.SpeedometerGauge
import com.uvos.downloader.ui.theme.GaugeNeedleRed
import com.uvos.downloader.ui.theme.IndustrialTextMuted
import com.uvos.downloader.ui.theme.IndustrialTextPrimary
import com.uvos.downloader.ui.theme.IndustrialTextSecondary
import com.uvos.downloader.ui.theme.LampAmber
import com.uvos.downloader.ui.theme.LampGreen
import com.uvos.downloader.ui.theme.LampRed
import com.uvos.downloader.ui.theme.UvosBg
import com.uvos.downloader.ui.theme.UvosBorder
import com.uvos.downloader.ui.theme.UvosBorderActive
import com.uvos.downloader.ui.theme.UvosCard
import com.uvos.downloader.ui.theme.UvosPanel
import com.uvos.downloader.ui.theme.UvosSurface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloaderMainScreen(
    viewModel: DownloaderViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val activeTasks by viewModel.activeTasks.collectAsState()
    val completedTasks by viewModel.completedTasks.collectAsState()
    val speedMetric by viewModel.speedMetric.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Active, 1 = History

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = UvosBg
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // UnderVoltOS Top Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "UVOS DOWNLOADER",
                            color = IndustrialTextPrimary,
                            fontSize = 18.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "СЕГМЕНТИРОВАННАЯ ШИНА ПЕРЕДАЧИ",
                            color = IndustrialTextSecondary,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Row(
                        modifier = Modifier
                            .background(UvosPanel, RoundedCornerShape(8.dp))
                            .border(1.dp, UvosBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(LampGreen, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ZERO TELEMETRY",
                            color = LampGreen,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // M4200 Analog Speedometer Gauge
            item {
                SpeedometerGauge(speedMetric = speedMetric)
            }

            // Quick Actions Buttons Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.setAddDialogOpen(true) },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GaugeNeedleRed),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "+ ДОБАВИТЬ URL",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = { viewModel.addSampleDownload() },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = IndustrialTextPrimary),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(UvosBorder)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "ТЕСТОВЫЙ ПОТОК",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Navigation Tabs (Active / Completed)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(UvosPanel, RoundedCornerShape(8.dp))
                        .border(1.dp, UvosBorder, RoundedCornerShape(8.dp))
                        .padding(4.dp)
                ) {
                    TabButton(
                        title = "АКТИВНЫЕ (${activeTasks.size})",
                        isSelected = selectedTab == 0,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = 0 }
                    )
                    TabButton(
                        title = "ЗАВЕРШЕННЫЕ (${completedTasks.size})",
                        isSelected = selectedTab == 1,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = 1 }
                    )
                }
            }

            // Task List Section
            if (selectedTab == 0) {
                if (activeTasks.isEmpty()) {
                    item {
                        EmptyTasksPlaceholder(
                            title = "НЕТ АКТИВНЫХ ЗАГРУЗОК",
                            subtitle = "Очередь чиста. Нажмите «+ Добавить URL» для инициализации многопоточного скачивания."
                        )
                    }
                } else {
                    items(activeTasks, key = { it.id }) { task ->
                        ActiveTaskCard(
                            task = task,
                            context = context,
                            onPause = { viewModel.pauseDownload(context, task.id) },
                            onResume = { viewModel.resumeDownload(context, task.id) },
                            onCancel = { viewModel.cancelDownload(context, task.id) }
                        )
                    }
                }
            } else {
                if (completedTasks.isEmpty()) {
                    item {
                        EmptyTasksPlaceholder(
                            title = "ИСТОРИЯ ПУСТА",
                            subtitle = "Завершенные и загруженные файлы будут отображаться здесь."
                        )
                    }
                } else {
                    items(completedTasks, key = { it.id }) { task ->
                        CompletedTaskCard(
                            task = task,
                            onDelete = { viewModel.deleteTask(task.id) }
                        )
                    }
                }
            }
        }
    }

    if (uiState.isAddDialogOpen) {
        AddDownloadDialog(
            uiState = uiState,
            onUrlChange = { viewModel.updateUrlInput(it) },
            onFileNameChange = { viewModel.updateFileName(it) },
            onThreadsChange = { viewModel.updateThreads(it) },
            onWifiChange = { viewModel.updateOnlyWifi(it) },
            onDismiss = { viewModel.setAddDialogOpen(false) },
            onConfirm = { viewModel.startNewDownload(context) }
        )
    }
}

@Composable
private fun TabButton(
    title: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) UvosCard else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (isSelected) Color.White else IndustrialTextMuted,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun ActiveTaskCard(
    task: DownloadTask,
    context: Context,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit
) {
    val isDownloading = task.status == DownloadStatus.DOWNLOADING

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(UvosPanel, RoundedCornerShape(12.dp))
            .border(1.dp, if (isDownloading) UvosBorderActive else UvosBorder, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        // Top File Title & Status Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.fileName,
                    color = IndustrialTextPrimary,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = task.url,
                    color = IndustrialTextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            StatusBadge(status = task.status)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Progress Bar
        LinearProgressIndicator(
            progress = { task.progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = if (isDownloading) GaugeNeedleRed else LampAmber,
            trackColor = Color(0xFF15171F),
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Metrics Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val downloadedMb = task.downloadedBytes / (1024f * 1024f)
            val totalMb = task.totalBytes / (1024f * 1024f)
            val sizeText = if (task.totalBytes > 0) {
                String.format(Locale.US, "%.1f / %.1f МБ (%.0f%%)", downloadedMb, totalMb, task.progress * 100)
            } else {
                String.format(Locale.US, "%.1f МБ", downloadedMb)
            }

            Text(
                text = sizeText,
                color = IndustrialTextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )

            Text(
                text = "ПОТОКОВ: ${task.numThreads}",
                color = IndustrialTextMuted,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // Chunks Breakdown Strip
        if (task.chunks.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            ChunkProgressStripView(
                chunks = task.chunks,
                isActive = isDownloading
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.height(36.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = LampRed),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(UvosBorder)),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(text = "ОТМЕНА", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isDownloading) {
                Button(
                    onClick = onPause,
                    modifier = Modifier.height(36.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = UvosCard),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(text = "ПАУЗА", color = IndustrialTextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            } else {
                Button(
                    onClick = onResume,
                    modifier = Modifier.height(36.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GaugeNeedleRed),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(text = "ВОЗОБНОВИТЬ", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CompletedTaskCard(
    task: DownloadTask,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    val dateStr = dateFormat.format(Date(task.createdAt))
    val sizeMb = task.downloadedBytes / (1024f * 1024f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(UvosPanel, RoundedCornerShape(12.dp))
            .border(1.dp, UvosBorder, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.fileName,
                    color = IndustrialTextPrimary,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$dateStr | ${String.format(Locale.US, "%.1f МБ", sizeMb)}",
                    color = IndustrialTextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.width(8.dp))
            StatusBadge(status = task.status)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Путь: ${task.destinationPath}",
            color = IndustrialTextMuted,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.height(32.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = IndustrialTextSecondary),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(UvosBorder)),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(text = "УДАЛИТЬ ИЗ СПИСКА", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
private fun StatusBadge(status: DownloadStatus) {
    val (text, color) = when (status) {
        DownloadStatus.DOWNLOADING -> "СКАЧИВАНИЕ" to LampGreen
        DownloadStatus.QUEUED -> "В ОЧЕРЕДИ" to LampAmber
        DownloadStatus.PAUSED -> "ПАУЗА" to LampAmber
        DownloadStatus.COMPLETED -> "ГОТОВО" to LampGreen
        DownloadStatus.ERROR -> "ОШИБКА" to LampRed
    }

    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun EmptyTasksPlaceholder(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(UvosPanel, RoundedCornerShape(12.dp))
            .border(1.dp, UvosBorder, RoundedCornerShape(12.dp))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(UvosCard, CircleShape)
                .border(1.dp, UvosBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.size(10.dp).background(IndustrialTextMuted, CircleShape))
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = title,
            color = IndustrialTextPrimary,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = subtitle,
            color = IndustrialTextMuted,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddDownloadDialog(
    uiState: DownloaderUiState,
    onUrlChange: (String) -> Unit,
    onFileNameChange: (String) -> Unit,
    onThreadsChange: (Int) -> Unit,
    onWifiChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = UvosPanel,
        shape = RoundedCornerShape(14.dp),
        title = {
            Text(
                text = "НОВЫЙ ПОТОК ЗАГРУЗКИ",
                color = IndustrialTextPrimary,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = uiState.urlInput,
                    onValueChange = onUrlChange,
                    label = { Text("URL ссылка для скачивания", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = IndustrialTextPrimary,
                        unfocusedTextColor = IndustrialTextPrimary,
                        focusedBorderColor = GaugeNeedleRed,
                        unfocusedBorderColor = UvosBorder
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = uiState.customFileName,
                    onValueChange = onFileNameChange,
                    label = { Text("Имя файла (опционально)", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = IndustrialTextPrimary,
                        unfocusedTextColor = IndustrialTextPrimary,
                        focusedBorderColor = GaugeNeedleRed,
                        unfocusedBorderColor = UvosBorder
                    ),
                    singleLine = true
                )

                Text(
                    text = "Количество параллельных сегментов:",
                    color = IndustrialTextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1, 2, 4, 8).forEach { count ->
                        val isSelected = uiState.numThreads == count
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) GaugeNeedleRed else UvosCard)
                                .border(1.dp, if (isSelected) GaugeNeedleRed else UvosBorder, RoundedCornerShape(6.dp))
                                .clickable { onThreadsChange(count) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$count",
                                color = if (isSelected) Color.White else IndustrialTextPrimary,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Только по Wi-Fi",
                        color = IndustrialTextPrimary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Switch(
                        checked = uiState.onlyWifi,
                        onCheckedChange = onWifiChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = GaugeNeedleRed,
                            checkedTrackColor = GaugeNeedleRed.copy(alpha = 0.4f)
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = uiState.urlInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = GaugeNeedleRed),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(text = "ЗАПУСТИТЬ", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "ОТМЕНА", color = IndustrialTextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        }
    )
}
