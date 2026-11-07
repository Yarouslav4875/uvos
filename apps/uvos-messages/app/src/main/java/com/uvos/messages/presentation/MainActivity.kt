package com.uvos.messages.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.uvos.messages.domain.model.SmsConversation
import com.uvos.messages.presentation.theme.GaugeNeedleRed
import com.uvos.messages.presentation.theme.LampGreen
import com.uvos.messages.presentation.theme.LampRed
import com.uvos.messages.presentation.theme.UVOSMessagesTheme
import com.uvos.messages.presentation.theme.UvosBg
import com.uvos.messages.presentation.theme.UvosCard
import com.uvos.messages.presentation.theme.UvosPanel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            UVOSMessagesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = UvosBg
                ) {
                    var selectedTab by remember { mutableIntStateOf(0) }
                    val sampleThreads = remember {
                        listOf(
                            SmsConversation(
                                id = "1",
                                address = "900",
                                lastMessage = "Код подтверждения: ••••. Никому не сообщайте код.",
                                timestamp = "19:12",
                                isOtpCode = true
                            ),
                            SmsConversation(
                                id = "2",
                                address = "+7 999 123-45-67",
                                lastMessage = "Привет! Забрал образ системы с флешки, всё скомпилировалось.",
                                timestamp = "18:05"
                            ),
                            SmsConversation(
                                id = "3",
                                address = "MTS_INFO",
                                lastMessage = "Пакет минут и гигабайт обновлен. Баланс в норме.",
                                timestamp = "Вчера"
                            )
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        // Верхняя панель приборов M4200
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(UvosPanel, RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusLampItem("RIL СВЯЗЬ", LampGreen)
                            StatusLampItem("REGEX ЩИТ", LampGreen)
                            StatusLampItem("0% ТЕЛЕМЕТРИИ", LampGreen)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Табы: Входящие / Бункер спама
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { selectedTab = 0 },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedTab == 0) GaugeNeedleRed else Color(0xFF2C3E50)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("ВХОДЯЩИЕ", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { selectedTab = 1 },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedTab == 1) GaugeNeedleRed else Color(0xFF2C3E50)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("БУНКЕР СПАМА (0)", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = if (selectedTab == 0) "ЗАЩИЩЕННЫЕ СООБЩЕНИЯ GSM/LTE:" else "ИЗОЛИРОВАННЫЙ СПАМ (БЕЗ УВЕДОМЛЕНИЙ):",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(sampleThreads, key = { it.id }) { item ->
                                SmsCard(item = item)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusLampItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            color = Color.White,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SmsCard(item: SmsConversation) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(UvosCard, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.address,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = item.timestamp,
                color = Color.Gray,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = item.lastMessage,
            color = Color(0xFFE8E4D8),
            fontSize = 12.sp,
            lineHeight = 16.sp
        )

        if (item.isOtpCode) {
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                color = Color(0xFF145A32),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "ЗАЩИТА OTP: ШТОРКА МАСКИРОВАНА",
                    color = Color(0xFFA9DFBF),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
