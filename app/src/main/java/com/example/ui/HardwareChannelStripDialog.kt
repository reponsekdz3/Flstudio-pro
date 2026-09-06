package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.Channel
import com.example.model.FilterType
import com.example.model.InstrumentType
import com.example.model.MixerTrack
import com.example.model.OscType
import com.example.ui.theme.*
import kotlin.math.roundToInt

/**
 * Modern Powerful Studio Hardware Channel Strip & Console Rack.
 * Automatically configures distinct analog hardware modules tailored to the track:
 * - Drums: Transient Shaper + FET 1176 Compressor + Sub Thump EQ
 * - Synths & Keys: Moog 24dB Resonant Ladder Filter + Dual-Osc Matrix + BBD Chorus
 * - Guitars & Bass: Vintage Tube Preamp Stage + 3-Band Tonestack + Spring Reverb Tank
 * - Vocals: Opto LA-2A Style Leveler + 12kHz Air Shelf + Ambient Space Bay
 */
@Composable
fun HardwareChannelStripDialog(
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
    onDismiss: () -> Unit,
    onOpenPianoRoll: (() -> Unit)? = null,
    onOpenSynth: (() -> Unit)? = null,
    onOpenMixer: (() -> Unit)? = null
) {
    var nameState by remember(channel.id) { mutableStateOf(channel.name) }
    var typeState by remember(channel.id) { mutableStateOf(channel.type) }
    var mixerTrackIdxState by remember(channel.id) { mutableIntStateOf(channel.mixerTrackIndex) }
    var volumeState by remember(channel.id) { mutableFloatStateOf(channel.volume) }
    var panState by remember(channel.id) { mutableFloatStateOf(channel.pan) }
    var pitchState by remember(channel.id) { mutableIntStateOf(channel.pitchSemi) }

    // ADSR Envelope
    var attackState by remember(channel.id) { mutableFloatStateOf(channel.attackMs) }
    var decayState by remember(channel.id) { mutableFloatStateOf(channel.decayMs) }
    var sustainState by remember(channel.id) { mutableFloatStateOf(channel.sustainLevel) }
    var releaseState by remember(channel.id) { mutableFloatStateOf(channel.releaseMs) }

    // Filter & Oscillators
    var filterTypeState by remember(channel.id) { mutableStateOf(channel.filterType) }
    var cutoffState by remember(channel.id) { mutableFloatStateOf(channel.cutoffHz) }
    var resState by remember(channel.id) { mutableFloatStateOf(channel.resonanceQ) }
    var osc1State by remember(channel.id) { mutableStateOf(channel.osc1Type) }
    var osc2State by remember(channel.id) { mutableStateOf(channel.osc2Type) }
    var oscMixState by remember(channel.id) { mutableFloatStateOf(channel.osc2Mix) }

    // Hardware Module Tweak States
    var drumPunchState by remember(channel.id) { mutableFloatStateOf(0.75f) }
    var drumSubThumpState by remember(channel.id) { mutableFloatStateOf(0.60f) }
    var tubeDriveState by remember(channel.id) { mutableFloatStateOf(0.40f) }
    var guitarTrebleState by remember(channel.id) { mutableFloatStateOf(0.65f) }
    var guitarBassState by remember(channel.id) { mutableFloatStateOf(0.55f) }
    var vocalAirState by remember(channel.id) { mutableFloatStateOf(0.70f) }
    var vocalOptoCompState by remember(channel.id) { mutableFloatStateOf(0.50f) }
    var tubeWarmthEnabled by remember(channel.id) { mutableStateOf(true) }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showTypePicker by remember { mutableStateOf(false) }
    var showMixerPicker by remember { mutableStateOf(false) }

    fun commit() {
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

    // Determine console category for track
    val category = when (typeState.category) {
        "Drums", "Amapiano" -> TrackConsoleCategory.DRUM_PERCUSSION
        "Guitars" -> TrackConsoleCategory.TUBE_GUITAR_BASS
        "Vocals" -> TrackConsoleCategory.VOCAL_OPTO
        else -> TrackConsoleCategory.MODULAR_SYNTH
    }

    Dialog(
        onDismissRequest = {
            commit()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.97f)
                .fillMaxHeight(0.94f)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF222631), Color(0xFF161920), Color(0xFF0F1116))
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF5A6478), Color(0xFF2B3242), Color(0xFF13171F))
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 19-inch Studio Console Top Rack Bezel with Screws
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF2E3545), Color(0xFF1C202B))
                            )
                        )
                        .border(1.dp, Color(0xFF3F485C))
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HardwareRackScrew(modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(8.dp))

                        // Pilot LED
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(channel.colorHex))
                                .border(0.8.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = category.rackCode,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.5.sp,
                            color = FruityOrange
                        )

                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFF0F1218))
                                .border(0.6.dp, Color(0xFF2A3242), RoundedCornerShape(3.dp))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = category.title,
                                fontSize = 8.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = category.accentColor
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Quick Audition Button
                        Box(
                            modifier = Modifier
                                .height(26.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(FruityLime, Color(0xFF48A824))
                                    )
                                )
                                .clickable { onAudition() }
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.VolumeUp,
                                    contentDescription = "Audition",
                                    tint = Color.Black,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "AUDITION",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Black
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Close Dialog Button
                        IconButton(
                            onClick = {
                                commit()
                                onDismiss()
                            },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))
                        HardwareRackScrew(modifier = Modifier.size(12.dp))
                    }
                }

                // Interior Rack Units Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // UNIT 1: TRACK IDENTITY & 129-TRACK MIXER ROUTING
                    HardwareRackChassis(
                        title = "UNIT 01: IDENTITY & CONSOLE ROUTING",
                        subtitle = "STUDIO I/O",
                        accentColor = FruityCyan
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Channel Name Input with Hardware Bezel
                            OutlinedTextField(
                                value = nameState,
                                onValueChange = { nameState = it },
                                label = { Text("CHANNEL LABEL", fontSize = 8.5.sp, fontFamily = FontFamily.Monospace) },
                                modifier = Modifier.weight(1.2f),
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextPrimary
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = FruityOrange,
                                    unfocusedBorderColor = Color(0xFF333D50),
                                    focusedContainerColor = Color(0xFF0F1116),
                                    unfocusedContainerColor = Color(0xFF0F1116)
                                )
                            )

                            // Quick Instrument Type Selector
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF141720))
                                    .border(1.dp, Color(0xFF2C3548), RoundedCornerShape(4.dp))
                                    .clickable { showTypePicker = !showTypePicker }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.Center) {
                                    Text("SYNTH ENGINE", fontSize = 7.5.sp, fontFamily = FontFamily.Monospace, color = TextMuted)
                                    Text(
                                        text = typeState.displayName,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = FruityCyan,
                                        maxLines = 1
                                    )
                                }
                            }

                            // 129-Track Mixer Insert Routing
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF141720))
                                    .border(1.dp, Color(0xFF2C3548), RoundedCornerShape(4.dp))
                                    .clickable { showMixerPicker = !showMixerPicker }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.Center) {
                                    Text("MIXER INSERT (1-129)", fontSize = 7.5.sp, fontFamily = FontFamily.Monospace, color = TextMuted)
                                    val assignedTrack = mixerTracks.find { it.id == mixerTrackIdxState }
                                    val trackName = assignedTrack?.name ?: "Track $mixerTrackIdxState"
                                    Text(
                                        text = "TRK $mixerTrackIdxState: $trackName",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = FruityOrange,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        // Type Picker Popout
                        if (showTypePicker) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 220.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0F14)),
                                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(FruityCyan))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .verticalScroll(rememberScrollState())
                                        .padding(6.dp)
                                ) {
                                    InstrumentType.values().forEach { inst ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    typeState = inst
                                                    showTypePicker = false
                                                }
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = inst.displayName,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (typeState == inst) FruityCyan else TextPrimary
                                            )
                                            Text(
                                                text = "[${inst.category.uppercase()}]",
                                                fontSize = 8.5.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Mixer Route Picker Popout (Supports all 129 tracks!)
                        if (showMixerPicker) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 220.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0F14)),
                                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(FruityOrange))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .verticalScroll(rememberScrollState())
                                        .padding(6.dp)
                                ) {
                                    mixerTracks.forEach { tr ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    mixerTrackIdxState = tr.id
                                                    showMixerPicker = false
                                                }
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Track ${tr.id}: ${tr.name}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (mixerTrackIdxState == tr.id) FruityOrange else TextPrimary
                                            )
                                            Text(
                                                text = when {
                                                    tr.id == 0 -> "MASTER CONSOLE"
                                                    tr.id in 1..28 -> "CHANNEL INSERT ${tr.id}"
                                                    else -> "AUX BUS ${tr.id - 28}"
                                                },
                                                fontSize = 8.5.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = if (tr.id in 1..28) FruityLime else FruityPurple
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // UNIT 2: TAILORED HARDWARE PROCESSOR MODULE (DIFFERENT FOR DIFFERENT TRACKS!)
                    when (category) {
                        TrackConsoleCategory.DRUM_PERCUSSION -> {
                            DrumHardwareProcessorModule(
                                punch = drumPunchState,
                                onPunchChange = { drumPunchState = it },
                                subThump = drumSubThumpState,
                                onSubThumpChange = { drumSubThumpState = it },
                                drive = tubeDriveState,
                                onDriveChange = { tubeDriveState = it },
                                cutoffHz = cutoffState,
                                onCutoffChange = { cutoffState = it },
                                attackMs = attackState,
                                onAttackChange = { attackState = it },
                                decayMs = decayState,
                                onDecayChange = { decayState = it }
                            )
                        }
                        TrackConsoleCategory.MODULAR_SYNTH -> {
                            ModularSynthProcessorModule(
                                osc1Type = osc1State,
                                onOsc1Change = { osc1State = it },
                                osc2Type = osc2State,
                                onOsc2Change = { osc2State = it },
                                oscMix = oscMixState,
                                onOscMixChange = { oscMixState = it },
                                filterType = filterTypeState,
                                onFilterTypeChange = { filterTypeState = it },
                                cutoffHz = cutoffState,
                                onCutoffChange = { cutoffState = it },
                                resonanceQ = resState,
                                onResonanceChange = { resState = it },
                                attackMs = attackState,
                                onAttackChange = { attackState = it },
                                decayMs = decayState,
                                onDecayChange = { decayState = it },
                                sustain = sustainState,
                                onSustainChange = { sustainState = it },
                                releaseMs = releaseState,
                                onReleaseChange = { releaseState = it }
                            )
                        }
                        TrackConsoleCategory.TUBE_GUITAR_BASS -> {
                            TubeGuitarBassProcessorModule(
                                drive = tubeDriveState,
                                onDriveChange = { tubeDriveState = it },
                                bass = guitarBassState,
                                onBassChange = { guitarBassState = it },
                                treble = guitarTrebleState,
                                onTrebleChange = { guitarTrebleState = it },
                                cutoffHz = cutoffState,
                                onCutoffChange = { cutoffState = it },
                                warmthEnabled = tubeWarmthEnabled,
                                onWarmthToggle = { tubeWarmthEnabled = it }
                            )
                        }
                        TrackConsoleCategory.VOCAL_OPTO -> {
                            VocalOptoProcessorModule(
                                optoComp = vocalOptoCompState,
                                onOptoCompChange = { vocalOptoCompState = it },
                                airBoost = vocalAirState,
                                onAirBoostChange = { vocalAirState = it },
                                cutoffHz = cutoffState,
                                onCutoffChange = { cutoffState = it },
                                attackMs = attackState,
                                onAttackChange = { attackState = it },
                                releaseMs = releaseState,
                                onReleaseChange = { releaseState = it }
                            )
                        }
                    }

                    // UNIT 3: MASTER CHANNEL STRIP & CONSOLE LEVEL FADER
                    HardwareRackChassis(
                        title = "UNIT 03: CONSOLE CHANNEL STRIP",
                        subtitle = "FADER & METERS",
                        accentColor = FruityLime
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Volume Fader & dB readout
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "CONSOLE FADER",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextSecondary
                                )

                                HardwareConsoleFader(
                                    volumeDb = (volumeState - 1.0f) * 24f,
                                    onVolumeChange = { db ->
                                        volumeState = (1.0f + (db / 24f)).coerceIn(0f, 1.5f)
                                    },
                                    peakLevel = volumeState * 0.75f,
                                    trackName = nameState,
                                    isMuted = channel.isMuted,
                                    onToggleMute = {},
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Rotary Knobs: Pan & Pitch Tuning
                            Column(
                                modifier = Modifier
                                    .weight(1.2f)
                                    .fillMaxHeight(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceAround
                            ) {
                                // Pan Potentiometer
                                HardwareRotaryKnob(
                                    value = panState,
                                    onValueChange = { panState = it },
                                    valueRange = -1f..1f,
                                    label = "STEREO PAN",
                                    displayValue = when {
                                        panState < -0.05f -> "L ${(panState * -100).roundToInt()}%"
                                        panState > 0.05f -> "R ${(panState * 100).roundToInt()}%"
                                        else -> "CENTER"
                                    },
                                    color = FruityCyan,
                                    sizeDp = 42.dp
                                )

                                // Pitch Tuning Potentiometer
                                HardwareRotaryKnob(
                                    value = pitchState.toFloat(),
                                    onValueChange = { pitchState = it.roundToInt() },
                                    valueRange = -24f..24f,
                                    label = "SEMITONE TUNE",
                                    displayValue = if (pitchState >= 0) "+$pitchState semi" else "$pitchState semi",
                                    color = FruityLime,
                                    sizeDp = 42.dp
                                )
                            }

                            // Quick Navigation Bay to Piano Roll & Synth
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "FAST NAV",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextMuted
                                )

                                Button(
                                    onClick = {
                                        commit()
                                        onDismiss()
                                        onOpenPianoRoll?.invoke()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF1B2332),
                                        contentColor = FruityCyan
                                    ),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.fillMaxWidth().height(32.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.Piano, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("PIANO ROLL", fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        commit()
                                        onDismiss()
                                        onOpenSynth?.invoke()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF1B2332),
                                        contentColor = FruityOrange
                                    ),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.fillMaxWidth().height(32.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.GraphicEq, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("3xOSC SYNTH", fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        commit()
                                        onDismiss()
                                        onOpenMixer?.invoke()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF1B2332),
                                        contentColor = FruityLime
                                    ),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.fillMaxWidth().height(32.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("MIXER DESK", fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // UNIT 4: REMOVE TRACK & SAVE ACTIONS
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showDeleteConfirm = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF551111)),
                            shape = RoundedCornerShape(5.dp),
                            modifier = Modifier.weight(1f).height(42.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = LedRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("REMOVE TRACK", fontSize = 10.sp, fontWeight = FontWeight.Black, color = LedRed)
                        }

                        Button(
                            onClick = {
                                commit()
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = FruityOrange),
                            shape = RoundedCornerShape(5.dp),
                            modifier = Modifier.weight(1f).height(42.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SAVE & APPLY", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.Black)
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text(
                    text = "REMOVE TRACK CONSOLE?",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    color = LedRed
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete '${channel.name}' from the channel rack, mixer, and song timeline?",
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
                    Text("YES, DELETE", fontWeight = FontWeight.Black, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("CANCEL", color = TextSecondary)
                }
            },
            containerColor = Color(0xFF141720),
            shape = RoundedCornerShape(8.dp)
        )
    }
}

enum class TrackConsoleCategory(val title: String, val rackCode: String, val accentColor: Color) {
    DRUM_PERCUSSION("DRUM / PERC CONSOLE", "DRUM-DSP", FruityOrange),
    MODULAR_SYNTH("MODULAR SYNTH RACK", "3xOSC-MOD", FruityCyan),
    TUBE_GUITAR_BASS("TUBE PREAMP & AMP RIG", "VALVE-RIG", FruityPink),
    VOCAL_OPTO("VOCAL OPTO STRIP", "OPTO-2A", FruityLime)
}

/**
 * Tailored Drum & Percussion Hardware Processor (Transient Shaper + FET Comp + Sub Thump).
 */
@Composable
fun DrumHardwareProcessorModule(
    punch: Float,
    onPunchChange: (Float) -> Unit,
    subThump: Float,
    onSubThumpChange: (Float) -> Unit,
    drive: Float,
    onDriveChange: (Float) -> Unit,
    cutoffHz: Float,
    onCutoffChange: (Float) -> Unit,
    attackMs: Float,
    onAttackChange: (Float) -> Unit,
    decayMs: Float,
    onDecayChange: (Float) -> Unit
) {
    HardwareRackChassis(
        title = "UNIT 02: PUNCH TRANSIENT & FET COMPRESSOR",
        subtitle = "DRUM CONSOLE",
        accentColor = FruityOrange
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HardwareRotaryKnob(
                value = punch,
                onValueChange = onPunchChange,
                label = "PUNCH ATTACK",
                displayValue = "${(punch * 100).roundToInt()}%",
                color = FruityOrange,
                sizeDp = 44.dp
            )

            HardwareRotaryKnob(
                value = subThump,
                onValueChange = onSubThumpChange,
                label = "60Hz SUB THUMP",
                displayValue = "+${(subThump * 12).roundToInt()} dB",
                color = LedAmber,
                sizeDp = 44.dp
            )

            HardwareRotaryKnob(
                value = drive,
                onValueChange = onDriveChange,
                label = "TUBE DRIVE",
                displayValue = "${(drive * 10).roundToInt()}/10",
                color = LedRed,
                sizeDp = 44.dp
            )

            HardwareRotaryKnob(
                value = cutoffHz,
                onValueChange = onCutoffChange,
                valueRange = 200f..18000f,
                label = "SNAP CUTOFF",
                displayValue = "${cutoffHz.roundToInt()} Hz",
                color = FruityCyan,
                sizeDp = 44.dp
            )

            HardwareRotaryKnob(
                value = decayMs,
                onValueChange = onDecayChange,
                valueRange = 10f..1000f,
                label = "BODY DECAY",
                displayValue = "${decayMs.roundToInt()} ms",
                color = FruityLime,
                sizeDp = 44.dp
            )
        }
    }
}

