package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Channel
import com.example.model.NoteEvent
import com.example.model.PlayMode
import com.example.ui.theme.*
import kotlin.math.roundToInt

enum class GridDivision(val label: String, val stepInterval: Int) {
    STEP_1_16("1/16 Step", 1),
    STEP_1_8("1/8 Beat", 2),
    STEP_1_4("1/4 Bar", 4),
    STEP_1_2("1/2 Bar", 8)
}

enum class ChordStampType(val label: String) {
    OFF("Single Note"),
    MAJOR("Maj Triad (1-3-5)"),
    MINOR("Min Triad (1-b3-5)"),
    MAJ7("Major 7th (1-3-5-7)"),
    MIN7("Minor 7th (1-b3-5-b7)"),
    DOM7("Dom 7th (1-3-5-b7)"),
    SUS4("Suspended 4th"),
    OCTAVE("Octave Stack (1-8)")
}

@Composable
fun PianoRollView(
    channels: List<Channel>,
    selectedChannelId: String,
    currentStep: Int,
    isPlaying: Boolean,
    bpm: Int,
    playMode: PlayMode,
    onTogglePlay: () -> Unit,
    onBpmChange: (Int) -> Unit,
    onChannelSelect: (String) -> Unit,
    onNoteToggle: (channelId: String, pitch: Int, step: Int) -> Unit,
    onAuditionNote: (channel: Channel, pitch: Int) -> Unit,
    onQuantizeNotes: (channelId: String, gridDivision: Int) -> Unit,
    onStampChord: (channelId: String, chordType: String, rootPitch: Int, step: Int) -> Unit,
    onClearNotes: (channelId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeChannel = channels.find { it.id == selectedChannelId } ?: channels.firstOrNull()

    var octaveOffset by remember { mutableIntStateOf(0) } // -2 to +2
    var showChannelPicker by remember { mutableStateOf(false) }
    var showChordPicker by remember { mutableStateOf(false) }
    var selectedChordType by remember { mutableStateOf(ChordStampType.OFF) }
    var selectedGridDivision by remember { mutableStateOf(GridDivision.STEP_1_16) }
    var showHardwareKeyboard by remember { mutableStateOf(true) }
    var pitchWheelValue by remember { mutableFloatStateOf(0f) }
    var modWheelValue by remember { mutableFloatStateOf(0.5f) }

    val horizontalScrollState = rememberScrollState()

    // Generate list of MIDI pitches for 2 octaves (25 keys total, e.g. C3 to C5 or C4 to C6)
    val baseMidi = 48 + (octaveOffset * 12) // 48 = C3, 60 = C4
    val pitches = remember(baseMidi) {
        (24 downTo 0).map { baseMidi + it }
    }

    // Live BPM timing calculations
    val stepMs = ((60000f / bpm) / 4f).roundToInt()
    val barMs = ((60000f / bpm) * 4f).roundToInt()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D0F14))
    ) {
        // TOP HARDWARE WORKSTATION HEADER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF232834), Color(0xFF151821))
                    )
                )
                .border(1.dp, Color(0xFF333E54))
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Channel Sound Selector Dropdown
            Row(verticalAlignment = Alignment.CenterVertically) {
                HardwareRackScrew(modifier = Modifier.size(9.dp))
                Spacer(modifier = Modifier.width(6.dp))

                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF1A1F2B))
                            .border(1.dp, Color(0xFF354054), RoundedCornerShape(4.dp))
                            .clickable { showChannelPicker = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(activeChannel?.colorHex ?: 0xFFFF6B00))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = (activeChannel?.name ?: "NO SOUND").uppercase(),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = FruityOrange,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showChannelPicker,
                        onDismissRequest = { showChannelPicker = false },
                        modifier = Modifier.background(StudioPanel)
                    ) {
                        channels.forEach { ch ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(Color(ch.colorHex))
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(ch.name, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            Text(ch.type.category.uppercase(), color = TextSecondary, fontSize = 8.5.sp)
                                        }
                                    }
                                },
                                onClick = {
                                    onChannelSelect(ch.id)
                                    showChannelPicker = false
                                }
                            )
                        }
                    }
                }
            }

            // Middle: BPM Engine Timing & Grid Snap Tools
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // BPM Live Metric Display
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF10141C))
                        .border(0.5.dp, StudioBorder, RoundedCornerShape(3.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "$bpm BPM",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = FruityOrange
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "1/16:${stepMs}ms",
                                fontSize = 7.5.sp,
                                fontFamily = FontFamily.Monospace,
                                color = FruityCyan
                            )
                        }
                    }
                }

                // Grid Snap Selector
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF141822))
                        .border(0.5.dp, StudioBorder, RoundedCornerShape(3.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SNAP:",
                        fontSize = 7.5.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted,
                        modifier = Modifier.padding(end = 2.dp)
                    )
                    Text(
                        text = selectedGridDivision.label.substringBefore(" "),
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = FruityLime,
                        modifier = Modifier.clickable {
                            selectedGridDivision = when (selectedGridDivision) {
                                GridDivision.STEP_1_16 -> GridDivision.STEP_1_8
                                GridDivision.STEP_1_8 -> GridDivision.STEP_1_4
                                GridDivision.STEP_1_4 -> GridDivision.STEP_1_2
                                GridDivision.STEP_1_2 -> GridDivision.STEP_1_16
                            }
                        }
                    )
                }

                // Quantize to BPM Grid Action
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF18202E))
                        .border(0.5.dp, FruityCyan.copy(alpha = 0.6f), RoundedCornerShape(3.dp))
                        .clickable {
                            activeChannel?.let { ch ->
                                onQuantizeNotes(ch.id, selectedGridDivision.stepInterval)
                            }
                        }
                        .padding(horizontal = 5.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "QUANTIZE",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = FruityCyan
                    )
                }
            }

            // Right: Octave +/- & Keyboard Toggle
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Octave controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF141720))
                        .border(0.5.dp, StudioBorder, RoundedCornerShape(3.dp))
                        .padding(horizontal = 3.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "OCT",
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted
                    )
                    IconButton(
                        onClick = { if (octaveOffset > -2) octaveOffset-- },
                        modifier = Modifier.size(18.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Down", tint = FruityCyan, modifier = Modifier.size(14.dp))
                    }
                    Text(
                        text = "${octaveOffset + 4}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = FruityOrange
                    )
                    IconButton(
                        onClick = { if (octaveOffset < 2) octaveOffset++ },
                        modifier = Modifier.size(18.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Up", tint = FruityCyan, modifier = Modifier.size(14.dp))
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Toggle Hardware Keys Bar
                IconButton(
                    onClick = { showHardwareKeyboard = !showHardwareKeyboard },
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (showHardwareKeyboard) FruityOrange else Color(0xFF1E2430))
                ) {
                    Icon(
                        imageVector = Icons.Default.Piano,
                        contentDescription = "Keys",
                        tint = if (showHardwareKeyboard) Color.Black else TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))
                HardwareRackScrew(modifier = Modifier.size(9.dp))
            }
        }

        // SECONDARY PRODUCTION TOOLS ROW (Chord Stamper, Clear, Transport)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF10131A))
                .border(0.5.dp, StudioBorder)
                .padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Chord Stamper Tool
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "CHORD STAMP:",
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.width(4.dp))
                Box {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (selectedChordType != ChordStampType.OFF) FruityOrange.copy(alpha = 0.2f) else Color(0xFF191D26))
                            .border(0.5.dp, if (selectedChordType != ChordStampType.OFF) FruityOrange else StudioBorder, RoundedCornerShape(3.dp))
                            .clickable { showChordPicker = true }
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = selectedChordType.label,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedChordType != ChordStampType.OFF) FruityOrange else TextPrimary
                        )
                    }

                    DropdownMenu(
                        expanded = showChordPicker,
                        onDismissRequest = { showChordPicker = false },
                        modifier = Modifier.background(StudioPanel)
                    ) {
                        ChordStampType.values().forEach { cType ->
                            DropdownMenuItem(
                                text = { Text(cType.label, fontSize = 10.sp, color = TextPrimary) },
                                onClick = {
                                    selectedChordType = cType
                                    showChordPicker = false
                                }
                            )
                        }
                    }
                }
            }

            // Quick Actions: Play / Pattern vs Song / Clear Notes
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Play / Pause mini-transport
                IconButton(
                    onClick = onTogglePlay,
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (isPlaying) FruityLime else Color(0xFF1D2330))
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = if (isPlaying) Color.Black else FruityLime,
                        modifier = Modifier.size(14.dp)
                    )
                }

                val totalNotes = activeChannel?.notes?.size ?: 0
                Text(
                    text = "$totalNotes NOTES",
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = FruityLime
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF2A1515))
                        .border(0.5.dp, Color(0xFFEF4444).copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                        .clickable {
                            activeChannel?.let { onClearNotes(it.id) }
                        }
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "CLEAR",
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFEF4444)
                    )
                }
            }
        }

        // TIMELINE STEP RULER (Steps 1.1 to 4.4)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0B0D12))
                .border(0.5.dp, StudioBorder)
        ) {
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(18.dp)
                    .background(StudioPanel),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "NOTE",
                    fontSize = 7.5.sp,
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
                for (s in 0 until 16) {
                    val isBeat = (s % 4) == 0
                    val isCurrent = isPlaying && currentStep == s
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(18.dp)
                            .background(if (isCurrent) FruityLime.copy(alpha = 0.25f) else Color.Transparent)
                            .border(0.5.dp, StudioBorder.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${(s / 4) + 1}.${(s % 4) + 1}",
                            fontSize = 7.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isBeat) FontWeight.Black else FontWeight.Normal,
                            color = if (isCurrent) FruityLime else if (isBeat) TextPrimary else TextMuted
                        )
                    }
                }
            }
        }

        // PIANO ROLL GRID MATRIX WITH LEFT PIANO KEYBOARD
        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(pitches) { pitch ->
                    val noteName = getMidiNoteName(pitch)
                    val isBlackKey = isAccidental(pitch)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(21.dp)
                            .border(0.5.dp, Color(0xFF1A1E27))
                    ) {
                        // Interactive Piano Key on the Left
                        Box(
                            modifier = Modifier
                                .width(48.dp)
                                .fillMaxHeight()
                                .background(if (isBlackKey) Color(0xFF14161C) else Color(0xFF282D3B))
                                .border(0.5.dp, Color(0xFF0E1015))
                                .clickable {
                                    activeChannel?.let { onAuditionNote(it, pitch) }
                                }
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Text(
                                text = noteName,
                                fontSize = 8.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (noteName.startsWith("C") && !isBlackKey) FontWeight.Black else FontWeight.Normal,
                                color = if (noteName.startsWith("C") && !isBlackKey) FruityOrange else TextSecondary
                            )
                        }

                        // 16 Step Grid Cells
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .horizontalScroll(horizontalScrollState)
                        ) {
                            for (step in 0 until 16) {
                                val isBeat = (step % 4) == 0
                                val isCurrentStep = isPlaying && currentStep == step
                                val existingNote = activeChannel?.notes?.find { it.pitch == pitch && it.startStep == step }

                                val cellBg = when {
                                    existingNote != null -> if (activeChannel?.type?.isMelodic == true) FruityCyan else FruityOrange
                                    isCurrentStep -> Color(0xFF38BDF8).copy(alpha = 0.15f)
                                    isBlackKey -> Color(0xFF111318)
                                    isBeat -> Color(0xFF171A22)
                                    else -> Color(0xFF13151D)
                                }

                                Box(
                                    modifier = Modifier
                                        .width(36.dp)
                                        .fillMaxHeight()
                                        .background(cellBg)
                                        .border(0.5.dp, StudioBorder.copy(alpha = 0.35f))
                                        .clickable {
                                            activeChannel?.let { ch ->
                                                if (selectedChordType != ChordStampType.OFF) {
                                                    onStampChord(ch.id, selectedChordType.name, pitch, step)
                                                    onAuditionNote(ch, pitch)
                                                } else {
                                                    onNoteToggle(ch.id, pitch, step)
                                                    if (existingNote == null) {
                                                        onAuditionNote(ch, pitch)
                                                    }
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (existingNote != null) {
                                        Text(
                                            text = noteName,
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // HARDWARE SYNTHESIZER PERFORMANCE KEYBOARD STRIP
        if (showHardwareKeyboard) {
            HardwareKeyboardConsole(
                activeChannel = activeChannel,
                baseMidi = baseMidi,
                pitchWheel = pitchWheelValue,
                onPitchWheelChange = { pitchWheelValue = it },
                modWheel = modWheelValue,
                onModWheelChange = { modWheelValue = it },
                onKeyPress = { pitch ->
                    activeChannel?.let { onAuditionNote(it, pitch) }
                }
            )
        }
    }
}

/**
 * Professional Hardware Synthesizer Performance Strip
 * Complete with tactile Ivory/Ebony keys, pitch-bend and modulation wheels
 */
@Composable
fun HardwareKeyboardConsole(
    activeChannel: Channel?,
    baseMidi: Int,
    pitchWheel: Float,
    onPitchWheelChange: (Float) -> Unit,
    modWheel: Float,
    onModWheelChange: (Float) -> Unit,
    onKeyPress: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1A1E27), Color(0xFF0B0D12))
                )
            )
            .border(1.dp, Color(0xFF333E54))
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Hardware Pitch and Mod Wheels
        Row(
            modifier = Modifier
                .width(46.dp)
                .fillMaxHeight(),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pitch Bend Wheel (Bipolar -1..+1 with center return)
            HardwareSpringWheel(
                label = "PITCH",
                value = pitchWheel,
                accentColor = FruityCyan,
                onValueChange = onPitchWheelChange
            )

            // Mod Wheel (Unipolar 0..1)
            HardwareSpringWheel(
                label = "MOD",
                value = modWheel,
                accentColor = FruityOrange,
                onValueChange = onModWheelChange
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Hardware Ivory & Ebony Piano Keys Row
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            horizontalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            // Render 14 white/black keyboard notes in view
            for (keyIdx in 0 until 14) {
                val midiNote = baseMidi + keyIdx
                val isBlack = isAccidental(midiNote)
                val keyName = getMidiNoteName(midiNote)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp))
                        .background(
                            if (isBlack) {
                                Brush.verticalGradient(
                                    listOf(Color(0xFF262A34), Color(0xFF101217))
                                )
                            } else {
                                Brush.verticalGradient(
                                    listOf(Color(0xFFFFFFFF), Color(0xFFD4D8E2))
                                )
                            }
                        )
                        .border(
                            0.5.dp,
                            if (isBlack) Color(0xFF0A0C10) else Color(0xFF8C95A8),
                            RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp)
                        )
                        .clickable { onKeyPress(midiNote) }
                        .padding(bottom = 3.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Text(
                        text = if (!isBlack || keyName.startsWith("C#")) keyName else "",
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = if (isBlack) FruityOrange else Color(0xFF1E2430)
                    )
                }
            }
        }
    }
}

@Composable
fun HardwareSpringWheel(
    label: String,
    value: Float,
    accentColor: Color,
    onValueChange: (Float) -> Unit
) {
    Box(
        modifier = Modifier
            .width(20.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0xFF131720))
            .border(0.5.dp, StudioBorder, RoundedCornerShape(3.dp))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = { onValueChange(0f) },
                    onDragCancel = { onValueChange(0f) },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val delta = -dragAmount.y * 0.02f
                        val updated = (value + delta).coerceIn(-1f, 1f)
                        onValueChange(updated)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .width(14.dp)
                    .height(3.dp)
                    .background(accentColor)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label.take(1),
                fontSize = 6.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                color = TextSecondary
            )
        }
    }
}

fun getMidiNoteName(midi: Int): String {
    val noteNames = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    val note = noteNames[midi % 12]
    val octave = (midi / 12) - 1
    return "$note$octave"
}

fun isAccidental(midi: Int): Boolean {
    val semitone = midi % 12
    return semitone == 1 || semitone == 3 || semitone == 6 || semitone == 8 || semitone == 10
}
