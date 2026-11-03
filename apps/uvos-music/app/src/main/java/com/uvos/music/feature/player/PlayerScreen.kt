package com.uvos.music.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.uvos.music.core.ui.components.StereoVuPanel
import com.uvos.music.core.ui.theme.M4200AccentCyan
import com.uvos.music.core.ui.theme.M4200AmberGlow
import com.uvos.music.core.ui.theme.M4200ChassisBezel
import com.uvos.music.core.ui.theme.M4200ChassisBorder
import com.uvos.music.core.ui.theme.M4200ChassisCard
import com.uvos.music.core.ui.theme.M4200MetalDark
import com.uvos.music.core.ui.theme.M4200MetalSilver
import com.uvos.music.core.ui.theme.M4200OverloadRed
import com.uvos.music.core.ui.theme.M4200ScaleBlack
import com.uvos.music.core.ui.theme.M4200TextDim
import com.uvos.music.core.ui.theme.M4200TextPrimary
import com.uvos.music.core.ui.theme.M4200TextSecondary
import com.uvos.music.core.ui.theme.StudioIcons

@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onNavigateToEqualizer: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val vuLevels by viewModel.vuLevelsFlow.collectAsState()

    val track = uiState.currentTrack
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Studio Top Bar Navigation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateToLibrary) {
                Icon(
                    imageVector = StudioIcons.QueueMusic,
                    contentDescription = "Library",
                    tint = M4200AmberGlow,
                    modifier = Modifier.size(26.dp)
                )
            }

            Text(
                text = "UVOS MASTER MONITOR",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = M4200TextSecondary,
                letterSpacing = 1.2.sp
            )

            IconButton(onClick = onNavigateToEqualizer) {
                Icon(
                    imageVector = StudioIcons.Equalizer,
                    contentDescription = "Equalizer",
                    tint = M4200AmberGlow,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Stereo Dual M4200 VU-Meter Panel
        StereoVuPanel(
            levels = vuLevels,
            padDb = uiState.vuPadDb,
            onTogglePad = { viewModel.toggleVuPad() }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Track Information Console
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(M4200ChassisCard)
                .border(1.dp, M4200ChassisBorder, RoundedCornerShape(10.dp))
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = track?.title ?: "No Media Loaded",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = M4200TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (track != null) "${track.artist}  •  ${track.album}" else "Select tracks from studio library",
                    fontFamily = FontFamily.Default,
                    fontSize = 13.sp,
                    color = M4200TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Hi-Res Audio Format Badge
                val formatText = track?.formatInfo?.let { fmt ->
                    val isLossless = fmt.isLossless
                    val rate = fmt.sampleRate / 1000
                    "${fmt.container.uppercase()} ${fmt.bitDepth}-bit / ${rate} kHz • ${if (isLossless) "LOSSLESS" else "${fmt.bitrateKbps} kbps"}"
                } ?: "DIRECT BIT-PERFECT PCM"

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(M4200ChassisBezel)
                        .border(1.dp, M4200ChassisBorder, RoundedCornerShape(4.dp))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = formatText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = M4200AccentCyan,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Seekbar & Timing Readout
        val duration = uiState.durationMs
        val position = uiState.currentPositionMs.coerceAtMost(duration)
        val progress = if (duration > 0) (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f

        Column(modifier = Modifier.fillMaxWidth()) {
            Slider(
                value = progress,
                onValueChange = { frac ->
                    val targetMs = (frac * duration).toLong()
                    viewModel.seekTo(targetMs)
                },
                colors = SliderDefaults.colors(
                    thumbColor = M4200AmberGlow,
                    activeTrackColor = M4200AmberGlow,
                    inactiveTrackColor = M4200ChassisBorder
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(position),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = M4200TextSecondary
                )
                Text(
                    text = formatTime(duration),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = M4200TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Vintage Studio Transport Control Rack
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(M4200ChassisCard)
                .border(1.dp, M4200ChassisBorder, RoundedCornerShape(12.dp))
                .padding(vertical = 10.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shuffle Button
            IconButton(onClick = { viewModel.toggleShuffle() }) {
                Icon(
                    imageVector = StudioIcons.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (uiState.isShuffleEnabled) M4200AmberGlow else M4200TextDim,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Skip Previous
            IconButton(onClick = { viewModel.skipToPrevious() }) {
                Icon(
                    imageVector = StudioIcons.SkipPrevious,
                    contentDescription = "Previous",
                    tint = M4200MetalSilver,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Heavy-Duty Master Play/Pause Switch Button
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(M4200AmberGlow, M4200AmberGlow.copy(alpha = 0.8f), M4200MetalDark)
                        )
                    )
                    .border(2.dp, M4200AmberGlow, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = { viewModel.togglePlayPause() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = if (uiState.isPlaying) StudioIcons.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                        tint = M4200ScaleBlack,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // Skip Next
            IconButton(onClick = { viewModel.skipToNext() }) {
                Icon(
                    imageVector = StudioIcons.SkipNext,
                    contentDescription = "Next",
                    tint = M4200MetalSilver,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Repeat Mode Button
            IconButton(onClick = { viewModel.toggleRepeat() }) {
                val icon = when (uiState.repeatMode) {
                    Player.REPEAT_MODE_ONE -> StudioIcons.RepeatOne
                    else -> StudioIcons.Repeat
                }
                val tint = when (uiState.repeatMode) {
                    Player.REPEAT_MODE_OFF -> M4200TextDim
                    else -> M4200AmberGlow
                }
                Icon(
                    imageVector = icon,
                    contentDescription = "Repeat",
                    tint = tint,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