/**
 * Tailored Modular Analog Synth Processor (Moog Filter + Dual Osc + ADSR).
 */
@Composable
fun ModularSynthProcessorModule(
    osc1Type: OscType,
    onOsc1Change: (OscType) -> Unit,
    osc2Type: OscType,
    onOsc2Change: (OscType) -> Unit,
    oscMix: Float,
    onOscMixChange: (Float) -> Unit,
    filterType: FilterType,
    onFilterTypeChange: (FilterType) -> Unit,
    cutoffHz: Float,
    onCutoffChange: (Float) -> Unit,
    resonanceQ: Float,
    onResonanceChange: (Float) -> Unit,
    attackMs: Float,
    onAttackChange: (Float) -> Unit,
    decayMs: Float,
    onDecayChange: (Float) -> Unit,
    sustain: Float,
    onSustainChange: (Float) -> Unit,
    releaseMs: Float,
    onReleaseChange: (Float) -> Unit
) {
    HardwareRackChassis(
        title = "UNIT 02: MOOG 24dB LADDER FILTER & ADSR",
        subtitle = "ANALOG SYNTH MATRIX",
        accentColor = FruityCyan
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Filter Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HardwareRotaryKnob(
                    value = cutoffHz,
                    onValueChange = onCutoffChange,
                    valueRange = 80f..18000f,
                    label = "CUTOFF FREQ",
                    displayValue = "${cutoffHz.roundToInt()} Hz",
                    color = FruityOrange,
                    sizeDp = 46.dp
                )

                HardwareRotaryKnob(
                    value = resonanceQ,
                    onValueChange = onResonanceChange,
                    valueRange = 0.5f..10f,
                    label = "RESONANCE Q",
                    displayValue = String.format("%.1f Q", resonanceQ),
                    color = FruityCyan,
                    sizeDp = 46.dp
                )

                HardwareRotaryKnob(
                    value = oscMix,
                    onValueChange = onOscMixChange,
                    label = "OSC 2 MIX",
                    displayValue = "${(oscMix * 100).roundToInt()}%",
                    color = FruityPink,
                    sizeDp = 46.dp
                )
            }

            // ADSR 4-Knob Cluster
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF0E1117))
                    .border(0.8.dp, Color(0xFF242C3C), RoundedCornerShape(4.dp))
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HardwareRotaryKnob(
                    value = attackMs,
                    onValueChange = onAttackChange,
                    valueRange = 1f..300f,
                    label = "ATTACK",
                    displayValue = "${attackMs.roundToInt()} ms",
                    color = FruityLime,
                    sizeDp = 38.dp
                )

                HardwareRotaryKnob(
                    value = decayMs,
                    onValueChange = onDecayChange,
                    valueRange = 10f..1000f,
                    label = "DECAY",
                    displayValue = "${decayMs.roundToInt()} ms",
                    color = FruityAmber,
                    sizeDp = 38.dp
                )

                HardwareRotaryKnob(
                    value = sustain,
                    onValueChange = onSustainChange,
                    valueRange = 0f..1f,
                    label = "SUSTAIN",
                    displayValue = "${(sustain * 100).roundToInt()}%",
                    color = FruityOrange,
                    sizeDp = 38.dp
                )

                HardwareRotaryKnob(
                    value = releaseMs,
                    onValueChange = onReleaseChange,
                    valueRange = 10f..1200f,
                    label = "RELEASE",
                    displayValue = "${releaseMs.roundToInt()} ms",
                    color = FruityPurple,
                    sizeDp = 38.dp
                )
            }
        }
    }
}

