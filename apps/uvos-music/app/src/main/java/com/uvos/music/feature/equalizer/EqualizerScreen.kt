package com.uvos.music.feature.equalizer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uvos.music.core.ui.components.MetalToggleSwitch
import com.uvos.music.core.ui.components.RotaryKnob
import com.uvos.music.core.ui.components.StudioFader
import com.uvos.music.core.ui.theme.M4200AccentCyan
import com.uvos.music.core.ui.theme.M4200AmberGlow
import com.uvos.music.core.ui.theme.M4200ChassisBezel
import com.uvos.music.core.ui.theme.M4200ChassisBorder
import com.uvos.music.core.ui.theme.M4200ChassisCard
import com.uvos.music.core.ui.theme.M4200MetalSilver
import com.uvos.music.core.ui.theme.M4200OverloadRed
import com.uvos.music.core.ui.theme.M4200TextDim
import com.uvos.music.core.ui.theme.M4200TextPrimary
import com.uvos.music.core.ui.theme.M4200TextSecondary
import com.uvos.music.core.ui.theme.StudioIcons
import com.uvos.music.domain.model.EqualizerPreset

@Composable
fun EqualizerScreen(
    viewModel: EqualizerViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()
    val presetScrollState = rememberScrollState()
    val faderScrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = M4200AmberGlow,
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = "MASTERING RACK & EQUALIZER",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = M4200TextSecondary,
                letterSpacing = 1.2.sp
            )

            IconButton(onClick = { viewModel.resetToFlat() }) {
                Icon(
                    imageVector = StudioIcons.RestartAlt,
                    contentDescription = "Reset",
                    tint = M4200MetalSilver,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Analog Console Mastering Control Unit (Toggles & Potentiometers)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(M4200ChassisCard)
                .border(1.dp, M4200ChassisBorder, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "ANALOG SATURATION & GAIN STAGE",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = M4200TextDim,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Master EQ Switch
                    MetalToggleSwitch(
                        checked = state.isEnabled,
                        onCheckedChange = { viewModel.setEnabled(it) },
                        label = "EQ ACTIVE",
                        ledColor = M4200AmberGlow
                    )

                    // Warm Tube Saturation Switch
                    MetalToggleSwitch(
                        checked = state.warmTube,
                        onCheckedChange = { viewModel.setWarmTube(it) },
                        label = "WARM TUBE",
                        ledColor = M4200OverloadRed
                    )

                    // Bass Boost Rotary Knob
                    RotaryKnob(
                        value = state.bassBoost,
                        onValueChange = { viewModel.setBassBoost(it) },
                        label = "BASS",
                        valueDisplay = "${(state.bassBoost * 100).toInt()}%",
                        size = 56.dp,
                        tintColor = M4200AmberGlow
                    )

                    // Pre-Gain Rotary Knob (-6 dB .. +6 dB)
                    val preGainNormalized = ((state.preGainDb + 6f) / 12f).coerceIn(0f, 1f)
                    RotaryKnob(
                        value = preGainNormalized,
                        onValueChange = { norm ->
                            val db = (norm * 12f) - 6f
                            viewModel.setPreGain(db)
                        },
                        label = "GAIN",
                        valueDisplay = "%+.1fdB".format(state.preGainDb),
                        size = 56.dp,
                        tintColor = M4200AccentCyan
                    )

                    // Balance Knob (-1.0 .. +1.0)
                    val balanceNorm = ((state.balance + 1f) / 2f).coerceIn(0f, 1f)
                    val balanceLabel = when {
                        state.balance < -0.1f -> "L%d".format((-state.balance * 100).toInt())
                        state.balance > 0.1f -> "R%d".format((state.balance * 100).toInt())
                        else -> "C"
                    }
                    RotaryKnob(
                        value = balanceNorm,
                        onValueChange = { norm ->
                            val pan = (norm * 2f) - 1f
                            viewModel.setBalance(pan)
                        },
                        label = "PAN",
                        valueDisplay = balanceLabel,
                        size = 56.dp,
                        tintColor = M4200MetalSilver
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Preset Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(presetScrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (preset in EqualizerPreset.PRESETS) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(M4200ChassisBezel)
                        .border(1.dp, M4200ChassisBorder, RoundedCornerShape(6.dp))
                        .clickable { viewModel.applyPreset(preset) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = preset.name,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = M4200TextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 10-Band ISO Studio Fader Rack
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(M4200ChassisCard)
                .border(1.dp, M4200ChassisBorder, RoundedCornerShape(12.dp))
                .padding(vertical = 14.dp, horizontal = 8.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "10-BAND ISO PRECISION GRAPHIC EQUALIZER",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = M4200TextDim,
                        letterSpacing = 1.sp
                    )

                    Text(
                        text = "RANGE: ±12 dB",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = M4200AmberGlow
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(faderScrollState),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (band in state.bands) {
                        StudioFader(
                            gainDb = band.gainDb,
                            onGainChange = { newGain ->
                                viewModel.setBandGain(band.index, newGain)
                            },
                            frequencyLabel = band.label,
                            height = 200.dp,
                            width = 38.dp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
    }
}
