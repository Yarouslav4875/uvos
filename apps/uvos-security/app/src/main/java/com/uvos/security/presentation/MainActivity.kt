package com.uvos.security.presentation

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import com.uvos.security.data.RootFirewallEngine
import com.uvos.security.domain.model.AppIsolationInfo
import com.uvos.security.presentation.components.ThreatLevelGaugeView
import com.uvos.security.presentation.theme.GaugeNeedleRed
import com.uvos.security.presentation.theme.LampGreen
import com.uvos.security.presentation.theme.LampRed
import com.uvos.security.presentation.theme.UVOSSecurityTheme
import com.uvos.security.presentation.theme.UvosBg
import com.uvos.security.presentation.theme.UvosCard
import com.uvos.security.presentation.theme.UvosPanel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val firewallEngine = RootFirewallEngine()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            UVOSSecurityTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = UvosBg
                ) {
                    val scope = rememberCoroutineScope()
                    var isRootActive by remember { mutableStateOf(false) }
                    var threatScore by remember { mutableFloatStateOf(0.15f) }
                    var appList by remember { mutableStateOf<List<AppIsolationInfo>>(emptyList()) }
                    var blockedCount by remember { mutableIntStateOf(0) }

                    LaunchedEffect(Unit) {
                        val root = firewallEngine.checkRoot()
                        isRootActive = root
                        if (root) {
                            firewallEngine.initFirewallChain()
                        }
                        loadApps(packageManager) { list ->
                            appList = list
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        // Верхний прибор тревоги M4200
                        ThreatLevelGaugeView(
                            score = threatScore,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Панель статусов индикаторов
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(UvosPanel, RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusLampItem("ROOT", isRootActive)
                            StatusLampItem("NETFILTER", isRootActive)
                            StatusLampItem("ИЗОЛЯЦИЯ", blockedCount > 0)

                            Text(
                                text = "БЛОК: $blockedCount",
                                color = if (blockedCount > 0) GaugeNeedleRed else Color.Gray,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Кнопки экстренного реагирования
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        firewallEngine.dropAllNetwork()
                                        threatScore = 0.95f
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GaugeNeedleRed),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("ПОЛНАЯ ИЗОЛЯЦИЯ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    scope.launch {
                                        firewallEngine.flushFirewall()
                                        threatScore = 0.15f
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C3E50)),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("СБРОС ЩИТА", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "КОНТРОЛЬ ПРИЛОЖЕНИЙ (PER-UID NETFILTER):",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Список приложений
                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(appList, key = { it.packageName }) { app ->
                                AppControlCard(
                                    app = app,
                                    onToggleNetwork = { isBlocked ->
                                        scope.launch {
                                            if (isBlocked) {
                                                firewallEngine.blockUidNetwork(app.uid)
                                                blockedCount++
                                                threatScore = (threatScore + 0.1f).coerceAtMost(1f)
                                            } else {
                                                firewallEngine.unblockUidNetwork(app.uid)
                                                blockedCount = (blockedCount - 1).coerceAtLeast(0)
                                            }
                                            appList = appList.map {
                                                if (it.packageName == app.packageName) it.copy(isNetworkBlocked = isBlocked) else it
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun loadApps(pm: PackageManager, onLoaded: (List<AppIsolationInfo>) -> Unit) {
        val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val userApps = installed.filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
            .take(30)
            .map {
                AppIsolationInfo(
                    packageName = it.packageName,
                    appName = pm.getApplicationLabel(it).toString(),
                    uid = it.uid,
                    isNetworkBlocked = false,
                    isSensorsSpoofed = true,
                    isContactsStubbed = true
                )
            }
        onLoaded(userApps)
    }
}

@Composable
fun StatusLampItem(label: String, active: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(if (active) LampGreen else LampRed, RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            color = Color.White,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp
        )
    }
}

@Composable
fun AppControlCard(
    app: AppIsolationInfo,
    onToggleNetwork: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(UvosCard, RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.appName,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp
            )
            Text(
                text = "UID: ${app.uid}  |  ${if (app.isNetworkBlocked) "СЕТЬ: ОТРЕЗАНА" else "СЕТЬ: ОТКРЫТА"}",
                color = if (app.isNetworkBlocked) GaugeNeedleRed else Color(0xFF2ECC71),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            )
        }

        Switch(
            checked = app.isNetworkBlocked,
            onCheckedChange = onToggleNetwork,
            colors = SwitchDefaults.colors(
                checkedThumbColor = GaugeNeedleRed,
                checkedTrackColor = Color(0xFF4A1010)
            )
        )
    }
}
