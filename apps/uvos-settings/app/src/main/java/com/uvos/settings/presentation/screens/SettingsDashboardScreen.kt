package com.uvos.settings.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uvos.settings.domain.model.AppFilterCategory
import com.uvos.settings.domain.model.SystemProfile
import com.uvos.settings.presentation.components.AppAuditItemCard
import com.uvos.settings.presentation.components.CpuFrequencyCluster
import com.uvos.settings.presentation.components.InstrumentTile
import com.uvos.settings.presentation.components.M4200DialGauge
import com.uvos.settings.presentation.components.M4200ToggleSwitch
import com.uvos.settings.presentation.contract.SettingsIntent
import com.uvos.settings.presentation.contract.SettingsState
import com.uvos.settings.presentation.theme.*

@Composable
fun SettingsDashboardScreen(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
    onNavigateToProfile: (SystemProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(UvosBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Консольный заголовок
        item {
            Column {
                Text(
                    text = "UVOS КОНСОЛЬ УПРАВЛЕНИЯ",
                    color = UvosTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "Штабной пульт настройки платформы UnderVoltOS",
                    color = UvosTextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "HARDWARE: SNAPDRAGON 685 (SM6225) • 8 CORES • LINUX 5.15",
                    color = Color(0xFF4C5268),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Блок 1: Глобальный трёхпозиционный переключатель профилей
        item {
            M4200ToggleSwitch(
                currentProfile = state.currentProfile,
                onProfileSelected = { profile ->
                    onIntent(SettingsIntent.SelectProfile(profile))
                }
            )
        }

        // Блок 1b: Быстрый переход к экранам детальной настройки каждого профиля
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProfileQuickCard(
                    profile = SystemProfile.STERILE,
                    isActive = state.currentProfile == SystemProfile.STERILE,
                    accentColor = UvosRed,
                    onClick = { onNavigateToProfile(SystemProfile.STERILE) },
                    modifier = Modifier.weight(1f)
                )
                ProfileQuickCard(
                    profile = SystemProfile.COMMUNICATION,
                    isActive = state.currentProfile == SystemProfile.COMMUNICATION,
                    accentColor = UvosAmberLamp,
                    onClick = { onNavigateToProfile(SystemProfile.COMMUNICATION) },
                    modifier = Modifier.weight(1f)
                )
                ProfileQuickCard(
                    profile = SystemProfile.CUSTOM,
                    isActive = state.currentProfile == SystemProfile.CUSTOM,
                    accentColor = UvosGreenLamp,
                    onClick = { onNavigateToProfile(SystemProfile.CUSTOM) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Блок 2: Аппаратная телеметрия М4200 (Вольтметр и Температура SoC)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Вольтметр батареи
                M4200DialGauge(
                    value = state.batteryVoltageMv.toFloat(),
                    minValue = 3400f,
                    maxValue = 4450f,
                    unitTitle = "mV",
                    deviceLabel = "ВОЛЬТЫ АКБ",
                    modifier = Modifier.weight(1f),
                    dangerThreshold = 4350f
                )

                // Температура SoC Snapdragon 685
                M4200DialGauge(
                    value = state.socTemperatureC,
                    minValue = 20f,
                    maxValue = 85f,
                    unitTitle = "°C",
                    deviceLabel = "T(SOC) SM6225",
                    modifier = Modifier.weight(1f),
                    dangerThreshold = 65f
                )
            }
        }

        // Блок 2b: Дополнительные показатели батареи
        item {
            InstrumentTile(title = "СОСТОЯНИЕ ПИТАНИЯ (АКБ 5000 MAH)") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "ТОК ПОТРЕБЛЕНИЯ",
                            color = UvosTextSecondary,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${state.batteryCurrentMa} mA",
                            color = if (state.batteryCurrentMa > 0) Color(0xFF00FF66) else UvosAmberLamp,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "ТЕМПЕРАТУРА АКБ",
                            color = UvosTextSecondary,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${"%.1f".format(state.batteryTemperatureC)} °C",
                            color = if (state.batteryTemperatureC > 45f) UvosRed else UvosTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "НАПРЯЖЕНИЕ",
                            color = UvosTextSecondary,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${"%.2f".format(state.batteryVoltageMv / 1000f)} V",
                            color = UvosTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Блок 3: Тактовые частоты 8 ядер процессора SM6225
        item {
            CpuFrequencyCluster(
                coreFrequenciesMhz = state.coreFrequenciesMhz,
                cpuLoadPercent = state.cpuTotalLoadPercent
            )
        }

        // Блок 4: Аудит и изоляция приложений
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "РЕВИЗИЯ И АУДИТ ПРИЛОЖЕНИЙ",
                        color = UvosTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1E2230))
                            .border(1.dp, Color(0xFF33384D), RoundedCornerShape(6.dp))
                            .clickable { onIntent(SettingsIntent.RefreshAppsList) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (state.isAuditScanning) "СКАНИРОВАНИЕ..." else "ОБНОВИТЬ",
                            color = if (state.isAuditScanning) UvosAmberLamp else UvosTextSecondary,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Фильтр категорий
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AppFilterCategory.entries.forEach { category ->
                        val isSelected = state.appFilter == category
                        val categoryLabel = when (category) {
                            AppFilterCategory.ALL -> "ВСЕ"
                            AppFilterCategory.SYSTEM -> "СИСТЕМА"
                            AppFilterCategory.USER -> "ЮЗЕР"
                            AppFilterCategory.ISOLATED -> "ИЗОЛЯЦИЯ"
                            AppFilterCategory.THREATS -> "ОПАСНО"
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) Color(0xFF282D3E) else Color(0xFF14161E))
                                .border(1.dp, if (isSelected) UvosTextPrimary else Color(0xFF242735), RoundedCornerShape(6.dp))
                                .clickable { onIntent(SettingsIntent.FilterApps(category)) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = categoryLabel,
                                color = if (isSelected) UvosTextPrimary else UvosTextSecondary,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // Список приложений
        if (state.installedApps.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (state.isAuditScanning) "Сканирование пакетов системы..." else "Нет приложений в данной категории",
                        color = UvosTextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        } else {
            items(state.installedApps, key = { it.packageName }) { app ->
                AppAuditItemCard(
                    app = app,
                    onToggleIsolation = { pkg, isolate ->
                        onIntent(SettingsIntent.ToggleAppIsolation(pkg, isolate))
                    },
                    onTerminateApp = { pkg ->
                        onIntent(SettingsIntent.TerminateApp(pkg))
                    }
                )
            }
        }
    }
}

@Composable
private fun ProfileQuickCard(
    profile: SystemProfile,
    isActive: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isActive) Color(0xFF1B1E29) else Color(0xFF14161E))
            .border(1.dp, if (isActive) accentColor else Color(0xFF242735), RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(if (isActive) accentColor else Color(0xFF33384D), CircleShape)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = profile.displayName,
            color = if (isActive) UvosTextPrimary else UvosTextSecondary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "НАСТРОЙКИ →",
            color = if (isActive) accentColor else Color(0xFF4C5268),
            fontSize = 7.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
