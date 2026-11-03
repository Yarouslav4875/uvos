package com.uvos.gallery.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uvos.gallery.presentation.theme.UvosDialIvory
import com.uvos.gallery.presentation.theme.UvosGreenSterile
import com.uvos.gallery.presentation.theme.UvosMuted
import com.uvos.gallery.presentation.theme.UvosTileSurface

@Composable
fun RetroToolbar(
    itemCount: Int,
    vaultCount: Int,
    currentTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(UvosTileSurface)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Upper telemetry readout
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "UVOS // GALLERY M4200",
                    color = UvosDialIvory,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "CORE: AIR-GAPPED AIR-DROP ISOLATION",
                    color = UvosMuted,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Zero-Net Hardware Status LED
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF14161E))
                    .border(1.dp, Color(0xFF2A2D3A), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(UvosGreenSterile)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "ZERO-NET: ACTIVE",
                    color = UvosGreenSterile,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Hardware Instrument Dual Selector (MediaStore vs Air-Gapped Vault)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF0E0E12))
                .padding(2.dp)
        ) {
            SelectorTab(
                text = "ОТКРЫТОЕ ХРАНИЛИЩЕ [$itemCount]",
                isSelected = currentTab == 0,
                modifier = Modifier.weight(1f),
                onClick = { onTabSelected(0) }
            )
            SelectorTab(
                text = "КРИПТО-СЕЙФ [$vaultCount]",
                isSelected = currentTab == 1,
                modifier = Modifier.weight(1f),
                onClick = { onTabSelected(1) }
            )
        }
    }
}

@Composable
private fun SelectorTab(
    text: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) Color(0xFF2A2D3A) else Color.Transparent)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.text.ClickableText(
            text = androidx.compose.ui.text.AnnotatedString(text),
            onClick = { onClick() },
            style = androidx.compose.ui.text.TextStyle(
                color = if (isSelected) UvosGreenSterile else UvosMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        )
    }
}
