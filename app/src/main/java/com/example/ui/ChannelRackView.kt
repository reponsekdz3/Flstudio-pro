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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Channel
import com.example.ui.theme.*
import kotlin.math.roundToInt

enum class RackSizeMode(val label: String) {
    COMPACT("MIN"),
    STANDARD("MED"),
    EXPANDED("MAX"),
    AUTO_FIT("FIT")
}

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
    onVolumeChange: (channelId: String, volume: Float) -> Unit,
    onPanChange: (channelId: String, pan: Float) -> Unit,
    onMixerTrackChange: (channelId: String, trackIndex: Int) -> Unit,
    onFillSteps: (channelId: String, interval: Int) -> Unit,
    onClearSteps: (channelId: String) -> Unit,
    onInvertSteps: (channelId: String) -> Unit,
    onCloneChannel: (channelId: String) -> Unit,
    onDeleteChannel: (channelId: String) -> Unit,
    onOpenPianoRoll: (channelId: String) -> Unit,
    onOpenSynth: (channelId: String) -> Unit,
    onOpenInspector: (channelId: String) -> Unit,
    onAddChannelClick: () -> Unit,
    onRecordAudioClick: () -> Unit,
    onSwingChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val horizontalScrollState = rememberScrollState()
    var userSizeMode by remember { mutableStateOf(RackSizeMode.AUTO_FIT) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(StudioDarkBg)
    ) {
        val screenWidth = maxWidth
        val isWide = screenWidth > 600.dp

        val labelWidth: Dp = when {
            screenWidth > 750.dp -> 135.dp
            screenWidth > 480.dp -> 110.dp
            else -> 88.dp
        }

        val stepWidth: Dp = when (userSizeMode) {
            RackSizeMode.COMPACT -> 18.dp
            RackSizeMode.STANDARD -> 25.dp
            RackSizeMode.EXPANDED -> 34.dp
            RackSizeMode.AUTO_FIT -> {
                val overhead = labelWidth + 140.dp
                val availableForSteps = screenWidth - overhead
                val calculated = (availableForSteps - 20.dp) / 16
                calculated.coerceIn(16.dp, 36.dp)
            }
        }

        val stepHeight: Dp = when (userSizeMode) {
            RackSizeMode.COMPACT -> 22.dp
            RackSizeMode.STANDARD -> 28.dp
            RackSizeMode.EXPANDED -> 34.dp
            RackSizeMode.AUTO_FIT -> if (isWide) 30.dp else 26.dp
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Hardware Channel Rack Sub-Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF252A36), Color(0xFF161920))
                        )
                    )
                    .border(1.dp, Color(0xFF333D52))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Title and track count
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HardwareRackScrew(modifier = Modifier.size(9.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.ViewKanban,
                        contentDescription = null,
                        tint = FruityOrange,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (screenWidth < 400.dp) "CHANNEL RACK" else "FL CHANNEL RACK STEP SEQUENCER",
                        fontSize = if (screenWidth < 400.dp) 9.sp else 10.5.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = TextPrimary,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${channels.size} TRK",
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = FruityCyan
                    )
                }

                // Controls (Size Selector + Swing Slider)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Zoom mode selector
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF131720))
                            .border(0.5.dp, StudioBorder, RoundedCornerShape(3.dp))
                            .padding(horizontal = 2.dp, vertical = 1.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        RackSizeMode.values().forEach { mode ->
                            val active = userSizeMode == mode
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(if (active) FruityOrange else Color.Transparent)
                                    .clickable { userSizeMode = mode }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = mode.label,
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (active) Color.Black else TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Swing % Control
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF131720))
                            .border(0.5.dp, StudioBorder, RoundedCornerShape(3.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "SWING",
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = TextMuted,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text(
                            text = "$swing%",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = FruityOrange
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))
                    HardwareRackScrew(modifier = Modifier.size(9.dp))
                }
            }

            // Step numbers ruler row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F1116))
                    .border(0.5.dp, StudioBorder)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Spacer matching channel control headers
                Box(
                    modifier = Modifier.width(labelWidth + 124.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "CHANNELS / MIX / PAN",
                        fontSize = 7.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted
                    )
                }

                // 16 Step Beat Markers
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(horizontalScrollState),
                    horizontalArrangement = Arrangement.spacedBy(2.5.dp)
                ) {
                    for (stepIdx in 0 until 16) {
                        val isBeat = (stepIdx % 4) == 0
                        val isCurrent = isPlaying && currentStep == stepIdx
                        Box(
                            modifier = Modifier
                                .width(stepWidth)
                                .height(14.dp)
                                .background(if (isCurrent) FruityOrange.copy(alpha = 0.3f) else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${(stepIdx / 4) + 1}.${(stepIdx % 4) + 1}",
                                fontSize = 7.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isBeat) FontWeight.Black else FontWeight.Normal,
                                color = if (isCurrent) FruityOrange else if (isBeat) TextPrimary else TextMuted
                            )
                        }
                    }
                }
            }

            // Channels List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                items(channels, key = { it.id }) { channel ->
                    val isSelected = channel.id == selectedChannelId
                    ChannelRowItem(
                        channel = channel,
                        isSelected = isSelected,
                        currentStep = currentStep,
                        isPlaying = isPlaying,
                        labelWidth = labelWidth,
                        stepWidth = stepWidth,
                        stepHeight = stepHeight,
                        horizontalScrollState = horizontalScrollState,
                        onStepToggle = { sIdx ->
                            onStepToggle(channel.id, sIdx)
                            onAudition(channel)
                        },
                        onMute = { onChannelMute(channel.id) },
                        onSolo = { onChannelSolo(channel.id) },
                        onSelect = { onChannelSelect(channel.id) },
                        onAudition = { onAudition(channel) },
                        onVolumeChange = { v -> onVolumeChange(channel.id, v) },
                        onPanChange = { p -> onPanChange(channel.id, p) },
                        onMixerTrackChange = { t -> onMixerTrackChange(channel.id, t) },
                        onFillSteps = { interval -> onFillSteps(channel.id, interval) },
                        onClearSteps = { onClearSteps(channel.id) },
                        onInvertSteps = { onInvertSteps(channel.id) },
                        onCloneChannel = { onCloneChannel(channel.id) },
                        onDeleteChannel = { onDeleteChannel(channel.id) },
                        onOpenPianoRoll = { onOpenPianoRoll(channel.id) },
                        onOpenSynth = { onOpenSynth(channel.id) },
                        onOpenInspector = { onOpenInspector(channel.id) }
                    )
                }
            }

            // Bottom Rack Toolbar (+ Generator, + Record)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF222734), Color(0xFF161922), Color(0xFF0F1116))
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFF454E62), Color(0xFF272D3A), Color(0xFF13161C))
                        ),
                        shape = androidx.compose.ui.graphics.RectangleShape
                    )
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HardwareRackScrew(modifier = Modifier.size(9.dp))

                Button(
                    onClick = onAddChannelClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
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
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "+ ADD GENERATOR / VAULT",
                        fontSize = 9.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }

                OutlinedButton(
                    onClick = onRecordAudioClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
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
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "RECORD SAMPLE / MIC",
                        fontSize = 9.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }

                HardwareRackScrew(modifier = Modifier.size(9.dp))
            }
        }
    }
}

