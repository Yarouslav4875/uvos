package com.uvos.calendar.ui.month

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uvos.calendar.domain.model.CalendarEvent
import com.uvos.calendar.ui.dial.RetroCalendarDialView
import com.uvos.calendar.ui.event.EditEventBottomSheet
import com.uvos.calendar.ui.theme.IndicatorGreen
import com.uvos.calendar.ui.theme.M4200DialIvory
import com.uvos.calendar.ui.theme.M4200NeedleRed
import com.uvos.calendar.ui.theme.UvosBg
import com.uvos.calendar.ui.theme.UvosBorder
import com.uvos.calendar.ui.theme.UvosCard
import com.uvos.calendar.ui.theme.UvosPanel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun MonthGridScreen(
    viewModel: CalendarViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Прибор M4200, 1: Сетка месяца

    val monthNamesRu = listOf(
        "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
        "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
    )
    val currentMonthName = monthNamesRu.getOrElse(uiState.currentMonth) { "Месяц" }

    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        containerColor = UvosBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openNewEventSheet() },
                containerColor = M4200NeedleRed,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить событие")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Industrial Header Bar
            IndustrialTopBar(
                onExportIcs = {
                    scope.launch {
                        val ics = viewModel.exportIcs()
                        clipboardManager.setText(AnnotatedString(ics))
                        Toast.makeText(context, "ICS скопирован в буфер обмена", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            // Month Navigation Control Bar
            MonthNavigationHeader(
                monthName = currentMonthName,
                year = uiState.currentYear,
                onPreviousMonth = { viewModel.previousMonth() },
                onNextMonth = { viewModel.nextMonth() },
                onToday = { viewModel.jumpToToday() }
            )

            // Switcher: [ ПРИБОР M4200 ] | [ СЕТКА МЕСЯЦА ]
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = UvosPanel,
                contentColor = Color.White,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = M4200NeedleRed
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "ПРИБОР M4200",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "СЕТКА МЕСЯЦА",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                )
            }

            // Main Content Area
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Tab Content: Dial or Grid
                item {
                    if (selectedTab == 0) {
                        // Retro M4200 Dial Instrument
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            RetroCalendarDialView(
                                currentDay = uiState.selectedDay,
                                daysInMonth = uiState.daysInMonth,
                                dayOfWeek = uiState.todayDayOfWeek,
                                monthName = currentMonthName,
                                year = uiState.currentYear,
                                daysWithEvents = uiState.daysWithEvents,
                                modifier = Modifier.size(280.dp)
                            )
                        }
                    } else {
                        // Month Grid View
                        MonthCalendarGrid(
                            daysInMonth = uiState.daysInMonth,
                            firstDayOfWeek = uiState.firstDayOfWeekInMonth,
                            selectedDay = uiState.selectedDay,
                            todayDay = if (uiState.currentYear == uiState.todayYear && uiState.currentMonth == uiState.todayMonth) uiState.todayDay else -1,
                            daysWithEvents = uiState.daysWithEvents,
                            onDayClick = { viewModel.selectDay(it) }
                        )
                    }
                }

                // Selected Date Header
                item {
                    SelectedDateHeader(
                        day = uiState.selectedDay,
                        monthName = currentMonthName,
                        year = uiState.currentYear,
                        eventsCount = uiState.selectedDayEvents.size
                    )
                }

                // Events for Selected Day
                if (uiState.selectedDayEvents.isEmpty()) {
                    item {
                        EmptyDayEventsCard(onAddEvent = { viewModel.openNewEventSheet() })
                    }
                } else {
                    items(uiState.selectedDayEvents, key = { it.id }) { event ->
                        EventItemCard(
                            event = event,
                            onClick = { viewModel.openEditEventSheet(event) }
                        )
                    }
                }
            }
        }
    }

    // Edit/Add Event Sheet
    if (uiState.isEditSheetOpen && uiState.editingEvent != null) {
        EditEventBottomSheet(
            event = uiState.editingEvent!!,
            onSave = { viewModel.saveEvent(it) },
            onDelete = if (uiState.editingEvent!!.id != 0L) { { viewModel.deleteEvent(it) } } else null,
            onDismiss = { viewModel.closeEditSheet() }
        )
    }
}

