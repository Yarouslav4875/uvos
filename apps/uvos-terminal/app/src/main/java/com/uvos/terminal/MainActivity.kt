package com.uvos.terminal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.uvos.terminal.ui.TerminalScreen
import com.uvos.terminal.ui.theme.UVOSTerminalTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            UVOSTerminalTheme {
                TerminalScreen()
            }
        }
    }
}