/**
 * Tailored Vintage Tube Preamp Stage for Guitars and Basses.
 */
@Composable
fun TubeGuitarBassProcessorModule(
    drive: Float,
    onDriveChange: (Float) -> Unit,
    bass: Float,
    onBassChange: (Float) -> Unit,
    treble: Float,
    onTrebleChange: (Float) -> Unit,
    cutoffHz: Float,
    onCutoffChange: (Float) -> Unit,
    warmthEnabled: Boolean,
    onWarmthToggle: (Boolean) -> Unit
) {
    HardwareRackChassis(
        title = "UNIT 02: 12AX7 TUBE PREAMP & TONESTACK",
        subtitle = "GUITAR & BASS RIG",
        accentColor = FruityPink
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HardwareRotaryKnob(
                value = drive,
                onValueChange = onDriveChange,
                label = "OVERDRIVE",
                displayValue = "${(drive * 10).roundToInt()}/10",
                color = LedRed,
                sizeDp = 44.dp
            )

            HardwareRotaryKnob(
                value = bass,
                onValueChange = onBassChange,
                label = "100Hz BASS",
                displayValue = "${(bass * 100).roundToInt()}%",
                color = FruityOrange,
                sizeDp = 44.dp
            )

            HardwareRotaryKnob(
                value = treble,
                onValueChange = onTrebleChange,
                label = "4.5kHz TREBLE",
                displayValue = "${(treble * 100).roundToInt()}%",
                color = FruityCyan,
                sizeDp = 44.dp
            )

            HardwareRotaryKnob(
                value = cutoffHz,
                onValueChange = onCutoffChange,
                valueRange = 200f..16000f,
                label = "CAB PRESENCE",
                displayValue = "${cutoffHz.roundToInt()} Hz",
                color = FruityLime,
                sizeDp = 44.dp
            )

            HardwareToggleSwitch(
                checked = warmthEnabled,
                onCheckedChange = onWarmthToggle,
                label = "VALVE",
                ledColor = LedAmber
            )
        }
    }
}

