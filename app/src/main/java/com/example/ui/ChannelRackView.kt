package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Channel
import com.example.ui.theme.*

@Composable
fun ChannelRackView(
    channels: List<Channel>,
    selectedChannelId: String,
    currentStep: Int,
    isPlaying: Boolean,
    swing: Int,
    onStepToggle: (channelId: String, stepIndex: Int) -> Unit,
    onChannelMute: (channelId: String) -> Unit,
    onChannelSolo: (channelId: String) -> Unit,
    onChannelSelect: (channelId: String) -> Unit,
    onAudition: (channel: Channel) -> Unit,
    onOpenPianoRoll: (channelId: String) -> Unit,
    onOpenSynth: (channelId: String) -> Unit,
    onOpenInspector: (channelId: String) -> Unit,
    onAddChannelClick: () -> Unit,
    onRecordAudioClick: () -> Unit,
    onSwingChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val horizontalScrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioDarkBg)
    ) {
        // Channel Rack Sub-Header with Hardware Styling
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
                Icon(
                    imageVector = Icons.Default.ViewKanban,
                    contentDescription = null,
                    tint = FruityOrange,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "CHANNEL RACK STEP SEQUENCER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    color = TextPrimary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${channels.size} TRACKS",
                    fontSize = 9.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = FruityCyan
                )
            }

            // Swing control
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "SWING",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = FruityAmber,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Slider(
                    value = swing.toFloat(),
                    onValueChange = { onSwingChange(it.toInt()) },
                    valueRange = 0f..100f,
                    modifier = Modifier
                        .width(75.dp)
                        .height(20.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = FruityAmber,
                        activeTrackColor = FruityAmber,
                        inactiveTrackColor = StudioPanelLight
                    )
                )
                Text(
                    text = "$swing%",
                    fontSize = 10.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }

        // Channels List with Step Sequencer Grid
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(channels, key = { it.id }) { channel ->
                    ChannelRowItem(
                        channel = channel,
                        isSelected = channel.id == selectedChannelId,
                        currentStep = currentStep,
                        isPlaying = isPlaying,
                        horizontalScrollState = horizontalScrollState,
                        onStepToggle = { stepIdx -> onStepToggle(channel.id, stepIdx) },
                        onMute = { onChannelMute(channel.id) },
                        onSolo = { onChannelSolo(channel.id) },
                        onSelect = {
                            onChannelSelect(channel.id)
                            onAudition(channel)
                        },
                        onOpenPianoRoll = { onOpenPianoRoll(channel.id) },
                        onOpenSynth = { onOpenSynth(channel.id) },
                        onOpenInspector = { onOpenInspector(channel.id) }
                    )
                }
            }
        }

        // Hardware Rack Bottom Bay (+ Channel, + Mic Record)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(Color(0xFF222734), Color(0xFF161922), Color(0xFF0F1116))
                    )
                )
                .border(
                    width = 1.2.dp,
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(Color(0xFF454E62), Color(0xFF272D3A), Color(0xFF13161C))
                    ),
                    shape = androidx.compose.ui.graphics.RectangleShape
                )
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HardwareRackScrew(modifier = Modifier.size(9.dp))

            Button(
                onClick = onAddChannelClick,
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .testTag("add_channel_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FruityOrange,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "VAULT / ADD GENERATOR",
                    fontSize = 10.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
            }

            OutlinedButton(
                onClick = onRecordAudioClick,
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .testTag("record_audio_button"),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(FruityPink)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = FruityPink
                ),
                shape = RoundedCornerShape(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "RECORD SAMPLE / MIC",
                    fontSize = 10.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
            }

            HardwareRackScrew(modifier = Modifier.size(9.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChannelRowItem(
    channel: Channel,
    isSelected: Boolean,
    currentStep: Int,
    isPlaying: Boolean,
    horizontalScrollState: androidx.compose.foundation.ScrollState,
    onStepToggle: (Int) -> Unit,
    onMute: () -> Unit,
    onSolo: () -> Unit,
    onSelect: () -> Unit,
    onOpenPianoRoll: () -> Unit,
    onOpenSynth: () -> Unit,
    onOpenInspector: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) StudioPanelLight else StudioPanel)
            .border(
                1.dp,
                if (isSelected) FruityOrange.copy(alpha = 0.6f) else StudioBorder
            )
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Mute / Solo Buttons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Mute Button (Green indicator)
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(if (!channel.isMuted) FruityLime else Color(0xFF333A48))
                    .clickable { onMute() },
                contentAlignment = Alignment.Center
            ) {
                if (channel.isMuted) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color.Gray)
                    )
                }
            }

            // Solo Button
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (channel.isSolo) FruityAmber else StudioPanelLight)
                    .border(0.5.dp, StudioBorder, RoundedCornerShape(3.dp))
                    .clickable { onSolo() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "S",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (channel.isSolo) Color.Black else TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Channel Name & Click to Open Modern Powerful Hardware Strip / Mix Inspector
        Row(
            modifier = Modifier
                .width(115.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                        listOf(
                            Color(channel.colorHex).copy(alpha = 0.25f),
                            Color(0xFF1E2330)
                        )
                    )
                )
                .border(1.dp, Color(channel.colorHex).copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                .clickable {
                    onSelect()
                    onOpenInspector()
                }
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(Color(channel.colorHex))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = channel.name,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(3.dp))

        // Hardware Channel Strip & Mix Console Button
        IconButton(
            onClick = onOpenInspector,
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFF222836))
                .border(0.8.dp, FruityOrange.copy(alpha = 0.6f), RoundedCornerShape(3.dp))
        ) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = "Open Hardware Mix & Channel Strip",
                tint = FruityOrange,
                modifier = Modifier.size(15.dp)
            )
        }

        Spacer(modifier = Modifier.width(3.dp))

        // Quick Piano Roll Jump for EVERY sound selected
        IconButton(
            onClick = onOpenPianoRoll,
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFF1C2230))
                .border(0.8.dp, if (channel.type.isMelodic) FruityCyan.copy(alpha = 0.6f) else Color(0xFF384358), RoundedCornerShape(3.dp))
        ) {
            Icon(
                imageVector = Icons.Default.Piano,
                contentDescription = "Open Piano Roll for this sound",
                tint = if (channel.type.isMelodic) FruityCyan else FruityOrange,
                modifier = Modifier.size(15.dp)
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // 16 Step Buttons (Classic FL Studio 4-beat color blocks)
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(horizontalScrollState),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            for (stepIdx in 0 until 16) {
                val isBeatGroupA = (stepIdx / 4) % 2 == 0
                val isStepActive = stepIdx < channel.steps.size && channel.steps[stepIdx]
                val isCurrentStepPlaying = isPlaying && currentStep == stepIdx

                val defaultBlockColor = if (isBeatGroupA) StepBeatGroupA else StepBeatGroupB
                val buttonColor by animateColorAsState(
                    targetValue = when {
                        isCurrentStepPlaying && isStepActive -> Color.White
                        isStepActive -> if (channel.type.isMelodic) FruityCyan else FruityOrange
                        isCurrentStepPlaying -> Color(0xFF6B7280)
                        else -> defaultBlockColor
                    },
                    label = "stepColor"
                )

                Box(
                    modifier = Modifier
                        .width(22.dp)
                        .height(26.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(buttonColor)
                        .border(
                            width = if (isCurrentStepPlaying) 1.5.dp else 0.5.dp,
                            color = if (isCurrentStepPlaying) Color.White else StudioBorder,
                            shape = RoundedCornerShape(3.dp)
                        )
                        .clickable { onStepToggle(stepIdx) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isStepActive) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(if (isCurrentStepPlaying) FruityOrange else Color.White)
                        )
                    }
                }
            }
        }
    }
}
