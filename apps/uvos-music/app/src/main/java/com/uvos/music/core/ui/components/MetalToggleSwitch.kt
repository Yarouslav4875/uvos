package com.uvos.music.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uvos.music.core.ui.theme.M4200AccentCyan
import com.uvos.music.core.ui.theme.M4200AmberGlow
import com.uvos.music.core.ui.theme.M4200BrassCap
import com.uvos.music.core.ui.theme.M4200ChassisBezel
import com.uvos.music.core.ui.theme.M4200ChassisBorder
import com.uvos.music.core.ui.theme.M4200MetalDark
import com.uvos.music.core.ui.theme.M4200MetalSilver
import com.uvos.music.core.ui.theme.M4200OverloadRed
import com.uvos.music.core.ui.theme.M4200ScaleBlack
import com.uvos.music.core.ui.theme.M4200TextDim
import com.uvos.music.core.ui.theme.M4200TextPrimary
import com.uvos.music.core.ui.theme.M4200TextSecondary

/**
 * Heavy-duty studio rack metal toggle switch with pilot LED indicator and haptic feedback.
 */
@Composable
fun MetalToggleSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    height: Dp = 56.dp,
    width: Dp = 36.dp,
    ledColor: Color = M4200AmberGlow
) {
    val haptic = LocalHapticFeedback.current
    val leverProgress by animateFloatAsState(
        targetValue = if (checked) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 140),
        label = "LeverAnimation"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Pilot LED indicator
        Box(
            modifier = Modifier.size(8.dp)
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.width / 2f

                if (checked) {
                    // Glow
                    drawCircle(
                        color = ledColor.copy(alpha = 0.4f),
                        radius = radius * 1.8f,
                        center = center
                    )
                    // Bright Core
                    drawCircle(
                        color = ledColor,
                        radius = radius,
                        center = center
                    )
                } else {
                    // Dim off state
                    drawCircle(
                        color = M4200MetalDark,
                        radius = radius,
                        center = center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Switch body and mechanical lever
        Box(
            modifier = Modifier
                .width(width)
                .height(height)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onCheckedChange(!checked)
                }
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                val centerX = w / 2f

                // 1. Threaded collar / chassis plate background
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(M4200ChassisBezel, M4200ScaleBlack)
                    ),
                    topLeft = Offset(2f, 2f),
                    size = Size(w - 4f, h - 4f),
                    cornerRadius = CornerRadius(6f, 6f)
                )

                // Hex nut collar
                drawRoundRect(
                    color = M4200ChassisBorder,
                    topLeft = Offset(2f, 2f),
                    size = Size(w - 4f, h - 4f),
                    cornerRadius = CornerRadius(6f, 6f),
                    style = Stroke(width = 1.5f)
                )

                // 2. Lever slot / bushing hole
                val slotH = h * 0.65f
                val slotW = 10f
                val slotTop = (h - slotH) / 2f
                drawRoundRect(
                    color = M4200ScaleBlack,
                    topLeft = Offset(centerX - slotW / 2f, slotTop),
                    size = Size(slotW, slotH),
                    cornerRadius = CornerRadius(4f, 4f)
                )

                // 3. Bat-handle Metal Lever
                // Interpolate lever tip Y between top (checked) and bottom (unchecked)
                val tipY = (slotTop + 4f) + (1f - leverProgress) * (slotH - 8f)
                val pivotY = h / 2f

                // Lever shadow
                drawLine(
                    color = Color.Black.copy(alpha = 0.6f),
                    start = Offset(centerX + 2f, pivotY + 2f),
                    end = Offset(centerX + 2f, tipY + 2f),
                    strokeWidth = 6f,
                    cap = StrokeCap.Round
                )

                // Metallic Lever Stem
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(M4200MetalSilver, M4200MetalDark, M4200MetalSilver)
                    ),
                    start = Offset(centerX, pivotY),
                    end = Offset(centerX, tipY),
                    strokeWidth = 5f,
                    cap = StrokeCap.Round
                )

                // Machined Ball/Bat Handle Tip
                val ballRadius = 6f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White, M4200MetalSilver, M4200MetalDark),
                        center = Offset(centerX - 1.5f, tipY - 1.5f),
                        radius = ballRadius * 1.2f
                    ),
                    radius = ballRadius,
                    center = Offset(centerX, tipY)
                )

                // Pivot Base ring
                drawCircle(
                    color = M4200BrassCap,
                    radius = 4f,
                    center = Offset(centerX, pivotY),
                    style = Stroke(width = 1.5f)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Switch Label
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = if (checked) M4200TextPrimary else M4200TextDim
        )
    }
}
