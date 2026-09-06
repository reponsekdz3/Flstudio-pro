package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.*
import com.example.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun AudioRecordDialog(
    isRecording: Boolean,
    liveAmplitude: Float,
    onStartRecording: (onStarted: (Boolean) -> Unit) -> Unit,
    onStopAndSave: (sampleName: String) -> Unit,
    onDismiss: () -> Unit
) {
    var sampleName by remember { mutableStateOf("Vocal Sample 1") }
    var recordingStatusText by remember { mutableStateOf("Ready to record microphone audio") }
    var hasRecordedData by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = StudioPanel),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(StudioBorder)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = FruityPink,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "FL AUDIO RECORDER",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Waveform / Amplitude meter display
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF090B0E))
                        .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isRecording) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            for (i in 0 until 18) {
                                val barHeightRatio = (liveAmplitude * (0.3f + (i % 5) * 0.15f)).coerceIn(0.1f, 1f)
                                Box(
                                    modifier = Modifier
                                        .width(6.dp)
                                        .fillMaxHeight(barHeightRatio)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(if (liveAmplitude > 0.7f) LedRed else FruityPink)
                                )
                            }
                        }
                    } else {
                        Text(
                            text = recordingStatusText,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Record / Stop Control Button
                if (!isRecording) {
                    Button(
                        onClick = {
                            recordingStatusText = "Recording live audio..."
                            onStartRecording { success ->
                                if (!success) {
                                    recordingStatusText = "Microphone access failed or permission needed"
                                } else {
                                    hasRecordedData = true
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("start_mic_recording_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FruityPink,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.FiberManualRecord, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("START RECORDING", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            hasRecordedData = true
                            recordingStatusText = "Sample captured!"
                            onStopAndSave(sampleName)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("stop_mic_recording_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LedRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("STOP & ADD TO PROJECT", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Sample Name TextField
                OutlinedTextField(
                    value = sampleName,
                    onValueChange = { sampleName = it },
                    label = { Text("Sample Name", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = FruityPink,
                        unfocusedBorderColor = StudioBorder
                    )
                )
            }
        }
    }
}

@Composable
fun AddChannelDialog(
    onAddChannel: (type: InstrumentType, name: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }

    val categories = listOf("All", "Amapiano", "EDM", "Hip-Hop", "Pop", "Drums", "Bass", "Guitars", "Synth", "Vocals", "FX")

    val allInstruments = InstrumentType.values().toList()
    val filteredInstruments = allInstruments.filter { inst ->
        val matchesCat = if (selectedCategory == "All") true else inst.category.equals(selectedCategory, ignoreCase = true)
        val matchesSearch = searchQuery.isBlank() ||
                inst.displayName.contains(searchQuery, ignoreCase = true) ||
                inst.category.contains(searchQuery, ignoreCase = true)
        matchesCat && matchesSearch
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = StudioPanel),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(StudioBorder)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                // Header with hardware styling
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HardwareRackScrew(modifier = Modifier.size(10.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "SOUND GENERATOR & INSTRUMENT VAULT",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = FruityOrange
                            )
                            Text(
                                text = "Select from ${allInstruments.size} hardware instruments & genre engines",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Box
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search kicks, 808s, amapiano, edm, guitars, synths...", fontSize = 11.sp, color = TextSecondary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = FruityOrange, modifier = Modifier.size(18.dp))
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = FruityOrange,
                        unfocusedBorderColor = StudioBorder,
                        focusedContainerColor = Color(0xFF0D0F14),
                        unfocusedContainerColor = Color(0xFF0D0F14)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Category Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = selectedCategory == cat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSelected) FruityOrange else StudioPanelLight)
                                .border(1.dp, if (isSelected) FruityOrangeGlow else StudioBorder, RoundedCornerShape(4.dp))
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = cat.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                color = if (isSelected) Color.Black else TextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Grid of Instruments
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredInstruments) { inst ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAddChannel(inst, inst.displayName) },
                            colors = CardDefaults.cardColors(containerColor = StudioPanelLight),
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(StudioBorder))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            when (inst.category) {
                                                "Amapiano" -> Color(0xFFEAB308).copy(alpha = 0.2f)
                                                "EDM" -> Color(0xFF3B82F6).copy(alpha = 0.2f)
                                                "Hip-Hop" -> Color(0xFF8B5CF6).copy(alpha = 0.2f)
                                                "Pop" -> Color(0xFFFF4081).copy(alpha = 0.2f)
                                                "Guitars" -> Color(0xFF10B981).copy(alpha = 0.2f)
                                                "Bass" -> Color(0xFF00E5FF).copy(alpha = 0.2f)
                                                else -> FruityOrange.copy(alpha = 0.2f)
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (inst.isMelodic) Icons.Default.Piano else Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = when (inst.category) {
                                            "Amapiano" -> Color(0xFFEAB308)
                                            "EDM" -> Color(0xFF3B82F6)
                                            "Hip-Hop" -> Color(0xFF8B5CF6)
                                            "Pop" -> Color(0xFFFF4081)
                                            "Guitars" -> Color(0xFF10B981)
                                            "Bass" -> Color(0xFF00E5FF)
                                            else -> FruityOrange
                                        },
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = inst.displayName,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = inst.category.uppercase(),
                                        fontSize = 8.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = TextSecondary
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

@Composable
fun TrackInspectorDialog(
    channel: Channel,
    mixerTracks: List<MixerTrack>,
    onUpdateChannel: (
        name: String,
        type: InstrumentType,
        mixerTrackIndex: Int,
        volume: Float,
        pan: Float,
        pitchSemi: Int,
        attackMs: Float,
        decayMs: Float,
        sustainLevel: Float,
        releaseMs: Float,
        filterType: FilterType,
        cutoffHz: Float,
        resonanceQ: Float,
        osc1Type: OscType,
        osc2Type: OscType,
        oscMix: Float
    ) -> Unit,
    onAudition: () -> Unit,
    onDeleteChannel: () -> Unit,
    onDismiss: () -> Unit
) {
    var nameState by remember(channel.id) { mutableStateOf(channel.name) }
    var typeState by remember(channel.id) { mutableStateOf(channel.type) }
    var mixerTrackIdxState by remember(channel.id) { mutableIntStateOf(channel.mixerTrackIndex) }
    var volumeState by remember(channel.id) { mutableFloatStateOf(channel.volume) }
    var panState by remember(channel.id) { mutableFloatStateOf(channel.pan) }
    var pitchState by remember(channel.id) { mutableIntStateOf(channel.pitchSemi) }
    var attackState by remember(channel.id) { mutableFloatStateOf(channel.attackMs) }
    var decayState by remember(channel.id) { mutableFloatStateOf(channel.decayMs) }
    var sustainState by remember(channel.id) { mutableFloatStateOf(channel.sustainLevel) }
    var releaseState by remember(channel.id) { mutableFloatStateOf(channel.releaseMs) }
    var filterTypeState by remember(channel.id) { mutableStateOf(channel.filterType) }
    var cutoffState by remember(channel.id) { mutableFloatStateOf(channel.cutoffHz) }
    var resState by remember(channel.id) { mutableFloatStateOf(channel.resonanceQ) }
    var osc1State by remember(channel.id) { mutableStateOf(channel.osc1Type) }
    var osc2State by remember(channel.id) { mutableStateOf(channel.osc2Type) }
    var oscMixState by remember(channel.id) { mutableFloatStateOf(channel.osc2Mix) }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showTypePicker by remember { mutableStateOf(false) }
    var showMixerPicker by remember { mutableStateOf(false) }

    fun commitChanges() {
        onUpdateChannel(
            nameState,
            typeState,
            mixerTrackIdxState,
            volumeState,
            panState,
            pitchState,
            attackState,
            decayState,
            sustainState,
            releaseState,
            filterTypeState,
            cutoffState,
            resState,
            osc1State,
            osc2State,
            oscMixState
        )
    }

    Dialog(
        onDismissRequest = {
            commitChanges()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .padding(6.dp),
            colors = CardDefaults.cardColors(containerColor = StudioDarkBg),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(FruityOrange)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp)
            ) {
                // Top Hardware Chassis Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(StudioPanel)
                        .border(1.dp, StudioBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HardwareRackScrew(modifier = Modifier.size(10.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .width(6.dp)
                                .height(28.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(channel.colorHex))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "CHANNEL SETTINGS / TRACK INSPECTOR",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = FruityOrange
                            )
                            Text(
                                text = "${typeState.displayName} • Routed to Track $mixerTrackIdxState",
                                fontSize = 9.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Audition button
                        IconButton(
                            onClick = { onAudition() },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(FruityLime)
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = "Audition", tint = Color.Black, modifier = Modifier.size(16.dp))
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Close button
                        IconButton(
                            onClick = {
                                commitChanges()
                                onDismiss()
                            },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Track Name & Instrument Type Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = StudioPanel),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(StudioBorder))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "TRACK IDENTITY & SYNTHESIS TYPE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = FruityAmber
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = nameState,
                            onValueChange = { nameState = it },
                            label = { Text("Track / Channel Name", fontSize = 10.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = FruityOrange,
                                unfocusedBorderColor = StudioBorder
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Instrument Type Button
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(StudioPanelLight)
                                    .border(1.dp, StudioBorder, RoundedCornerShape(6.dp))
                                    .clickable { showTypePicker = !showTypePicker }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Column {
                                    Text("INSTRUMENT TYPE", fontSize = 8.sp, color = TextSecondary)
                                    Text(typeState.displayName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FruityCyan)
                                }
                            }

                            // Mixer Route Button
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(StudioPanelLight)
                                    .border(1.dp, StudioBorder, RoundedCornerShape(6.dp))
                                    .clickable { showMixerPicker = !showMixerPicker }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Column {
                                    Text("MIXER INSERT", fontSize = 8.sp, color = TextSecondary)
                                    val trackName = mixerTracks.getOrNull(mixerTrackIdxState)?.name ?: "Track $mixerTrackIdxState"
                                    Text("Track $mixerTrackIdxState: $trackName", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FruityOrange)
                                }
                            }
                        }

                        // Type Picker Dropdown
                        if (showTypePicker) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF10131B)),
                                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(FruityCyan))
                            ) {
                                Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(6.dp)) {
                                    InstrumentType.values().forEach { inst ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    typeState = inst
                                                    showTypePicker = false
                                                }
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(inst.displayName, fontSize = 11.sp, color = if (typeState == inst) FruityCyan else TextPrimary)
                                            Text(inst.category, fontSize = 9.sp, color = TextSecondary)
                                        }
                                    }
                                }
                            }
                        }

                        // Mixer Route Picker Dropdown
                        if (showMixerPicker) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF10131B)),
                                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(FruityOrange))
                            ) {
                                Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(6.dp)) {
                                    mixerTracks.take(15).forEach { tr ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    mixerTrackIdxState = tr.id
                                                    showMixerPicker = false
                                                }
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Track ${tr.id}: ${tr.name}", fontSize = 11.sp, color = if (mixerTrackIdxState == tr.id) FruityOrange else TextPrimary)
                                            Text(if (tr.id == 0) "MASTER" else "INSERT", fontSize = 9.sp, color = TextSecondary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Hardware Knobs: Volume, Pan, Pitch Tuning
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = StudioPanel),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(StudioBorder))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "ANALOG CONSOLE CONTROLS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = FruityAmber
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Volume Fader Slider
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("VOLUME", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary, modifier = Modifier.width(60.dp))
                            Slider(
                                value = volumeState,
                                onValueChange = { volumeState = it },
                                valueRange = 0f..1.5f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(thumbColor = FruityOrange, activeTrackColor = FruityOrange)
                            )
                            Text(
                                text = "${(volumeState * 100).roundToInt()}%",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = FruityOrange,
                                modifier = Modifier.width(42.dp)
                            )
                        }

                        // Pan Slider
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("PAN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary, modifier = Modifier.width(60.dp))
                            Slider(
                                value = panState,
                                onValueChange = { panState = it },
                                valueRange = -1f..1f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(thumbColor = FruityCyan, activeTrackColor = FruityCyan)
                            )
                            val panText = when {
                                panState < -0.05f -> "L ${(panState * -100).roundToInt()}%"
                                panState > 0.05f -> "R ${(panState * 100).roundToInt()}%"
                                else -> "C"
                            }
                            Text(
                                text = panText,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = FruityCyan,
                                modifier = Modifier.width(42.dp)
                            )
                        }

                        // Pitch Tuning (-24 to +24 semitones)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("PITCH", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary, modifier = Modifier.width(60.dp))
                            Slider(
                                value = pitchState.toFloat(),
                                onValueChange = { pitchState = it.roundToInt() },
                                valueRange = -24f..24f,
                                steps = 47,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(thumbColor = FruityLime, activeTrackColor = FruityLime)
                            )
                            Text(
                                text = if (pitchState >= 0) "+$pitchState semi" else "$pitchState semi",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = FruityLime,
                                modifier = Modifier.width(65.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ADSR Filter & Envelope
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = StudioPanel),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(StudioBorder))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "ADSR ENVELOPE & FILTER SECTION",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = FruityAmber
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Filter Cutoff & Resonance
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("CUTOFF", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary, modifier = Modifier.width(60.dp))
                            Slider(
                                value = cutoffState,
                                onValueChange = { cutoffState = it },
                                valueRange = 200f..18000f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(thumbColor = FruityOrange, activeTrackColor = FruityOrange)
                            )
                            Text("${cutoffState.roundToInt()} Hz", fontSize = 9.5.sp, fontFamily = FontFamily.Monospace, color = FruityOrange, modifier = Modifier.width(65.dp))
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("RESONANCE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary, modifier = Modifier.width(60.dp))
                            Slider(
                                value = resState,
                                onValueChange = { resState = it },
                                valueRange = 0.5f..10f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(thumbColor = FruityOrangeGlow, activeTrackColor = FruityOrangeGlow)
                            )
                            Text(String.format("%.1f Q", resState), fontSize = 9.5.sp, fontFamily = FontFamily.Monospace, color = FruityOrangeGlow, modifier = Modifier.width(65.dp))
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // ADSR Envelope Sliders
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("ATTACK: ${attackState.roundToInt()}ms", fontSize = 9.sp, color = TextSecondary)
                                Slider(value = attackState, onValueChange = { attackState = it }, valueRange = 1f..300f)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("DECAY: ${decayState.roundToInt()}ms", fontSize = 9.sp, color = TextSecondary)
                                Slider(value = decayState, onValueChange = { decayState = it }, valueRange = 10f..1000f)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("SUSTAIN: ${(sustainState * 100).roundToInt()}%", fontSize = 9.sp, color = TextSecondary)
                                Slider(value = sustainState, onValueChange = { sustainState = it }, valueRange = 0f..1f)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("RELEASE: ${releaseState.roundToInt()}ms", fontSize = 9.sp, color = TextSecondary)
                                Slider(value = releaseState, onValueChange = { releaseState = it }, valueRange = 10f..1200f)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Action Buttons: Save Settings & REMOVE TRACK
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Remove Track Button
                    Button(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F1D1D)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove Track", tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("REMOVE TRACK", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    // Apply / Save Button
                    Button(
                        onClick = {
                            commitChanges()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FruityOrange),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save", tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SAVE & CLOSE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text(
                    text = "Remove Track?",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEF4444)
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to remove '${channel.name}' from the channel rack and all project patterns?",
                    fontSize = 11.5.sp,
                    color = TextPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteChannel()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("DELETE TRACK", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("CANCEL", color = TextSecondary)
                }
            },
            containerColor = StudioPanel,
            shape = RoundedCornerShape(10.dp)
        )
    }
}
