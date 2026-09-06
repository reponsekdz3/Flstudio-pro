package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MixerTrack
import com.example.ui.theme.*

enum class MixerCategoryFilter(val label: String) {
    ALL("ALL (129)"),
    MASTER("MASTER"),
    INSERTS("INSERTS (1-28)"),
    AUX_SENDS("AUX BUSSES (1-100)")
}

@Composable
fun MixerView(
    mixerTracks: List<MixerTrack>,
    selectedTrackId: Int,
    masterPeakL: Float,
    masterPeakR: Float,
    trackPeaks: FloatArray = FloatArray(129),
    onSelectTrack: (Int) -> Unit,
    onVolumeChange: (trackId: Int, volumeDb: Float) -> Unit,
    onPanChange: (trackId: Int, pan: Float) -> Unit,
    onToggleMute: (trackId: Int) -> Unit,
    onEqChange: (trackId: Int, enabled: Boolean, low: Float, mid: Float, high: Float) -> Unit,
    onDelayChange: (trackId: Int, enabled: Boolean, timeMs: Float, feedback: Float, wet: Float) -> Unit,
    onReverbChange: (trackId: Int, enabled: Boolean, room: Float, wet: Float) -> Unit,
    onDistChange: (trackId: Int, enabled: Boolean, drive: Float, mix: Float) -> Unit,
    onChorusChange: ((trackId: Int, enabled: Boolean, rate: Float, depth: Float, mix: Float) -> Unit)? = null,
    onCompChange: ((trackId: Int, enabled: Boolean, thresholdDb: Float, ratio: Float) -> Unit)? = null,
    onAuxSendChange: ((trackId: Int, auxId: Int, sendLevel: Float) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val currentTrack = mixerTracks.find { it.id == selectedTrackId } ?: mixerTracks.firstOrNull()
    var categoryFilter by remember { mutableStateOf(MixerCategoryFilter.ALL) }
    val consoleScrollState = rememberScrollState()

    val filteredTracks = remember(mixerTracks, categoryFilter) {
        when (categoryFilter) {
            MixerCategoryFilter.ALL -> mixerTracks
            MixerCategoryFilter.MASTER -> mixerTracks.filter { it.id == 0 }
            MixerCategoryFilter.INSERTS -> mixerTracks.filter { it.id in 1..28 }
            MixerCategoryFilter.AUX_SENDS -> mixerTracks.filter { it.id in 29..128 }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F1116))
    ) {
        // Hardware Console Top Header Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF252A36), Color(0xFF161920))
                    )
                )
                .border(1.dp, Color(0xFF333D52))
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HardwareRackScrew(modifier = Modifier.size(11.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = FruityOrange,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "VIRTUAL HARDWARE MIXING CONSOLE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "65 CHANNELS • 50 AUX SENDS",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = FruityCyan
                )
            }
            HardwareRackScrew(modifier = Modifier.size(11.dp))
        }

        // Category Filter Selector Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF13151B))
                .border(0.5.dp, StudioBorder)
                .padding(horizontal = 8.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MixerCategoryFilter.values().forEach { filter ->
                val isSelected = filter == categoryFilter
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (isSelected) FruityOrange else Color(0xFF1E222D))
                        .clickable { categoryFilter = filter }
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = filter.label,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (isSelected) Color.Black else TextSecondary
                    )
                }
            }
        }

        // Horizontal Console Channel Strips Row (Scrollable across all 65 channels)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0A0C10))
                .border(0.5.dp, Color(0xFF1E232E))
                .horizontalScroll(consoleScrollState)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            filteredTracks.forEach { track ->
                val isSelected = track.id == selectedTrackId
                val isMaster = track.id == 0
                val isAux = track.isAuxBus
                val peak = trackPeaks.getOrElse(track.id) { 0f }

                Box(
                    modifier = Modifier
                        .width(68.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            when {
                                isSelected -> Color(0xFF2C1E14)
                                isMaster -> Color(0xFF1A2234)
                                isAux -> Color(0xFF1E1728)
                                else -> Color(0xFF151820)
                            }
                        )
                        .border(
                            1.dp,
                            if (isSelected) FruityOrange else if (isMaster) Color(0xFF38BDF8) else Color(0xFF282E3D),
                            RoundedCornerShape(4.dp)
                        )
                        .clickable { onSelectTrack(track.id) }
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when {
                                    isMaster -> "MST"
                                    isAux -> "A${track.auxBusIndex}"
                                    else -> "#${track.id}"
                                },
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = if (isSelected) FruityOrange else if (isAux) FruityPurple else TextPrimary
                            )
                            // Mini Activity LED
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(if (peak > 0.05f) LedGreen else Color(0xFF202632))
                            )
                        }

                        Text(
                            text = track.name,
                            fontSize = 7.5.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (isSelected) TextPrimary else TextSecondary,
                            maxLines = 1
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        // Mini VU Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(Color(0xFF090A0D))
                        ) {
                            val normPeak = (peak / 1.2f).coerceIn(0f, 1f)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(normPeak)
                                    .fillMaxHeight()
                                    .background(if (peak > 0.9f) LedRed else if (peak > 0.65f) LedAmber else LedGreen)
                            )
                        }
                    }
                }
            }
        }

        // Main Channel Hardware Bay (Channel Strip Fader + Rack FX & Aux Sends)
        if (currentTrack != null) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // LEFT: Machined Console Channel Strip Fader
                val trackPeak = if (currentTrack.id == 0) (masterPeakL + masterPeakR) / 2f else trackPeaks.getOrElse(currentTrack.id) { 0f }
                HardwareConsoleFader(
                    volumeDb = currentTrack.volumeDb,
                    onVolumeChange = { onVolumeChange(currentTrack.id, it) },
                    peakLevel = trackPeak,
                    trackName = currentTrack.name,
                    isMuted = currentTrack.isMuted,
                    onToggleMute = { onToggleMute(currentTrack.id) },
                    modifier = Modifier.fillMaxHeight()
                )

                // CENTER & RIGHT: Studio Rack Hardware Plugins & Aux Send Matrix
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Quick Track Pan & Routing Header Rack Card
                    item {
                        HardwareRackChassis(
                            title = "CONSOLE CHANNEL: ${currentTrack.name}",
                            subtitle = if (currentTrack.isAuxBus) "AUX BUS #${currentTrack.auxBusIndex}" else "INSERT #${currentTrack.id}",
                            accentColor = FruityCyan
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val panVal = (currentTrack.pan * 100).toInt()
                                val panStr = when {
                                    panVal < 0 -> "L $panVal%"
                                    panVal > 0 -> "R +$panVal%"
                                    else -> "CENTER"
                                }
                                HardwareRotaryKnob(
                                    value = currentTrack.pan,
                                    onValueChange = { onPanChange(currentTrack.id, it) },
                                    valueRange = -1.0f..1.0f,
                                    label = "PAN",
                                    displayValue = panStr,
                                    color = FruityCyan,
                                    sizeDp = 44.dp
                                )

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "STEREO VU",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = TextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                        val pL = if (currentTrack.id == 0) masterPeakL else trackPeak * 0.95f
                                        val pR = if (currentTrack.id == 0) masterPeakR else trackPeak * 1.05f
                                        HardwareVuMeter(peak = pL, height = 50.dp, width = 8.dp)
                                        HardwareVuMeter(peak = pR, height = 50.dp, width = 8.dp)
                                    }
                                }

                                HardwareToggleSwitch(
                                    checked = !currentTrack.isMuted,
                                    onCheckedChange = { onToggleMute(currentTrack.id) },
                                    label = if (currentTrack.isMuted) "MUTED" else "ARMED",
                                    ledColor = if (currentTrack.isMuted) LedRed else LedGreen
                                )
                            }
                        }
                    }

                    // 50-Bus Auxiliary Send Routing Subpanel (Only for non-aux channels)
                    if (!currentTrack.isAuxBus) {
                        item {
                            HardwareRackChassis(
                                title = "50-BUS AUXILIARY SENDS MATRIX",
                                subtitle = "SEND TO REVERB, DELAY, CHORUS, DRIVE & COMP BUSSES",
                                accentColor = FruityPurple
                            ) {
                                Text(
                                    text = "Send this track to any of the 50 studio auxiliary effect busses:",
                                    fontSize = 8.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val quickAuxBusses = listOf(
                                        15 to "Aux 1: Hall Reverb",
                                        16 to "Aux 2: Tape Echo",
                                        17 to "Aux 3: Vintage Chorus",
                                        18 to "Aux 4: Tube Overdrive",
                                        19 to "Aux 5: Parallel Comp",
                                        20 to "Aux 6: Shimmer Verb",
                                        21 to "Aux 7: Slap Echo",
                                        22 to "Aux 8: Flanger",
                                        23 to "Aux 9: Warm Saturation",
                                        24 to "Aux 10: Room Verb"
                                    )

                                    items(quickAuxBusses) { (auxTrackId, busName) ->
                                        val sendAmount = currentTrack.auxSends[auxTrackId] ?: 0f
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFF171A24))
                                                .border(0.8.dp, Color(0xFF2D3546), RoundedCornerShape(4.dp))
                                                .padding(6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            HardwareRotaryKnob(
                                                value = sendAmount,
                                                onValueChange = { newLvl ->
                                                    onAuxSendChange?.invoke(currentTrack.id, auxTrackId, newLvl)
                                                },
                                                valueRange = 0f..1f,
                                                label = busName.substringBefore(":"),
                                                displayValue = "${(sendAmount * 100).toInt()}%",
                                                color = FruityPurple,
                                                sizeDp = 42.dp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // FX Slot 1: Fruity Parametric EQ 2 (Hardware Rack)
                    item {
                        HardwareRackChassis(
                            title = "FRUITY PARAMETRIC EQ 2",
                            subtitle = "ANALOG FREQUENCY RESPONSE CURVE",
                            accentColor = FruityCyan,
                            headerTrailing = {
                                HardwareToggleSwitch(
                                    checked = currentTrack.eqEnabled,
                                    onCheckedChange = { en ->
                                        onEqChange(currentTrack.id, en, currentTrack.eqLowGain, currentTrack.eqMidGain, currentTrack.eqHighGain)
                                    },
                                    label = "POWER",
                                    ledColor = FruityCyan
                                )
                            }
                        ) {
                            // Phosphor CRT Spectrum Display
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(65.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF080B10))
                                    .border(1.dp, Color(0xFF1E2838), RoundedCornerShape(4.dp))
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val w = size.width
                                    val h = size.height
                                    val midY = h / 2f

                                    // CRT grid lines
                                    drawLine(Color(0xFF151D28), Offset(0f, midY), Offset(w, midY), strokeWidth = 1f)
                                    drawLine(Color(0xFF151D28), Offset(w * 0.33f, 0f), Offset(w * 0.33f, h), strokeWidth = 1f)
                                    drawLine(Color(0xFF151D28), Offset(w * 0.66f, 0f), Offset(w * 0.66f, h), strokeWidth = 1f)

                                    val path = Path()
                                    path.moveTo(0f, midY - (currentTrack.eqLowGain * 2.2f))
                                    path.cubicTo(
                                        w * 0.25f, midY - (currentTrack.eqLowGain * 2.0f),
                                        w * 0.35f, midY - (currentTrack.eqMidGain * 2.5f),
                                        w * 0.5f, midY - (currentTrack.eqMidGain * 2.5f)
                                    )
                                    path.cubicTo(
                                        w * 0.65f, midY - (currentTrack.eqMidGain * 2.5f),
                                        w * 0.75f, midY - (currentTrack.eqHighGain * 2.0f),
                                        w, midY - (currentTrack.eqHighGain * 2.2f)
                                    )

                                    drawPath(
                                        path = path,
                                        color = if (currentTrack.eqEnabled) FruityCyan else Color(0xFF4B5563),
                                        style = Stroke(width = 2.4.dp.toPx())
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                HardwareRotaryKnob(
                                    value = currentTrack.eqLowGain,
                                    onValueChange = { onEqChange(currentTrack.id, currentTrack.eqEnabled, it, currentTrack.eqMidGain, currentTrack.eqHighGain) },
                                    valueRange = -12f..12f,
                                    label = "LOW 100Hz",
                                    displayValue = "${currentTrack.eqLowGain.toInt()} dB",
                                    color = FruityCyan,
                                    sizeDp = 44.dp
                                )
                                HardwareRotaryKnob(
                                    value = currentTrack.eqMidGain,
                                    onValueChange = { onEqChange(currentTrack.id, currentTrack.eqEnabled, currentTrack.eqLowGain, it, currentTrack.eqHighGain) },
                                    valueRange = -12f..12f,
                                    label = "MID 1.2kHz",
                                    displayValue = "${currentTrack.eqMidGain.toInt()} dB",
                                    color = FruityCyan,
                                    sizeDp = 44.dp
                                )
                                HardwareRotaryKnob(
                                    value = currentTrack.eqHighGain,
                                    onValueChange = { onEqChange(currentTrack.id, currentTrack.eqEnabled, currentTrack.eqLowGain, currentTrack.eqMidGain, it) },
                                    valueRange = -12f..12f,
                                    label = "HIGH 8kHz",
                                    displayValue = "${currentTrack.eqHighGain.toInt()} dB",
                                    color = FruityCyan,
                                    sizeDp = 44.dp
                                )
                            }
                        }
                    }

                    // FX Slot 2: Fruity Delay 3 (Analog BBD Delay Rack)
                    item {
                        HardwareRackChassis(
                            title = "FRUITY DELAY 3",
                            subtitle = "ANALOG BUCKET-BRIGADE ECHO",
                            accentColor = FruityLime,
                            headerTrailing = {
                                HardwareToggleSwitch(
                                    checked = currentTrack.delayEnabled,
                                    onCheckedChange = { en ->
                                        onDelayChange(currentTrack.id, en, currentTrack.delayTimeMs, currentTrack.delayFeedback, currentTrack.delayWet)
                                    },
                                    label = "POWER",
                                    ledColor = FruityLime
                                )
                            }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                HardwareRotaryKnob(
                                    value = currentTrack.delayTimeMs,
                                    onValueChange = { onDelayChange(currentTrack.id, currentTrack.delayEnabled, it, currentTrack.delayFeedback, currentTrack.delayWet) },
                                    valueRange = 50f..600f,
                                    label = "TIME",
                                    displayValue = "${currentTrack.delayTimeMs.toInt()} ms",
                                    color = FruityLime,
                                    sizeDp = 44.dp
                                )
                                HardwareRotaryKnob(
                                    value = currentTrack.delayFeedback,
                                    onValueChange = { onDelayChange(currentTrack.id, currentTrack.delayEnabled, currentTrack.delayTimeMs, it, currentTrack.delayWet) },
                                    valueRange = 0.05f..0.85f,
                                    label = "FEEDBACK",
                                    displayValue = "${(currentTrack.delayFeedback * 100).toInt()}%",
                                    color = FruityLime,
                                    sizeDp = 44.dp
                                )
                                HardwareRotaryKnob(
                                    value = currentTrack.delayWet,
                                    onValueChange = { onDelayChange(currentTrack.id, currentTrack.delayEnabled, currentTrack.delayTimeMs, currentTrack.delayFeedback, it) },
                                    valueRange = 0f..1f,
                                    label = "WET MIX",
                                    displayValue = "${(currentTrack.delayWet * 100).toInt()}%",
                                    color = FruityLime,
                                    sizeDp = 44.dp
                                )
                            }
                        }
                    }

                    // FX Slot 3: Fruity Reverb 2 (Studio Plate & Hall Rack)
                    item {
                        HardwareRackChassis(
                            title = "FRUITY REVERB 2",
                            subtitle = "STUDIO PLATE & HALL ALGORITHM",
                            accentColor = FruityPurple,
                            headerTrailing = {
                                HardwareToggleSwitch(
                                    checked = currentTrack.reverbEnabled,
                                    onCheckedChange = { en ->
                                        onReverbChange(currentTrack.id, en, currentTrack.reverbRoom, currentTrack.reverbWet)
                                    },
                                    label = "POWER",
                                    ledColor = FruityPurple
                                )
                            }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                HardwareRotaryKnob(
                                    value = currentTrack.reverbRoom,
                                    onValueChange = { onReverbChange(currentTrack.id, currentTrack.reverbEnabled, it, currentTrack.reverbWet) },
                                    valueRange = 0.1f..0.95f,
                                    label = "ROOM SIZE",
                                    displayValue = "${(currentTrack.reverbRoom * 100).toInt()}%",
                                    color = FruityPurple,
                                    sizeDp = 44.dp
                                )
                                HardwareRotaryKnob(
                                    value = currentTrack.reverbWet,
                                    onValueChange = { onReverbChange(currentTrack.id, currentTrack.reverbEnabled, currentTrack.reverbRoom, it) },
                                    valueRange = 0f..1f,
                                    label = "WET/DRY",
                                    displayValue = "${(currentTrack.reverbWet * 100).toInt()}%",
                                    color = FruityPurple,
                                    sizeDp = 44.dp
                                )
                            }
                        }
                    }

                    // FX Slot 4: Fruity Fast Dist (Vacuum Tube Overdrive Rack)
                    item {
                        HardwareRackChassis(
                            title = "FRUITY FAST DIST",
                            subtitle = "WARM ANALOG TUBE SATURATION",
                            accentColor = FruityPink,
                            headerTrailing = {
                                HardwareToggleSwitch(
                                    checked = currentTrack.distEnabled,
                                    onCheckedChange = { en ->
                                        onDistChange(currentTrack.id, en, currentTrack.distDrive, currentTrack.distMix)
                                    },
                                    label = "POWER",
                                    ledColor = FruityPink
                                )
                            }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                HardwareRotaryKnob(
                                    value = currentTrack.distDrive,
                                    onValueChange = { onDistChange(currentTrack.id, currentTrack.distEnabled, it, currentTrack.distMix) },
                                    valueRange = 1.0f..6.0f,
                                    label = "DRIVE",
                                    displayValue = String.format("%.1fx", currentTrack.distDrive),
                                    color = FruityPink,
                                    sizeDp = 44.dp
                                )

                                // Illuminated Vacuum Tube Visualizer
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .width(22.dp)
                                            .height(38.dp)
                                            .clip(RoundedCornerShape(11.dp))
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(Color(0xFF2A1C16), Color(0xFF130E0C))
                                                )
                                            )
                                            .border(1.dp, Color(0xFF4B3426), RoundedCornerShape(11.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (currentTrack.distEnabled) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(FruityOrange)
                                            )
                                        }
                                    }
                                    Text("12AX7", fontSize = 7.sp, color = TextMuted)
                                }

                                HardwareRotaryKnob(
                                    value = currentTrack.distMix,
                                    onValueChange = { onDistChange(currentTrack.id, currentTrack.distEnabled, currentTrack.distDrive, it) },
                                    valueRange = 0f..1f,
                                    label = "MIX",
                                    displayValue = "${(currentTrack.distMix * 100).toInt()}%",
                                    color = FruityPink,
                                    sizeDp = 44.dp
                                )
                            }
                        }
                    }

                    // FX Slot 5: Fruity Vintage BBD Chorus (NEW Hardware Plugin!)
                    item {
                        HardwareRackChassis(
                            title = "FRUITY VINTAGE CHORUS",
                            subtitle = "DUAL-STAGE BUCKET BRIGADE ENSEMBLE",
                            accentColor = Color(0xFF38BDF8),
                            headerTrailing = {
                                HardwareToggleSwitch(
                                    checked = currentTrack.chorusEnabled,
                                    onCheckedChange = { en ->
                                        onChorusChange?.invoke(currentTrack.id, en, currentTrack.chorusRate, currentTrack.chorusDepth, currentTrack.chorusMix)
                                    },
                                    label = "POWER",
                                    ledColor = Color(0xFF38BDF8)
                                )
                            }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                HardwareRotaryKnob(
                                    value = currentTrack.chorusRate,
                                    onValueChange = { onChorusChange?.invoke(currentTrack.id, currentTrack.chorusEnabled, it, currentTrack.chorusDepth, currentTrack.chorusMix) },
                                    valueRange = 0.2f..4.0f,
                                    label = "LFO RATE",
                                    displayValue = String.format("%.1f Hz", currentTrack.chorusRate),
                                    color = Color(0xFF38BDF8),
                                    sizeDp = 44.dp
                                )
                                HardwareRotaryKnob(
                                    value = currentTrack.chorusDepth,
                                    onValueChange = { onChorusChange?.invoke(currentTrack.id, currentTrack.chorusEnabled, currentTrack.chorusRate, it, currentTrack.chorusMix) },
                                    valueRange = 0.1f..1.0f,
                                    label = "DEPTH",
                                    displayValue = "${(currentTrack.chorusDepth * 100).toInt()}%",
                                    color = Color(0xFF38BDF8),
                                    sizeDp = 44.dp
                                )
                                HardwareRotaryKnob(
                                    value = currentTrack.chorusMix,
                                    onValueChange = { onChorusChange?.invoke(currentTrack.id, currentTrack.chorusEnabled, currentTrack.chorusRate, currentTrack.chorusDepth, it) },
                                    valueRange = 0f..1.0f,
                                    label = "WET MIX",
                                    displayValue = "${(currentTrack.chorusMix * 100).toInt()}%",
                                    color = Color(0xFF38BDF8),
                                    sizeDp = 44.dp
                                )
                            }
                        }
                    }

                    // FX Slot 6: Fruity Optical Compressor (NEW Hardware Plugin!)
                    item {
                        HardwareRackChassis(
                            title = "FRUITY OPTICAL COMPRESSOR",
                            subtitle = "VINTAGE VCA / OPTO LEVELING AMPLIFIER",
                            accentColor = Color(0xFFF59E0B),
                            headerTrailing = {
                                HardwareToggleSwitch(
                                    checked = currentTrack.compEnabled,
                                    onCheckedChange = { en ->
                                        onCompChange?.invoke(currentTrack.id, en, currentTrack.compThresholdDb, currentTrack.compRatio)
                                    },
                                    label = "POWER",
                                    ledColor = Color(0xFFF59E0B)
                                )
                            }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                HardwareRotaryKnob(
                                    value = currentTrack.compThresholdDb,
                                    onValueChange = { onCompChange?.invoke(currentTrack.id, currentTrack.compEnabled, it, currentTrack.compRatio) },
                                    valueRange = -36f..0f,
                                    label = "THRESHOLD",
                                    displayValue = "${currentTrack.compThresholdDb.toInt()} dB",
                                    color = Color(0xFFF59E0B),
                                    sizeDp = 44.dp
                                )

                                // Gain Reduction Meter
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("GAIN REDUCTION", fontSize = 7.sp, fontFamily = FontFamily.Monospace, color = TextMuted)
                                    val grVal = if (currentTrack.compEnabled) (trackPeak * 0.5f).coerceIn(0f, 1f) else 0f
                                    Box(
                                        modifier = Modifier
                                            .width(60.dp)
                                            .height(10.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(Color(0xFF090B0E))
                                            .border(0.5.dp, Color(0xFF2A3140))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(grVal)
                                                .fillMaxHeight()
                                                .background(LedAmber)
                                        )
                                    }
                                }

                                HardwareRotaryKnob(
                                    value = currentTrack.compRatio,
                                    onValueChange = { onCompChange?.invoke(currentTrack.id, currentTrack.compEnabled, currentTrack.compThresholdDb, it) },
                                    valueRange = 1.5f..12.0f,
                                    label = "RATIO",
                                    displayValue = String.format("%.1f:1", currentTrack.compRatio),
                                    color = Color(0xFFF59E0B),
                                    sizeDp = 44.dp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
