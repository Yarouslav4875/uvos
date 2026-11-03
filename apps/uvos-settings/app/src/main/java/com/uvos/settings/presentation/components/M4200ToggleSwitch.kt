package com.uvos.settings.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uvos.settings.domain.model.SystemProfile
import com.uvos.settings.presentation.theme.*

/**
 * Трёхпозиционный промышленный тумблер выбора профиля системы UVOS
 * с металлическим рычагом кулисы и контрольными лампами индикации.
 */
@Composable
fun M4200ToggleSwitch(
    currentProfile: SystemProfile,
    onProfileSelected: (SystemProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    val profiles = listOf(SystemProfile.STERILE, SystemProfile.COMMUNICATION, SystemProfile.CUSTOM)
    val selectedIndex = profiles.indexOf(currentProfile).coerceAtLeast(0)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(UvosDarkSurface, RoundedCornerShape(16.dp))
            .border(1.5.dp, UvosDarkSurfaceBorder, RoundedCornerShape(16.dp))
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "СЕЛЕКТОР ПРОФИЛЯ UVOS",
            color = UvosTextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.5.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            profiles.forEach { profile ->
                val isSelected = profile == currentProfile
                val lampColor = when (profile) {
                    SystemProfile.STERILE -> if (isSelected) UvosRed else Color(0xFF3A0D0D)
                    SystemProfile.COMMUNICATION -> if (isSelected) UvosAmberLamp else Color(0xFF382700)
                    SystemProfile.CUSTOM -> if (isSelected) UvosGreenLamp else Color(0xFF063013)
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onProfileSelected(profile) }
                ) {
                    // Контрольная лампа состояния
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .shadow(if (isSelected) 8.dp else 0.dp, CircleShape, spotColor = lampColor)
                            .background(lampColor, CircleShape)
                            .border(1.dp, Color(0xFF43485E), CircleShape)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Обозначение позиции тумблера
                    Text(
                        text = profile.displayName,
                        color = if (isSelected) UvosTextPrimary else Color(0xFF565B70),
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Металлическая кулиса механизма тумблера
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(UvosCutoutBg)
                .border(1.dp, Color(0xFF202330), RoundedCornerShape(15.dp))
                .padding(3.dp)
        ) {
            val alignBias by animateFloatAsState(
                targetValue = when (selectedIndex) {
                    0 -> -1f
                    1 -> 0f
                    else -> 1f
                },
                animationSpec = spring(dampingRatio = 0.7f, stiffness = 150f),
                label = "ToggleLeverBias"
            )

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.33f)
                    .align(Alignment(alignBias, 0f))
                    .clip(RoundedCornerShape(11.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF8E93A6), Color(0xFF4C5061), Color(0xFF2C303E))
                        )
                    )
                    .border(1.dp, Color(0xFFAEB4CC), RoundedCornerShape(11.dp))
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Описание выбранного режима
        Text(
            text = currentProfile.description,
            color = UvosTextSecondary,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 12.sp
        )
    }
}
