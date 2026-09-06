package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PlayMode
import com.example.viewmodel.StudioTab
import com.example.ui.theme.*

/**
 * 3-Tick Modern Studio Workstation Hardware Header with Integrated Hardware Navigation Bay.
 * Tick 1: Master Rack Console & Preset Engine
 * Tick 2: Studio Hardware Transport & Engine Deck
 * Tick 3: Tactile Hardware Navigation Bay (5 Studio Workspaces + Fast Actions)
 */
@Composable
fun TopBarTransport(
    isPlaying: Boolean,
    playMode: PlayMode,
    bpm: Int,
    currentBar: Int,
    currentStep: Int,
    masterPeakL: Float,
    masterPeakR: Float,
    projectPresetName: String,
    patterns: List<com.example.model.Pattern> = emptyList(),
    selectedPatternId: Int = 1,
    selectedTab: StudioTab = StudioTab.CHANNEL_RACK,
    onSelectTab: (StudioTab) -> Unit = {},
    onTogglePlay: () -> Unit,
    onStop: () -> Unit,
    onRecordClick: () -> Unit,
    onModeChange: (PlayMode) -> Unit,
    onBpmChange: (Int) -> Unit,
    onTapTempo: () -> Unit,
    onPresetSelect: (String) -> Unit,
    onSelectPattern: (Int) -> Unit = {},
    onAddPattern: () -> Unit = {},
    onSaveProject: () -> Unit = {},
    onExportAudio: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showPresetMenu by remember { mutableStateOf(false) }
    var showPatternMenu by remember { mutableStateOf(false) }
    val currentPattern = patterns.find { it.id == selectedPatternId } ?: patterns.firstOrNull()
    val navScrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF222733),
                        Color(0xFF171A22),
                        Color(0xFF101217)
                    )
                )
            )
            .border(
                width = 1.2.dp,
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF424C60), Color(0xFF232835), Color(0xFF0E1015))
                ),
                shape = androidx.compose.ui.graphics.RectangleShape
            )
    ) {
        // ==========================================
        // TICK 1: MASTER HARDWARE CONSOLE & PRESET HUB
        // ==========================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF282E3E), Color(0xFF1A1E29))
                    )
                )
                .border(
                    0.8.dp,
                    Color(0xFF3B4459)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // FL Studio Studio Hardware Badge & Preset Selector
            Row(verticalAlignment = Alignment.CenterVertically) {
                HardwareRackScrew(modifier = Modifier.size(9.dp))
                Spacer(modifier = Modifier.width(6.dp))

                // Brand Badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF141720))
                        .border(0.8.dp, FruityOrange.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(FruityOrange)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "FL STUDIO",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = TextPrimary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "PRO",
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = FruityCyan
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Preset Dropdown
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF181D27))
                            .border(0.6.dp, StudioBorder, RoundedCornerShape(3.dp))
                            .clickable { showPresetMenu = true }
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = projectPresetName.uppercase(),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = FruityAmber,
                            maxLines = 1
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = FruityAmber,
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showPresetMenu,
                        onDismissRequest = { showPresetMenu = false },
                        modifier = Modifier.background(StudioPanel)
                    ) {
                        val presets = listOf(
                            "Amapiano Fever",
                            "Alan Walker EDM",
                            "Drake Moody Trap",
                            "Katy Perry Pop",
                            "Guitar Studio Legends",
                            "Trap 808 Heat",
                            "Cyber Electro",
                            "Lo-Fi Chill"
                        )
                        presets.forEach { preset ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = preset,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (preset == projectPresetName) FruityOrange else TextPrimary
                                    )
                                },
                                onClick = {
                                    onPresetSelect(preset)
                                    showPresetMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // Center / Right: Timecode LCD Display & Master Stereo VU
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Timecode Clock
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF090B0E))
                        .border(0.8.dp, Color(0xFF222938), RoundedCornerShape(3.dp))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    val barStr = currentBar.toString().padStart(3, '0')
                    val beat = (currentStep / 4) + 1
                    val stepInBeat = (currentStep % 4) + 1
                    Text(
                        text = "$barStr:$beat:0$stepInBeat",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isPlaying) LedGreen else FruityCyan,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Stereo VU Peak Meter & Limiter Status
                Row(
                    modifier = Modifier
                        .height(18.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF0A0C10))
                        .border(0.5.dp, StudioBorder, RoundedCornerShape(2.dp))
                        .padding(horizontal = 3.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    StereoVuBar(peak = masterPeakL)
                    StereoVuBar(peak = masterPeakR)
                }

                Spacer(modifier = Modifier.width(6.dp))
                HardwareRackScrew(modifier = Modifier.size(9.dp))
            }
        }

        // ==========================================
        // TICK 2: STUDIO HARDWARE TRANSPORT & ENGINE DECK
        // ==========================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF1E232F), Color(0xFF141720))
                    )
                )
                .border(
                    width = 0.6.dp,
                    color = Color(0xFF2D3546)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: PAT / SONG dual rocker switch & Pattern Selector
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Rocker Switch
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF0F1218))
                        .border(0.8.dp, StudioBorder, RoundedCornerShape(4.dp))
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (playMode == PlayMode.PATTERN) FruityOrange else Color.Transparent)
                            .clickable { onModeChange(PlayMode.PATTERN) }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "PAT",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = if (playMode == PlayMode.PATTERN) Color.Black else TextSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (playMode == PlayMode.SONG) FruityLime else Color.Transparent)
                            .clickable { onModeChange(PlayMode.SONG) }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "SONG",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = if (playMode == PlayMode.SONG) Color.Black else TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Pattern Picker
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF161A23))
                            .border(0.6.dp, StudioBorder, RoundedCornerShape(3.dp))
                            .clickable { showPatternMenu = true }
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(Color(currentPattern?.colorHex ?: 0xFFFF7300))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = currentPattern?.name ?: "Pat 1",
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    DropdownMenu(
                        expanded = showPatternMenu,
                        onDismissRequest = { showPatternMenu = false },
                        modifier = Modifier.background(StudioPanel)
                    ) {
                        patterns.forEach { pat ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        pat.name,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (pat.id == selectedPatternId) FruityOrange else TextPrimary
                                    )
                                },
                                onClick = {
                                    onSelectPattern(pat.id)
                                    showPatternMenu = false
                                }
                            )
                        }
                        Divider(color = StudioBorder)
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "+ New Pattern",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = FruityCyan
                                )
                            },
                            onClick = {
                                onAddPattern()
                                showPatternMenu = false
                            }
                        )
                    }
                }
            }

            // Center: Tactile Transport Hardware Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                // Play / Pause Button with illuminated neon bezel
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(
                            if (isPlaying) {
                                Brush.radialGradient(
                                    listOf(Color(0xFF16A34A), Color(0xFF065F46))
                                )
                            } else {
                                Brush.verticalGradient(
                                    listOf(Color(0xFF262C3A), Color(0xFF161A22))
                                )
                            }
                        )
                        .border(
                            width = 1.2.dp,
                            color = if (isPlaying) LedGreen else Color(0xFF3F495F),
                            shape = RoundedCornerShape(5.dp)
                        )
                        .clickable { onTogglePlay() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = if (isPlaying) Color.White else FruityLime,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Stop Button
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF242936), Color(0xFF141720))
                            )
                        )
                        .border(1.dp, Color(0xFF384256), RoundedCornerShape(5.dp))
                        .clickable { onStop() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Record Button with Pulsing LED Ring
                val pulseAlpha by rememberInfiniteTransition().animateFloat(
                    initialValue = 0.4f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                )
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF2A1C20), Color(0xFF190F12))
                            )
                        )
                        .border(1.dp, LedRed.copy(alpha = if (isPlaying) pulseAlpha else 0.7f), RoundedCornerShape(5.dp))
                        .clickable { onRecordClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(11.dp)
                            .clip(CircleShape)
                            .background(LedRed)
                    )
                }
            }

            // Right: Digital BPM LCD with Stepper Buttons and TAP Tempo
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // BPM Stepper [-]
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF171B24))
                        .border(0.6.dp, StudioBorder, RoundedCornerShape(3.dp))
                        .clickable { onBpmChange((bpm - 1).coerceIn(40, 260)) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("-", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }

                // BPM Display
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF0B0D11))
                        .border(0.8.dp, Color(0xFF202634), RoundedCornerShape(3.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "$bpm",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = FruityAmber
                    )
                }

                // BPM Stepper [+]
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF171B24))
                        .border(0.6.dp, StudioBorder, RoundedCornerShape(3.dp))
                        .clickable { onBpmChange((bpm + 1).coerceIn(40, 260)) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("+", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }

                // TAP tempo pad
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF1C2230))
                        .border(0.7.dp, FruityAmber.copy(alpha = 0.6f), RoundedCornerShape(3.dp))
                        .clickable { onTapTempo() }
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "TAP",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = FruityAmber
                    )
                }
            }
        }

        // ==========================================
        // TICK 3: TACTILE HARDWARE NAVIGATION BAY (Arranged in 3 ticks)
        // ==========================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF181C26), Color(0xFF0F1218))
                    )
                )
                .border(
                    width = 0.8.dp,
                    color = Color(0xFF242B3A)
                )
                .padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 5 Studio Navigation Hardware Switches
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(navScrollState),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val navItems = listOf(
                    Triple(StudioTab.CHANNEL_RACK, Icons.Default.ViewKanban, "RACK"),
                    Triple(StudioTab.PIANO_ROLL, Icons.Default.Piano, "PIANO ROLL"),
                    Triple(StudioTab.PLAYLIST, Icons.Default.ViewTimeline, "PLAYLIST"),
                    Triple(StudioTab.MIXER, Icons.Default.Tune, "MIXER"),
                    Triple(StudioTab.SYNTH, Icons.Default.GraphicEq, "3xOSC SYNTH")
                )

                navItems.forEach { (tab, icon, label) ->
                    val isSelected = selectedTab == tab
                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (isSelected) {
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF382314), Color(0xFF24150A))
                                    )
                                } else {
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF1B202B), Color(0xFF12151D))
                                    )
                                }
                            )
                            .border(
                                width = if (isSelected) 1.2.dp else 0.6.dp,
                                color = if (isSelected) FruityOrange else Color(0xFF2C3446),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .clickable { onSelectTab(tab) }
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Active LED dot
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) FruityOrange else Color(0xFF2D364A))
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (isSelected) FruityOrange else TextSecondary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = label,
                                fontSize = 8.5.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.5.sp,
                                color = if (isSelected) Color.White else TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Quick Studio Hardware Actions [SAVE] & [EXPORT]
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Save Project Snapshot
                Box(
                    modifier = Modifier
                        .height(26.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF131B26))
                        .border(0.7.dp, FruityCyan.copy(alpha = 0.6f), RoundedCornerShape(3.dp))
                        .clickable { onSaveProject() }
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save Project",
                            tint = FruityCyan,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "SAVE",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = FruityCyan
                        )
                    }
                }

                // Export Audio
                Box(
                    modifier = Modifier
                        .height(26.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF251A12))
                        .border(0.7.dp, FruityOrange.copy(alpha = 0.6f), RoundedCornerShape(3.dp))
                        .clickable { onExportAudio() }
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Export WAV Audio",
                            tint = FruityOrange,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "BOUNCE",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = FruityOrange
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StereoVuBar(peak: Float) {
    val clampedPeak = peak.coerceIn(0f, 1f)
    val segments = 8

    Column(
        modifier = Modifier
            .width(5.dp)
            .fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        for (i in (segments - 1) downTo 0) {
            val threshold = i.toFloat() / segments
            val isActive = clampedPeak > threshold
            val color = when {
                i >= 7 -> LedRed
                i >= 5 -> LedYellow
                else -> LedGreen
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(0.5.dp))
                    .background(if (isActive) color else color.copy(alpha = 0.15f))
            )
        }
    }
}