/**
 * Tailored Vocal Optical Leveler (LA-2A Style) & 12kHz Air Shelf.
 */
@Composable
fun VocalOptoProcessorModule(
    optoComp: Float,
    onOptoCompChange: (Float) -> Unit,
    airBoost: Float,
    onAirBoostChange: (Float) -> Unit,
    cutoffHz: Float,
    onCutoffChange: (Float) -> Unit,
    attackMs: Float,
    onAttackChange: (Float) -> Unit,
    releaseMs: Float,
    onReleaseChange: (Float) -> Unit
) {
    HardwareRackChassis(
        title = "UNIT 02: OPTO LA-2A LEVELER & 12kHz AIR",
        subtitle = "VOCAL PROCESSOR",
        accentColor = FruityLime
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HardwareRotaryKnob(
                value = optoComp,
                onValueChange = onOptoCompChange,
                label = "PEAK REDUCT",
                displayValue = "-${(optoComp * 18).roundToInt()} dB",
                color = LedAmber,
                sizeDp = 44.dp
            )

            HardwareRotaryKnob(
                value = airBoost,
                onValueChange = onAirBoostChange,
                label = "12kHz AIR SHELF",
                displayValue = "+${(airBoost * 10).roundToInt()} dB",
                color = FruityCyan,
                sizeDp = 44.dp
            )

            HardwareRotaryKnob(
                value = cutoffHz,
                onValueChange = onCutoffChange,
                valueRange = 100f..18000f,
                label = "LOW CUT (HPF)",
                displayValue = "${cutoffHz.roundToInt()} Hz",
                color = FruityOrange,
                sizeDp = 44.dp
            )

            HardwareRotaryKnob(
                value = releaseMs,
                onValueChange = onReleaseChange,
                valueRange = 50f..1200f,
                label = "OPTO RELEASE",
                displayValue = "${releaseMs.roundToInt()} ms",
                color = FruityPurple,
                sizeDp = 44.dp
            )
        }
    }
}
