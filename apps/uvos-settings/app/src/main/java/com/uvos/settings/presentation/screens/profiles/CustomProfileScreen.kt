package com.uvos.settings.presentation.screens.profiles

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import com.uvos.settings.domain.model.AudioStreamType
import com.uvos.settings.domain.model.SystemFontFamily
import com.uvos.settings.domain.model.SystemProfile
import com.uvos.settings.presentation.components.InstrumentTile
import com.uvos.settings.presentation.contract.SettingsIntent
import com.uvos.settings.presentation.contract.SettingsState
import com.uvos.settings.presentation.theme.*

@Composable
fun CustomProfileScreen(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCustomActive = state.currentProfile == SystemProfile.CUSTOM

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(UvosBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Заголовок и кнопка назад
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(UvosTile)
                    .border(1.dp, UvosTileBorder, RoundedCornerShape(8.dp))
                    .clickable { onBack() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "← КОНСОЛЬ",
                    color = UvosTextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "ПРОФИЛЬ: СВОЙ",
                    color = UvosGreenLamp,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Выборочная настройка и тонкий тюнинг системы",
                    color = UvosTextSecondary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Статус баннер
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (isCustomActive) Color(0xFF082210) else Color(0xFF161820))
                .border(1.5.dp, if (isCustomActive) UvosGreenLamp else UvosDarkSurfaceBorder, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(if (isCustomActive) UvosGreenLamp else Color(0xFF4C5061), CircleShape)
                        .border(2.dp, Color(0xFF1C1E26), CircleShape)
                )
                Column {
                    Text(
                        text = if (isCustomActive) "РЕЖИМ АКТИВЕН: ПОЛЬЗОВАТЕЛЬСКИЙ ЩИТ" else "РЕЖИМ НЕАКТИВЕН",
                        color = if (isCustomActive) UvosGreenLamp else UvosTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = if (isCustomActive)
                            "Все параметры подчиняются индивидуальным переключателям ниже."
                        else
                            "Нажмите кнопку внизу для активации профиля «СВОЙ».",
                        color = UvosTextPrimary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Блок 1: Частота экрана и дисплей
        InstrumentTile(title = "ПАРАМЕТРЫ ДИСПЛЕЯ (AMOLED 120HZ)") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Частота обновления
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ЧАСТОТА ОБНОВЛЕНИЯ",
                        color = UvosTextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(60, 120).forEach { hz ->
                            val isSelected = state.refreshRateHz == hz
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) UvosGreenLamp else Color(0xFF13151C))
                                    .border(1.dp, if (isSelected) Color(0xFF00FF66) else Color(0xFF282C3C), RoundedCornerShape(6.dp))
                                    .clickable { onIntent(SettingsIntent.ChangeRefreshRate(hz)) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "$hz ГЦ",
                                    color = if (isSelected) Color(0xFF0A0B0E) else UvosTextPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                // Яркость
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "ЯРКОСТЬ ПОДСВЕТКИ",
                            color = UvosTextPrimary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${(state.screenBrightness * 100 / 255)} %",
                            color = UvosGreenLamp,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Slider(
                        value = state.screenBrightness.toFloat(),
                        onValueChange = { onIntent(SettingsIntent.ChangeBrightness(it.toInt())) },
                        valueRange = 1f..255f,
                        colors = SliderDefaults.colors(
                            thumbColor = UvosGreenLamp,
                            activeTrackColor = UvosGreenLamp,
                            inactiveTrackColor = Color(0xFF282C3C)
                        )
                    )
                }

                // Таймаут отключения
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ТАЙМАУТ ЭКРАНА",
                        color = UvosTextPrimary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(15 to "15с", 30 to "30с", 60 to "1м", 120 to "2м", 300 to "5м").forEach { (sec, label) ->
                            val isSelected = state.screenTimeoutSeconds == sec
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) UvosGreenLamp else Color(0xFF13151C))
                                    .border(1.dp, if (isSelected) Color(0xFF00FF66) else Color(0xFF282C3C), RoundedCornerShape(6.dp))
                                    .clickable { onIntent(SettingsIntent.ChangeScreenTimeout(sec)) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color(0xFF0A0B0E) else UvosTextSecondary,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }

        // Блок 2: Аудиоканалы
        InstrumentTile(title = "ДИСКРЕТНЫЕ АУДИОКАНАЛЫ") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                VolumeRow(
                    label = "РИНГТОН И ВЫЗОВЫ",
                    level = state.volumeRing,
                    maxLevel = 15,
                    onLevelChange = { onIntent(SettingsIntent.ChangeVolume(AudioStreamType.RING, it)) }
                )
                VolumeRow(
                    label = "МУЛЬТИМЕДИА",
                    level = state.volumeMedia,
                    maxLevel = 15,
                    onLevelChange = { onIntent(SettingsIntent.ChangeVolume(AudioStreamType.MEDIA, it)) }
                )
                VolumeRow(
                    label = "СИГНАЛ БУДИЛЬНИКА",
                    level = state.volumeAlarm,
                    maxLevel = 7,
                    onLevelChange = { onIntent(SettingsIntent.ChangeVolume(AudioStreamType.ALARM, it)) }
                )
            }
        }

        // Блок 3: Индустриальный шрифт
        InstrumentTile(title = "МОНОШИРИННЫЙ СИСТЕМНЫЙ ШРИФТ") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SystemFontFamily.entries.forEach { font ->
                    val isSelected = state.activeFont == font
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color(0xFF16251B) else Color(0xFF13151C))
                            .border(1.dp, if (isSelected) UvosGreenLamp else Color(0xFF242735), RoundedCornerShape(8.dp))
                            .clickable { onIntent(SettingsIntent.SelectFont(font)) }
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = font.displayName,
                                color = if (isSelected) UvosGreenLamp else UvosTextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = font.familyCode,
                                color = UvosTextSecondary,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        if (isSelected) {
                            Text(
                                text = "АКТИВЕН",
                                color = UvosGreenLamp,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // Кнопка применения / активации
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (isCustomActive) Color(0xFF0F351A) else UvosGreenLamp)
                .border(1.5.dp, if (isCustomActive) UvosGreenLamp else Color(0xFF33FF88), RoundedCornerShape(12.dp))
                .clickable {
                    onIntent(SettingsIntent.SelectProfile(SystemProfile.CUSTOM))
                }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isCustomActive) "ПОДТВЕРДИТЬ РЕЖИМ «СВОЙ»" else "АКТИВИРОВАТЬ РЕЖИМ «СВОЙ»",
                color = if (isCustomActive) Color.White else Color(0xFF0A0B0E),
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun VolumeRow(
    label: String,
    level: Int,
    maxLevel: Int,
    onLevelChange: (Int) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                color = UvosTextPrimary,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "$level / $maxLevel",
                color = UvosGreenLamp,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
        Slider(
            value = level.toFloat(),
            onValueChange = { onLevelChange(it.toInt()) },
            valueRange = 0f..maxLevel.toFloat(),
            steps = maxLevel - 1,
            colors = SliderDefaults.colors(
                thumbColor = UvosGreenLamp,
                activeTrackColor = UvosGreenLamp,
                inactiveTrackColor = Color(0xFF282C3C)
            )
        )
    }
}
