package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.example.ui.theme.*

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
    onTogglePlay: () -> Unit,
    onStop: () -> Unit,
    onRecordClick: () -> Unit,
    onModeChange: (PlayMode) -> Unit,
    onBpmChange: (Int) -> Unit,
    onTapTempo: () -> Unit,
    onPresetSelect: (String) -> Unit,
    onSelectPattern: (Int) -> Unit = {},
    onAddPattern: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showPresetMenu by remember { mutableStateOf(false) }
    var showPatternMenu by remember { mutableStateOf(false) }
    val currentPattern = patterns.find { it.id == selectedPatternId } ?: patterns.firstOrNull()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF282D3A),
                        Color(0xFF1B1F28),
                        Color(0xFF12151B)
                    )
                )
            )
            .border(
                width = 1.2.dp,
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF4C566A), Color(0xFF2D3442), Color(0xFF141820))
                ),
                shape = androidx.compose.ui.graphics.RectangleShape
            )
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        // Top rack chassis accent line with corner mounting screws
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 3.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HardwareRackScrew(modifier = Modifier.size(9.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(FruityOrange)
                )
                Text(
                    text = "MASTER CONSOLE RACK CONTROLLER",
                    fontSize = 7.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp,
                    color = TextSecondary.copy(alpha = 0.8f)
                )
            }
            HardwareRackScrew(modifier = Modifier.size(9.dp))
        }

        // Row 1: Logo, Preset, and Master Peak VU Meter
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // FL Studio Logo & Brand
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { showPresetMenu = true }
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(FruityOrange, FruityOrangeGlow)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "FL Studio Icon",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "FL STUDIO",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(StudioPanelLight)
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "PRO",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = FruityOrange
                            )
                        }
                    }
                    Text(
                        text = "$projectPresetName ▼",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                DropdownMenu(
                    expanded = showPresetMenu,
                    onDismissRequest = { showPresetMenu = false },
                    modifier = Modifier.background(StudioPanel)
                ) {
                    DropdownMenuItem(
                        text = { Text("🌍 Amapiano Fever (113 BPM - Log Drum)", color = TextPrimary, fontWeight = FontWeight.Bold) },
                        onClick = {
                            onPresetSelect("Amapiano Fever")
                            showPresetMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("⚡ Alan Walker EDM (128 BPM - Anthem)", color = TextPrimary, fontWeight = FontWeight.Bold) },
                        onClick = {
                            onPresetSelect("Alan Walker EDM")
                            showPresetMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("🦉 Drake Moody Trap (136 BPM - OVO 808)", color = TextPrimary, fontWeight = FontWeight.Bold) },
                        onClick = {
                            onPresetSelect("Drake Moody Trap")
                            showPresetMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("✨ Katy Perry Pop (122 BPM - Chorus)", color = TextPrimary, fontWeight = FontWeight.Bold) },
                        onClick = {
                            onPresetSelect("Katy Perry Pop")
                            showPresetMenu = false
                        }
                    )
                    HorizontalDivider(color = StudioBorder)
                    DropdownMenuItem(
                        text = { Text("🎸 Guitar Studio Legends (124 BPM)", color = TextPrimary) },
                        onClick = {
                            onPresetSelect("Guitar Studio Legends")
                            showPresetMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("🔥 Trap 808 Heat (138 BPM)", color = TextPrimary) },
                        onClick = {
                            onPresetSelect("Trap 808 Heat")
                            showPresetMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("⚡ Cyber Electro (128 BPM)", color = TextPrimary) },
                        onClick = {
                            onPresetSelect("Cyber Electro")
                            showPresetMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("☕ Lo-Fi Chill (84 BPM)", color = TextPrimary) },
                        onClick = {
                            onPresetSelect("Lo-Fi Chill")
                            showPresetMenu = false
                        }
                    )
                }
            }

            // Digital Bar / Beat Clock Display
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF090B0E))
                    .border(1.dp, Color(0xFF1E232E), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                val barStr = String.format("%03d", currentBar + 1)
                val beatStr = String.format("%02d", (currentStep / 4) + 1)
                val tickStr = String.format("%02d", (currentStep % 4) * 25)
                Text(
                    text = "$barStr:$beatStr:$tickStr",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = LedCyan,
                    letterSpacing = 1.sp
                )
            }

            // Master Peak Stereo VU Meter
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .height(26.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF090B0E))
                    .border(1.dp, StudioBorder, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "VU",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    modifier = Modifier.padding(end = 4.dp)
                )
                StereoVuBar(peak = masterPeakL)
                Spacer(modifier = Modifier.width(3.dp))
                StereoVuBar(peak = masterPeakR)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Row 2: Transport Controls (Play, Stop, Rec, Mode, BPM)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // PAT / SONG Mode Switch
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(StudioPanel)
                    .border(1.dp, StudioBorder, RoundedCornerShape(6.dp))
                    .padding(2.dp)
            ) {
                val isPat = playMode == PlayMode.PATTERN
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isPat) FruityOrange else Color.Transparent)
                        .clickable { onModeChange(PlayMode.PATTERN) }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "PAT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPat) Color.Black else TextSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (!isPat) FruityOrange else Color.Transparent)
                        .clickable { onModeChange(PlayMode.SONG) }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "SONG",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (!isPat) Color.Black else TextSecondary
                    )
                }
            }

            // Pattern Selector Dropdown (Classic FL Studio Pattern Selector)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF141720))
                    .border(1.dp, Color(0xFF2C3548), RoundedCornerShape(4.dp))
                    .clickable { showPatternMenu = true }
                    .padding(horizontal = 7.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = (currentPattern?.name ?: "PATTERN 1").uppercase(),
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = FruityAmber
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = "Select Pattern",
                        tint = FruityAmber,
                        modifier = Modifier.size(14.dp)
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
                                    color = if (pat.id == selectedPatternId) FruityOrange else TextPrimary,
                                    fontWeight = if (pat.id == selectedPatternId) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.sp
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = FruityLime, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("+ New Pattern", color = FruityLime, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        },
                        onClick = {
                            onAddPattern()
                            showPatternMenu = false
                        }
                    )
                }
            }

            // Transport Buttons (Play, Stop, Record)
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Play Button
                val playColor by animateColorAsState(
                    targetValue = if (isPlaying) FruityLime else StudioPanelLight,
                    label = "playColor"
                )
                IconButton(
                    onClick = onTogglePlay,
                    modifier = Modifier
                        .testTag("play_button")
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(playColor)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play or Pause",
                        tint = if (isPlaying) Color.Black else TextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Stop Button
                IconButton(
                    onClick = onStop,
                    modifier = Modifier
                        .testTag("stop_button")
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(StudioPanel)
                        .border(1.dp, StudioBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop",
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Record Button
                IconButton(
                    onClick = onRecordClick,
                    modifier = Modifier
                        .testTag("record_button")
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(StudioPanel)
                        .border(1.dp, LedRed.copy(alpha = 0.5f), CircleShape)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(LedRed)
                    )
                }
            }

            // BPM & Tap Tempo
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(StudioPanel)
                    .border(1.dp, StudioBorder, RoundedCornerShape(6.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                IconButton(
                    onClick = { onBpmChange(bpm - 1) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Decrease BPM",
                        tint = TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Text(
                    text = "$bpm",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = FruityOrange,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )

                IconButton(
                    onClick = { onBpmChange(bpm + 1) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Increase BPM",
                        tint = TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(StudioPanelLight)
                        .clickable { onTapTempo() }
                        .padding(horizontal = 5.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "TAP",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = FruityAmber
                    )
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
            .width(6.dp)
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
                    .clip(RoundedCornerShape(1.dp))
                    .background(if (isActive) color else color.copy(alpha = 0.15f))
            )
        }
    }
}
