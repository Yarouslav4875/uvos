package com.uvos.clock.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uvos.clock.ui.theme.BrassGold
import com.uvos.clock.ui.theme.NeedleRed

@Composable
fun RetroToggleSwitch(
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    labelTop: String = "ВКЛ",
    labelBottom: String = "ВЫКЛ",
    subLabel: String? = null,
    activeColor: Color = NeedleRed
) {
    // Анимация угла наклона рычага тумблера: -30 градусов (ВВЕРХ/ВКЛ), +30 градусов (ВНИЗ/ВЫКЛ)
    val leverAngle by animateFloatAsState(
        targetValue = if (isChecked) -28f else 28f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "leverAngle"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF14161E),
        modifier = modifier
            .padding(4.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                onCheckedChange(!isChecked)
            }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            if (subLabel != null) {
                Text(
                    text = subLabel,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFA0A6B8)
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Метка верхнего положения
            Text(
                text = labelTop,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = if (isChecked) activeColor else Color(0xFF4A4E5E)
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Графика механического тумблера
            Canvas(modifier = Modifier.size(width = 38.dp, height = 64.dp)) {
                val w = size.width
                val h = size.height

                // Металлическая пластина основания тумблера
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF323646), Color(0xFF1F222D), Color(0xFF2C303E))
                    ),
                    topLeft = Offset(0f, 0f),
                    size = Size(w, h),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
                drawRoundRect(
                    color = Color(0xFF485066),
                    topLeft = Offset(0f, 0f),
                    size = Size(w, h),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx())
                )

                // Винты крепления по углам пластины
                val screwR = 2.dp.toPx()
                listOf(
                    Offset(4.dp.toPx(), 4.dp.toPx()),
                    Offset(w - 4.dp.toPx(), 4.dp.toPx()),
                    Offset(4.dp.toPx(), h - 4.dp.toPx()),
                    Offset(w - 4.dp.toPx(), h - 4.dp.toPx())
                ).forEach { pos ->
                    drawCircle(color = Color(0xFF1A1D27), radius = screwR, center = pos)
                    drawLine(
                        color = Color(0xFF5A6278),
                        start = Offset(pos.x - screwR * 0.7f, pos.y),
                        end = Offset(pos.x + screwR * 0.7f, pos.y),
                        strokeWidth = 1f
                    )
                }

                // Внутренняя щель тумблера (темная прорезь)
                val slotW = 12.dp.toPx()
                val slotH = 40.dp.toPx()
                val slotX = (w - slotW) / 2f
                val slotY = (h - slotH) / 2f

                drawRoundRect(
                    color = Color(0xFF090A0E),
                    topLeft = Offset(slotX, slotY),
                    size = Size(slotW, slotH),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )

                // Гайка/втулка основания рычага
                val pivotCenter = Offset(w / 2f, h / 2f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(BrassGold, Color(0xFF4A380C)),
                        center = pivotCenter,
                        radius = 8.dp.toPx()
                    ),
                    radius = 8.dp.toPx(),
                    center = pivotCenter
                )

                // Металлический рычаг тумблера (вращается от центрального шарнира)
                rotate(degrees = leverAngle, pivot = pivotCenter) {
                    val leverLength = 22.dp.toPx()
                    val leverW = 4.dp.toPx()

                    // Стержень рычага
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFFE0E5F0), Color(0xFF8C95AB), Color(0xFF454B5C))
                        ),
                        start = pivotCenter,
                        end = Offset(pivotCenter.x, pivotCenter.y - leverLength),
                        strokeWidth = leverW
                    )

                    // Набалдашник рычага (хромированный шарик)
                    val tipCenter = Offset(pivotCenter.x, pivotCenter.y - leverLength)
                    val tipRadius = 5.dp.toPx()

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White,
                                if (isChecked) activeColor else Color(0xFFC0C7D8),
                                Color(0xFF282B36)
                            ),
                            center = Offset(tipCenter.x - 1.5f, tipCenter.y - 1.5f),
                            radius = tipRadius
                        ),
                        radius = tipRadius,
                        center = tipCenter
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Метка нижнего положения
            Text(
                text = labelBottom,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = if (!isChecked) Color(0xFFA0A6B8) else Color(0xFF4A4E5E)
            )
        }
    }
}
