package com.uvos.terminal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uvos.terminal.ui.theme.UvosCrimson

data class MacroItem(
    val id: String,
    val name: String,
    val description: String,
    val command: String,
    val requireRoot: Boolean = true,
    val accentColor: Color = Color(0xFFE02020)
)

object MacroRepository {
    val defaultMacros = listOf(
        MacroItem(
            id = "macro_sysctl",
            name = "sysctl-audit",
            description = "Аудит ключевых параметров безопасности и подсистемы памяти ядра Linux",
            command = "sysctl -a | grep -E 'kernel.(panic|kptr_restrict)|net.ipv4.(tcp_syncookies|ip_forward)|vm.(swappiness|drop_caches)'\n",
            requireRoot = true,
            accentColor = Color(0xFFFFB000)
        ),
        MacroItem(
            id = "macro_debloat",
            name = "debloat-status",
            description = "Проверка состояния телеметрических пакетов MIUI/HyperOS",
            command = "for pkg in com.miui.analytics com.miui.msa.global com.xiaomi.joyose com.miui.daemon com.xiaomi.finddevice; do state=$(pm list packages -u \"\$pkg\" 2>/dev/null); if [ -z \"\$state\" ]; then echo -e \"\\e[32m[PURGED]\\e[0m \$pkg\"; else echo -e \"\\e[31m[ACTIVE]\\e[0m \$pkg\"; fi; done\n",
            requireRoot = true,
            accentColor = Color(0xFF33FF33)
        ),
        MacroItem(
            id = "macro_killbg",
            name = "killall-bg",
            description = "Аварийное уничтожение фоновых процессов и спящих демонов",
            command = "am kill-all && echo 'Background tasks terminated.'\n",
            requireRoot = true,
            accentColor = Color(0xFFE02020)
        ),
        MacroItem(
            id = "macro_dropcache",
            name = "drop-caches",
            description = "Принудительный сброс буферов диска и очистка кэшей ОЗУ ядра",
            command = "sync && echo 3 > /proc/sys/vm/drop_caches && echo 'PageCache, dentries and inodes flushed.'\n",
            requireRoot = true,
            accentColor = Color(0xFFE02020)
        ),
        MacroItem(
            id = "macro_ping",
            name = "ping-test",
            description = "Локальная диагностика стека TCP/IP (lo и 1.1.1.1)",
            command = "ping -c 3 -W 2 127.0.0.1 && ping -c 3 -W 2 1.1.1.1\n",
            requireRoot = false,
            accentColor = Color(0xFFFFB000)
        ),
        MacroItem(
            id = "macro_cpu_info",
            name = "cpuinfo-freq",
            description = "Текущий статус частот кластеров Snapdragon 685",
            command = "cat /sys/devices/system/cpu/cpu*/cpufreq/scaling_cur_freq\n",
            requireRoot = false,
            accentColor = Color(0xFF33FF33)
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MacroPanelSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onExecuteMacro: (String) -> Unit
) {
    if (!isOpen) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0E0E12),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SYSTEM MACROS & SUBSYSTEM SCRIPTS",
                        color = UvosCrimson,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Детерминированный запуск команд в активной TTY сессии",
                        color = Color(0xFF8A90A2),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF1E212B))
                        .border(1.dp, Color(0xFF383C4F), RoundedCornerShape(4.dp))
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "CLOSE [X]",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(MacroRepository.defaultMacros) { macro ->
                    MacroCard(
                        macro = macro,
                        onClick = {
                            onExecuteMacro(macro.command)
                            onDismiss()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MacroCard(
    macro: MacroItem,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF14161E))
            .border(1.dp, macro.accentColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(10.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "> ${macro.name}",
                    color = macro.accentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (macro.requireRoot) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFF450A0A))
                                .border(0.5.dp, Color(0xFFE02020), RoundedCornerShape(3.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "ROOT",
                                color = Color(0xFFFF5555),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF1E212B))
                            .border(0.5.dp, Color(0xFF4B5168), RoundedCornerShape(3.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "EXEC ↵",
                            color = Color(0xFFDCDFEA),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = macro.description,
                color = Color(0xFFA0A5B5),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
