package com.uvos.gallery.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.uvos.gallery.domain.model.ThreatLevel
import com.uvos.gallery.presentation.theme.UvosAmberWarning
import com.uvos.gallery.presentation.theme.UvosBackground
import com.uvos.gallery.presentation.theme.UvosBlueEncrypted
import com.uvos.gallery.presentation.theme.UvosGreenSterile
import com.uvos.gallery.presentation.theme.UvosRedDanger

@Composable
fun ThreatIndicatorBadge(
    threatLevel: ThreatLevel,
    modifier: Modifier = Modifier,
    size: Dp = 14.dp,
    onClick: (() -> Unit)? = null
) {
    val badgeColor = when (threatLevel) {
        ThreatLevel.HIGH_RISK -> UvosRedDanger
        ThreatLevel.MEDIUM_RISK -> UvosAmberWarning
        ThreatLevel.STERILE -> UvosGreenSterile
        ThreatLevel.ENCRYPTED -> UvosBlueEncrypted
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(badgeColor)
            .border(1.5.dp, UvosBackground, CircleShape)
            .then(
                if (onClick != null) Modifier.clickable { onClick() } else Modifier
            )
    )
}
