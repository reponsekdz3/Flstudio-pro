package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.NoteEvent
import com.example.ui.theme.*
import com.example.viewmodel.FlStudioUiState
import com.example.viewmodel.FlStudioViewModel
import com.example.viewmodel.StudioTab

@Composable
fun MainScreen(
    viewModel: FlStudioViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.showRecordDialog(true)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        bottomBar = {
            StudioBottomNavigation(
                selectedTab = state.selectedTab,
                onSelectTab = { viewModel.selectTab(it) },
                modifier = Modifier.navigationBarsPadding()
            )
        },
        containerColor = StudioDarkBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Top Master FL Studio Transport
            TopBarTransport(
                isPlaying = state.isPlaying,
                playMode = state.playMode,
                bpm = state.bpm,
                currentBar = state.currentBar,
                currentStep = state.currentStep,
                masterPeakL = state.masterPeakL,
                masterPeakR = state.masterPeakR,
                projectPresetName = state.projectPresetName,
                patterns = state.patterns,
                selectedPatternId = state.selectedPatternId,
                onTogglePlay = { viewModel.togglePlay() },
                onStop = { viewModel.stopPlayback() },
                onRecordClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        viewModel.showRecordDialog(true)
                    } else {
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onModeChange = { viewModel.setPlayMode(it) },
                onBpmChange = { viewModel.setBpm(it) },
                onTapTempo = { viewModel.tapTempo() },
                onPresetSelect = { preset ->
                    when (preset) {
                        "Amapiano Fever" -> viewModel.loadPresetAmapiano()
                        "Alan Walker EDM" -> viewModel.loadPresetAlanWalkerEdm()
                        "Drake Moody Trap" -> viewModel.loadPresetDrakeMoodyTrap()
                        "Katy Perry Pop" -> viewModel.loadPresetKatyPerryPop()
                        "Guitar Studio Legends" -> viewModel.loadPresetGuitarRock()
                        "Trap 808 Heat" -> viewModel.loadPresetTrap()
                        "Cyber Electro" -> viewModel.loadPresetCyberElectro()
                        "Lo-Fi Chill" -> viewModel.loadPresetLoFi()
                    }
                },
                onSelectPattern = { viewModel.selectPattern(it) },
                onAddPattern = { viewModel.addPattern() },
                selectedTab = state.selectedTab,
                onSelectTab = { viewModel.selectTab(it) },
                onSaveProject = {
                    viewModel.openExportHub()
                },
                onExportAudio = {
                    viewModel.openExportHub()
                }
            )

            // Active Tab Screen Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (state.selectedTab) {
                    StudioTab.CHANNEL_RACK -> {
                        ChannelRackView(
                            channels = state.channels,
                            selectedChannelId = state.selectedChannelId,
                            currentStep = state.currentStep,
                            isPlaying = state.isPlaying,
                            swing = state.swing,
                            onStepToggle = { chId, stepIdx -> viewModel.toggleStep(chId, stepIdx) },
                            onChannelMute = { chId -> viewModel.toggleChannelMute(chId) },
                            onChannelSolo = { chId -> viewModel.toggleChannelSolo(chId) },
                            onChannelSelect = { chId -> viewModel.selectChannel(chId) },
                            onAudition = { channel -> viewModel.triggerLiveAudition(channel) },
                            onVolumeChange = { chId, vol -> viewModel.updateChannelVolume(chId, vol) },
                            onPanChange = { chId, pan -> viewModel.updateChannelPan(chId, pan) },
                            onMixerTrackChange = { chId, trk -> viewModel.updateChannelMixerTrack(chId, trk) },
                            onFillSteps = { chId, interval -> viewModel.fillChannelSteps(chId, interval) },
                            onClearSteps = { chId -> viewModel.clearChannelSteps(chId) },
                            onInvertSteps = { chId -> viewModel.invertChannelSteps(chId) },
                            onCloneChannel = { chId -> viewModel.cloneChannel(chId) },
                            onDeleteChannel = { chId -> viewModel.deleteChannel(chId) },
                            onOpenPianoRoll = { chId ->
                                viewModel.selectChannel(chId)
                                viewModel.selectTab(StudioTab.PIANO_ROLL)
                            },
                            onOpenSynth = { chId ->
                                viewModel.selectChannel(chId)
                                viewModel.selectTab(StudioTab.SYNTH)
                            },
                            onOpenInspector = { chId ->
                                viewModel.openTrackInspector(chId)
                            },
                            onAddChannelClick = { viewModel.showAddChannel(true) },
                            onRecordAudioClick = {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                    viewModel.showRecordDialog(true)
                                } else {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            onSwingChange = { viewModel.setSwing(it) }
                        )
                    }

                    StudioTab.PIANO_ROLL -> {
                        PianoRollView(
                            channels = state.channels,
                            selectedChannelId = state.selectedChannelId,
                            currentStep = state.currentStep,
                            isPlaying = state.isPlaying,
                            bpm = state.bpm,
                            playMode = state.playMode,
                            onTogglePlay = { viewModel.togglePlay() },
                            onBpmChange = { viewModel.setBpm(it) },
                            onChannelSelect = { viewModel.selectChannel(it) },
                            onNoteToggle = { chId, pitch, step -> viewModel.addOrRemovePianoNote(chId, pitch, step) },
                            onAuditionNote = { ch, pitch ->
                                viewModel.triggerLiveAudition(ch, NoteEvent(pitch = pitch, durationSteps = 2))
                            },
                            onQuantizeNotes = { chId, gridDivision ->
                                viewModel.quantizePianoRollNotes(chId, gridDivision)
                            },
                            onStampChord = { chId, chordType, rootPitch, step ->
                                viewModel.stampChord(chId, chordType, rootPitch, step)
                            },
                            onClearNotes = { chId ->
                                viewModel.clearChannelNotes(chId)
                            }
                        )
                    }

                    StudioTab.PLAYLIST -> {
                        PlaylistArrangerView(
                            clips = state.playlistClips,
                            currentBar = state.currentBar,
                            isPlaying = state.isPlaying,
                            playMode = state.playMode,
                            totalBars = state.totalBars,
                            onToggleClip = { track, bar -> viewModel.togglePlaylistClip(track, bar) },
                            onSwitchToSongMode = { viewModel.setPlayMode(com.example.model.PlayMode.SONG) }
                        )
                    }

                    StudioTab.MIXER -> {
                        MixerView(
                            mixerTracks = state.mixerTracks,
                            selectedTrackId = state.selectedMixerTrackId,
                            masterPeakL = state.masterPeakL,
                            masterPeakR = state.masterPeakR,
                            trackPeaks = state.trackPeaks,
                            onSelectTrack = { viewModel.selectMixerTrack(it) },
                            onVolumeChange = { id, vol -> viewModel.updateMixerVolume(id, vol) },
                            onPanChange = { id, pan -> viewModel.updateMixerPan(id, pan) },
                            onToggleMute = { id -> viewModel.toggleMixerMute(id) },
                            onEqChange = { id, en, low, mid, high -> viewModel.updateMixerEq(id, en, low, mid, high) },
                            onDelayChange = { id, en, time, fb, wet -> viewModel.updateMixerDelay(id, en, time, fb, wet) },
                            onReverbChange = { id, en, room, wet -> viewModel.updateMixerReverb(id, en, room, wet) },
                            onDistChange = { id, en, drive, mix -> viewModel.updateMixerDist(id, en, drive, mix) },
                            onChorusChange = { id, en, rate, depth, mix -> viewModel.updateMixerChorus(id, en, rate, depth, mix) },
                            onCompChange = { id, en, thresh, ratio -> viewModel.updateMixerComp(id, en, thresh, ratio) },
                            onAuxSendChange = { id, auxId, lvl -> viewModel.updateTrackAuxSend(id, auxId, lvl) }
                        )
                    }

                    StudioTab.SYNTH -> {
                        PluginSynthView(
                            channels = state.channels,
                            selectedChannelId = state.selectedChannelId,
                            onSelectChannel = { viewModel.selectChannel(it) },
                            onUpdateParams = { chId, o1, o2, mix, det, ft, cut, res, a, d, s, r ->
                                viewModel.updateSynthParams(chId, o1, o2, mix, det, ft, cut, res, a, d, s, r)
                            },
                            onPlayNote = { ch, pitch ->
                                viewModel.triggerLiveAudition(ch, NoteEvent(pitch = pitch, durationSteps = 2))
                            }
                        )
                    }
                }
            }
        }

        // Modals
        if (state.showRecordDialog) {
            AudioRecordDialog(
                isRecording = state.isMicRecording,
                liveAmplitude = state.micAmplitude,
                onStartRecording = { callback ->
                    viewModel.startMicRecording(callback)
                },
                onStopAndSave = { sampleName ->
                    viewModel.stopMicRecordingAndAddSample(sampleName)
                },
                onDismiss = { viewModel.showRecordDialog(false) }
            )
        }

        if (state.showAddChannelDialog) {
            AddChannelDialog(
                onAddChannel = { type, name ->
                    viewModel.addChannel(type, name)
                },
                onDismiss = { viewModel.showAddChannel(false) }
            )
        }

        if (state.showTrackInspectorDialog && state.inspectingChannelId != null) {
            val inspectingCh = state.channels.find { it.id == state.inspectingChannelId }
            if (inspectingCh != null) {
                HardwareChannelStripDialog(
                    channel = inspectingCh,
                    mixerTracks = state.mixerTracks,
                    onUpdateChannel = { name, type, mixerTrackIndex, volume, pan, pitchSemi, attackMs, decayMs, sustainLevel, releaseMs, filterType, cutoffHz, resonanceQ, osc1Type, osc2Type, oscMix ->
                        viewModel.updateChannelConfig(
                            channelId = inspectingCh.id,
                            name = name,
                            type = type,
                            mixerTrackIndex = mixerTrackIndex,
                            volume = volume,
                            pan = pan,
                            pitchSemi = pitchSemi,
                            attackMs = attackMs,
                            decayMs = decayMs,
                            sustainLevel = sustainLevel,
                            releaseMs = releaseMs,
                            filterType = filterType,
                            cutoffHz = cutoffHz,
                            resonanceQ = resonanceQ,
                            osc1Type = osc1Type,
                            osc2Type = osc2Type,
                            osc2Mix = oscMix
                        )
                    },
                    onAudition = { viewModel.triggerLiveAudition(inspectingCh) },
                    onDeleteChannel = {
                        viewModel.removeChannel(inspectingCh.id)
                        viewModel.closeTrackInspector()
                    },
                    onDismiss = { viewModel.closeTrackInspector() },
                    onOpenPianoRoll = {
                        viewModel.selectChannel(inspectingCh.id)
                        viewModel.closeTrackInspector()
                        viewModel.selectTab(StudioTab.PIANO_ROLL)
                    },
                    onOpenSynth = {
                        viewModel.selectChannel(inspectingCh.id)
                        viewModel.closeTrackInspector()
                        viewModel.selectTab(StudioTab.SYNTH)
                    },
                    onOpenMixer = {
                        viewModel.closeTrackInspector()
                        viewModel.selectTab(StudioTab.MIXER)
                    }
                )
            }
        }

        if (state.showExportHubDialog) {
            ProjectAndAudioExportDialog(
                viewModel = viewModel,
                onDismiss = { viewModel.closeExportHub() }
            )
        }
    }
}

