package com.uvos.terminal.ui

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.uvos.terminal.core.ColorProfile
import com.uvos.terminal.core.TerminalColors
import com.uvos.terminal.core.TerminalSession
import com.uvos.terminal.monitor.SystemMetricsMonitor
import com.uvos.terminal.ui.components.AccessoryBarView
import com.uvos.terminal.ui.components.GaugeHeaderView
import com.uvos.terminal.ui.components.KeyModifierState
import com.uvos.terminal.ui.components.MacroPanelSheet

@Composable
fun TerminalScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val session = remember { TerminalSession(context, scope) }
    val metricsMonitor = remember { SystemMetricsMonitor(scope) }

    var currentProfile by remember { mutableStateOf(ColorProfile.CRIMSON) }
    val colorScheme = remember(currentProfile) { TerminalColors.getScheme(currentProfile) }

    var ctrlState by remember { mutableStateOf(KeyModifierState.OFF) }
    var altState by remember { mutableStateOf(KeyModifierState.OFF) }
    var isMacroSheetOpen by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val metrics by metricsMonitor.metrics.collectAsState()

    // Initialize session and monitor
    LaunchedEffect(Unit) {
        session.start()
        metricsMonitor.start(session.isPtyActive, session.isRecording)
        focusRequester.requestFocus()
    }

    DisposableEffect(Unit) {
        onDispose {
            metricsMonitor.stop()
            session.destroy()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .background(colorScheme.background),
        containerColor = colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // 1. Top Industrial Instrument Gauge Header
            GaugeHeaderView(
                metrics = metrics,
                currentProfile = currentProfile,
                onCycleProfile = {
                    currentProfile = when (currentProfile) {
                        ColorProfile.CRIMSON -> ColorProfile.AMBER
                        ColorProfile.AMBER -> ColorProfile.GREEN
                        ColorProfile.GREEN -> ColorProfile.CRIMSON
                    }
                },
                onToggleRecording = {
                    session.toggleAuditRecording()
                },
                onOpenMacros = {
                    isMacroSheetOpen = true
                }
            )

            // 2. Center CRT Console with Terminal Text Grid
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                CrtShaderRenderer(
                    scheme = colorScheme,
                    scanlinesEnabled = true,
                    vignetteEnabled = true,
                    glowEnabled = true,
                    modifier = Modifier.fillMaxSize()
                ) {
                    TerminalView(
                        session = session,
                        colorScheme = colorScheme,
                        onTap = {
                            focusRequester.requestFocus()
                            keyboardController?.show()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Invisible input bridge for soft keyboard capture
                var hiddenText by remember { mutableStateOf(TextFieldValue("")) }

                BasicTextField(
                    value = hiddenText,
                    onValueChange = { newValue ->
                        val input = newValue.text
                        if (input.isNotEmpty()) {
                            for (ch in input) {
                                val isCtrl = ctrlState.isActive
                                val isAlt = altState.isActive
                                if (isCtrl || isAlt) {
                                    session.sendControlKey(ch, isCtrl, isAlt)
                                    if (ctrlState == KeyModifierState.LATCHED) ctrlState = KeyModifierState.OFF
                                    if (altState == KeyModifierState.LATCHED) altState = KeyModifierState.OFF
                                } else {
                                    session.write(ch.toString())
                                }
                            }
                            hiddenText = TextFieldValue("")
                        }
                    },
                    modifier = Modifier
                        .size(1.dp)
                        .focusRequester(focusRequester)
                        .onKeyEvent { keyEvent ->
                            if (keyEvent.type == KeyEventType.KeyDown) {
                                when (keyEvent.key) {
                                    Key.Enter -> {
                                        session.sendSpecialKey("ENTER")
                                        true
                                    }
                                    Key.Backspace -> {
                                        session.sendSpecialKey("BACKSPACE")
                                        true
                                    }
                                    Key.Tab -> {
                                        session.sendSpecialKey("TAB")
                                        true
                                    }
                                    Key.DirectionUp -> {
                                        session.sendSpecialKey("UP")
                                        true
                                    }
                                    Key.DirectionDown -> {
                                        session.sendSpecialKey("DOWN")
                                        true
                                    }
                                    Key.DirectionLeft -> {
                                        session.sendSpecialKey("LEFT")
                                        true
                                    }
                                    Key.DirectionRight -> {
                                        session.sendSpecialKey("RIGHT")
                                        true
                                    }
                                    else -> false
                                }
                            } else {
                                false
                            }
                        },
                    cursorBrush = SolidColor(colorScheme.cursor)
                )
            }

            // 3. Bottom Accessory Bar (ESC, TAB, CTRL, ALT, Special Keys)
            AccessoryBarView(
                ctrlState = ctrlState,
                altState = altState,
                onCtrlToggle = { ctrlState = ctrlState.next() },
                onAltToggle = { altState = altState.next() },
                onKeyClick = { key ->
                    when (key) {
                        "ESC", "TAB", "PIPE", "SLASH", "DASH", "UNDERSCORE", "TILDE",
                        "UP", "DOWN", "LEFT", "RIGHT" -> {
                            session.sendSpecialKey(key)
                        }
                    }
                }
            )
        }

        // Quick Macro Sheet
        MacroPanelSheet(
            isOpen = isMacroSheetOpen,
            onDismiss = { isMacroSheetOpen = false },
            onExecuteMacro = { command ->
                session.write(command)
            }
        )
    }
}
