package com.uvos.settings.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.uvos.settings.domain.model.AppIsolationBadge
import com.uvos.settings.domain.model.AuditedAppInfo
import com.uvos.settings.presentation.theme.UvosRed
import com.uvos.settings.presentation.theme.UvosTextPrimary
import com.uvos.settings.presentation.theme.UvosTextSecondary
import com.uvos.settings.presentation.theme.UvosTile
import com.uvos.settings.presentation.theme.UvosTileBorder

/**
 * Карточка аудита приложения с бейджами изоляции и быстрыми элементами управления
 */
@Composable
fun AppAuditItemCard(
    app: AuditedAppInfo,
    onToggleIsolation: (String, Boolean) -> Unit,
    onTerminateApp: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(UvosTile)
            .border(1.dp, UvosTileBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = app.appName,
                    color = UvosTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
                if (app.isIptablesBlocked) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "[DROP]",
                        color = UvosRed,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${app.packageName} • UID:${app.uid}",
                color = UvosTextSecondary,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (app.hasInternetPermission) "NET: YES" else "NET: NO",
                    color = if (app.hasInternetPermission) Color(0xFFFFB000) else Color(0xFF00FF66),
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "КАНАЛЫ: ${app.activeChannelsCount}",
                    color = UvosTextSecondary,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Бейдж изоляции
            val badgeBgColor = when (app.isolationBadge) {
                AppIsolationBadge.SYSTEM -> Color(0xFF242838)
                AppIsolationBadge.ISOLATED -> Color(0xFF063013)
                AppIsolationBadge.SINGLE_CHANNEL_NET -> Color(0xFF382700)
                AppIsolationBadge.EXPOSED -> Color(0xFF3A0D0D)
            }
            val badgeTextColor = Color(app.isolationBadge.colorHex)

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(badgeBgColor)
                    .border(1.dp, badgeTextColor, RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = app.isolationBadge.label,
                    color = badgeTextColor,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Кнопка изоляции / разблокировки
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (!app.isSystemApp) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (app.isIptablesBlocked) Color(0xFF1E3A24) else Color(0xFF3A1E1E))
                            .border(1.dp, if (app.isIptablesBlocked) Color(0xFF00FF66) else UvosRed, RoundedCornerShape(4.dp))
                            .clickable { onToggleIsolation(app.packageName, !app.isIptablesBlocked) }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (app.isIptablesBlocked) "СНЯТЬ" else "ИЗОЛИРОВАТЬ",
                            color = if (app.isIptablesBlocked) Color(0xFF00FF66) else UvosRed,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF252835))
                            .border(1.dp, Color(0xFF3E4358), RoundedCornerShape(4.dp))
                            .clickable { onTerminateApp(app.packageName) }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "СТОП",
                            color = UvosTextPrimary,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
