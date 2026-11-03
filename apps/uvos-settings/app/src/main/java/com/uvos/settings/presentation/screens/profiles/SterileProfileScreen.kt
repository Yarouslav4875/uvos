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
fun SterileProfileScreen(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSterileActive = state.currentProfile == SystemProfile.STERILE

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
                    text = "ПРОФИЛЬ: СТЕРИЛЬНО",
                    color = UvosRed,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Полная радиотишина и аппаратная изоляция",
                    color = UvosTextSecondary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Статус баннер аварийной изоляции
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (isSterileActive) Color(0xFF280B0B) else Color(0xFF161820))
                .border(1.5.dp, if (isSterileActive) UvosRed else UvosDarkSurfaceBorder, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(if (isSterileActive) UvosRed else Color(0xFF4C5061), CircleShape)
                        .border(2.dp, Color(0xFF1C1E26), CircleShape)
                )
                Column {
                    Text(
                        text = if (isSterileActive) "РЕЖИМ АКТИВЕН: ШТАТНАЯ БЛОКИРОВКА" else "РЕЖИМ НЕАКТИВЕН",
                        color = if (isSterileActive) UvosRed else UvosTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = if (isSterileActive)
                            "Все радиомодули и микрофон заглушены. Трафик блокирован."
                        else
                            "Нажмите кнопку ниже для активации радиомолчания.",
                        color = UvosTextPrimary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Матрица подсистем аппаратной изоляции
        InstrumentTile(title = "МАТРИЦА АППАРАТНЫХ БЛОКИРОВОК") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SubsystemRow(
                    name = "СОТОВАЯ СВЯЗЬ (GSM / LTE)",
                    status = if (isSterileActive) "ОТКЛЮЧЕНА [AIRPLANE ON]" else "СТАНДАРТ",
                    detail = "RIL отключен, стек радиомодема остановлен",
                    isLocked = isSterileActive
                )
                SubsystemRow(
                    name = "WI-FI / ТОЧКА ДОСТУПА",
                    status = if (isSterileActive) "DOWN [wlan0 down]" else "СТАНДАРТ",
                    detail = "Аппаратный сброс адаптера, интерфейс отключен",
                    isLocked = isSterileActive
                )
                SubsystemRow(
                    name = "BLUETOOTH / NFC",
                    status = if (isSterileActive) "ОТКЛЮЧЕНЫ [HCI DOWN]" else "СТАНДАРТ",
                    detail = "Драйверы периферии переведены в deep sleep",
                    isLocked = isSterileActive
                )
                SubsystemRow(
                    name = "КАМЕРА И МИКРОФОН",
                    status = if (isSterileActive) "ЗАГЛУШЕНЫ [SensorPrivacy]" else "РАЗРЕШЕНЫ",
                    detail = "Аппаратный мьют датчиков на уровне HAL",
                    isLocked = isSterileActive
                )
                SubsystemRow(
                    name = "ГЕОЛОКАЦИЯ (GPS / GNSS)",
                    status = if (isSterileActive) "ПОДМЕНА [0.000, 0.000]" else "СТАНДАРТ",
                    detail = "Фиктивный провайдер координат, спутники отключены",
                    isLocked = isSterileActive
                )
                SubsystemRow(
                    name = "СЕТЕВОЙ ЭКРАН ЯДРА",
                    status = if (isSterileActive) "DROP ALL [iptables -P DROP]" else "ACCEPT",
                    detail = "Нулевая пропускная способность входящих и исходящих пакетов",
                    isLocked = isSterileActive
                )
            }
        }

        // Zero Telemetry Invariant
        InstrumentTile(title = "ПОЛИТИКА ZERO TELEMETRY INVARIANT") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Строгое правило UnderVoltOS: в профиле «СТЕРИЛЬНО» ядро операционной системы гарантирует полную невозможность физической передачи радиосигналов и утечки криптографических ключей во внешнюю среду.",
                    color = UvosTextPrimary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "ИСХОДЯЩИЙ ТРАФИК:",
                        color = UvosTextSecondary,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = if (isSterileActive) "0 БАЙТ/СЕК" else "НЕ ОГРАНИЧЕН",
                        color = if (isSterileActive) Color(0xFF00FF66) else Color(0xFFFFB000),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Кнопка переключения / фиксации
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (isSterileActive) Color(0xFF3A1010) else UvosRed)
                .border(1.5.dp, if (isSterileActive) UvosRed else Color(0xFFFF6666), RoundedCornerShape(12.dp))
                .clickable {
                    onIntent(SettingsIntent.SelectProfile(SystemProfile.STERILE))
                }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isSterileActive) "ПОДТВЕРДИТЬ РЕЖИМ «СТЕРИЛЬНО»" else "АКТИВИРОВАТЬ РЕЖИМ «СТЕРИЛЬНО»",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun SubsystemRow(
    name: String,
    status: String,
    detail: String,
    isLocked: Boolean
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
                text = name,
                color = UvosTextPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = status,
                color = if (isLocked) UvosRed else UvosAmberLamp,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = detail,
            color = UvosTextSecondary,
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