@Composable
fun StudioBottomNavigation(
    selectedTab: StudioTab,
    onSelectTab: (StudioTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF222630), Color(0xFF161820), Color(0xFF0F1116))
                )
            )
            .border(
                width = 1.2.dp,
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF454E62), Color(0xFF272D3A), Color(0xFF13161C))
                ),
                shape = androidx.compose.ui.graphics.RectangleShape
            )
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val tabs = listOf(
            Triple(StudioTab.CHANNEL_RACK, Icons.Default.ViewKanban, "RACK"),
            Triple(StudioTab.PIANO_ROLL, Icons.Default.Piano, "PIANO ROLL"),
            Triple(StudioTab.PLAYLIST, Icons.Default.ViewTimeline, "PLAYLIST"),
            Triple(StudioTab.MIXER, Icons.Default.Tune, "MIXER"),
            Triple(StudioTab.SYNTH, Icons.Default.GraphicEq, "3xOSC SYNTH")
        )

        tabs.forEach { (tab, icon, label) ->
            val isSelected = selectedTab == tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .padding(horizontal = 2.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(
                        if (isSelected) {
                            Brush.verticalGradient(
                                listOf(Color(0xFF382314), Color(0xFF24150A))
                            )
                        } else {
                            Brush.verticalGradient(
                                listOf(Color(0xFF1E222C), Color(0xFF13161C))
                            )
                        }
                    )
                    .border(
                        width = if (isSelected) 1.2.dp else 0.8.dp,
                        color = if (isSelected) FruityOrange else Color(0xFF2E3648),
                        shape = RoundedCornerShape(5.dp)
                    )
                    .clickable { onSelectTab(tab) },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Hardware LED indicator pip
                    Box(
                        modifier = Modifier
                            .width(16.dp)
                            .height(2.5.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(if (isSelected) FruityOrange else Color(0xFF283040))
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isSelected) FruityOrange else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = label,
                        fontSize = 7.5.sp,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp,
                        color = if (isSelected) Color.White else TextSecondary,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
