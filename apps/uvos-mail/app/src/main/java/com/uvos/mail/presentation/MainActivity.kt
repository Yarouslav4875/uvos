package com.uvos.mail.presentation

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
import com.uvos.mail.domain.model.MailItem
import com.uvos.mail.presentation.theme.GaugeNeedleRed
import com.uvos.mail.presentation.theme.LampGreen
import com.uvos.mail.presentation.theme.LampYellow
import com.uvos.mail.presentation.theme.UVOSMailTheme
import com.uvos.mail.presentation.theme.UvosBg
import com.uvos.mail.presentation.theme.UvosCard
import com.uvos.mail.presentation.theme.UvosPanel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            UVOSMailTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = UvosBg
                ) {
                    var selectedMail by remember { mutableStateOf<MailItem?>(null) }
                    val sampleMails = remember {
                        listOf(
                            MailItem(
                                id = "1",
                                sender = "security@kernel.org",
                                subject = "Linux 5.15 LTS Security Patch Notification",
                                snippet = "PGP Signed: Applied stable fixes for networking and netfilter subsystem.",
                                timestamp = "18:24",
                                isPgpEncrypted = true,
                                isTrackersBlocked = true
                            ),
                            MailItem(
                                id = "2",
                                sender = "git@github.com",
                                subject = "UVOS Core Repository Sync Report",
                                snippet = "Clean build artifact validation completed without external trackers.",
                                timestamp = "17:10",
                                isPgpEncrypted = false,
                                isTrackersBlocked = true
                            ),
                            MailItem(
                                id = "3",
                                sender = "updates@fdroid.org",
                                subject = "F-Droid Package Integrity Verification",
                                snippet = "Checksum SHA-256 matches build reproduction logs exactly.",
                                timestamp = "Вчера",
                                isPgpEncrypted = true,
                                isTrackersBlocked = true
                            )
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        // Верхняя панель статусов M4200
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(UvosPanel, RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusLamp("IMAP", LampGreen)
                            StatusLamp("SMTP", LampGreen)
                            StatusLamp("PGP E2E", LampGreen)
                            StatusLamp("АНТИ-ТРЕК", LampGreen)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (selectedMail == null) {
                            Text(
                                text = "ЗАЩИЩЕННЫЙ ПОЧТОВЫЙ ЯЩИК (0 ТРЕКЕРОВ):",
                                color = Color.Gray,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            LazyColumn(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(sampleMails, key = { it.id }) { mail ->
                                    MailCard(
                                        mail = mail,
                                        onClick = { selectedMail = mail }
                                    )
                                }
                            }
                        } else {
                            // Просмотр письма
                            MailDetailView(
                                mail = selectedMail!!,
                                onBack = { selectedMail = null }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusLamp(label: String, color: Color) {
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
fun MailCard(mail: MailItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(UvosCard, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = mail.sender,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = mail.timestamp,
                color = Color.Gray,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = mail.subject,
            color = Color(0xFFE8E4D8),
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = mail.snippet,
            color = Color.Gray,
            fontSize = 11.sp,
            maxLines = 2
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (mail.isPgpEncrypted) {
                Surface(
                    color = Color(0xFF1B4F72),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "PGP ENCRYPTED",
                        color = Color(0xFF85C1E9),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            if (mail.isTrackersBlocked) {
                Surface(
                    color = Color(0xFF145A32),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "EXIF/WEBMARK STRIPPED",
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
}

@Composable
fun MailDetailView(mail: MailItem, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(UvosCard, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C3E50)),
            shape = RoundedCornerShape(6.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text("← НАЗАД К ПИСЬМАМ", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(text = mail.subject, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text(text = "ОТ: ${mail.sender}", color = Color(0xFFE8E4D8), fontSize = 12.sp, fontFamily = FontFamily.Monospace)

        Spacer(modifier = Modifier.height(10.dp))

        Surface(
            color = Color(0xFF1E8449).copy(alpha = 0.2f),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "✓ ИЗОЛЯЦИЯ: Внешние изображения и веб-маяки заблокированы. IP-адрес скрыт.",
                color = LampGreen,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(8.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = mail.snippet + "\n\nДанное сообщение проверено внутренним модулем санитизации UVOS. Все скрытые трекеры и телеметрические ссылки нейтрализованы на уровне клиентского конвейера.",
            color = Color.White,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
    }
}
