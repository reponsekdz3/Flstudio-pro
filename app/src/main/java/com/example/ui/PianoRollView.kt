package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Channel
import com.example.model.NoteEvent
import com.example.ui.theme.*

@Composable
fun PianoRollView(
    channels: List<Channel>,
    selectedChannelId: String,
    currentStep: Int,
    isPlaying: Boolean,
    onChannelSelect: (String) -> Unit,
    onNoteToggle: (channelId: String, pitch: Int, step: Int) -> Unit,
    onAuditionNote: (channel: Channel, pitch: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Allows editing Piano Roll for EVERY sound selected (synths, guitars, drums, samples)
    val activeChannel = channels.find { it.id == selectedChannelId } ?: channels.firstOrNull()

    var octaveOffset by remember { mutableStateOf(0) } // -1, 0, +1
    var showChannelPicker by remember { mutableStateOf(false) }

    val horizontalScrollState = rememberScrollState()

    // Generate list of MIDI pitches for 2 octaves
    val baseMidi = 48 + (octaveOffset * 12) // C3 = 48, C4 = 60, C5 = 72
    val pitches = remember(baseMidi) {
        (24 downTo 0).map { baseMidi + it }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F1116))
    ) {
        // Piano Roll Hardware Header / Toolbar
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

                // Target Channel Dropdown (Every Sound Selected)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF1E2430))
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
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = TextPrimary
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = FruityOrange,
                        modifier = Modifier.size(16.dp)
                    )

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
                                        Text(ch.name, color = TextPrimary, fontSize = 12.sp)
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

            // Octave controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF141720))
                    .border(0.8.dp, Color(0xFF2C3548), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "OCTAVE",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = TextSecondary,
                    modifier = Modifier.padding(end = 4.dp)
                )
                IconButton(
                    onClick = { if (octaveOffset > -2) octaveOffset-- },
                    modifier = Modifier.size(22.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Octave Down",
                        tint = FruityCyan
                    )
                }

                Text(
                    text = "${octaveOffset + 4}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = FruityOrange
                )

                IconButton(
                    onClick = { if (octaveOffset < 2) octaveOffset++ },
                    modifier = Modifier.size(22.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Octave Up",
                        tint = FruityCyan
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                val noteCount = activeChannel?.notes?.size ?: 0
                Text(
                    text = "$noteCount NOTES",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = FruityLime
                )
                Spacer(modifier = Modifier.width(6.dp))
                HardwareRackScrew(modifier = Modifier.size(10.dp))
            }
        }

        // Timeline Step Ruler
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F1116))
                .border(0.5.dp, StudioBorder)
        ) {
            // Spacer matching piano keys width
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(20.dp)
                    .background(StudioPanel),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "KEYS",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted
                )
            }

            // Step Header Numbers (1 to 16)
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
                            .height(20.dp)
                            .background(if (isCurrent) FruityLime.copy(alpha = 0.25f) else Color.Transparent)
                            .border(0.5.dp, StudioBorder.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${(s / 4) + 1}.${(s % 4) + 1}",
                            fontSize = 8.sp,
                            fontWeight = if (isBeat) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCurrent) FruityLime else if (isBeat) TextPrimary else TextMuted
                        )
                    }
                }
            }
        }

        // Note Matrix with Left Piano Keyboard
        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(pitches) { pitch ->
                    val noteName = getMidiNoteName(pitch)
                    val isBlackKey = isAccidental(pitch)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(22.dp)
                            .border(0.5.dp, Color(0xFF1E232E))
                    ) {
                        // Interactive Piano Key
                        Box(
                            modifier = Modifier
                                .width(48.dp)
                                .fillMaxHeight()
                                .background(if (isBlackKey) Color(0xFF16181F) else Color(0xFF2E3342))
                                .border(0.5.dp, Color(0xFF111318))
                                .clickable {
                                    activeChannel?.let { onAuditionNote(it, pitch) }
                                }
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Text(
                                text = noteName,
                                fontSize = 9.sp,
                                fontWeight = if (noteName.startsWith("C") && !isBlackKey) FontWeight.Bold else FontWeight.Normal,
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
                                    existingNote != null -> FruityCyan
                                    isCurrentStep -> Color(0xFF38BDF8).copy(alpha = 0.12f)
                                    isBlackKey -> Color(0xFF121419)
                                    isBeat -> Color(0xFF181B23)
                                    else -> Color(0xFF15171F)
                                }

                                Box(
                                    modifier = Modifier
                                        .width(36.dp)
                                        .fillMaxHeight()
                                        .background(cellBg)
                                        .border(0.5.dp, StudioBorder.copy(alpha = 0.35f))
                                        .clickable {
                                            activeChannel?.let { ch ->
                                                onNoteToggle(ch.id, pitch, step)
                                                if (existingNote == null) {
                                                    onAuditionNote(ch, pitch)
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
