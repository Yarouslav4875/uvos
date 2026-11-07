package com.uvos.clock.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uvos.clock.ui.theme.LampCyanDark
import com.uvos.clock.ui.theme.LampCyanLit
import com.uvos.clock.ui.theme.LampGreenDark
import com.uvos.clock.ui.theme.LampGreenLit
import com.uvos.clock.ui.theme.LampOrangeDark
import com.uvos.clock.ui.theme.LampOrangeLit
import com.uvos.clock.ui.theme.LampRedDark
import com.uvos.clock.ui.theme.LampRedLit
import com.uvos.clock.ui.theme.LampYellowDark
import com.uvos.clock.ui.theme.LampYellowLit

enum class LampColor {
    RED, YELLOW, ORANGE, CYAN, GREEN
}

@Composable
fun LampView(
    color: LampColor,
    isLit: Boolean = true,
    size: Dp = 24.dp,
    label: String? = null,
    isPulsing: Boolean = false,
    modifier: Modifier = Modifier
) {
    val (litColor, darkColor) = when (color) {
        LampColor.RED -> Pair(LampRedLit, LampRedDark)
        LampColor.YELLOW -> Pair(LampYellowLit, LampYellowDark)
        LampColor.ORANGE -> Pair(LampOrangeLit, LampOrangeDark)
        LampColor.CYAN -> Pair(LampCyanLit, LampCyanDark)
        LampColor.GREEN -> Pair(LampGreenLit, LampGreenDark)
    }

    val pulseAlpha by if (isPulsing && isLit) {
        val infiniteTransition = rememberInfiniteTransition(label = "LampPulse")
        infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAlpha"
        )
    } else {
        androidx.compose.runtime.remember(isLit) {
            androidx.compose.runtime.mutableFloatStateOf(if (isLit) 1.0f else 0.2f)
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val radius = this.size.minDimension / 2f
            val center = Offset(this.size.width / 2f, this.size.height / 2f)

            // Металлический ободок лампы (безель)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF6A728A), Color(0xFF222634)),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
            drawCircle(
                color = Color(0xFF151820),
                radius = radius * 0.88f,
                center = center
            )

            // Свечение (Glow halo)
            if (isLit) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            litColor.copy(alpha = 0.65f * pulseAlpha),
                            litColor.copy(alpha = 0.0f)
                        ),
                        center = center,
                        radius = radius * 1.5f
                    ),
                    radius = radius * 1.5f,
                    center = center
                )
            }

            // Стеклянная линза лампы
            val lensColor = if (isLit) litColor.copy(alpha = pulseAlpha) else darkColor
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        if (isLit) Color.White.copy(alpha = 0.8f * pulseAlpha) else lensColor,
                        lensColor
                    ),
                    center = Offset(center.x - radius * 0.2f, center.y - radius * 0.2f),
                    radius = radius * 0.75f
                ),
                radius = radius * 0.72f,
                center = center
            )

            // Блик на стекле
            drawCircle(
                color = Color.White.copy(alpha = if (isLit) 0.5f else 0.2f),
                radius = radius * 0.22f,
                center = Offset(center.x - radius * 0.28f, center.y - radius * 0.28f)
            )

            // Тонкая окантовка линзы
            drawCircle(
                color = Color(0xFF0F1117),
                radius = radius * 0.74f,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )
        }

        if (label != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = if (isLit) litColor else Color(0xFF6B7280)
            )
        }
    }
}
