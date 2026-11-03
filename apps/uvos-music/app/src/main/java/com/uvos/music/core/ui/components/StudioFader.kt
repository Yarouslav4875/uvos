package com.uvos.music.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uvos.music.core.ui.theme.M4200AccentCyan
import com.uvos.music.core.ui.theme.M4200AmberGlow
import com.uvos.music.core.ui.theme.M4200ChassisBezel
import com.uvos.music.core.ui.theme.M4200ChassisBorder
import com.uvos.music.core.ui.theme.M4200MetalDark
import com.uvos.music.core.ui.theme.M4200MetalSilver
import com.uvos.music.core.ui.theme.M4200OverloadRed
import com.uvos.music.core.ui.theme.M4200ScaleBlack
import com.uvos.music.core.ui.theme.M4200TextDim
import com.uvos.music.core.ui.theme.M4200TextPrimary
import com.uvos.music.core.ui.theme.M4200TextSecondary
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Vintage Studio Console Vertical Equalizer Fader.
 * Range: -12.0 dB to +12.0 dB with center detent at 0.0 dB.
 */
@Composable
fun StudioFader(
    gainDb: Float, // -12.0f .. +12.0f
    onGainChange: (Float) -> Unit,
    frequencyLabel: String,
    modifier: Modifier = Modifier,
    height: Dp = 180.dp,
    width: Dp = 32.dp
) {
    val haptic = LocalHapticFeedback.current
    var lastHapticGain by remember { mutableFloatStateOf(gainDb) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Gain readout at top (e.g. +3.5 or 0.0)
        val gainText = when {
            gainDb > 0.05f -> "+%.1f".format(gainDb)
            abs(gainDb) <= 0.05f -> "0.0"
            else -> "%.1f".format(gainDb)
        }
        val isBoost = gainDb > 0.05f
        val isCut = gainDb < -0.05f

        Text(
            text = gainText,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = when {
                isBoost -> M4200AmberGlow
                isCut -> M4200AccentCyan
                else -> M4200TextSecondary
            }
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Vertical Fader Slot with Metal Knob
        Box(
            modifier = Modifier
                .width(width)
                .height(height)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        // Moving up increases gain, moving down decreases
                        val heightPx = size.height.toFloat()
                        val deltaDb = (-dragAmount.y / heightPx) * 24.0f
                        var newGain = (gainDb + deltaDb).coerceIn(-12.0f, 12.0f)

                        // Snap to center (0.0 dB) if within 0.4 dB
                        if (abs(newGain) < 0.4f && abs(gainDb) >= 0.4f) {
                            newGain = 0.0f
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        } else if (abs(newGain - lastHapticGain) > 1.5f) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            lastHapticGain = newGain
                        }

                        onGainChange(newGain)
                    }
                }
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                val centerX = w / 2f

                val marginY = 16f
                val trackTop = marginY
                val trackBottom = h - marginY
                val trackHeight = trackBottom - trackTop

                // Normalized position: 0.0 at top (+12 dB), 1.0 at bottom (-12 dB)
                val normPos = 1.0f - ((gainDb + 12.0f) / 24.0f)
                val knobCenterY = trackTop + (normPos * trackHeight)

                // 1. Draw Slot/Track Cutout
                val slotWidth = 6f
                drawRoundRect(
                    color = M4200ScaleBlack,
                    topLeft = Offset(centerX - slotWidth / 2f, trackTop),
                    size = Size(slotWidth, trackHeight),
                    cornerRadius = CornerRadius(3f, 3f)
                )

                // Slot Bezel outline
                drawRoundRect(
                    color = M4200ChassisBorder,
                    topLeft = Offset(centerX - slotWidth / 2f, trackTop),
                    size = Size(slotWidth, trackHeight),
                    cornerRadius = CornerRadius(3f, 3f),
                    style = Stroke(width = 1.2f)
                )

                // 2. Scale Tick Marks: +12, +6, 0, -6, -12
                val dbTicks = listOf(12f, 6f, 0f, -6f, -12f)
                for (tickDb in dbTicks) {
                    val tickNorm = 1.0f - ((tickDb + 12.0f) / 24.0f)
                    val tickY = trackTop + (tickNorm * trackHeight)
                    val isZero = tickDb == 0f
                    val tickLength = if (isZero) 14f else 8f
                    val tickColor = if (isZero) M4200AmberGlow else M4200MetalDark

                    drawLine(
                        color = tickColor,
                        start = Offset(centerX - tickLength, tickY),
                        end = Offset(centerX + tickLength, tickY),
                        strokeWidth = if (isZero) 2f else 1.2f,
                        cap = StrokeCap.Round
                    )
                }

                // 3. Draw Vintage Brushed Metallic Slider Cap (Fader Handle)
                val knobW = w - 6f
                val knobH = 26f
                val knobTop = knobCenterY - knobH / 2f
                val knobLeft = centerX - knobW / 2f

                // Drop shadow
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.5f),
                    topLeft = Offset(knobLeft + 1f, knobTop + 3f),
                    size = Size(knobW, knobH),
                    cornerRadius = CornerRadius(4f, 4f)
                )

                // Metal Handle Body Gradient (Brushed aluminum look)
                val knobBrush = Brush.verticalGradient(
                    colors = listOf(
                        M4200MetalSilver,
                        M4200MetalDark,
                        M4200MetalSilver.copy(alpha = 0.85f),
                        M4200MetalDark
                    ),
                    startY = knobTop,
                    endY = knobTop + knobH
                )

                drawRoundRect(
                    brush = knobBrush,
                    topLeft = Offset(knobLeft, knobTop),
                    size = Size(knobW, knobH),
                    cornerRadius = CornerRadius(4f, 4f)
                )

                // Bezel stroke
                drawRoundRect(
                    color = M4200ChassisBorder,
                    topLeft = Offset(knobLeft, knobTop),
                    size = Size(knobW, knobH),
                    cornerRadius = CornerRadius(4f, 4f),
                    style = Stroke(width = 1.5f)
                )

                // Center White Indicator Line on Fader Knob
                drawLine(
                    color = if (isBoost) M4200AmberGlow else if (isCut) M4200AccentCyan else Color.White,
                    start = Offset(knobLeft + 3f, knobCenterY),
                    end = Offset(knobLeft + knobW - 3f, knobCenterY),
                    strokeWidth = 2.5f,
                    cap = StrokeCap.Round
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Center Frequency Label at bottom (e.g. "31", "1k", "16k")
        Text(
            text = frequencyLabel,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = M4200TextPrimary
        )
    }
}
