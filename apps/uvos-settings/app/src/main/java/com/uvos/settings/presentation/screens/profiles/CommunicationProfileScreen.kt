package com.uvos.settings.presentation.screens.profiles

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uvos.settings.domain.model.SystemProfile
import com.uvos.settings.presentation.components.InstrumentTile
import com.uvos.settings.presentation.contract.SettingsIntent
import com.uvos.settings.presentation.contract.SettingsState
import com.uvos.settings.presentation.theme.*

@Composable
fun CommunicationProfileScreen(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCommActive = state.currentProfile == SystemProfile.COMMUNICATION

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(UvosBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Заголовок и кнопка назад
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(UvosTile)
                    .border(1.dp, UvosTileBorder, RoundedCornerShape(8.dp))
                    .clickable { onBack() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "← КОНСОЛЬ",
                    color = UvosTextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "ПРОФИЛЬ: СВЯЗЬ",
                    color = UvosAmberLamp,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Минимальный радиодоступ (Голос + SMS)",
                    color = UvosTextSecondary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Статус баннер
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (isCommActive) Color(0xFF281C06) else Color(0xFF161820))
                .border(1.5.dp, if (isCommActive) UvosAmberLamp else UvosDarkSurfaceBorder, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(if (isCommActive) UvosAmberLamp else Color(0xFF4C5061), CircleShape)
                        .border(2.dp, Color(0xFF1C1E26), CircleShape)
                )
                Column {
                    Text(
                        text = if (isCommActive) "РЕЖИМ АКТИВЕН: ОГРАНИЧЕННЫЙ ШЛЮЗ" else "РЕЖИМ НЕАКТИВЕН",
                        color = if (isCommActive) UvosAmberLamp else UvosTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = if (isCommActive)
                            "Разрешены звонки и SMS. Все сторонние приложения отрезаны от сети."
                        else
                            "Нажмите кнопку ниже для переключения на профиль «СВЯЗЬ».",
                        color = UvosTextPrimary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Правила шлюза связи
        InstrumentTile(title = "МАТРИЦА ШЛЮЗА «СВЯЗЬ»") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CommRuleRow(
                    title = "СОТОВЫЙ МОДЕМ (RIL UID 1001)",
                    status = "РАЗРЕШЕН (ГОЛОС + SMS)",
                    desc = "Базовая телефония, экстренные службы и SMS",
                    isAllowed = true
                )
                CommRuleRow(
                    title = "ПЕРЕДАЧА ДАННЫХ (INTERNET)",
                    status = "WHITELIST ONLY (DNS: 53)",
                    desc = "Интернет заблокирован для всех пользовательских пакетов",
                    isAllowed = false
                )
                CommRuleRow(
                    title = "WI-FI И BLUETOOTH",
                    status = "ОТКЛЮЧЕНЫ",
                    desc = "Радиомодули отключены для устранения радиоследа",
                    isAllowed = false
                )
                CommRuleRow(
                    title = "МИКРОФОН",
                    status = "ТОЛЬКО ДЛЯ ВЫЗОВОВ",
                    desc = "Аппаратный доступ предоставлен исключительно телефонному стеку",
                    isAllowed = true
                )
                CommRuleRow(
                    title = "КАМЕРА",
                    status = "АППАРАТНО ЗАГЛУШЕНА",
                    desc = "SensorPrivacy API блокирует оптические датчики",
                    isAllowed = false
                )
                CommRuleRow(
                    title = "ФАЙРВОЛ ЯДРА (IPTABLES)",
                    status = "FILTER: UID 1001 ACCEPT, REST DROP",
                    desc = "Аппаратный файрвол отбрасывает пакеты сторонних приложений",
                    isAllowed = false
                )
            }
        }

        // Инструкция и рекомендации
        InstrumentTile(title = "НАЗНАЧЕНИЕ ПРОФИЛЯ") {
            Text(
                text = "Профиль «СВЯЗЬ» предназначен для полевого использования, когда требуется возможность принимать входящие звонки и SMS от доверенных абонентов, исключая сетевую фоновую активность приложений, трекинг геолокации и сканирование эфира сетями Wi-Fi.",
                color = UvosTextPrimary,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 14.sp
            )
        }

        // Кнопка активации
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (isCommActive) Color(0xFF3B2A06) else UvosAmberLamp)
                .border(1.5.dp, if (isCommActive) UvosAmberLamp else Color(0xFFFFCC33), RoundedCornerShape(12.dp))
                .clickable {
                    onIntent(SettingsIntent.SelectProfile(SystemProfile.COMMUNICATION))
                }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isCommActive) "ПОДТВЕРДИТЬ РЕЖИМ «СВЯЗЬ»" else "АКТИВИРОВАТЬ РЕЖИМ «СВЯЗЬ»",
                color = if (isCommActive) Color.White else Color(0xFF141416),
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun CommRuleRow(
    title: String,
    status: String,
    desc: String,
    isAllowed: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF13151C))
            .border(1.dp, Color(0xFF242735), RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = UvosTextPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = status,
                color = if (isAllowed) Color(0xFF00FF66) else UvosAmberLamp,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = desc,
            color = UvosTextSecondary,
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
