package com.uvos.vpn.presentation

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uvos.vpn.core.UvosVpnService
import com.uvos.vpn.presentation.components.SpeedometerGaugeView
import com.uvos.vpn.presentation.theme.GaugeNeedleRed
import com.uvos.vpn.presentation.theme.LampGreen
import com.uvos.vpn.presentation.theme.LampRed
import com.uvos.vpn.presentation.theme.UVOSVpnTheme
import com.uvos.vpn.presentation.theme.UvosBg
import com.uvos.vpn.presentation.theme.UvosCard
import com.uvos.vpn.presentation.theme.UvosPanel

class MainActivity : ComponentActivity() {

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            startVpnService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            UVOSVpnTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = UvosBg
                ) {
                    val isRunning by UvosVpnService.isRunning.collectAsState()
                    val traffic by UvosVpnService.trafficState.collectAsState()

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        // Верхняя пара стрелочных спидометров скорости (TX / RX)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SpeedometerGaugeView(
                                title = "ПЕРЕДАЧА (TX)",
                                speedKbSec = traffic.txBytesSec / 1024f,
                                modifier = Modifier.weight(1f)
                            )
                            SpeedometerGaugeView(
                                title = "ПРИЕМ (RX)",
                                speedKbSec = traffic.rxBytesSec / 1024f,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Панель статуса крипто-туннеля
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(UvosPanel, RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(if (isRunning) LampGreen else LampRed, RoundedCornerShape(5.dp))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isRunning) "ТУННЕЛЬ: АКТИВЕН" else "ТУННЕЛЬ: ОТКЛЮЧЕН",
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = "KILL-SWITCH: ВКЛ",
                                color = GaugeNeedleRed,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Главная кнопка подключения / отключения
                        Button(
                            onClick = {
                                if (isRunning) {
                                    stopVpnService()
                                } else {
                                    prepareAndStartVpn()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRunning) Color(0xFFC0392B) else Color(0xFF27AE60)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (isRunning) "РАЗОРВАТЬ ТУННЕЛЬ" else "ЗАПУСТИТЬ ЗАЩИТУ UVOS",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Карточка конфигурации
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(UvosCard, RoundedCornerShape(12.dp))
                                .padding(14.dp)
                        ) {
                            Text(
                                text = "ПАРАМЕТРЫ ТУННЕЛИРОВАНИЯ:",
                                color = Color.Gray,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("ПРОТОКОЛ: WireGuard (ChaCha20-Poly1305)", color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            Text("ЗАЩИТА ОТ УТЕЧЕК: DNS + IPv6 Disabled", color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            Text("РАЗДЕЛЬНОЕ ТУННЕЛИРОВАНИЕ: СТРОГОЕ (Все через туннель)", color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }

    private fun prepareAndStartVpn() {
        val vpnIntent = VpnService.prepare(this)
        if (vpnIntent != null) {
            vpnPermissionLauncher.launch(vpnIntent)
        } else {
            startVpnService()
        }
    }

    private fun startVpnService() {
        val intent = Intent(this, UvosVpnService::class.java)
        startForegroundService(intent)
    }

    private fun stopVpnService() {
        val intent = Intent(this, UvosVpnService::class.java).apply {
            action = "STOP"
        }
        startService(intent)
    }
}
