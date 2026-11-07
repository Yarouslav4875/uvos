package com.uvos.clock.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uvos.clock.data.AlarmEntity
import com.uvos.clock.data.AlarmScheduler
import com.uvos.clock.data.ClockDatabaseHelper
import com.uvos.clock.ui.components.LampColor
import com.uvos.clock.ui.components.LampView
import com.uvos.clock.ui.components.RetroToggleSwitch
import com.uvos.clock.ui.theme.BrassGold
import com.uvos.clock.ui.theme.GunmetalBlack
import com.uvos.clock.ui.theme.IndustrialPanel
import com.uvos.clock.ui.theme.NeedleRed
import com.uvos.clock.ui.theme.SlateCard
import com.uvos.clock.ui.theme.TextPhosphor

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AlarmScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val dbHelper = remember { ClockDatabaseHelper(context) }
    val alarmScheduler = remember { AlarmScheduler(context) }

    val alarms = remember { mutableStateListOf<AlarmEntity>() }
    var showAddDialog by remember { mutableStateOf(false) }

    fun refreshAlarms() {
        alarms.clear()
        alarms.addAll(dbHelper.getAllAlarms())
    }

    LaunchedEffect(Unit) {
        refreshAlarms()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GunmetalBlack)
            .padding(16.dp)
    ) {
        // Заголовок и кнопка добавления
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(IndustrialPanel)
                .border(1.dp, Color(0xFF2E3344), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LampView(
                    color = LampColor.RED,
                    isLit = alarms.any { it.isEnabled },
                    size = 16.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "СИСТЕМА БУДИЛЬНИКОВ (DOZE WAKE)",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = if (alarms.any { it.isEnabled }) NeedleRed else Color(0xFF8B92A5)
                )
            }

            Button(
                onClick = { showAddDialog = true },
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrassGold,
                    contentColor = Color.Black
                ),
                modifier = Modifier.height(34.dp)
            ) {
                Text(
                    text = "+ БУДИЛЬНИК",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (alarms.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "НЕТ АКТИВНЫХ БУДИЛЬНИКОВ",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = Color(0xFF6E758C)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Нажмите «+ БУДИЛЬНИК» для настройки времени",
                        fontSize = 11.sp,
                        color = Color(0xFF4C5264)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(alarms, key = { it.id }) { alarm ->
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (alarm.isEnabled) SlateCard else IndustrialPanel
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                if (alarm.isEnabled) Color(0xFF42495D) else Color(0xFF262A37),
                                RoundedCornerShape(10.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = alarm.getFormattedTime(),
                                        fontSize = 32.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = if (alarm.isEnabled) Color.White else Color(0xFF6E758C)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    if (alarm.isCrescendo) {
                                        Text(
                                            text = "НАРАСТАЮЩИЙ",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.sp,
                                            color = BrassGold,
                                            modifier = Modifier
                                                .background(Color(0xFF2A2410), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = alarm.label,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (alarm.isEnabled) TextPhosphor else Color(0xFF6E758C)
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = alarm.getDaysDescription(),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = Color(0xFF8B92A7)
                                )
                            }

                            // Правая часть: кнопка удаления и ретро-тумблер
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedButton(
                                    onClick = {
                                        alarmScheduler.cancelAlarm(alarm.id)
                                        dbHelper.deleteAlarm(alarm.id)
                                        refreshAlarms()
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .height(36.dp)
                                        .padding(end = 8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color(0xFF9E3A3A)
                                    )
                                ) {
                                    Text("УДАЛИТЬ", fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                }

                                RetroToggleSwitch(
                                    isChecked = alarm.isEnabled,
                                    onCheckedChange = { newState ->
                                        val updated = alarm.copy(isEnabled = newState)
                                        dbHelper.updateAlarm(updated)
                                        if (newState) {
                                            alarmScheduler.scheduleAlarm(updated)
                                        } else {
                                            alarmScheduler.cancelAlarm(updated.id)
                                        }
                                        refreshAlarms()
                                    },
                                    labelTop = "ВКЛ",
                                    labelBottom = "ВЫКЛ",
                                    activeColor = NeedleRed
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Диалог создания будильника
    if (showAddDialog) {
        var hour by remember { mutableIntStateOf(7) }
        var minute by remember { mutableIntStateOf(0) }
        var label by remember { mutableStateOf("Дежурный подъем") }
        var isCrescendo by remember { mutableStateOf(true) }
        var daysOfWeekMask by remember { mutableIntStateOf(31) } // Mon-Fri default

        val dayNames = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = IndustrialPanel,
            title = {
                Text(
                    text = "НОВЫЙ БУДИЛЬНИК M4200",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Выбор времени
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Часы
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Button(
                                onClick = { hour = (hour + 1) % 24 },
                                shape = RoundedCornerShape(4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SlateCard),
                                modifier = Modifier.height(28.dp)
                            ) { Text("▲", fontSize = 10.sp) }
                            Text(
                                text = "%02d".format(hour),
                                fontSize = 36.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Button(
                                onClick = { hour = if (hour > 0) hour - 1 else 23 },
                                shape = RoundedCornerShape(4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SlateCard),
                                modifier = Modifier.height(28.dp)
                            ) { Text("▼", fontSize = 10.sp) }
                        }

                        Text(
                            text = ":",
                            fontSize = 36.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = BrassGold,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        // Минуты
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Button(
                                onClick = { minute = (minute + 5) % 60 },
                                shape = RoundedCornerShape(4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SlateCard),
                                modifier = Modifier.height(28.dp)
                            ) { Text("▲", fontSize = 10.sp) }
                            Text(
                                text = "%02d".format(minute),
                                fontSize = 36.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Button(
                                onClick = { minute = if (minute >= 5) minute - 5 else 55 },
                                shape = RoundedCornerShape(4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SlateCard),
                                modifier = Modifier.height(28.dp)
                            ) { Text("▼", fontSize = 10.sp) }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Дни недели
                    Text(
                        text = "ДНИ ПОВТОРЕНИЯ",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8B92A7)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (i in 0 until 7) {
                            val isSelected = (daysOfWeekMask and (1 shl i)) != 0
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) BrassGold else SlateCard)
                                    .clickable {
                                        daysOfWeekMask = daysOfWeekMask xor (1 shl i)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayNames[i],
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.Black else Color(0xFFB0B7C8)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Название
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = { Text("Название будильника", fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrassGold,
                            unfocusedBorderColor = Color(0xFF3E4458),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Переключатель плавного нарастания
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "ПЛАВНОЕ НАРАСТАНИЕ ЗВУКА",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Громкость плавно растет 30 секунд",
                                fontSize = 10.sp,
                                color = Color(0xFF8B92A7)
                            )
                        }

                        RetroToggleSwitch(
                            isChecked = isCrescendo,
                            onCheckedChange = { isCrescendo = it },
                            labelTop = "ДА",
                            labelBottom = "НЕТ",
                            activeColor = BrassGold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newAlarm = AlarmEntity(
                            hour = hour,
                            minute = minute,
                            isEnabled = true,
                            daysOfWeek = daysOfWeekMask,
                            label = label.ifBlank { "Будильник" },
                            isCrescendo = isCrescendo
                        )
                        val id = dbHelper.insertAlarm(newAlarm)
                        val inserted = newAlarm.copy(id = id)
                        alarmScheduler.scheduleAlarm(inserted)
                        refreshAlarms()
                        showAddDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeedleRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("СОХРАНИТЬ", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showAddDialog = false },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("ОТМЕНА", fontFamily = FontFamily.Monospace)
                }
            }
        )
    }
}
