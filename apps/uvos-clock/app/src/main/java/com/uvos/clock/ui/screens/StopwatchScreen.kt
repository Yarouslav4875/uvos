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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uvos.clock.ui.components.AnalogStopwatchDial
import com.uvos.clock.ui.components.LampColor
import com.uvos.clock.ui.components.LampView
import com.uvos.clock.ui.theme.BrassGold
import com.uvos.clock.ui.theme.GunmetalBlack
import com.uvos.clock.ui.theme.IndustrialPanel
import com.uvos.clock.ui.theme.LampGreenLit
import com.uvos.clock.ui.theme.NeedleRed
import com.uvos.clock.ui.theme.SlateCard
import com.uvos.clock.ui.theme.TextPhosphor
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

data class LapRecord(
    val lapIndex: Int,
    val lapTimeMillis: Long,
    val totalTimeMillis: Long
)

@Composable
fun StopwatchScreen(modifier: Modifier = Modifier) {
    var isRunning by remember { mutableStateOf(false) }
    var elapsedMillis by remember { mutableLongStateOf(0L) }
    var baseStartTime by remember { mutableLongStateOf(0L) }
    var accumulatedTime by remember { mutableLongStateOf(0L) }

    val laps = remember { mutableStateListOf<LapRecord>() }
    var lastLapTotal by remember { mutableLongStateOf(0L) }

    LaunchedEffect(isRunning) {
        if (isRunning) {
            baseStartTime = System.currentTimeMillis() - accumulatedTime
            while (isActive && isRunning) {
                elapsedMillis = System.currentTimeMillis() - baseStartTime
                delay(16) // ~60 FPS для плавной 1/100 баллистики
            }
        }
    }

    val totalSec = elapsedMillis / 1000
    val minutes = (totalSec / 60) % 60
    val seconds = totalSec % 60
    val hundredths = (elapsedMillis % 1000) / 10

    val digitalFormatted = "%02d:%02d.%02d".format(minutes, seconds, hundredths)

    // Определение самого быстрого и медленного круга
    val minLapTime = if (laps.size > 1) laps.minOfOrNull { it.lapTimeMillis } else null
    val maxLapTime = if (laps.size > 1) laps.maxOfOrNull { it.lapTimeMillis } else null

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GunmetalBlack)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Заголовок статуса
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
                    color = if (isRunning) LampColor.GREEN else LampColor.YELLOW,
                    isLit = true,
                    isPulsing = isRunning,
                    size = 16.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isRunning) "ХРОНОГРАФ: ХОД (1/100s)" else "ХРОНОГРАФ: ОСТАНОВЛЕН",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = if (isRunning) TextPhosphor else BrassGold
                )
            }

            Text(
                text = "КРУГОВ: ${laps.size}",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = Color(0xFFA5ABC0)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Стрелочный циферблат секундомера М4200 с демпфированием
        AnalogStopwatchDial(
            elapsedMillis = elapsedMillis,
            isRunning = isRunning,
            size = 240.dp
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Цифровое табло секундомера
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = SlateCard),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF383E50), RoundedCornerShape(10.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = digitalFormatted,
                    fontSize = 36.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Кнопки управления секундомером
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Кнопка СБРОС (RESET)
            OutlinedButton(
                onClick = {
                    if (!isRunning) {
                        elapsedMillis = 0L
                        accumulatedTime = 0L
                        lastLapTotal = 0L
                        laps.clear()
                    }
                },
                enabled = !isRunning && elapsedMillis > 0,
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
                    fontSize = 12.sp
                )
            }

            // Кнопка КРУГ (LAP)
            Button(
                onClick = {
                    if (isRunning) {
                        val currentLapTime = elapsedMillis - lastLapTotal
                        laps.add(0, LapRecord(laps.size + 1, currentLapTime, elapsedMillis))
                        lastLapTotal = elapsedMillis
                    }
                },
                enabled = isRunning,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2C3242),
                    contentColor = BrassGold,
                    disabledContainerColor = Color(0xFF1B1E27),
                    disabledContentColor = Color(0xFF444857)
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text(
                    text = "КРУГ",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            // Кнопка ПУСК / СТОП
            Button(
                onClick = {
                    if (isRunning) {
                        accumulatedTime = elapsedMillis
                        isRunning = false
                    } else {
                        isRunning = true
                    }
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) NeedleRed else Color(0xFF1F8A4C),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .weight(1.2f)
                    .height(48.dp)
            ) {
                Text(
                    text = if (isRunning) "СТОП" else "СТАРТ",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Таблица кругов (Laps)
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = IndustrialPanel),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(1.dp, Color(0xFF2A2E3D), RoundedCornerShape(10.dp))
        ) {
            if (laps.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "НЕТ ЗАПИСЕЙ КРУГОВ",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = Color(0xFF676E83)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Нажмите «КРУГ» во время отсчета",
                        fontSize = 11.sp,
                        color = Color(0xFF4B5163)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("КРУГ", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color(0xFF8B92A7))
                            Text("ВРЕМЯ КРУГА", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color(0xFF8B92A7))
                            Text("ОБЩЕЕ ВРЕМЯ", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color(0xFF8B92A7))
                        }
                    }

                    itemsIndexed(laps) { _, lap ->
                        val lapMin = (lap.lapTimeMillis / 1000 / 60) % 60
                        val lapSec = (lap.lapTimeMillis / 1000) % 60
                        val lapCs = (lap.lapTimeMillis % 1000) / 10
                        val formattedLap = "%02d:%02d.%02d".format(lapMin, lapSec, lapCs)

                        val totMin = (lap.totalTimeMillis / 1000 / 60) % 60
                        val totSec = (lap.totalTimeMillis / 1000) % 60
                        val totCs = (lap.totalTimeMillis % 1000) / 10
                        val formattedTot = "%02d:%02d.%02d".format(totMin, totSec, totCs)

                        val lapColor = when {
                            lap.lapTimeMillis == minLapTime -> LampGreenLit
                            lap.lapTimeMillis == maxLapTime -> NeedleRed
                            else -> Color.White
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "#%02d".format(lap.lapIndex),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFFA5ABC0)
                            )
                            Text(
                                text = "+$formattedLap",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = lapColor
                            )
                            Text(
                                text = formattedTot,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = Color(0xFF8B92A7)
                            )
                        }
                    }
                }
            }
        }
    }
}
