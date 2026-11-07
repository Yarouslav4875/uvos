package com.uvos.clock.ui.alarm

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uvos.clock.audio.AscendingToneGenerator
import com.uvos.clock.data.AlarmEntity
import com.uvos.clock.data.AlarmScheduler
import com.uvos.clock.receiver.AlarmBroadcastReceiver
import com.uvos.clock.ui.components.AnalogM4200Dial
import com.uvos.clock.ui.components.LampColor
import com.uvos.clock.ui.components.LampView
import com.uvos.clock.ui.theme.BrassGold
import com.uvos.clock.ui.theme.GunmetalBlack
import com.uvos.clock.ui.theme.NeedleRed
import com.uvos.clock.ui.theme.SlateCard
import com.uvos.clock.ui.theme.UVOSClockTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Calendar

class AlarmRingingActivity : ComponentActivity() {

    private var toneGenerator: AscendingToneGenerator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Разблокировка экрана и вывод поверх экрана блокировки
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val alarmId = intent.getLongExtra(AlarmBroadcastReceiver.EXTRA_ALARM_ID, -1L)
        val alarmLabel = intent.getStringExtra(AlarmBroadcastReceiver.EXTRA_ALARM_LABEL) ?: "Будильник UVOS"
        val isCrescendo = intent.getBooleanExtra(AlarmBroadcastReceiver.EXTRA_IS_CRESCENDO, true)

        toneGenerator = AscendingToneGenerator(this)
        toneGenerator?.start(rampDurationSec = if (isCrescendo) 30 else 3)

        setContent {
            UVOSClockTheme {
                AlarmRingingScreen(
                    label = alarmLabel,
                    onDismiss = {
                        stopAlarm()
                        finish()
                    },
                    onSnooze = {
                        stopAlarm()
                        snoozeAlarm(alarmId, alarmLabel)
                        finish()
                    }
                )
            }
        }
    }

    private fun stopAlarm() {
        toneGenerator?.stop()
        toneGenerator = null
    }

    private fun snoozeAlarm(alarmId: Long, label: String) {
        val cal = Calendar.getInstance().apply {
            add(Calendar.MINUTE, 5)
        }
        val snoozeAlarm = AlarmEntity(
            id = if (alarmId > 0) alarmId else System.currentTimeMillis(),
            hour = cal.get(Calendar.HOUR_OF_DAY),
            minute = cal.get(Calendar.MINUTE),
            isEnabled = true,
            daysOfWeek = 0, // однократный
            label = "Повтор: $label",
            isCrescendo = true
        )
        AlarmScheduler(this).scheduleAlarm(snoozeAlarm)
    }

    override fun onDestroy() {
        stopAlarm()
        super.onDestroy()
    }
}

@Composable
fun AlarmRingingScreen(
    label: String,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit
) {
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (isActive) {
            currentTimeMillis = System.currentTimeMillis()
            delay(50)
        }
    }

    val cal = Calendar.getInstance().apply { timeInMillis = currentTimeMillis }
    val hours = cal.get(Calendar.HOUR_OF_DAY)
    val minutes = cal.get(Calendar.MINUTE)
    val seconds = cal.get(Calendar.SECOND)
    val millis = cal.get(Calendar.MILLISECOND)

    val formattedTime = "%02d:%02d".format(hours, minutes)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GunmetalBlack)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Верхний индикатор тревоги
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LampView(
                color = LampColor.RED,
                isLit = true,
                isPulsing = true,
                size = 32.dp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "СИГНАЛ БУДИЛЬНИКА М4200",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = NeedleRed
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontFamily = FontFamily.Monospace,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // Центральный прибор
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AnalogM4200Dial(
                hours = hours,
                minutes = minutes,
                seconds = seconds,
                millis = millis,
                isSmoothSweep = true,
                size = 260.dp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = formattedTime,
                fontSize = 48.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // Кнопки управления (Дремать / Отключить)
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeedleRed,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "ОТКЛЮЧИТЬ СИГНАЛ",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onSnooze,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = BrassGold
                )
            ) {
                Text(
                    text = "ДРЕМАТЬ +5 МИНУТ",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}
