package com.example.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*

@Composable
fun PluginSynthView(
    channels: List<Channel>,
    selectedChannelId: String,
    onSelectChannel: (String) -> Unit,
    onUpdateParams: (
        channelId: String,
        osc1: OscType?,
        osc2: OscType?,
        osc2Mix: Float?,
        detuneCents: Float?,
        filterType: FilterType?,
        cutoffHz: Float?,
        resonanceQ: Float?,
        attackMs: Float?,
        decayMs: Float?,
        sustainLevel: Float?,
        releaseMs: Float?
    ) -> Unit,
    onPlayNote: (channel: Channel, midiPitch: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val synthChannels = channels.filter { it.type.isMelodic }
    val currentChannel = synthChannels.find { it.id == selectedChannelId } ?: synthChannels.firstOrNull() ?: channels.firstOrNull()

    var showChannelMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioDarkBg)
    ) {
        // Plugin Synth Header with Hardware Rack Styling
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(Color(0xFF252A36), Color(0xFF161920))
                    )
                )
                .border(1.dp, Color(0xFF333D52))
                .padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HardwareRackScrew(modifier = Modifier.size(10.dp))
                Spacer(modifier = Modifier.width(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF1E2430))
                        .border(1.dp, Color(0xFF354054), RoundedCornerShape(4.dp))
                        .clickable { showChannelMenu = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(FruityOrange)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "FRUITY 3xOSC HARDWARE: ${currentChannel?.name?.uppercase() ?: "SYNTH"}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = TextPrimary
                    )
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = FruityOrange, modifier = Modifier.size(16.dp))

                    DropdownMenu(
                        expanded = showChannelMenu,
                        onDismissRequest = { showChannelMenu = false },
                        modifier = Modifier.background(StudioPanel)
                    ) {
                        channels.forEach { ch ->
                            DropdownMenuItem(
                                text = { Text(ch.name, color = TextPrimary) },
                                onClick = {
                                    onSelectChannel(ch.id)
                                    showChannelMenu = false
                                }
                            )
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "ANALOG ENGINE",
                    fontSize = 8.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = FruityCyan,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                HardwareRackScrew(modifier = Modifier.size(10.dp))
            }
        }

        if (currentChannel != null) {
            // Scrollable synth tweak controls
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Section 1: Oscillators (OSC 1 & OSC 2)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = StudioPanel),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(StudioBorder))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "OSCILLATOR GENERATORS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = FruityOrange,
                                letterSpacing = 1.sp
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // OSC 1 Waveform selector
                            Text("OSC 1 Waveform", fontSize = 9.sp, color = TextSecondary)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                OscType.values().forEach { osc ->
                                    val isSelected = currentChannel.osc1Type == osc
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (isSelected) FruityOrange else StudioPanelLight)
                                            .border(0.5.dp, StudioBorder, RoundedCornerShape(4.dp))
                                            .clickable {
                                                onUpdateParams(currentChannel.id, osc, null, null, null, null, null, null, null, null, null, null)
                                            }
                                            .padding(vertical = 5.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = osc.displayName,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.Black else TextSecondary
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // OSC 2 Waveform selector
                            Text("OSC 2 Waveform (Harmonics)", fontSize = 9.sp, color = TextSecondary)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                OscType.values().forEach { osc ->
                                    val isSelected = currentChannel.osc2Type == osc
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (isSelected) FruityCyan else StudioPanelLight)
                                            .border(0.5.dp, StudioBorder, RoundedCornerShape(4.dp))
                                            .clickable {
                                                onUpdateParams(currentChannel.id, null, osc, null, null, null, null, null, null, null, null, null)
                                            }
                                            .padding(vertical = 5.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = osc.displayName,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.Black else TextSecondary
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Osc2 Mix & Detune cents
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("OSC 2 Mix: ${(currentChannel.osc2Mix * 100).toInt()}%", fontSize = 9.sp, color = TextSecondary)
                                    Slider(
                                        value = currentChannel.osc2Mix,
                                        onValueChange = {
                                            onUpdateParams(currentChannel.id, null, null, it, null, null, null, null, null, null, null, null)
                                        },
                                        valueRange = 0f..1f,
                                        colors = SliderDefaults.colors(thumbColor = FruityCyan, activeTrackColor = FruityCyan)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Detune: ${currentChannel.detuneCents.toInt()} Cents", fontSize = 9.sp, color = TextSecondary)
                                    Slider(
                                        value = currentChannel.detuneCents,
                                        onValueChange = {
                                            onUpdateParams(currentChannel.id, null, null, null, it, null, null, null, null, null, null, null)
                                        },
                                        valueRange = -50f..50f,
                                        colors = SliderDefaults.colors(thumbColor = FruityCyan, activeTrackColor = FruityCyan)
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 2: Fruity Filter Section
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = StudioPanel),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(StudioBorder))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "FRUITY STATE-VARIABLE FILTER",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FruityLime,
                                    letterSpacing = 1.sp
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    FilterType.values().forEach { ft ->
                                        val isSel = currentChannel.filterType == ft
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(if (isSel) FruityLime else StudioPanelLight)
                                                .clickable {
                                                    onUpdateParams(currentChannel.id, null, null, null, null, ft, null, null, null, null, null, null)
                                                }
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = ft.displayName,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSel) Color.Black else TextSecondary
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Cutoff: ${currentChannel.cutoffHz.toInt()} Hz", fontSize = 9.sp, color = TextSecondary)
                                    Slider(
                                        value = currentChannel.cutoffHz,
                                        onValueChange = {
                                            onUpdateParams(currentChannel.id, null, null, null, null, null, it, null, null, null, null, null)
                                        },
                                        valueRange = 100f..15000f,
                                        colors = SliderDefaults.colors(thumbColor = FruityLime, activeTrackColor = FruityLime)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Resonance Q: ${String.format("%.1f", currentChannel.resonanceQ)}", fontSize = 9.sp, color = TextSecondary)
                                    Slider(
                                        value = currentChannel.resonanceQ,
                                        onValueChange = {
                                            onUpdateParams(currentChannel.id, null, null, null, null, null, null, it, null, null, null, null)
                                        },
                                        valueRange = 0.5f..8.0f,
                                        colors = SliderDefaults.colors(thumbColor = FruityLime, activeTrackColor = FruityLime)
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 3: ADSR Envelope
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = StudioPanel),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(StudioBorder))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "ADSR AMPLITUDE ENVELOPE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = FruityPurple,
                                letterSpacing = 1.sp
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Visual Envelope Canvas
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(45.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF090B0E))
                                    .border(1.dp, StudioBorder, RoundedCornerShape(4.dp))
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val w = size.width
                                    val h = size.height

                                    val aRatio = (currentChannel.attackMs / 300f).coerceIn(0.05f, 0.35f)
                                    val dRatio = (currentChannel.decayMs / 500f).coerceIn(0.1f, 0.35f)
                                    val sRatio = (1f - currentChannel.sustainLevel).coerceIn(0.1f, 0.9f)
                                    val rRatio = (currentChannel.releaseMs / 600f).coerceIn(0.1f, 0.3f)

                                    val p1 = Offset(w * aRatio, 4f)
                                    val p2 = Offset(w * (aRatio + dRatio), h * sRatio)
                                    val p3 = Offset(w * 0.75f, h * sRatio)
                                    val p4 = Offset(w * (0.75f + rRatio).coerceAtMost(0.98f), h - 4f)

                                    val path = Path()
                                    path.moveTo(0f, h - 4f)
                                    path.lineTo(p1.x, p1.y)
                                    path.lineTo(p2.x, p2.y)
                                    path.lineTo(p3.x, p3.y)
                                    path.lineTo(p4.x, p4.y)

                                    drawPath(path, FruityPurple, style = Stroke(width = 2.5f))
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // 4 ADSR Sliders
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("A: ${currentChannel.attackMs.toInt()}ms", fontSize = 8.sp, color = TextSecondary)
                                    Slider(
                                        value = currentChannel.attackMs,
                                        onValueChange = {
                                            onUpdateParams(currentChannel.id, null, null, null, null, null, null, null, it, null, null, null)
                                        },
                                        valueRange = 1f..300f,
                                        colors = SliderDefaults.colors(thumbColor = FruityPurple, activeTrackColor = FruityPurple)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("D: ${currentChannel.decayMs.toInt()}ms", fontSize = 8.sp, color = TextSecondary)
                                    Slider(
                                        value = currentChannel.decayMs,
                                        onValueChange = {
                                            onUpdateParams(currentChannel.id, null, null, null, null, null, null, null, null, it, null, null)
                                        },
                                        valueRange = 10f..600f,
                                        colors = SliderDefaults.colors(thumbColor = FruityPurple, activeTrackColor = FruityPurple)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("S: ${(currentChannel.sustainLevel * 100).toInt()}%", fontSize = 8.sp, color = TextSecondary)
                                    Slider(
                                        value = currentChannel.sustainLevel,
                                        onValueChange = {
                                            onUpdateParams(currentChannel.id, null, null, null, null, null, null, null, null, null, it, null)
                                        },
                                        valueRange = 0f..1f,
                                        colors = SliderDefaults.colors(thumbColor = FruityPurple, activeTrackColor = FruityPurple)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("R: ${currentChannel.releaseMs.toInt()}ms", fontSize = 8.sp, color = TextSecondary)
                                    Slider(
                                        value = currentChannel.releaseMs,
                                        onValueChange = {
                                            onUpdateParams(currentChannel.id, null, null, null, null, null, null, null, null, null, null, it)
                                        },
                                        valueRange = 10f..800f,
                                        colors = SliderDefaults.colors(thumbColor = FruityPurple, activeTrackColor = FruityPurple)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Live Playable Piano Keyboard (2 Octaves: C3 to C5)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StudioPanel)
                    .border(1.dp, StudioBorder)
                    .padding(top = 4.dp, bottom = 8.dp)
            ) {
                Text(
                    text = "PLAY SYNTH LIVE (AUDITION)",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = FruityOrange,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                val scrollKeysState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .horizontalScroll(scrollKeysState)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val startMidi = 48 // C3
                    val totalKeys = 25 // 2 full octaves C3 to C5
                    for (i in 0 until totalKeys) {
                        val midi = startMidi + i
                        val isBlack = isAccidental(midi)
                        val name = getMidiNoteName(midi)

                        Box(
                            modifier = Modifier
                                .width(if (isBlack) 24.dp else 32.dp)
                                .fillMaxHeight(if (isBlack) 0.65f else 1.0f)
                                .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                                .background(if (isBlack) Color(0xFF14161C) else Color(0xFFE2E8F0))
                                .border(
                                    1.dp,
                                    if (isBlack) Color.Black else Color(0xFFCBD5E1),
                                    RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp)
                                )
                                .clickable { onPlayNote(currentChannel, midi) }
                                .padding(bottom = 4.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Text(
                                text = name,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isBlack) Color.White else Color.Black
                            )
                        }
                    }
                }
            }
        }
    }
}
