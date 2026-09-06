package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.PatternClip
import com.example.model.PlayMode
import com.example.ui.theme.*

enum class ArrangementTool {
    DRAW, PAINT, ERASE, MUTE_TOOL, SLICE
}

data class ArrangementMarker(val bar: Int, val label: String, val color: Color)

@Composable
fun PlaylistArrangerView(
    clips: List<PatternClip>,
    currentBar: Int,
    isPlaying: Boolean,
    playMode: PlayMode,
    totalBars: Int,
    onToggleClip: (trackIndex: Int, barIndex: Int) -> Unit,
    onSwitchToSongMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Dynamic Custom Labels for Arranger Tracks
    var trackNames by remember {
        mutableStateOf(
            listOf(
                "Track 1: Drums 808",
                "Track 2: Bassline",
                "Track 3: Lead Synth",
                "Track 4: Guitar Clean",
                "Track 5: Guitar Distortion",
                "Track 6: Pluck Arp",
                "Track 7: Vocals",
                "Track 8: FX Ambience",
                "Track 9: Percussion",
                "Track 10: Synth Chords",
                "Track 11: Sub Bass",
                "Track 12: Synth Pad"
            )
        )
    }

    val defaultColors = listOf(
        0xFFFF6B00, 0xFF00F0FF, 0xFF05FFA1, 0xFFFF9E0B,
        0xFFEF4444, 0xFFB388FF, 0xFFFF2A6D, 0xFFFFAA00,
        0xFF38BDF8, 0xFFA855F7, 0xFF34D399, 0xFFF43F5E
    )

    var trackMutedState by remember { mutableStateOf(BooleanArray(12) { false }) }
    var trackSoloState by remember { mutableStateOf(BooleanArray(12) { false }) }

    var selectedTool by remember { mutableStateOf(ArrangementTool.DRAW) }
    var zoomScale by remember { mutableStateOf(1) } // 0: compact (42dp), 1: standard (56dp), 2: wide (76dp)
    val barCellWidth = when (zoomScale) {
        0 -> 42.dp
        2 -> 76.dp
        else -> 56.dp
    }

    // Section arrangement markers (Intro, Verse, Chorus, Bridge, Drop, Outro)
    var songMarkers by remember {
        mutableStateOf(
            listOf(
                ArrangementMarker(0, "INTRO", Color(0xFF38BDF8)),
                ArrangementMarker(4, "VERSE 1", Color(0xFF10B981)),
                ArrangementMarker(8, "CHORUS", FruityOrange),
                ArrangementMarker(12, "DROP", FruityPink),
                ArrangementMarker(16, "VERSE 2", Color(0xFF10B981)),
                ArrangementMarker(20, "CHORUS 2", FruityOrange),
                ArrangementMarker(24, "OUTRO", Color(0xFF8B5CF6))
            )
        )
    }

    // Track Renaming & Color Customization Dialog
    var editingTrackIndex by remember { mutableStateOf<Int?>(null) }
    var editingTrackLabel by remember { mutableStateOf("") }

    val horizontalScrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F1116))
    ) {
        // Hardware Rack Top Chassis Strip for Playlist
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF262C38), Color(0xFF181B22), Color(0xFF101217))
                    )
                )
                .border(1.dp, Color(0xFF354054))
                .padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HardwareRackScrew(modifier = Modifier.size(10.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.ViewTimeline,
                    contentDescription = null,
                    tint = FruityLime,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "HARDWARE MULTI-TRACK PLAYLIST ARRANGEMENT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = TextPrimary,
                    letterSpacing = 0.8.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (playMode != PlayMode.SONG) {
                    Button(
                        onClick = onSwitchToSongMode,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FruityOrange,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("SWITCH TO SONG MODE", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(FruityLime.copy(alpha = 0.15f))
                            .border(1.dp, FruityLime.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(FruityLime)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "SONG MODE ACTIVE",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = FruityLime
                        )
                    }
                }
                HardwareRackScrew(modifier = Modifier.size(10.dp))
            }
        }

        // Hardware Timeline Tools & Customization Bar (Draw, Paint, Erase, Mute, Zoom, Markers)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF141720))
                .border(0.5.dp, Color(0xFF242A38))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Tools: Draw (Pencil), Paint (Brush), Erase (Trash), Mute Tool
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "TOOLS:",
                    fontSize = 8.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                listOf(
                    Pair(ArrangementTool.DRAW, Icons.Default.Edit),
                    Pair(ArrangementTool.PAINT, Icons.Default.Brush),
                    Pair(ArrangementTool.ERASE, Icons.Default.Delete),
                    Pair(ArrangementTool.MUTE_TOOL, Icons.Default.VolumeOff)
                ).forEach { (tool, icon) ->
                    val isSel = selectedTool == tool
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (isSel) FruityOrange else Color(0xFF1F2432))
                            .border(0.8.dp, if (isSel) FruityOrangeGlow else Color(0xFF323B4E), RoundedCornerShape(3.dp))
                            .clickable { selectedTool = tool },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = tool.name,
                            tint = if (isSel) Color.Black else TextPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            // Timeline Zoom Controls (- / +)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "ZOOM:",
                    fontSize = 8.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF1F2432))
                        .border(0.8.dp, Color(0xFF323B4E), RoundedCornerShape(3.dp))
                        .clickable { if (zoomScale > 0) zoomScale-- },
                    contentAlignment = Alignment.Center
                ) {
                    Text("-", fontSize = 13.sp, fontWeight = FontWeight.Black, color = TextPrimary)
                }
                Text(
                    text = when (zoomScale) {
                        0 -> "S"
                        2 -> "L"
                        else -> "M"
                    },
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = FruityCyan
                )
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF1F2432))
                        .border(0.8.dp, Color(0xFF323B4E), RoundedCornerShape(3.dp))
                        .clickable { if (zoomScale < 2) zoomScale++ },
                    contentAlignment = Alignment.Center
                ) {
                    Text("+", fontSize = 12.sp, fontWeight = FontWeight.Black, color = TextPrimary)
                }
            }
        }

        // Section Markers Header Ribbon (INTRO, VERSE, CHORUS, DROP, OUTRO)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0D0F14))
                .border(0.5.dp, Color(0xFF222836))
        ) {
            Box(
                modifier = Modifier
                    .width(135.dp)
                    .height(20.dp)
                    .background(Color(0xFF131720))
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "SECTION MARKERS",
                    fontSize = 7.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(20.dp)
                    .horizontalScroll(horizontalScrollState)
            ) {
                for (b in 0 until totalBars) {
                    val marker = songMarkers.find { it.bar == b }
                    Box(
                        modifier = Modifier
                            .width(barCellWidth)
                            .fillMaxHeight()
                            .background(
                                if (marker != null) marker.color.copy(alpha = 0.25f) else Color.Transparent
                            )
                            .border(0.5.dp, if (marker != null) marker.color.copy(alpha = 0.5f) else Color(0xFF1A1F2B)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (marker != null) {
                            Text(
                                text = marker.label,
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = marker.color
                            )
                        }
                    }
                }
            }
        }

        // Timeline Bar Number Header (Bars 1 to totalBars with LED Playhead)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0A0C10))
                .border(0.5.dp, StudioBorder)
        ) {
            Box(
                modifier = Modifier
                    .width(135.dp)
                    .height(24.dp)
                    .background(StudioPanel)
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "TRACK NAME / BUS",
                    fontSize = 8.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted
                )
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(horizontalScrollState)
            ) {
                for (b in 0 until totalBars) {
                    val isCurrent = isPlaying && playMode == PlayMode.SONG && currentBar == b
                    Box(
                        modifier = Modifier
                            .width(barCellWidth)
                            .height(24.dp)
                            .background(if (isCurrent) FruityLime.copy(alpha = 0.35f) else Color.Transparent)
                            .border(0.5.dp, StudioBorder.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isCurrent) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(FruityLime)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                            }
                            Text(
                                text = "Bar ${b + 1}",
                                fontSize = 8.5.sp,
                                fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Normal,
                                fontFamily = FontFamily.Monospace,
                                color = if (isCurrent) FruityLime else TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // Multi-track Arrangement Grid with Mute/Solo, Color Badges, & Click-to-Rename
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(trackNames.size) { trackIdx ->
                val trackName = trackNames[trackIdx]
                val trackColor = defaultColors[trackIdx % defaultColors.size]
                val isMuted = trackMutedState.getOrElse(trackIdx) { false }
                val isSolo = trackSoloState.getOrElse(trackIdx) { false }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .border(0.5.dp, StudioBorder)
                ) {
                    // Track Header Hardware Strip
                    Row(
                        modifier = Modifier
                            .width(135.dp)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(trackColor).copy(alpha = if (isMuted) 0.05f else 0.18f),
                                        StudioPanel
                                    )
                                )
                            )
                            .border(0.5.dp, StudioBorder)
                            .clickable {
                                editingTrackIndex = trackIdx
                                editingTrackLabel = trackName
                            }
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(3.5.dp, 28.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(if (isMuted) Color.Gray else Color(trackColor))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text(
                                    text = trackName,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isMuted) TextMuted else TextPrimary,
                                    maxLines = 1
                                )
                                Text(
                                    text = "TAP TO RENAME",
                                    fontSize = 6.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextSecondary.copy(alpha = 0.6f)
                                )
                            }
                        }

                        // Mute & Solo Toggles for Track
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(if (!isMuted) FruityLime else Color(0xFF282E3A))
                                    .clickable {
                                        val next = trackMutedState.clone()
                                        next[trackIdx] = !isMuted
                                        trackMutedState = next
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isMuted) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(Color.Gray)
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(if (isSolo) FruityAmber else Color(0xFF282E3A))
                                    .clickable {
                                        val next = trackSoloState.clone()
                                        next[trackIdx] = !isSolo
                                        trackSoloState = next
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "S",
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isSolo) Color.Black else TextMuted
                                )
                            }
                        }
                    }

                    // Bar Cells along timeline
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .horizontalScroll(horizontalScrollState)
                    ) {
                        for (bar in 0 until totalBars) {
                            val clip = clips.find { it.trackIndex == trackIdx && it.startBar == bar }
                            val isCurrentBar = isPlaying && playMode == PlayMode.SONG && currentBar == bar

                            val cellBg = when {
                                clip != null -> Color(clip.colorHex).copy(alpha = if (isMuted) 0.3f else 0.85f)
                                isCurrentBar -> FruityLime.copy(alpha = 0.08f)
                                (bar % 4) == 0 -> Color(0xFF161921)
                                else -> Color(0xFF12141A)
                            }

                            Box(
                                modifier = Modifier
                                    .width(barCellWidth)
                                    .fillMaxHeight()
                                    .background(cellBg)
                                    .border(0.5.dp, StudioBorder.copy(alpha = 0.35f))
                                    .clickable {
                                        when (selectedTool) {
                                            ArrangementTool.ERASE -> {
                                                if (clip != null) onToggleClip(trackIdx, bar)
                                            }
                                            else -> {
                                                onToggleClip(trackIdx, bar)
                                            }
                                        }
                                    }
                                    .padding(2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (clip != null) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = clip.patternName,
                                            fontSize = 7.5.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color.Black
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(0.85f)
                                                .height(2.dp)
                                                .background(Color.Black.copy(alpha = 0.4f))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog for renaming & customizing timeline tracks
    if (editingTrackIndex != null) {
        val idx = editingTrackIndex!!
        Dialog(onDismissRequest = { editingTrackIndex = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(12.dp),
                colors = CardDefaults.cardColors(containerColor = StudioPanel),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(StudioBorder)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            HardwareRackScrew(modifier = Modifier.size(9.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "CUSTOMIZE TIMELINE TRACK #${idx + 1}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = FruityOrange
                            )
                        }
                        IconButton(onClick = { editingTrackIndex = null }, modifier = Modifier.size(22.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = editingTrackLabel,
                        onValueChange = { editingTrackLabel = it },
                        label = { Text("Track Label & Arrangement Role", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = FruityOrange,
                            unfocusedBorderColor = StudioBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("QUICK GENRE PRESETS:", fontSize = 9.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Amapiano Log Drum", "EDM Lead 01", "808 Sub Kick", "Vocal Hook", "Hi-Hat Rolls", "Guitar Solo").forEach { preset ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(StudioPanelLight)
                                    .border(1.dp, StudioBorder, RoundedCornerShape(4.dp))
                                    .clickable { editingTrackLabel = "Track ${idx + 1}: $preset" }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(preset, fontSize = 8.5.sp, color = TextPrimary, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val next = trackNames.toMutableList()
                            next[idx] = editingTrackLabel.ifBlank { "Track ${idx + 1}" }
                            trackNames = next
                            editingTrackIndex = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = FruityOrange, contentColor = Color.Black),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("SAVE TRACK SETTINGS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
