package com.uvos.clock.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.uvos.clock.ui.components.LampColor
import com.uvos.clock.ui.components.LampView
import com.uvos.clock.ui.screens.AlarmScreen
import com.uvos.clock.ui.screens.ClockScreen
import com.uvos.clock.ui.screens.StopwatchScreen
import com.uvos.clock.ui.screens.TasksScreen
import com.uvos.clock.ui.screens.TimerScreen
import com.uvos.clock.ui.theme.BrassGold
import com.uvos.clock.ui.theme.GunmetalBlack
import com.uvos.clock.ui.theme.IndustrialPanel
import com.uvos.clock.ui.theme.NeedleRed
import com.uvos.clock.ui.theme.TextPhosphor
import com.uvos.clock.ui.theme.UVOSClockTheme

class ClockTasksActivity : ComponentActivity() {

    private var selectedTabState = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleIntent(intent)

        setContent {
            UVOSClockTheme {
                MainClockApp(
                    selectedTab = selectedTabState.intValue,
                    onTabSelected = { selectedTabState.intValue = it }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val tab = intent?.getIntExtra("OPEN_TAB", -1) ?: -1
        if (tab in 0..4) {
            selectedTabState.intValue = tab
        }
    }
}

@Composable
fun MainClockApp(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        containerColor = GunmetalBlack,
        bottomBar = {
            RetroIndustrialNavBar(
                selectedTab = selectedTab,
                onTabSelected = onTabSelected
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> ClockScreen()
                1 -> StopwatchScreen()
                2 -> TimerScreen()
                3 -> AlarmScreen()
                4 -> TasksScreen()
            }
        }
    }
}

@Composable
fun RetroIndustrialNavBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf(
        Pair("ЧАСЫ", LampColor.GREEN),
        Pair("СЕКУНД", LampColor.YELLOW),
        Pair("ТАЙМЕР", LampColor.CYAN),
        Pair("БУДИЛЬНИК", LampColor.RED),
        Pair("ЗАДАЧИ", LampColor.ORANGE)
    )

    Surface(
        color = IndustrialPanel,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF2B2F3E))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, item ->
                val isSelected = selectedTab == index
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) Color(0xFF222634) else Color.Transparent)
                        .border(
                            1.dp,
                            if (isSelected) BrassGold.copy(alpha = 0.6f) else Color.Transparent,
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { onTabSelected(index) }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LampView(
                        color = item.second,
                        isLit = isSelected,
                        size = 12.dp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.first,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 10.sp,
                        color = if (isSelected) Color.White else Color(0xFF7E869E)
                    )
                }
            }
        }
    }
}
