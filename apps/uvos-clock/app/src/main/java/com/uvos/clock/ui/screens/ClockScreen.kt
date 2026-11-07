package com.uvos.clock.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uvos.clock.ui.components.AnalogM4200Dial
import com.uvos.clock.ui.components.LampColor
import com.uvos.clock.ui.components.LampView
import com.uvos.clock.ui.components.RetroToggleSwitch
import com.uvos.clock.ui.theme.BrassGold
import com.uvos.clock.ui.theme.GunmetalBlack
import com.uvos.clock.ui.theme.IndustrialPanel
import com.uvos.clock.ui.theme.NeedleRed
import com.uvos.clock.ui.theme.SlateCard
import com.uvos.clock.ui.theme.TextPhosphor
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun ClockScreen(modifier: Modifier = Modifier) {
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var isSmoothSweep by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (isActive) {
            currentTimeMillis = System.currentTimeMillis()
            delay(if (isSmoothSweep) 33 else 200)
        }
    }

    val cal = Calendar.getInstance().apply { timeInMillis = currentTimeMillis }
    val hours = cal.get(Calendar.HOUR_OF_DAY)
    val minutes = cal.get(Calendar.MINUTE)
    val seconds = cal.get(Calendar.SECOND)
    val millis = cal.get(Calendar.MILLISECOND)

    val dateFormat = remember { SimpleDateFormat("EEEE, d MMMM yyyy", Locale("ru")) }
    val timeZone = remember { TimeZone.getDefault().id }

    val formattedDigitalTime = "%02d:%02d:%02d".format(hours, minutes, seconds)
    val formattedMillis = ".%03d".format(millis)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GunmetalBlack)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Шапка прибора
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
                LampView(color = LampColor.GREEN, isLit = true, size = 16.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ПРИБОР М4200 • СИНХРОНИЗИРОВАНО",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = TextPhosphor
                )
            }

            Text(
                text = timeZone,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = Color(0xFF8B92A5)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Аналоговый циферблат М4200 с демпфированной баллистикой
        AnalogM4200Dial(
            hours = hours,
            minutes = minutes,
            seconds = seconds,
            millis = millis,
            isSmoothSweep = isSmoothSweep,
            size = 280.dp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Цифровое табло (Люминесцентный индикатор)
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SlateCard),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF3B4052), RoundedCornerShape(12.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = formattedDigitalTime,
                        fontSize = 38.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = formattedMillis,
                        fontSize = 20.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = BrassGold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = dateFormat.format(Date(currentTimeMillis)).uppercase(Locale("ru")),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFA5ABC0)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Text(
                        text = "EPOCH: ${currentTimeMillis / 1000}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = Color(0xFF6E758B)
                    )
                    Text(
                        text = "КВАРЦ 32.768 кГц",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = Color(0xFF6E758B)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Блок переключения режима баллистики стрелок (Тумблер)
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = IndustrialPanel),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF2C3140), RoundedCornerShape(12.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "БАЛЛИСТИКА СТРЕЛКИ",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isSmoothSweep) "Плавный ход (хронограф)" else "Демпфированный шаг (кварц/маятник)",
                        fontSize = 11.sp,
                        color = Color(0xFF8B92A5)
                    )
                }

                RetroToggleSwitch(
                    isChecked = isSmoothSweep,
                    onCheckedChange = { isSmoothSweep = it },
                    labelTop = "ПЛАВН",
                    labelBottom = "ДЕКР",
                    activeColor = BrassGold
                )
            }
        }
    }
}
