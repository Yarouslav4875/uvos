package com.uvos.calendar.ui.event

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import com.uvos.calendar.domain.model.CalendarEvent
import com.uvos.calendar.domain.model.RecurrenceFrequency
import com.uvos.calendar.domain.model.RecurrenceRule
import com.uvos.calendar.domain.model.ReminderAlert
import com.uvos.calendar.ui.theme.IndicatorAmber
import com.uvos.calendar.ui.theme.IndicatorCyan
import com.uvos.calendar.ui.theme.IndicatorGreen
import com.uvos.calendar.ui.theme.IndicatorPurple
import com.uvos.calendar.ui.theme.M4200NeedleRed
import com.uvos.calendar.ui.theme.UvosBg
import com.uvos.calendar.ui.theme.UvosBorder
import com.uvos.calendar.ui.theme.UvosCard
import com.uvos.calendar.ui.theme.UvosPanel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEventBottomSheet(
    event: CalendarEvent,
    onSave: (CalendarEvent) -> Unit,
    onDelete: ((Long) -> Unit)?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember { mutableStateOf(event.title) }
    var description by remember { mutableStateOf(event.description ?: "") }
    var location by remember { mutableStateOf(event.location ?: "") }
    var startTimestamp by remember { mutableLongStateOf(event.startTimestamp) }
    var endTimestamp by remember { mutableLongStateOf(event.endTimestamp) }
    var isAllDay by remember { mutableStateOf(event.isAllDay) }
    var selectedColor by remember { mutableStateOf(event.colorHex) }
    var recurrenceFreq by remember {
        mutableStateOf(RecurrenceRule.fromRRuleString(event.recurrenceRule).frequency)
    }

    // Default reminder 15 min if present
    var reminderMinutes by remember {
        mutableIntStateOf(event.reminders.firstOrNull()?.minutesBefore ?: 15)
    }
    var hasReminder by remember {
        mutableStateOf(event.reminders.isNotEmpty())
    }

    val dateTimeFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = UvosPanel,
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (event.id == 0L) "НОВОЕ СОБЫТИЕ" else "РЕДАКТИРОВАНИЕ",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )

                if (event.id != 0L && onDelete != null) {
                    OutlinedButton(
                        onClick = { onDelete(event.id) },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = M4200NeedleRed
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, M4200NeedleRed)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Удалить", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Удалить", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            // Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Название события", fontFamily = FontFamily.Monospace) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = M4200NeedleRed,
                    unfocusedBorderColor = UvosBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = UvosCard,
                    unfocusedContainerColor = UvosCard
                )
            )

            // Location
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Местоположение", fontFamily = FontFamily.Monospace) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = M4200NeedleRed,
                    unfocusedBorderColor = UvosBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = UvosCard,
                    unfocusedContainerColor = UvosCard
                )
            )

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Описание и заметки", fontFamily = FontFamily.Monospace) },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = M4200NeedleRed,
                    unfocusedBorderColor = UvosBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = UvosCard,
                    unfocusedContainerColor = UvosCard
                )
            )

            // All Day switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isAllDay = !isAllDay }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isAllDay,
                    onCheckedChange = { isAllDay = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = M4200NeedleRed,
                        uncheckedColor = UvosBorder
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Весь день",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Time / Date Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Start Time Card
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, UvosBorder, RoundedCornerShape(8.dp))
                        .clickable {
                            showDateTimePicker(context, startTimestamp) { newTs ->
                                startTimestamp = newTs
                                if (endTimestamp <= startTimestamp) {
                                    endTimestamp = startTimestamp + 3600_000L
                                }
                            }
                        },
                    color = UvosCard
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("НАЧАЛО", fontSize = 11.sp, color = Color(0xFFA0A5B5), fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(4.dp))
                        val text = if (isAllDay) dateFormat.format(Date(startTimestamp)) else dateTimeFormat.format(Date(startTimestamp))
                        Text(text, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }

                // End Time Card
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, UvosBorder, RoundedCornerShape(8.dp))
                        .clickable {
                            showDateTimePicker(context, endTimestamp) { newTs ->
                                if (newTs >= startTimestamp) {
                                    endTimestamp = newTs
                                }
                            }
                        },
                    color = UvosCard
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("ОКОНЧАНИЕ", fontSize = 11.sp, color = Color(0xFFA0A5B5), fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(4.dp))
                        val text = if (isAllDay) dateFormat.format(Date(endTimestamp)) else dateTimeFormat.format(Date(endTimestamp))
                        Text(text, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            // Recurrence Frequency
            Text("ПОВТОРЕНИЕ СОБЫТИЯ", fontSize = 12.sp, color = Color(0xFFA0A5B5), fontFamily = FontFamily.Monospace)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RecurrenceFrequency.values().forEach { freq ->
                    val isSelected = recurrenceFreq == freq
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) M4200NeedleRed else UvosCard)
                            .border(1.dp, if (isSelected) M4200NeedleRed else UvosBorder, RoundedCornerShape(6.dp))
                            .clickable { recurrenceFreq = freq }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val shortLabel = when (freq) {
                            RecurrenceFrequency.NONE -> "Нет"
                            RecurrenceFrequency.DAILY -> "День"
                            RecurrenceFrequency.WEEKLY -> "Нед."
                            RecurrenceFrequency.MONTHLY -> "Мес."
                            RecurrenceFrequency.YEARLY -> "Год"
                        }
                        Text(
                            text = shortLabel,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else Color(0xFFD0D5DD),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Reminders (AlarmManager exact trigger)
            Text("НАПОМИНАНИЕ (ALARM MANAGER)", fontSize = 12.sp, color = Color(0xFFA0A5B5), fontFamily = FontFamily.Monospace)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { hasReminder = !hasReminder }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = hasReminder,
                    onCheckedChange = { hasReminder = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = M4200NeedleRed,
                        uncheckedColor = UvosBorder
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Включить точное звуковое оповещение",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            if (hasReminder) {
                val reminderOptions = listOf(5, 15, 30, 60, 1440)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    reminderOptions.forEach { mins ->
                        val isSel = reminderMinutes == mins
                        val label = when (mins) {
                            5 -> "5м"
                            15 -> "15м"
                            30 -> "30м"
                            60 -> "1ч"
                            1440 -> "1д"
                            else -> "${mins}м"
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) M4200NeedleRed else UvosCard)
                                .border(1.dp, if (isSel) M4200NeedleRed else UvosBorder, RoundedCornerShape(6.dp))
                                .clickable { reminderMinutes = mins }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) Color.White else Color(0xFFD0D5DD),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Color Picker
            Text("МАРКЕР ЦИФЕРБЛАТА", fontSize = 12.sp, color = Color(0xFFA0A5B5), fontFamily = FontFamily.Monospace)
            val colors = listOf(
                "#E02020", // Red
                "#F39C12", // Amber
                "#00BCD4", // Cyan
                "#2ECC71", // Green
                "#9B59B6"  // Purple
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                colors.forEach { hex ->
                    val colorObj = Color(android.graphics.Color.parseColor(hex))
                    val isSelected = selectedColor.equals(hex, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(colorObj)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) Color.White else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { selectedColor = hex }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Button(
                onClick = {
                    if (title.isBlank()) {
                        title = "Событие"
                    }
                    val remindersList = if (hasReminder) {
                        listOf(
                            ReminderAlert(
                                eventId = event.id,
                                minutesBefore = reminderMinutes
                            )
                        )
                    } else emptyList()

                    val updated = event.copy(
                        title = title.trim(),
                        description = description.trim().ifEmpty { null },
                        location = location.trim().ifEmpty { null },
                        startTimestamp = startTimestamp,
                        endTimestamp = endTimestamp,
                        isAllDay = isAllDay,
                        colorHex = selectedColor,
                        recurrenceRule = RecurrenceRule(recurrenceFreq).toRRuleString(),
                        reminders = remindersList
                    )
                    onSave(updated)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = M4200NeedleRed),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "ЗАФИКСИРОВАТЬ В БАЗЕ M4200",
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun showDateTimePicker(
    context: android.content.Context,
    currentTimestamp: Long,
    onDateSelected: (Long) -> Unit
) {
    val cal = Calendar.getInstance().apply { timeInMillis = currentTimestamp }
    val year = cal.get(Calendar.YEAR)
    val month = cal.get(Calendar.MONTH)
    val day = cal.get(Calendar.DAY_OF_MONTH)
    val hour = cal.get(Calendar.HOUR_OF_DAY)
    val minute = cal.get(Calendar.MINUTE)

    DatePickerDialog(context, { _, y, m, d ->
        TimePickerDialog(context, { _, h, min ->
            val resultCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, y)
                set(Calendar.MONTH, m)
                set(Calendar.DAY_OF_MONTH, d)
                set(Calendar.HOUR_OF_DAY, h)
                set(Calendar.MINUTE, min)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            onDateSelected(resultCal.timeInMillis)
        }, hour, minute, true).show()
    }, year, month, day).show()
}
