package com.uvos.clock.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uvos.clock.data.AlarmScheduler
import com.uvos.clock.data.ClockDatabaseHelper
import com.uvos.clock.data.TaskEntity
import com.uvos.clock.ui.components.LampColor
import com.uvos.clock.ui.components.LampView
import com.uvos.clock.ui.components.RetroToggleSwitch
import com.uvos.clock.ui.theme.BrassGold
import com.uvos.clock.ui.theme.GunmetalBlack
import com.uvos.clock.ui.theme.IndustrialPanel
import com.uvos.clock.ui.theme.NeedleRed
import com.uvos.clock.ui.theme.SlateCard
import com.uvos.clock.ui.theme.TextPhosphor

@Composable
fun TasksScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val dbHelper = remember { ClockDatabaseHelper(context) }
    val alarmScheduler = remember { AlarmScheduler(context) }

    val tasks = remember { mutableStateListOf<TaskEntity>() }
    var selectedFilterPriority by remember { mutableIntStateOf(0) } // 0 = Все, 1..4 = Квадранты
    var showAddTaskDialog by remember { mutableStateOf(false) }

    fun refreshTasks() {
        tasks.clear()
        val list = if (selectedFilterPriority == 0) {
            dbHelper.getAllTasks()
        } else {
            dbHelper.getTasksByPriority(selectedFilterPriority)
        }
        tasks.addAll(list)
    }

    LaunchedEffect(selectedFilterPriority) {
        refreshTasks()
    }

    val allTasks = remember(tasks.size, selectedFilterPriority) { dbHelper.getAllTasks() }
    val countQ1 = allTasks.count { it.priority == 1 && !it.isCompleted }
    val countQ2 = allTasks.count { it.priority == 2 && !it.isCompleted }
    val countQ3 = allTasks.count { it.priority == 3 && !it.isCompleted }
    val countQ4 = allTasks.count { it.priority == 4 && !it.isCompleted }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GunmetalBlack)
            .padding(16.dp)
    ) {
        // Шапка матрицы Эйзенхауэра
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
                LampView(color = LampColor.RED, isLit = countQ1 > 0, size = 16.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "МАТРИЦА ЭЙЗЕНХАУЭРА UVOS",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color.White
                )
            }

            Button(
                onClick = { showAddTaskDialog = true },
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrassGold,
                    contentColor = Color.Black
                ),
                modifier = Modifier.height(34.dp)
            ) {
                Text(
                    text = "+ ЗАДАЧА",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2x2 Матрица Эйзенхауэра (Приборная панель из 4 квадрантов)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Квадрант 1: Срочно и Важно (Красная лампа)
            QuadrantTile(
                title = "1. СРОЧНО / ВАЖНО",
                subtitle = "СДЕЛАЙ СЕЙЧАС",
                count = countQ1,
                lampColor = LampColor.RED,
                isSelected = selectedFilterPriority == 1,
                onClick = {
                    selectedFilterPriority = if (selectedFilterPriority == 1) 0 else 1
                },
                modifier = Modifier.weight(1f)
            )

            // Квадрант 2: Не срочно, но Важно (Желтая лампа)
            QuadrantTile(
                title = "2. НЕ СРОЧНО / ВАЖНО",
                subtitle = "ЗАПЛАНИРУЙ",
                count = countQ2,
                lampColor = LampColor.YELLOW,
                isSelected = selectedFilterPriority == 2,
                onClick = {
                    selectedFilterPriority = if (selectedFilterPriority == 2) 0 else 2
                },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Квадрант 3: Срочно, но не важно (Оранжевая лампа)
            QuadrantTile(
                title = "3. СРОЧНО / НЕ ВАЖНО",
                subtitle = "ДЕЛЕГИРУЙ",
                count = countQ3,
                lampColor = LampColor.ORANGE,
                isSelected = selectedFilterPriority == 3,
                onClick = {
                    selectedFilterPriority = if (selectedFilterPriority == 3) 0 else 3
                },
                modifier = Modifier.weight(1f)
            )

            // Квадрант 4: Не срочно и не важно (Серая лампа)
            QuadrantTile(
                title = "4. НЕ СРОЧНО / НЕ ВАЖНО",
                subtitle = "ОТСЕЙ / РУТИНА",
                count = countQ4,
                lampColor = LampColor.CYAN,
                isSelected = selectedFilterPriority == 4,
                onClick = {
                    selectedFilterPriority = if (selectedFilterPriority == 4) 0 else 4
                },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Заголовок текущего фильтра
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when (selectedFilterPriority) {
                    1 -> "ФИЛЬТР: КВАДРАНТ I (СРОЧНО И ВАЖНО)"
                    2 -> "ФИЛЬТР: КВАДРАНТ II (ВАЖНО, НЕ СРОЧНО)"
                    3 -> "ФИЛЬТР: КВАДРАНТ III (СРОЧНО, НЕ ВАЖНО)"
                    4 -> "ФИЛЬТР: КВАДРАНТ IV (НЕ СРОЧНО, НЕ ВАЖНО)"
                    else -> "ВСЕ ЗАДАЧИ МАТРИЦЫ"
                },
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = BrassGold
            )

            if (selectedFilterPriority != 0) {
                Text(
                    text = "СБРОСИТЬ ФИЛЬТР [x]",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = Color(0xFFA5ABC0),
                    modifier = Modifier.clickable { selectedFilterPriority = 0 }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Список задач
        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "НЕТ ЗАДАЧ В ДАННОЙ КАТЕГОРИИ",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = Color(0xFF6E758C)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tasks, key = { it.id }) { task ->
                    val lampColor = when (task.priority) {
                        1 -> LampColor.RED
                        2 -> LampColor.YELLOW
                        3 -> LampColor.ORANGE
                        else -> LampColor.CYAN
                    }

                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (task.isCompleted) Color(0xFF13151D) else SlateCard
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                if (task.isCompleted) Color(0xFF222632) else Color(0xFF383E52),
                                RoundedCornerShape(8.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Чекбокс выполнения
                            Checkbox(
                                checked = task.isCompleted,
                                onCheckedChange = { checked ->
                                    val updated = task.copy(isCompleted = checked)
                                    dbHelper.updateTask(updated)
                                    refreshTasks()
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = BrassGold,
                                    uncheckedColor = Color(0xFF636B82)
                                )
                            )

                            // Индикатор лампы приоритета
                            LampView(
                                color = lampColor,
                                isLit = !task.isCompleted,
                                size = 14.dp
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            // Текст задачи
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = task.title,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (task.isCompleted) Color(0xFF6E758C) else Color.White,
                                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                )

                                if (!task.description.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = task.description,
                                        fontSize = 11.sp,
                                        color = if (task.isCompleted) Color(0xFF505566) else Color(0xFFA5ABC0)
                                    )
                                }
                            }

                            // Кнопка удаления
                            OutlinedButton(
                                onClick = {
                                    dbHelper.deleteTask(task.id)
                                    refreshTasks()
                                },
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.height(30.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFF8B3A3A)
                                )
                            ) {
                                Text("✕", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Диалог создания задачи с ретро-тумблерами Эйзенхауэра
    if (showAddTaskDialog) {
        var title by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var isUrgent by remember { mutableStateOf(true) }
        var isImportant by remember { mutableStateOf(true) }

        val dynamicPriority = TaskEntity.priorityFromToggles(isUrgent, isImportant)
        val dynamicLampColor = when (dynamicPriority) {
            1 -> LampColor.RED
            2 -> LampColor.YELLOW
            3 -> LampColor.ORANGE
            else -> LampColor.CYAN
        }
        val dynamicQuadrantLabel = when (dynamicPriority) {
            1 -> "КВАДРАНТ I: СРОЧНО И ВАЖНО (СДЕЛАЙ НЕЗАМЕДЛИТЕЛЬНО)"
            2 -> "КВАДРАНТ II: ВАЖНО, НЕ СРОЧНО (СТРАТЕГИЧЕСКИЙ ПЛАН)"
            3 -> "КВАДРАНТ III: СРОЧНО, НЕ ВАЖНО (ДЕЛЕГИРОВАНИЕ/ОПЕРАТИВНО)"
            else -> "КВАДРАНТ IV: НЕ СРОЧНО И НЕ ВАЖНО (РУТИНА/ОТСЕЯТЬ)"
        }

        AlertDialog(
            onDismissRequest = { showAddTaskDialog = false },
            containerColor = IndustrialPanel,
            title = {
                Text(
                    text = "НОВАЯ ЗАДАЧА ЭЙЗЕНХАУЭРА",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Название задачи", fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrassGold,
                            unfocusedBorderColor = Color(0xFF3E4458),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Описание (опционально)", fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrassGold,
                            unfocusedBorderColor = Color(0xFF3E4458),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Блок ретро-тумблеров определения приоритета матрицы
                    Text(
                        text = "ТУМБЛЕРЫ ПРИОРИТЕТА МАТРИЦЫ:",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8B92A7)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF13151D))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Тумблер 1: Срочность
                        RetroToggleSwitch(
                            isChecked = isUrgent,
                            onCheckedChange = { isUrgent = it },
                            subLabel = "СРОЧНОСТЬ",
                            labelTop = "СРОЧНО",
                            labelBottom = "НЕ СРОЧНО",
                            activeColor = NeedleRed
                        )

                        // Тумблер 2: Важность
                        RetroToggleSwitch(
                            isChecked = isImportant,
                            onCheckedChange = { isImportant = it },
                            subLabel = "ВАЖНОСТЬ",
                            labelTop = "ВАЖНО",
                            labelBottom = "ОБЫЧНО",
                            activeColor = BrassGold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Динамическая карточка предпросмотра квадранта
                    Card(
                        shape = RoundedCornerShape(6.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateCard),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF3B4154), RoundedCornerShape(6.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LampView(color = dynamicLampColor, isLit = true, size = 16.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = dynamicQuadrantLabel,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            val newTask = TaskEntity(
                                title = title.trim(),
                                description = description.trim().ifEmpty { null },
                                priority = dynamicPriority,
                                isCompleted = false,
                                createdAt = System.currentTimeMillis()
                            )
                            dbHelper.insertTask(newTask)
                            refreshTasks()
                            showAddTaskDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrassGold,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("ДОБАВИТЬ", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showAddTaskDialog = false },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("ОТМЕНА", fontFamily = FontFamily.Monospace)
                }
            }
        )
    }
}

@Composable
fun QuadrantTile(
    title: String,
    subtitle: String,
    count: Int,
    lampColor: LampColor,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF282D3D) else SlateCard
        ),
        modifier = modifier
            .border(
                1.5.dp,
                if (isSelected) BrassGold else Color(0xFF2E3344),
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LampView(color = lampColor, isLit = count > 0, size = 14.dp)
                Text(
                    text = count.toString(),
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (count > 0) Color.White else Color(0xFF6E758C)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = title,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = Color.White
            )

            Text(
                text = subtitle,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = Color(0xFF8B92A7)
            )
        }
    }
}
