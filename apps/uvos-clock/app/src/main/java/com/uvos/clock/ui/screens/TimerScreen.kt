package com.uvos.clock.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uvos.clock.service.TimerForegroundService
import com.uvos.clock.ui.components.LampColor
import com.uvos.clock.ui.components.LampView
import com.uvos.clock.ui.components.TimerKnobControl
import com.uvos.clock.ui.theme.BrassGold
import com.uvos.clock.ui.theme.GunmetalBlack
import com.uvos.clock.ui.theme.IndustrialPanel
import com.uvos.clock.ui.theme.NeedleRed
import com.uvos.clock.ui.theme.SlateCard
import com.uvos.clock.ui.theme.TextPhosphor

@Composable
fun TimerScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val remainingMillis by TimerForegroundService.remainingTimeMillis.collectAsState()
    val totalMillis by TimerForegroundService.totalTimeMillis.collectAsState()
    val timerState by TimerForegroundService.timerState.collectAsState()

    var selectedMinutes by remember { mutableIntStateOf(10) }

    val isRunning = timerState == TimerForegroundService.TimerState.RUNNING
    val isPaused = timerState == TimerForegroundService.TimerState.PAUSED
    val isFinished = timerState == TimerForegroundService.TimerState.FINISHED

    // Вычисляем отображаемое цифровое время
    val currentMillis = if (isRunning || isPaused || isFinished) remainingMillis else selectedMinutes * 60_000L
    val totalSec = currentMillis / 1000
    val displayHours = totalSec / 3600
    val displayMinutes = (totalSec % 3600) / 60
    val displaySeconds = totalSec % 60

    val digitalFormatted = if (displayHours > 0) {
        "%02d:%02d:%02d".format(displayHours, displayMinutes, displaySeconds)
    } else {
        "%02d:%02d".format(displayMinutes, displaySeconds)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GunmetalBlack)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Шапка статуса
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
                    color = when {
                        isFinished -> LampColor.RED
                        isRunning -> LampColor.GREEN
                        isPaused -> LampColor.YELLOW
                        else -> LampColor.CYAN
                    },
                    isLit = true,
                    isPulsing = isRunning || isFinished,
                    size = 16.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        isFinished -> "ВРЕМЯ ИСТЕКЛО!"
                        isRunning -> "ТАЙМЕР: ОБРАТНЫЙ ОТСЧЕТ"
                        isPaused -> "ТАЙМЕР: ПАУЗА"
                        else -> "ТАЙМЕР: РЕГУЛИРОВКА РУЧКОЙ"
                    },
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = when {
                        isFinished -> NeedleRed
                        isRunning -> TextPhosphor
                        isPaused -> BrassGold
                        else -> Color(0xFFB0B7C8)
                    }
                )
            }

            Text(
                text = "ФОНОВЫЙ СЕРВИС",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = Color(0xFF8B92A5)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Круговой поворотный регулятор Knob Control
        TimerKnobControl(
            selectedMinutes = selectedMinutes,
            onMinutesChanged = { mins ->
                if (!isRunning && !isPaused) {
                    selectedMinutes = mins
                }
            },
            remainingMillis = remainingMillis,
            totalMillis = totalMillis,
            isRunning = isRunning || isPaused,
            size = 270.dp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Цифровое табло таймера
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = SlateCard),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF383E50), RoundedCornerShape(10.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = digitalFormatted,
                    fontSize = 42.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (isFinished) NeedleRed else Color.White
                )
                Text(
                    text = if (!isRunning && !isPaused) "Вращайте ручку или выберите пресет" else "Остаток времени по таймеру M4200",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF8B92A7)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Быстрые пресеты таймера (Кнопки быстрых интервалов)
        if (!isRunning && !isPaused) {
            Text(
                text = "БЫСТРЫЕ ПРЕСЕТЫ",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8B92A7),
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))

            val presets = listOf(
                Pair(1, "1м"),
                Pair(3, "3м"),
                Pair(5, "5м"),
                Pair(15, "15м"),
                Pair(25, "25м (Помодоро)"),
                Pair(45, "45м")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (p in presets.take(3)) {
                    Button(
                        onClick = { selectedMinutes = p.first },
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedMinutes == p.first) BrassGold else IndustrialPanel,
                            contentColor = if (selectedMinutes == p.first) Color.Black else Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(p.second, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (p in presets.drop(3)) {
                    Button(
                        onClick = { selectedMinutes = p.first },
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedMinutes == p.first) BrassGold else IndustrialPanel,
                            contentColor = if (selectedMinutes == p.first) Color.Black else Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(p.second, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Кнопки управления таймером
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Кнопка СБРОС
            OutlinedButton(
                onClick = {
                    TimerForegroundService.resetTimer(context)
                },
                enabled = isRunning || isPaused || isFinished,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFC0C7DC),
                    disabledContentColor = Color(0xFF4A4E5E)
                )
            ) {
                Text(
                    text = "СБРОС",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            // Кнопка ПУСК / ПАУЗА / ПРОДОЛЖИТЬ
            Button(
                onClick = {
                    when {
                        isRunning -> TimerForegroundService.pauseTimer(context)
                        isPaused -> TimerForegroundService.resumeTimer(context)
                        else -> TimerForegroundService.startTimer(context, selectedMinutes * 60_000L)
                    }
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when {
                        isRunning -> NeedleRed
                        isPaused -> BrassGold
                        else -> Color(0xFF1F8A4C)
                    },
                    contentColor = if (isPaused) Color.Black else Color.White
                ),
                modifier = Modifier
                    .weight(1.3f)
                    .height(48.dp)
            ) {
                Text(
                    text = when {
                        isRunning -> "ПАУЗА"
                        isPaused -> "ПРОДОЛЖИТЬ"
                        else -> "ЗАПУСК"
                    },
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}
