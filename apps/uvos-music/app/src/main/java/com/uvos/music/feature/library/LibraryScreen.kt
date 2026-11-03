package com.uvos.music.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uvos.music.core.ui.theme.M4200AccentCyan
import com.uvos.music.core.ui.theme.M4200AmberGlow
import com.uvos.music.core.ui.theme.M4200ChassisBezel
import com.uvos.music.core.ui.theme.M4200ChassisBorder
import com.uvos.music.core.ui.theme.M4200ChassisCard
import com.uvos.music.core.ui.theme.M4200MetalSilver
import com.uvos.music.core.ui.theme.M4200TextDim
import com.uvos.music.core.ui.theme.M4200TextPrimary
import com.uvos.music.core.ui.theme.M4200TextSecondary
import com.uvos.music.core.ui.theme.StudioIcons
import com.uvos.music.domain.model.AudioTrack

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onNavigateToPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateToPlayer) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Player",
                    tint = M4200AmberGlow,
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = "OFFLINE MEDIA VAULT",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = M4200TextSecondary,
                letterSpacing = 1.2.sp
            )

            IconButton(onClick = { viewModel.loadMedia() }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Rescan Media",
                    tint = M4200MetalSilver,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Search Input Bar
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = "Search tracks, artists, albums...",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = M4200TextDim
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = M4200TextSecondary
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = M4200ChassisCard,
                unfocusedContainerColor = M4200ChassisCard,
                focusedBorderColor = M4200AmberGlow,
                unfocusedBorderColor = M4200ChassisBorder,
                focusedTextColor = M4200TextPrimary,
                unfocusedTextColor = M4200TextPrimary
            ),
            shape = RoundedCornerShape(8.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Filter Tabs
        val tabs = LibraryTab.values()
        TabRow(
            selectedTabIndex = uiState.selectedTab.ordinal,
            containerColor = M4200ChassisCard,
            contentColor = M4200AmberGlow,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[uiState.selectedTab.ordinal]),
                    color = M4200AmberGlow
                )
            }
        ) {
            tabs.forEach { tab ->
                Tab(
                    selected = uiState.selectedTab == tab,
                    onClick = { viewModel.selectTab(tab) },
                    text = {
                        Text(
                            text = when (tab) {
                                LibraryTab.ALL_TRACKS -> "ALL"
                                LibraryTab.HI_RES -> "HI-RES"
                                LibraryTab.ARTISTS -> "ARTISTS"
                                LibraryTab.ALBUMS -> "ALBUMS"
                            },
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Play All / Count Info
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${uiState.filteredTracks.size} TRACKS FOUND",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = M4200TextDim,
                letterSpacing = 1.sp
            )

            if (uiState.filteredTracks.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(M4200ChassisBezel)
                        .border(1.dp, M4200ChassisBorder, RoundedCornerShape(6.dp))
                        .clickable {
                            viewModel.playAll()
                            onNavigateToPlayer()
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play All",
                        tint = M4200AmberGlow,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "PLAY ALL",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = M4200AmberGlow
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Track List
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = M4200AmberGlow)
            }
        } else if (uiState.filteredTracks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (uiState.searchQuery.isNotEmpty()) "No matching tracks found." else "No local media files detected.\nPlace audio files in Music or Storage folder.",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = M4200TextDim,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(uiState.filteredTracks, key = { it.id }) { track ->
                    TrackListItem(
                        track = track,
                        onClick = {
                            viewModel.playTrack(track)
                            onNavigateToPlayer()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackListItem(
    track: AudioTrack,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(M4200ChassisCard)
            .border(1.dp, M4200ChassisBorder, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Equalizer Icon Box
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(M4200ChassisBezel)
                    .border(1.dp, M4200ChassisBorder, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = StudioIcons.GraphicEq,
                    contentDescription = null,
                    tint = if (track.formatInfo.isHiRes) M4200AccentCyan else M4200AmberGlow,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = M4200TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${track.artist}  •  ${track.album}",
                    fontFamily = FontFamily.Default,
                    fontSize = 11.sp,
                    color = M4200TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(horizontalAlignment = Alignment.End) {
            // Format Badge (FLAC 24/96, etc.)
            val fmt = track.formatInfo
            val fmtText = "${fmt.container.uppercase()} ${fmt.bitDepth}b"
            Text(
                text = fmtText,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (fmt.isLossless) M4200AccentCyan else M4200TextDim
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = formatDuration(track.durationMs),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = M4200TextDim
            )
        }
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