@Composable
private fun IndustrialTopBar(
    onExportIcs: () -> Unit
) {
    Surface(
        color = UvosPanel,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "UVOS CALENDAR // M4200",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(IndicatorGreen)
                    )
                    Text(
                        text = "100% ОФФЛАЙН • ZERO TELEMETRY",
                        color = Color(0xFFA0A5B5),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            IconButton(onClick = onExportIcs) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = "Экспорт ICS",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun MonthNavigationHeader(
    monthName: String,
    year: Int,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit
) {
    Surface(
        color = UvosCard,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onPreviousMonth) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Предыдущий", tint = Color.White)
            }

            Text(
                text = "${monthName.uppercase()} $year",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                fontFamily = FontFamily.Monospace
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(UvosBorder)
                        .clickable { onToday() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "СЕГОДНЯ",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = onNextMonth) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Следующий", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun MonthCalendarGrid(
    daysInMonth: Int,
    firstDayOfWeek: Int, // Calendar.SUNDAY=1, Calendar.MONDAY=2...
    selectedDay: Int,
    todayDay: Int,
    daysWithEvents: Set<Int>,
    onDayClick: (Int) -> Unit
) {
    val dowLabels = listOf("ПН", "ВТ", "СР", "ЧТ", "ПТ", "СБ", "ВС")

    // Map Calendar.DAY_OF_WEEK (Sun=1, Mon=2..Sat=7) to offset from Monday (Mon=0..Sun=6)
    val offset = when (firstDayOfWeek) {
        Calendar.MONDAY -> 0
        Calendar.TUESDAY -> 1
        Calendar.WEDNESDAY -> 2
        Calendar.THURSDAY -> 3
        Calendar.FRIDAY -> 4
        Calendar.SATURDAY -> 5
        Calendar.SUNDAY -> 6
        else -> 0
    }

    val totalSlots = offset + daysInMonth
    val rows = (totalSlots + 6) / 7

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(UvosPanel)
            .border(1.dp, UvosBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        // Day of Week Header
        Row(modifier = Modifier.fillMaxWidth()) {
            dowLabels.forEachIndexed { idx, label ->
                val isWeekend = idx >= 5
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isWeekend) M4200NeedleRed else Color(0xFFA0A5B5)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Grid Rows
        for (r in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val slotIdx = r * 7 + col
                    val dayNum = slotIdx - offset + 1

                    if (dayNum in 1..daysInMonth) {
                        val isSelected = dayNum == selectedDay
                        val isToday = dayNum == todayDay
                        val hasEvent = dayNum in daysWithEvents

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    when {
                                        isSelected -> M4200NeedleRed
                                        isToday -> UvosCard
                                        else -> Color.Transparent
                                    }
                                )
                                .border(
                                    width = if (isToday && !isSelected) 1.5.dp else 0.dp,
                                    color = if (isToday && !isSelected) M4200NeedleRed else Color.Transparent,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable { onDayClick(dayNum) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = dayNum.toString(),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else Color(0xFFECEFF4)
                                )
                                if (hasEvent) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) Color.White else M4200NeedleRed)
                                    )
                                }
                            }
                        }
                    } else {
                        // Empty slot outside current month
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedDateHeader(
    day: Int,
    monthName: String,
    year: Int,
    eventsCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "РАСПИСАНИЕ: $day ${monthName.uppercase()} $year",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp
        )

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(UvosCard)
                .border(1.dp, UvosBorder, RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = "СОБЫТИЙ: $eventsCount",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFA0A5B5)
            )
        }
    }
}

@Composable
private fun EmptyDayEventsCard(
    onAddEvent: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = UvosPanel,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, UvosBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "На этот день событий не запланировано.",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = Color(0xFFA0A5B5)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(UvosCard)
                    .border(1.dp, M4200NeedleRed, RoundedCornerShape(6.dp))
                    .clickable { onAddEvent() }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "+ ДОБАВИТЬ В КАЛЕНДАРЬ",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = M4200NeedleRed
                )
            }
        }
    }
}

@Composable
private fun EventItemCard(
    event: CalendarEvent,
    onClick: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeText = if (event.isAllDay) {
        "ВЕСЬ ДЕНЬ"
    } else {
        "${timeFormat.format(Date(event.startTimestamp))} - ${timeFormat.format(Date(event.endTimestamp))}"
    }

    val eventColor = try {
        Color(android.graphics.Color.parseColor(event.colorHex))
    } catch (_: Exception) {
        M4200NeedleRed
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, UvosBorder, RoundedCornerShape(10.dp))
            .clickable { onClick() },
        color = UvosPanel
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Indicator pill
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(eventColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = timeText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = eventColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = event.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )

                if (!event.location.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = "Локация",
                            tint = Color(0xFFA0A5B5),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = event.location,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFA0A5B5)
                        )
                    }
                }

                if (!event.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = event.description,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF8A8F9E),
                        maxLines = 2
                    )
                }
            }

            if (event.reminders.isNotEmpty()) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = "Напоминание активно",
                    tint = M4200NeedleRed,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