@Composable
fun ChannelRowItem(
    channel: Channel,
    isSelected: Boolean,
    currentStep: Int,
    isPlaying: Boolean,
    labelWidth: Dp,
    stepWidth: Dp,
    stepHeight: Dp,
    horizontalScrollState: androidx.compose.foundation.ScrollState,
    onStepToggle: (Int) -> Unit,
    onMute: () -> Unit,
    onSolo: () -> Unit,
    onSelect: () -> Unit,
    onAudition: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onPanChange: (Float) -> Unit,
    onMixerTrackChange: (Int) -> Unit,
    onFillSteps: (Int) -> Unit,
    onClearSteps: () -> Unit,
    onInvertSteps: () -> Unit,
    onCloneChannel: () -> Unit,
    onDeleteChannel: () -> Unit,
    onOpenPianoRoll: () -> Unit,
    onOpenSynth: () -> Unit,
    onOpenInspector: () -> Unit
) {
    var showContextMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) StudioPanelLight else StudioPanel)
            .border(
                1.dp,
                if (isSelected) FruityOrange.copy(alpha = 0.7f) else StudioBorder
            )
            .padding(horizontal = 3.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // FL Studio Channel Selector LED (Green when selected)
        Box(
            modifier = Modifier
                .width(5.dp)
                .height(stepHeight.coerceAtLeast(20.dp))
                .clip(RoundedCornerShape(1.dp))
                .background(if (isSelected) FruityLime else Color(0xFF222834))
                .border(0.5.dp, if (isSelected) FruityLime else Color(0xFF141820), RoundedCornerShape(1.dp))
                .clickable { onSelect() }
        )

        Spacer(modifier = Modifier.width(3.dp))

        // Mute / Solo Buttons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Mute Button (Green LED indicator)
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(if (!channel.isMuted) FruityLime else Color(0xFF333A48))
                    .clickable { onMute() },
                contentAlignment = Alignment.Center
            ) {
                if (channel.isMuted) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(Color.Gray)
                    )
                }
            }

            // Solo Button (Amber 'S')
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (channel.isSolo) FruityAmber else StudioPanelLight)
                    .border(0.5.dp, StudioBorder, RoundedCornerShape(3.dp))
                    .clickable { onSolo() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "S",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (channel.isSolo) Color.Black else TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.width(3.dp))

        // Pan Knob (Draggable Micro-Knob)
        ChannelMicroKnob(
            label = "PAN",
            value = channel.pan,
            displayValue = when {
                channel.pan < -0.05f -> "L${(-channel.pan * 100).toInt()}"
                channel.pan > 0.05f -> "R${(channel.pan * 100).toInt()}"
                else -> "C"
            },
            accentColor = FruityCyan,
            onValueChange = onPanChange,
            minValue = -1.0f,
            maxValue = 1.0f
        )

        Spacer(modifier = Modifier.width(3.dp))

        // Vol Knob (Draggable Micro-Knob)
        ChannelMicroKnob(
            label = "VOL",
            value = channel.volume,
            displayValue = "${(channel.volume * 100).toInt()}%",
            accentColor = FruityOrange,
            onValueChange = onVolumeChange,
            minValue = 0.0f,
            maxValue = 1.5f
        )

        Spacer(modifier = Modifier.width(3.dp))

        // Mixer Track Selector LCD: e.g. "FX: --" or "FX: 01"
        Box(
            modifier = Modifier
                .width(32.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFF10141C))
                .border(0.5.dp, Color(0xFF333E54), RoundedCornerShape(3.dp))
                .clickable {
                    val nextTrack = if (channel.mixerTrackIndex >= 28) 0 else channel.mixerTrackIndex + 1
                    onMixerTrackChange(nextTrack)
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (channel.mixerTrackIndex == 0) "--" else "${channel.mixerTrackIndex}",
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = if (channel.mixerTrackIndex == 0) TextMuted else FruityOrange
            )
        }

        Spacer(modifier = Modifier.width(3.dp))

        // Channel Name Plate (Click auditions sound, double-tap opens settings)
        Row(
            modifier = Modifier
                .width(labelWidth)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(channel.colorHex).copy(alpha = 0.35f),
                            Color(0xFF1B202B)
                        )
                    )
                )
                .border(0.8.dp, Color(channel.colorHex).copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                .clickable {
                    onSelect()
                    onAudition()
                }
                .padding(horizontal = 4.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color(channel.colorHex))
            )
            Spacer(modifier = Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel.name,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = channel.type.category.uppercase(),
                    fontSize = 6.5.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace,
                    color = Color(channel.colorHex).copy(alpha = 0.85f),
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.width(2.dp))

        // Channel Context Menu Button (3 dots)
        Box {
            IconButton(
                onClick = { showContextMenu = true },
                modifier = Modifier.size(18.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = TextSecondary,
                    modifier = Modifier.size(12.dp)
                )
            }

            DropdownMenu(
                expanded = showContextMenu,
                onDismissRequest = { showContextMenu = false },
                modifier = Modifier.background(StudioPanel)
            ) {
                DropdownMenuItem(
                    text = { Text("Fill each 2 steps (8th notes)", fontSize = 11.sp, color = TextPrimary) },
                    onClick = {
                        onFillSteps(2)
                        showContextMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Fill each 4 steps (4-on-the-floor)", fontSize = 11.sp, color = FruityOrange, fontWeight = FontWeight.Bold) },
                    onClick = {
                        onFillSteps(4)
                        showContextMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Fill each 8 steps", fontSize = 11.sp, color = TextPrimary) },
                    onClick = {
                        onFillSteps(8)
                        showContextMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Invert steps", fontSize = 11.sp, color = TextPrimary) },
                    onClick = {
                        onInvertSteps()
                        showContextMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Clear all steps", fontSize = 11.sp, color = TextSecondary) },
                    onClick = {
                        onClearSteps()
                        showContextMenu = false
                    }
                )
                HorizontalDivider(color = StudioBorder)
                DropdownMenuItem(
                    text = { Text("Open Piano Roll", fontSize = 11.sp, color = FruityCyan) },
                    onClick = {
                        onSelect()
                        onOpenPianoRoll()
                        showContextMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Open 3xOSC Synth Editor", fontSize = 11.sp, color = FruityOrange) },
                    onClick = {
                        onSelect()
                        onOpenSynth()
                        showContextMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Open Channel Settings / Inspector", fontSize = 11.sp, color = TextPrimary) },
                    onClick = {
                        onSelect()
                        onOpenInspector()
                        showContextMenu = false
                    }
                )
                HorizontalDivider(color = StudioBorder)
                DropdownMenuItem(
                    text = { Text("Clone Channel", fontSize = 11.sp, color = TextPrimary) },
                    onClick = {
                        onCloneChannel()
                        showContextMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete Channel", fontSize = 11.sp, color = Color(0xFFEF4444)) },
                    onClick = {
                        onDeleteChannel()
                        showContextMenu = false
                    }
                )
            }
        }

        Spacer(modifier = Modifier.width(3.dp))

        // 16 Step Buttons with Responsive Size
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(horizontalScrollState),
            horizontalArrangement = Arrangement.spacedBy(2.5.dp)
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
                        .width(stepWidth)
                        .height(stepHeight)
                        .clip(RoundedCornerShape(2.5.dp))
                        .background(buttonColor)
                        .border(
                            width = if (isCurrentStepPlaying) 1.2.dp else 0.5.dp,
                            color = if (isCurrentStepPlaying) Color.White else StudioBorder,
                            shape = RoundedCornerShape(2.5.dp)
                        )
                        .clickable { onStepToggle(stepIdx) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isStepActive) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(if (isCurrentStepPlaying) FruityOrange else Color.White)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChannelMicroKnob(
    label: String,
    value: Float,
    displayValue: String,
    accentColor: Color,
    onValueChange: (Float) -> Unit,
    minValue: Float,
    maxValue: Float
) {
    var isDragging by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .width(22.dp)
            .height(20.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0xFF131720))
            .border(0.5.dp, if (isDragging) accentColor else StudioBorder, RoundedCornerShape(3.dp))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val delta = -dragAmount.y * 0.015f + dragAmount.x * 0.015f
                        val updated = (value + delta).coerceIn(minValue, maxValue)
                        onValueChange(updated)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = displayValue,
                fontSize = 7.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = accentColor,
                maxLines = 1
            )
        }
    }
}
