package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioEngine
import com.example.audio.AudioRecorder
import com.example.model.*
import com.example.project.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class StudioTab(val label: String) {
    CHANNEL_RACK("Channel Rack"),
    PIANO_ROLL("Piano Roll"),
    PLAYLIST("Playlist"),
    MIXER("Mixer"),
    SYNTH("3xOSC Synth")
}

data class FlStudioUiState(
    val selectedTab: StudioTab = StudioTab.CHANNEL_RACK,
    val isPlaying: Boolean = false,
    val isRecording: Boolean = false,
    val playMode: PlayMode = PlayMode.PATTERN,
    val bpm: Int = 130,
    val swing: Int = 15,
    val metronomeEnabled: Boolean = false,
    val currentStep: Int = 0,
    val currentBar: Int = 0,
    val totalBars: Int = 16,
    val channels: List<Channel> = emptyList(),
    val selectedChannelId: String = "",
    val patterns: List<Pattern> = emptyList(),
    val selectedPatternId: Int = 1,
    val mixerTracks: List<MixerTrack> = emptyList(),
    val selectedMixerTrackId: Int = 0,
    val playlistClips: List<PatternClip> = emptyList(),
    val masterPeakL: Float = 0f,
    val masterPeakR: Float = 0f,
    val trackPeaks: FloatArray = FloatArray(AudioEngine.TOTAL_MIXER_TRACKS),
    val isMicRecording: Boolean = false,
    val micAmplitude: Float = 0f,
    val showRecordDialog: Boolean = false,
    val showAddChannelDialog: Boolean = false,
    val inspectingChannelId: String? = null,
    val showTrackInspectorDialog: Boolean = false,
    val showExportHubDialog: Boolean = false,
    val projectPresetName: String = "Guitar Studio Legends"
)

class FlStudioViewModel : ViewModel() {

    val audioEngine = AudioEngine()
    val audioRecorder = AudioRecorder()

    private val _uiState = MutableStateFlow(FlStudioUiState())
    val uiState = _uiState.asStateFlow()

    private var tapTimes = mutableListOf<Long>()

    init {
        loadPresetGuitarRock()
        audioEngine.start()

        viewModelScope.launch {
            audioEngine.currentStep.collect { step ->
                _uiState.update { it.copy(currentStep = step) }
            }
        }

        viewModelScope.launch {
            audioEngine.currentBar.collect { bar ->
                _uiState.update { it.copy(currentBar = bar) }
            }
        }

        viewModelScope.launch {
            audioEngine.masterPeakL.collect { peakL ->
                _uiState.update { it.copy(masterPeakL = peakL) }
            }
        }

        viewModelScope.launch {
            audioEngine.masterPeakR.collect { peakR ->
                _uiState.update { it.copy(masterPeakR = peakR) }
            }
        }

        viewModelScope.launch {
            audioEngine.trackPeaks.collect { peaks ->
                _uiState.update { it.copy(trackPeaks = peaks) }
            }
        }

        viewModelScope.launch {
            audioRecorder.liveAmplitude.collect { amp ->
                _uiState.update { it.copy(micAmplitude = amp) }
            }
        }

        viewModelScope.launch {
            audioRecorder.isRecording.collect { rec ->
                _uiState.update { it.copy(isMicRecording = rec) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.stopEngine()
    }

    fun openExportHub() {
        _uiState.update { it.copy(showExportHubDialog = true) }
    }

    fun closeExportHub() {
        _uiState.update { it.copy(showExportHubDialog = false) }
    }

    fun loadCustomProject(loaded: LoadedProjectData) {
        _uiState.update {
            it.copy(
                bpm = loaded.bpm,
                swing = loaded.swing,
                playMode = loaded.playMode,
                totalBars = loaded.totalBars,
                channels = loaded.channels,
                selectedChannelId = loaded.channels.firstOrNull()?.id ?: "",
                patterns = loaded.patterns,
                selectedPatternId = loaded.selectedPatternId,
                mixerTracks = loaded.mixerTracks,
                playlistClips = loaded.playlistClips,
                projectPresetName = loaded.projectName
            )
        }
        syncEngineState()
    }

    fun updateChannelMixerTrack(channelId: String, trackIndex: Int) {
        val clamped = trackIndex.coerceIn(0, 128)
        _uiState.update { state ->
            val updated = state.channels.map { ch ->
                if (ch.id == channelId) ch.copy(mixerTrackIndex = clamped) else ch
            }
            state.copy(channels = updated)
        }
        syncEngineState()
    }

    fun fillChannelSteps(channelId: String, interval: Int) {
        _uiState.update { state ->
            val updated = state.channels.map { ch ->
                if (ch.id == channelId) {
                    val newSteps = BooleanArray(16) { idx -> idx % interval == 0 }
                    ch.copy(steps = newSteps)
                } else ch
            }
            val currentPat = state.patterns.find { it.id == state.selectedPatternId }
            val updatedPatterns = if (currentPat != null) {
                val curSteps = updated.find { it.id == channelId }?.steps ?: BooleanArray(16)
                val newMap = currentPat.channelSteps.toMutableMap()
                newMap[channelId] = curSteps.clone()
                state.patterns.map { if (it.id == currentPat.id) it.copy(channelSteps = newMap) else it }
            } else state.patterns
            state.copy(channels = updated, patterns = updatedPatterns)
        }
        syncEngineState()
    }

    fun clearChannelSteps(channelId: String) {
        _uiState.update { state ->
            val updated = state.channels.map { ch ->
                if (ch.id == channelId) ch.copy(steps = BooleanArray(16) { false }) else ch
            }
            val currentPat = state.patterns.find { it.id == state.selectedPatternId }
            val updatedPatterns = if (currentPat != null) {
                val newMap = currentPat.channelSteps.toMutableMap()
                newMap[channelId] = BooleanArray(16) { false }
                state.patterns.map { if (it.id == currentPat.id) it.copy(channelSteps = newMap) else it }
            } else state.patterns
            state.copy(channels = updated, patterns = updatedPatterns)
        }
        syncEngineState()
    }

    fun invertChannelSteps(channelId: String) {
        _uiState.update { state ->
            val updated = state.channels.map { ch ->
                if (ch.id == channelId) {
                    val inv = BooleanArray(16) { idx -> !ch.steps[idx] }
                    ch.copy(steps = inv)
                } else ch
            }
            val currentPat = state.patterns.find { it.id == state.selectedPatternId }
            val updatedPatterns = if (currentPat != null) {
                val curSteps = updated.find { it.id == channelId }?.steps ?: BooleanArray(16)
                val newMap = currentPat.channelSteps.toMutableMap()
                newMap[channelId] = curSteps.clone()
                state.patterns.map { if (it.id == currentPat.id) it.copy(channelSteps = newMap) else it }
            } else state.patterns
            state.copy(channels = updated, patterns = updatedPatterns)
        }
        syncEngineState()
    }

    fun cloneChannel(channelId: String) {
        _uiState.update { state ->
            val target = state.channels.find { it.id == channelId } ?: return@update state
            val cloned = target.copy(
                id = java.util.UUID.randomUUID().toString(),
                name = "${target.name} (Clone)",
                steps = target.steps.clone(),
                notes = target.notes.toList()
            )
            state.copy(channels = state.channels + cloned)
        }
        syncEngineState()
    }

    fun deleteChannel(channelId: String) {
        _uiState.update { state ->
            if (state.channels.size <= 1) return@update state
            val remaining = state.channels.filter { it.id != channelId }
            val newSelected = if (state.selectedChannelId == channelId) remaining.first().id else state.selectedChannelId
            state.copy(channels = remaining, selectedChannelId = newSelected)
        }
        syncEngineState()
    }

    fun quantizePianoRollNotes(channelId: String, gridDivision: Int) {
        val division = gridDivision.coerceAtLeast(1)
        _uiState.update { state ->
            val updated = state.channels.map { ch ->
                if (ch.id == channelId) {
                    val quantized = ch.notes.map { note ->
                        val snapped = ((note.startStep + division / 2) / division) * division
                        note.copy(startStep = snapped.coerceIn(0, 15))
                    }
                    ch.copy(notes = quantized)
                } else ch
            }
            state.copy(channels = updated)
        }
        syncEngineState()
    }

    fun stampChord(channelId: String, chordType: String, rootPitch: Int, step: Int) {
        val intervals = when (chordType.uppercase()) {
            "MIN", "MINOR" -> listOf(0, 3, 7)
            "MAJ7", "MAJOR 7TH" -> listOf(0, 4, 7, 11)
            "MIN7", "MINOR 7TH" -> listOf(0, 3, 7, 10)
            "DOM7", "7TH" -> listOf(0, 4, 7, 10)
            "SUS4" -> listOf(0, 5, 7)
            "OCTAVE" -> listOf(0, 12)
            else -> listOf(0, 4, 7) // Major Triad
        }

        _uiState.update { state ->
            val updated = state.channels.map { ch ->
                if (ch.id == channelId) {
                    val newNotes = ch.notes.toMutableList()
                    for (offset in intervals) {
                        val pitch = rootPitch + offset
                        newNotes.removeAll { it.pitch == pitch && it.startStep == step }
                        newNotes.add(NoteEvent(pitch = pitch, startStep = step, durationSteps = 2, velocity = 0.85f))
                    }
                    ch.copy(notes = newNotes)
                } else ch
            }
            state.copy(channels = updated)
        }
        syncEngineState()
    }

    fun clearChannelNotes(channelId: String) {
        _uiState.update { state ->
            val updated = state.channels.map { ch ->
                if (ch.id == channelId) ch.copy(notes = emptyList()) else ch
            }
            state.copy(channels = updated)
        }
        syncEngineState()
    }

    fun selectTab(tab: StudioTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun togglePlay() {
        val newState = !_uiState.value.isPlaying
        _uiState.update { it.copy(isPlaying = newState) }
        if (newState) {
            syncEngineState()
            audioEngine.play()
        } else {
            audioEngine.pause()
        }
    }

    fun stopPlayback() {
        _uiState.update { it.copy(isPlaying = false, currentStep = 0, currentBar = 0) }
        audioEngine.stop()
    }

    fun setPlayMode(mode: PlayMode) {
        _uiState.update { it.copy(playMode = mode) }
        audioEngine.playMode = mode
    }

    fun setBpm(newBpm: Int) {
        val clamped = newBpm.coerceIn(60, 220)
        _uiState.update { it.copy(bpm = clamped) }
        audioEngine.bpm = clamped.toFloat()
    }

    fun tapTempo() {
        val now = System.currentTimeMillis()
        tapTimes.add(now)
        if (tapTimes.size > 4) tapTimes.removeAt(0)
        if (tapTimes.size >= 2) {
            val intervals = mutableListOf<Long>()
            for (i in 1 until tapTimes.size) {
                intervals.add(tapTimes[i] - tapTimes[i - 1])
            }
            val avgInterval = intervals.average()
            if (avgInterval in 250.0..1000.0) {
                val calculatedBpm = (60000.0 / avgInterval).toInt()
                setBpm(calculatedBpm)
            }
        }
    }

    fun setSwing(newSwing: Int) {
        val clamped = newSwing.coerceIn(0, 100)
        _uiState.update { it.copy(swing = clamped) }
        audioEngine.swingPercent = clamped.toFloat()
    }

    fun selectChannel(channelId: String) {
        _uiState.update { it.copy(selectedChannelId = channelId) }
    }

    // Pattern Management
    fun selectPattern(patternId: Int) {
        val pattern = _uiState.value.patterns.find { it.id == patternId } ?: return
        _uiState.update { state ->
            val updatedChannels = state.channels.map { ch ->
                val pSteps = pattern.channelSteps[ch.id] ?: ch.steps
                val pNotes = pattern.channelNotes[ch.id] ?: ch.notes
                ch.copy(steps = pSteps, notes = pNotes)
            }
            state.copy(
                selectedPatternId = patternId,
                channels = updatedChannels
            )
        }
        audioEngine.currentPatternId = patternId
        syncEngineState()
    }

    fun addPattern(name: String? = null) {
        _uiState.update { state ->
            val nextId = (state.patterns.maxOfOrNull { it.id } ?: 0) + 1
            val newPatName = name ?: "Pattern $nextId"
            val colors = listOf(0xFFFF7300, 0xFF38BDF8, 0xFF10B981, 0xFFF59E0B, 0xFFEC4899, 0xFF8B5CF6)
            val newPat = Pattern(
                id = nextId,
                name = newPatName,
                colorHex = colors[(nextId - 1) % colors.size]
            )
            state.copy(
                patterns = state.patterns + newPat,
                selectedPatternId = nextId
            )
        }
        syncEngineState()
    }

    fun duplicatePattern(patternId: Int) {
        _uiState.update { state ->
            val source = state.patterns.find { it.id == patternId } ?: return@update state
            val nextId = (state.patterns.maxOfOrNull { it.id } ?: 0) + 1
            val dup = source.copy(
                id = nextId,
                name = "${source.name} (Copy)"
            )
            state.copy(
                patterns = state.patterns + dup,
                selectedPatternId = nextId
            )
        }
        syncEngineState()
    }

    fun renamePattern(patternId: Int, newName: String) {
        _uiState.update { state ->
            val updated = state.patterns.map { if (it.id == patternId) it.copy(name = newName) else it }
            state.copy(patterns = updated)
        }
        syncEngineState()
    }

    fun toggleStep(channelId: String, stepIndex: Int) {
        _uiState.update { state ->
            val updatedChannels = state.channels.map { ch ->
                if (ch.id == channelId) {
                    val newSteps = ch.steps.clone()
                    newSteps[stepIndex] = !newSteps[stepIndex]
                    ch.copy(steps = newSteps)
                } else ch
            }
            val currentPat = state.patterns.find { it.id == state.selectedPatternId }
            val updatedPatterns = if (currentPat != null) {
                val curSteps = updatedChannels.find { it.id == channelId }?.steps ?: BooleanArray(16)
                val newMap = currentPat.channelSteps.toMutableMap()
                newMap[channelId] = curSteps.clone()
                state.patterns.map { if (it.id == currentPat.id) it.copy(channelSteps = newMap) else it }
            } else state.patterns

            state.copy(channels = updatedChannels, patterns = updatedPatterns)
        }
        syncEngineState()
    }

    fun toggleChannelMute(channelId: String) {
        _uiState.update { state ->
            val updated = state.channels.map { ch ->
                if (ch.id == channelId) ch.copy(isMuted = !ch.isMuted) else ch
            }
            state.copy(channels = updated)
        }
        syncEngineState()
    }

    fun toggleChannelSolo(channelId: String) {
        _uiState.update { state ->
            val target = state.channels.find { it.id == channelId } ?: return@update state
            val makeSolo = !target.isSolo
            val updated = state.channels.map { ch ->
                if (ch.id == channelId) {
                    ch.copy(isSolo = makeSolo, isMuted = false)
                } else {
                    ch.copy(isSolo = false, isMuted = makeSolo)
                }
            }
            state.copy(channels = updated)
        }
        syncEngineState()
    }

    fun updateChannelVolume(channelId: String, volume: Float) {
        _uiState.update { state ->
            val updated = state.channels.map { ch ->
                if (ch.id == channelId) ch.copy(volume = volume.coerceIn(0f, 1.5f)) else ch
            }
            state.copy(channels = updated)
        }
        syncEngineState()
    }

    fun updateChannelPan(channelId: String, pan: Float) {
        _uiState.update { state ->
            val updated = state.channels.map { ch ->
                if (ch.id == channelId) ch.copy(pan = pan.coerceIn(-1f, 1f)) else ch
            }
            state.copy(channels = updated)
        }
        syncEngineState()
    }

    fun updateChannelPitch(channelId: String, pitchSemi: Int) {
        _uiState.update { state ->
            val updated = state.channels.map { ch ->
                if (ch.id == channelId) ch.copy(pitchSemi = pitchSemi.coerceIn(-24, 24)) else ch
            }
            state.copy(channels = updated)
        }
        syncEngineState()
    }

    fun addOrRemovePianoNote(channelId: String, pitch: Int, startStep: Int) {
        _uiState.update { state ->
            val updatedChannels = state.channels.map { ch ->
                if (ch.id == channelId) {
                    val existing = ch.notes.find { it.pitch == pitch && it.startStep == startStep }
                    val newNotes = if (existing != null) {
                        ch.notes.filter { it.id != existing.id }
                    } else {
                        ch.notes + NoteEvent(pitch = pitch, startStep = startStep, durationSteps = 2, velocity = 0.85f)
                    }
                    ch.copy(notes = newNotes)
                } else ch
            }
            val currentPat = state.patterns.find { it.id == state.selectedPatternId }
            val updatedPatterns = if (currentPat != null) {
                val curNotes = updatedChannels.find { it.id == channelId }?.notes ?: emptyList()
                val newMap = currentPat.channelNotes.toMutableMap()
                newMap[channelId] = curNotes
                state.patterns.map { if (it.id == currentPat.id) it.copy(channelNotes = newMap) else it }
            } else state.patterns

            state.copy(channels = updatedChannels, patterns = updatedPatterns)
        }
        syncEngineState()
    }

    fun updateSynthParams(
        channelId: String,
        osc1: OscType? = null,
        osc2: OscType? = null,
        osc2Mix: Float? = null,
        detuneCents: Float? = null,
        filterType: FilterType? = null,
        cutoffHz: Float? = null,
        resonanceQ: Float? = null,
        attackMs: Float? = null,
        decayMs: Float? = null,
        sustainLevel: Float? = null,
        releaseMs: Float? = null
    ) {
        _uiState.update { state ->
            val updated = state.channels.map { ch ->
                if (ch.id == channelId) {
                    ch.copy(
                        osc1Type = osc1 ?: ch.osc1Type,
                        osc2Type = osc2 ?: ch.osc2Type,
                        osc2Mix = osc2Mix ?: ch.osc2Mix,
                        detuneCents = detuneCents ?: ch.detuneCents,
                        filterType = filterType ?: ch.filterType,
                        cutoffHz = cutoffHz ?: ch.cutoffHz,
                        resonanceQ = resonanceQ ?: ch.resonanceQ,
                        attackMs = attackMs ?: ch.attackMs,
                        decayMs = decayMs ?: ch.decayMs,
                        sustainLevel = sustainLevel ?: ch.sustainLevel,
                        releaseMs = releaseMs ?: ch.releaseMs
                    )
                } else ch
            }
            state.copy(channels = updated)
        }
        syncEngineState()
    }

    fun triggerLiveAudition(channel: Channel, note: NoteEvent? = null, velocity: Float = 1.0f) {
        audioEngine.triggerNote(channel, note, velocity)
    }

    fun selectMixerTrack(trackId: Int) {
        _uiState.update { it.copy(selectedMixerTrackId = trackId) }
    }

    fun updateMixerVolume(trackId: Int, volumeDb: Float) {
        _uiState.update { state ->
            val updated = state.mixerTracks.map { tr ->
                if (tr.id == trackId) tr.copy(volumeDb = volumeDb.coerceIn(-48f, 6f)) else tr
            }
            state.copy(mixerTracks = updated)
        }
        syncEngineState()
    }

    fun updateMixerPan(trackId: Int, pan: Float) {
        _uiState.update { state ->
            val updated = state.mixerTracks.map { tr ->
                if (tr.id == trackId) tr.copy(pan = pan.coerceIn(-1f, 1f)) else tr
            }
            state.copy(mixerTracks = updated)
        }
        syncEngineState()
    }

    fun toggleMixerMute(trackId: Int) {
        _uiState.update { state ->
            val updated = state.mixerTracks.map { tr ->
                if (tr.id == trackId) tr.copy(isMuted = !tr.isMuted) else tr
            }
            state.copy(mixerTracks = updated)
        }
        syncEngineState()
    }

    fun updateMixerEq(trackId: Int, enabled: Boolean, lowGain: Float, midGain: Float, highGain: Float) {
        _uiState.update { state ->
            val updated = state.mixerTracks.map { tr ->
                if (tr.id == trackId) tr.copy(
                    eqEnabled = enabled,
                    eqLowGain = lowGain,
                    eqMidGain = midGain,
                    eqHighGain = highGain
                ) else tr
            }
            state.copy(mixerTracks = updated)
        }
        syncEngineState()
    }

    fun updateMixerDelay(trackId: Int, enabled: Boolean, timeMs: Float, feedback: Float, wet: Float) {
        _uiState.update { state ->
            val updated = state.mixerTracks.map { tr ->
                if (tr.id == trackId) tr.copy(
                    delayEnabled = enabled,
                    delayTimeMs = timeMs,
                    delayFeedback = feedback,
                    delayWet = wet
                ) else tr
            }
            state.copy(mixerTracks = updated)
        }
        syncEngineState()
    }

    fun updateMixerReverb(trackId: Int, enabled: Boolean, room: Float, wet: Float) {
        _uiState.update { state ->
            val updated = state.mixerTracks.map { tr ->
                if (tr.id == trackId) tr.copy(
                    reverbEnabled = enabled,
                    reverbRoom = room,
                    reverbWet = wet
                ) else tr
            }
            state.copy(mixerTracks = updated)
        }
        syncEngineState()
    }

    fun updateMixerDist(trackId: Int, enabled: Boolean, drive: Float, mix: Float) {
        _uiState.update { state ->
            val updated = state.mixerTracks.map { tr ->
                if (tr.id == trackId) tr.copy(
                    distEnabled = enabled,
                    distDrive = drive,
                    distMix = mix
                ) else tr
            }
            state.copy(mixerTracks = updated)
        }
        syncEngineState()
    }

    fun updateMixerChorus(trackId: Int, enabled: Boolean, rate: Float, depth: Float, mix: Float) {
        _uiState.update { state ->
            val updated = state.mixerTracks.map { tr ->
                if (tr.id == trackId) tr.copy(
                    chorusEnabled = enabled,
                    chorusRate = rate,
                    chorusDepth = depth,
                    chorusMix = mix
                ) else tr
            }
            state.copy(mixerTracks = updated)
        }
        syncEngineState()
    }

    fun updateMixerComp(trackId: Int, enabled: Boolean, thresholdDb: Float, ratio: Float) {
        _uiState.update { state ->
            val updated = state.mixerTracks.map { tr ->
                if (tr.id == trackId) tr.copy(
                    compEnabled = enabled,
                    compThresholdDb = thresholdDb,
                    compRatio = ratio
                ) else tr
            }
            state.copy(mixerTracks = updated)
        }
        syncEngineState()
    }

    fun updateTrackAuxSend(trackId: Int, auxId: Int, sendLevel: Float) {
        _uiState.update { state ->
            val updated = state.mixerTracks.map { tr ->
                if (tr.id == trackId) {
                    val sends = tr.auxSends.toMutableMap()
                    sends[auxId] = sendLevel.coerceIn(0f, 1f)
                    tr.copy(auxSends = sends)
                } else tr
            }
            state.copy(mixerTracks = updated)
        }
        syncEngineState()
    }

    fun togglePlaylistClip(trackIndex: Int, barIndex: Int) {
        _uiState.update { state ->
            val existing = state.playlistClips.find { it.trackIndex == trackIndex && it.startBar == barIndex }
            val updated = if (existing != null) {
                state.playlistClips.filter { it.id != existing.id }
            } else {
                val activePat = state.patterns.find { it.id == state.selectedPatternId }
                val clipName = activePat?.name ?: "Pattern ${state.selectedPatternId}"
                val clipColor = activePat?.colorHex ?: 0xFFFF7300
                state.playlistClips + PatternClip(
                    patternIndex = state.selectedPatternId,
                    patternName = clipName,
                    colorHex = clipColor,
                    trackIndex = trackIndex,
                    startBar = barIndex,
                    lengthBars = 1
                )
            }
            state.copy(playlistClips = updated)
        }
        syncEngineState()
    }

    fun addChannel(type: InstrumentType, name: String) {
        val colorHex = when (type) {
            InstrumentType.KICK -> 0xFFFF5722
            InstrumentType.AMAPIANO_KICK -> 0xFFFF7043
            InstrumentType.POP_SUB_KICK -> 0xFFFF4081
            InstrumentType.EDM_DROP_KICK -> 0xFFE11D48
            InstrumentType.SNARE -> 0xFFE91E63
            InstrumentType.POP_ACOUSTIC_SNARE -> 0xFFF43F5E
            InstrumentType.TRAP_SNARE_RIM -> 0xFFD946EF
            InstrumentType.CLAP -> 0xFFFF9800
            InstrumentType.AMAPIANO_CLAP_ROLL -> 0xFFFB923C
            InstrumentType.HIHAT_CLOSED, InstrumentType.HIHAT_OPEN -> 0xFFFFEB3B
            InstrumentType.TRAP_HIHAT_ROLL -> 0xFFFACC15
            InstrumentType.AMAPIANO_SHAKER -> 0xFF84CC16
            InstrumentType.AMAPIANO_WHISTLE -> 0xFF06B6D4
            InstrumentType.AFRO_PERCUSSION -> 0xFFF97316
            InstrumentType.BASS_808 -> 0xFF00E5FF
            InstrumentType.AMAPIANO_LOG_DRUM -> 0xFFEAB308
            InstrumentType.DRAKE_DISTORTED_808 -> 0xFF8B5CF6
            InstrumentType.DRILL_808_SLIDE -> 0xFF6366F1
            InstrumentType.EDM_SIDECHAIN_BASS -> 0xFF3B82F6
            InstrumentType.SYNTH_SUB_BASS -> 0xFF0284C7
            InstrumentType.SYNTH_LEAD -> 0xFF00E676
            InstrumentType.EDM_SUPERSAW -> 0xFF10B981
            InstrumentType.POP_BRIGHT_SYNTH -> 0xFFF59E0B
            InstrumentType.DRAKE_UNDERWATER_KEYS -> 0xFF818CF8
            InstrumentType.PLUCK -> 0xFFB388FF
            InstrumentType.EDM_PLUCK -> 0xFFA855F7
            InstrumentType.SYNTH_PAD -> 0xFF38BDF8
            InstrumentType.DRAKE_VOCAL_PAD -> 0xFFC084FC
            InstrumentType.BRASS_STAB -> 0xFFF43F5E
            InstrumentType.BELL_CHIME -> 0xFF2DD4BF
            InstrumentType.GUITAR_ELECTRIC_CLEAN -> 0xFF38BDF8
            InstrumentType.GUITAR_DISTORTION -> 0xFFEF4444
            InstrumentType.GUITAR_ACOUSTIC -> 0xFFF59E0B
            InstrumentType.GUITAR_NYLON -> 0xFF10B981
            InstrumentType.GUITAR_FUNK -> 0xFFA855F7
            InstrumentType.GUITAR_BASS -> 0xFF6366F1
            InstrumentType.POP_VOCAL_LEAD -> 0xFFEC4899
            InstrumentType.MIC_SAMPLE -> 0xFFFF4081
            InstrumentType.FX_RISER -> 0xFF14B8A6
            InstrumentType.FX_CRASH -> 0xFFE0E7FF
            else -> when (type.category) {
                "Strings" -> 0xFFD946EF
                "Pianos" -> 0xFF38BDF8
                "Brass & Winds" -> 0xFFF59E0B
                "Ethnic" -> 0xFF10B981
                "Drums" -> 0xFFFF5722
                "Amapiano" -> 0xFFEAB308
                "EDM" -> 0xFF3B82F6
                "Hip-Hop" -> 0xFF8B5CF6
                "Pop" -> 0xFFFF4081
                "Bass" -> 0xFF00E5FF
                "Guitars" -> 0xFF10B981
                "Vocals" -> 0xFFFF4081
                "FX" -> 0xFF14B8A6
                else -> 0xFFFF6B00
            }
        }
        val targetInsert = when (type.category) {
            "Drums", "Amapiano" -> if (type == InstrumentType.AMAPIANO_LOG_DRUM) 2 else 1
            "Bass", "Hip-Hop" -> if (type == InstrumentType.TRAP_SNARE_RIM || type == InstrumentType.TRAP_HIHAT_ROLL) 1 else 2
            "Guitars" -> if (type == InstrumentType.GUITAR_BASS) 9 else 3
            "Pianos" -> 6
            "Strings" -> 5
            "Brass & Winds" -> 8
            "Ethnic" -> 10
            "Synth", "EDM", "Pop" -> if (type == InstrumentType.POP_SUB_KICK || type == InstrumentType.EDM_DROP_KICK) 1 else 4
            "Vocals" -> 7
            "FX" -> 11
            else -> 1
        }
        val newChannel = Channel(
            name = name.ifBlank { type.displayName },
            type = type,
            colorHex = colorHex,
            mixerTrackIndex = targetInsert
        )
        _uiState.update {
            it.copy(
                channels = it.channels + newChannel,
                selectedChannelId = newChannel.id,
                showAddChannelDialog = false
            )
        }
        syncEngineState()
    }

    fun openTrackInspector(channelId: String) {
        _uiState.update {
            it.copy(
                inspectingChannelId = channelId,
                showTrackInspectorDialog = true,
                selectedChannelId = channelId
            )
        }
    }

    fun closeTrackInspector() {
        _uiState.update {
            it.copy(
                showTrackInspectorDialog = false,
                inspectingChannelId = null
            )
        }
    }

    fun removeChannel(channelId: String) {
        _uiState.update { state ->
            val remaining = state.channels.filter { it.id != channelId }
            val nextSelected = if (state.selectedChannelId == channelId) {
                remaining.firstOrNull()?.id ?: ""
            } else {
                state.selectedChannelId
            }
            val updatedPatterns = state.patterns.map { pat ->
                pat.copy(
                    channelSteps = pat.channelSteps.filterKeys { it != channelId },
                    channelNotes = pat.channelNotes.filterKeys { it != channelId }
                )
            }
            state.copy(
                channels = remaining,
                selectedChannelId = nextSelected,
                showTrackInspectorDialog = false,
                inspectingChannelId = null,
                patterns = updatedPatterns
            )
        }
        syncEngineState()
    }

    fun updateChannelConfig(
        channelId: String,
        name: String? = null,
        type: InstrumentType? = null,
        mixerTrackIndex: Int? = null,
        volume: Float? = null,
        pan: Float? = null,
        pitchSemi: Int? = null,
        attackMs: Float? = null,
        decayMs: Float? = null,
        sustainLevel: Float? = null,
        releaseMs: Float? = null,
        filterType: FilterType? = null,
        cutoffHz: Float? = null,
        resonanceQ: Float? = null,
        osc1Type: OscType? = null,
        osc2Type: OscType? = null,
        osc2Mix: Float? = null
    ) {
        _uiState.update { state ->
            val updatedChannels = state.channels.map { ch ->
                if (ch.id == channelId) {
                    ch.copy(
                        name = name ?: ch.name,
                        type = type ?: ch.type,
                        mixerTrackIndex = mixerTrackIndex ?: ch.mixerTrackIndex,
                        volume = volume ?: ch.volume,
                        pan = pan ?: ch.pan,
                        pitchSemi = pitchSemi ?: ch.pitchSemi,
                        attackMs = attackMs ?: ch.attackMs,
                        decayMs = decayMs ?: ch.decayMs,
                        sustainLevel = sustainLevel ?: ch.sustainLevel,
                        releaseMs = releaseMs ?: ch.releaseMs,
                        filterType = filterType ?: ch.filterType,
                        cutoffHz = cutoffHz ?: ch.cutoffHz,
                        resonanceQ = resonanceQ ?: ch.resonanceQ,
                        osc1Type = osc1Type ?: ch.osc1Type,
                        osc2Type = osc2Type ?: ch.osc2Type,
                        osc2Mix = osc2Mix ?: ch.osc2Mix
                    )
                } else ch
            }
            state.copy(channels = updatedChannels)
        }
        syncEngineState()
    }

    fun showAddChannel(show: Boolean) {
        _uiState.update { it.copy(showAddChannelDialog = show) }
    }

    fun showRecordDialog(show: Boolean) {
        _uiState.update { it.copy(showRecordDialog = show) }
    }

    fun startMicRecording(onStarted: (Boolean) -> Unit) {
        audioRecorder.startRecording(viewModelScope, onStarted)
    }

    fun stopMicRecordingAndAddSample(sampleName: String) {
        val buffer = audioRecorder.stopRecording()
        if (buffer.isNotEmpty()) {
            val vocalChannel = Channel(
                name = sampleName.ifBlank { "Recorded Vocal" },
                type = InstrumentType.MIC_SAMPLE,
                colorHex = 0xFFFF4081,
                mixerTrackIndex = 5,
                sampleBuffer = buffer
            )
            // Put a trigger on step 0
            vocalChannel.steps[0] = true
            _uiState.update {
                it.copy(
                    channels = it.channels + vocalChannel,
                    selectedChannelId = vocalChannel.id,
                    showRecordDialog = false
                )
            }
            syncEngineState()
        } else {
            _uiState.update { it.copy(showRecordDialog = false) }
        }
    }

    private fun createStandardMixerTracks(): List<MixerTrack> {
        val list = mutableListOf<MixerTrack>()
        // Track 0: Master
        list.add(MixerTrack(id = 0, name = "Master", volumeDb = 0f, eqEnabled = true, compEnabled = true))

        // Tracks 1..28: 28 Channel Inserts
        val insertNames = listOf(
            "Drums", "808 Sub", "Guitars", "Synths", "Plucks",
            "Percussion", "Vocals", "Lead", "Bass", "Pad",
            "FX", "Keys", "Strings", "Brass",
            "Log Drum", "Amapiano Shakers", "Sub Low", "Mid Bass",
            "Vocal Chops", "Drop Synth", "Acoustic", "Clean Strat",
            "Distortion Lead", "Nylon Arp", "Rhodes", "Backing Vox",
            "Drum Bus", "Music Bus"
        )
        for (i in 1..28) {
            val name = insertNames.getOrElse(i - 1) { "Insert $i" }
            list.add(
                MixerTrack(
                    id = i,
                    name = name,
                    volumeDb = 0f,
                    auxSends = mapOf(29 to 0.25f, 30 to 0.15f) // Default sends to Aux 1 (Reverb) & Aux 2 (Delay)
                )
            )
        }

        // Tracks 29..128: 100 Dedicated Auxiliary Send Busses
        val auxNames = listOf(
            "Hall Reverb", "Tape Echo", "Analog Chorus", "Tube Overdrive", "Parallel Comp",
            "Shimmer Verb", "Slapback Echo", "Stereo Flanger", "Warm Saturation", "Room Ambience",
            "Spring Reverb", "Ping-Pong Delay", "Dimension Chorus", "Fuzz Distortion", "Punchy Comp",
            "Plate Reverb", "Stereo Widener", "Phaser FX", "Tape Saturation", "Chamber Verb",
            "Lo-Fi Echo", "Bright Chorus", "Hard Clipper", "Vocal Comp", "Cathedral Reverb",
            "Mod Delay", "Rotary Speaker", "Sub Bass Exciter", "Opto Comp", "Dark Plate Verb",
            "Multitap Delay", "Ensemble FX", "Guitar Amp Drive", "Mastering Comp", "Gated Reverb",
            "Duck Delay", "Micro Pitch", "Overdrive Stomp", "Fast Limiter", "Space Echo",
            "Reverse Delay", "Vibrato FX", "Bitcrusher FX", "Slammer Comp", "Ambient Cloud",
            "Analog BBD Echo", "Dual Chorus", "Screamer Drive", "Glue Comp", "Master Aux 50",
            "Warm Plate", "Vintage 80s Verb", "Stereo Tape Ping", "Sub Boom Verb", "Lush Chorus",
            "Resonant Phaser", "Tape Flutter", "Tube Warmth", "FET 1176 Comp", "Opto LA2A",
            "Amapiano Log Reverb", "Afro Perc Delay", "Alan Walker Supersaw Reverb", "Sidechain Duck Delay",
            "OVO 808 Saturator", "Moody Low-Pass Delay", "Katy Perry Pop Plate", "Glitter Shimmer",
            "Arena Stadium Verb", "Crystal Delay", "Granular Space", "Dynamic Stereo Panner",
            "Exciter Air", "Transient Shaper", "Multiband Glue", "Sunset Reverb", "Dreamy Chorus",
            "Lo-Fi Vinyl Crackle FX", "Telephone EQ Verb", "Spring Tank Reverb", "Bouncing Ball Delay",
            "Sub Harmonic Synth", "Saturator Hard", "Vocal Doubler", "Harmonic Enhancer",
            "Dark Room Verb", "Ping Pong Flutter", "Space Chorus", "Analog Preamp Drive",
            "Master Bus Limiter", "Sidechain Comp Bus", "Drum Room Smasher", "Vocal Air Lift",
            "Ethereal Cavern Verb", "Ghost Delay", "Stereo Widener Pro", "Pumping Bus Comp",
            "Clean Boost", "Vintage Console Saturation", "Infinite Ambient Reverb"
        )

        for (auxIdx in 1..100) {
            val trackId = 28 + auxIdx
            val auxLabel = auxNames.getOrElse(auxIdx - 1) { "Aux FX $auxIdx" }
            val isReverb = auxIdx % 5 == 1
            val isDelay = auxIdx % 5 == 2
            val isChorus = auxIdx % 5 == 3
            val isDist = auxIdx % 5 == 4
            val isComp = auxIdx % 5 == 0

            list.add(
                MixerTrack(
                    id = trackId,
                    name = "Aux $auxIdx: $auxLabel",
                    isAuxBus = true,
                    auxBusIndex = auxIdx,
                    volumeDb = 0f,
                    reverbEnabled = isReverb,
                    reverbRoom = 0.75f,
                    reverbWet = 0.75f,
                    delayEnabled = isDelay,
                    delayTimeMs = 280f,
                    delayFeedback = 0.45f,
                    delayWet = 0.7f,
                    chorusEnabled = isChorus,
                    chorusDepth = 0.5f,
                    chorusRate = 1.2f,
                    chorusMix = 0.6f,
                    distEnabled = isDist,
                    distDrive = 3.0f,
                    distMix = 0.5f,
                    compEnabled = isComp,
                    compThresholdDb = -18f,
                    compRatio = 4f
                )
            )
        }
        return list
    }

    fun loadPresetGuitarRock() {
        val stratClean = Channel(
            name = "Fender Clean Strat",
            type = InstrumentType.GUITAR_ELECTRIC_CLEAN,
            colorHex = 0xFF38BDF8,
            mixerTrackIndex = 3,
            steps = BooleanArray(16) { it % 4 == 0 || it == 14 },
            notes = listOf(
                NoteEvent(pitch = 64, startStep = 0, durationSteps = 3), // E4
                NoteEvent(pitch = 67, startStep = 4, durationSteps = 3), // G4
                NoteEvent(pitch = 69, startStep = 8, durationSteps = 3), // A4
                NoteEvent(pitch = 71, startStep = 12, durationSteps = 2), // B4
                NoteEvent(pitch = 67, startStep = 14, durationSteps = 2)  // G4
            )
        )
        val rockLead = Channel(
            name = "Marshall Heavy Distortion",
            type = InstrumentType.GUITAR_DISTORTION,
            colorHex = 0xFFEF4444,
            mixerTrackIndex = 3,
            steps = BooleanArray(16) { it == 0 || it == 3 || it == 6 || it == 10 || it == 12 },
            notes = listOf(
                NoteEvent(pitch = 52, startStep = 0, durationSteps = 3), // E3
                NoteEvent(pitch = 55, startStep = 3, durationSteps = 3), // G3
                NoteEvent(pitch = 57, startStep = 6, durationSteps = 4), // A3
                NoteEvent(pitch = 55, startStep = 10, durationSteps = 2), // G3
                NoteEvent(pitch = 52, startStep = 12, durationSteps = 4)  // E3
            )
        )
        val acoustic = Channel(
            name = "Steel String Acoustic",
            type = InstrumentType.GUITAR_ACOUSTIC,
            colorHex = 0xFFF59E0B,
            mixerTrackIndex = 3,
            steps = BooleanArray(16) { it % 2 == 0 }
        )
        val nylon = Channel(
            name = "Spanish Nylon Solo",
            type = InstrumentType.GUITAR_NYLON,
            colorHex = 0xFF10B981,
            mixerTrackIndex = 3,
            steps = BooleanArray(16) { it == 2 || it == 6 || it == 10 || it == 14 },
            notes = listOf(
                NoteEvent(pitch = 76, startStep = 2, durationSteps = 2), // E5
                NoteEvent(pitch = 74, startStep = 6, durationSteps = 2), // D5
                NoteEvent(pitch = 72, startStep = 10, durationSteps = 2), // C5
                NoteEvent(pitch = 71, startStep = 14, durationSteps = 2)  // B4
            )
        )
        val bass = Channel(
            name = "Precision Bass",
            type = InstrumentType.GUITAR_BASS,
            colorHex = 0xFF6366F1,
            mixerTrackIndex = 9,
            steps = BooleanArray(16) { it == 0 || it == 6 || it == 10 },
            notes = listOf(
                NoteEvent(pitch = 40, startStep = 0, durationSteps = 5),
                NoteEvent(pitch = 43, startStep = 6, durationSteps = 4),
                NoteEvent(pitch = 45, startStep = 10, durationSteps = 5)
            )
        )
        val kick = Channel(
            name = "Rock Punch Kick",
            type = InstrumentType.KICK,
            colorHex = 0xFFFF6B00,
            mixerTrackIndex = 1,
            steps = BooleanArray(16) { it == 0 || it == 8 || it == 10 }
        )
        val snare = Channel(
            name = "Vintage Wood Snare",
            type = InstrumentType.SNARE,
            colorHex = 0xFFFF2A6D,
            mixerTrackIndex = 1,
            steps = BooleanArray(16) { it == 4 || it == 12 }
        )
        val hihat = Channel(
            name = "Rock Hi-Hat",
            type = InstrumentType.HIHAT_CLOSED,
            colorHex = 0xFFFFE600,
            mixerTrackIndex = 1,
            steps = BooleanArray(16) { it % 2 == 0 }
        )

        val tracks = createStandardMixerTracks().map { tr ->
            if (tr.id == 3) { // Guitars insert track
                tr.copy(
                    distEnabled = true,
                    distDrive = 2.4f,
                    distMix = 0.35f,
                    chorusEnabled = true,
                    chorusMix = 0.25f,
                    auxSends = mapOf(29 to 0.4f, 30 to 0.3f, 31 to 0.2f)
                )
            } else tr
        }

        val pat1 = Pattern(
            id = 1,
            name = "Rock Rhythm & Solos",
            colorHex = 0xFFEF4444,
            channelSteps = mapOf(
                stratClean.id to stratClean.steps.clone(),
                rockLead.id to rockLead.steps.clone(),
                acoustic.id to acoustic.steps.clone(),
                nylon.id to nylon.steps.clone(),
                bass.id to bass.steps.clone(),
                kick.id to kick.steps.clone(),
                snare.id to snare.steps.clone(),
                hihat.id to hihat.steps.clone()
            ),
            channelNotes = mapOf(
                stratClean.id to stratClean.notes,
                rockLead.id to rockLead.notes,
                nylon.id to nylon.notes,
                bass.id to bass.notes
            )
        )
        val pat2 = Pattern(
            id = 2,
            name = "Acoustic & Nylon Chords",
            colorHex = 0xFFF59E0B,
            channelSteps = mapOf(
                acoustic.id to BooleanArray(16) { true },
                nylon.id to BooleanArray(16) { it % 2 == 0 },
                bass.id to BooleanArray(16) { it == 0 || it == 8 }
            )
        )
        val pat3 = Pattern(
            id = 3,
            name = "Distortion Solo Break",
            colorHex = 0xFF38BDF8,
            channelSteps = mapOf(
                rockLead.id to BooleanArray(16) { it % 2 == 1 },
                kick.id to BooleanArray(16) { it % 4 == 0 },
                snare.id to BooleanArray(16) { it == 4 || it == 12 }
            )
        )

        val clips = listOf(
            PatternClip(patternIndex = 1, patternName = "Rock Rhythm", colorHex = 0xFFEF4444, trackIndex = 0, startBar = 0, lengthBars = 2),
            PatternClip(patternIndex = 1, patternName = "Rock Rhythm", colorHex = 0xFFEF4444, trackIndex = 0, startBar = 2, lengthBars = 2),
            PatternClip(patternIndex = 2, patternName = "Acoustic Break", colorHex = 0xFFF59E0B, trackIndex = 1, startBar = 4, lengthBars = 2),
            PatternClip(patternIndex = 3, patternName = "Distortion Solo", colorHex = 0xFF38BDF8, trackIndex = 2, startBar = 6, lengthBars = 2)
        )

        _uiState.update {
            it.copy(
                bpm = 124,
                swing = 10,
                channels = listOf(stratClean, rockLead, acoustic, nylon, bass, kick, snare, hihat),
                selectedChannelId = rockLead.id,
                patterns = listOf(pat1, pat2, pat3),
                selectedPatternId = 1,
                mixerTracks = tracks,
                playlistClips = clips,
                projectPresetName = "Guitar Studio Legends"
            )
        }
        syncEngineState()
    }

    fun loadPresetTrap() {
        val kick = Channel(
            name = "808 Kick",
            type = InstrumentType.KICK,
            colorHex = 0xFFFF6B00,
            mixerTrackIndex = 1,
            steps = BooleanArray(16) { it == 0 || it == 6 || it == 10 }
        )
        val snare = Channel(
            name = "Snappy Snare",
            type = InstrumentType.SNARE,
            colorHex = 0xFFFF2A6D,
            mixerTrackIndex = 1,
            steps = BooleanArray(16) { it == 4 || it == 12 }
        )
        val clap = Channel(
            name = "Fruity Clap",
            type = InstrumentType.CLAP,
            colorHex = 0xFFFFAA00,
            mixerTrackIndex = 1,
            steps = BooleanArray(16) { it == 4 || it == 12 }
        )
        val hihat = Channel(
            name = "Trap Hi-Hat",
            type = InstrumentType.HIHAT_CLOSED,
            colorHex = 0xFFFFE600,
            mixerTrackIndex = 1,
            steps = BooleanArray(16) { it % 2 == 0 || it == 11 || it == 15 }
        )
        val bass = Channel(
            name = "Sub 808 Bass",
            type = InstrumentType.BASS_808,
            colorHex = 0xFF00F0FF,
            mixerTrackIndex = 2,
            steps = BooleanArray(16) { it == 0 || it == 6 || it == 10 },
            notes = listOf(
                NoteEvent(pitch = 36, startStep = 0, durationSteps = 4),
                NoteEvent(pitch = 36, startStep = 6, durationSteps = 3),
                NoteEvent(pitch = 39, startStep = 10, durationSteps = 4)
            )
        )
        val electricGuitar = Channel(
            name = "Trap Clean Guitar",
            type = InstrumentType.GUITAR_ELECTRIC_CLEAN,
            colorHex = 0xFF38BDF8,
            mixerTrackIndex = 3,
            steps = BooleanArray(16) { it == 0 || it == 6 || it == 12 },
            notes = listOf(
                NoteEvent(pitch = 60, startStep = 0, durationSteps = 3),
                NoteEvent(pitch = 63, startStep = 6, durationSteps = 3),
                NoteEvent(pitch = 67, startStep = 12, durationSteps = 3)
            )
        )
        val synthLead = Channel(
            name = "Fruity 3xOSC Lead",
            type = InstrumentType.SYNTH_LEAD,
            colorHex = 0xFF05FFA1,
            mixerTrackIndex = 4,
            osc1Type = OscType.SAWTOOTH,
            osc2Type = OscType.SQUARE,
            cutoffHz = 3500f,
            resonanceQ = 2.2f,
            steps = BooleanArray(16) { it == 0 || it == 3 || it == 7 || it == 10 || it == 14 },
            notes = listOf(
                NoteEvent(pitch = 60, startStep = 0, durationSteps = 2),
                NoteEvent(pitch = 63, startStep = 3, durationSteps = 2),
                NoteEvent(pitch = 67, startStep = 7, durationSteps = 2),
                NoteEvent(pitch = 65, startStep = 10, durationSteps = 2),
                NoteEvent(pitch = 70, startStep = 14, durationSteps = 2)
            )
        )
        val pluck = Channel(
            name = "Fruity Pluck",
            type = InstrumentType.PLUCK,
            colorHex = 0xFFB388FF,
            mixerTrackIndex = 5,
            cutoffHz = 5200f,
            decayMs = 80f,
            steps = BooleanArray(16) { it % 4 == 2 },
            notes = listOf(
                NoteEvent(pitch = 72, startStep = 2, durationSteps = 1),
                NoteEvent(pitch = 75, startStep = 6, durationSteps = 1),
                NoteEvent(pitch = 79, startStep = 10, durationSteps = 1),
                NoteEvent(pitch = 77, startStep = 14, durationSteps = 1)
            )
        )

        val tracks = createStandardMixerTracks()

        val pat1 = Pattern(
            id = 1,
            name = "808 Trap Heat",
            colorHex = 0xFFFF6B00,
            channelSteps = mapOf(
                kick.id to kick.steps.clone(),
                snare.id to snare.steps.clone(),
                clap.id to clap.steps.clone(),
                hihat.id to hihat.steps.clone(),
                bass.id to bass.steps.clone(),
                electricGuitar.id to electricGuitar.steps.clone(),
                synthLead.id to synthLead.steps.clone(),
                pluck.id to pluck.steps.clone()
            ),
            channelNotes = mapOf(
                bass.id to bass.notes,
                electricGuitar.id to electricGuitar.notes,
                synthLead.id to synthLead.notes,
                pluck.id to pluck.notes
            )
        )
        val pat2 = Pattern(
            id = 2,
            name = "Guitar & Pluck Verse",
            colorHex = 0xFF38BDF8,
            channelSteps = mapOf(
                electricGuitar.id to BooleanArray(16) { it % 4 == 0 },
                pluck.id to BooleanArray(16) { it % 2 == 1 },
                hihat.id to BooleanArray(16) { it % 2 == 0 }
            )
        )

        val clips = listOf(
            PatternClip(patternIndex = 1, patternName = "808 Trap Heat", colorHex = 0xFFFF6B00, trackIndex = 0, startBar = 0, lengthBars = 2),
            PatternClip(patternIndex = 1, patternName = "808 Trap Heat", colorHex = 0xFFFF6B00, trackIndex = 0, startBar = 2, lengthBars = 2),
            PatternClip(patternIndex = 2, patternName = "Guitar Verse", colorHex = 0xFF38BDF8, trackIndex = 1, startBar = 4, lengthBars = 2)
        )

        _uiState.update {
            it.copy(
                bpm = 138,
                swing = 20,
                channels = listOf(kick, snare, clap, hihat, bass, electricGuitar, synthLead, pluck),
                selectedChannelId = synthLead.id,
                patterns = listOf(pat1, pat2),
                selectedPatternId = 1,
                mixerTracks = tracks,
                playlistClips = clips,
                projectPresetName = "Trap 808 Heat"
            )
        }
        syncEngineState()
    }

    fun loadPresetCyberElectro() {
        val kick = Channel(
            name = "Electro Kick",
            type = InstrumentType.KICK,
            colorHex = 0xFFFF3366,
            mixerTrackIndex = 1,
            steps = BooleanArray(16) { it % 4 == 0 }
        )
        val clap = Channel(
            name = "Club Clap",
            type = InstrumentType.CLAP,
            colorHex = 0xFFFF9900,
            mixerTrackIndex = 1,
            steps = BooleanArray(16) { it == 4 || it == 12 }
        )
        val hihat = Channel(
            name = "Offbeat Hat",
            type = InstrumentType.HIHAT_OPEN,
            colorHex = 0xFFFFEE00,
            mixerTrackIndex = 1,
            steps = BooleanArray(16) { it % 4 == 2 }
        )
        val guitarFunk = Channel(
            name = "Electro Funk Guitar",
            type = InstrumentType.GUITAR_FUNK,
            colorHex = 0xFFA855F7,
            mixerTrackIndex = 3,
            steps = BooleanArray(16) { it % 2 == 1 },
            notes = listOf(
                NoteEvent(pitch = 65, startStep = 1, durationSteps = 1),
                NoteEvent(pitch = 68, startStep = 3, durationSteps = 1),
                NoteEvent(pitch = 70, startStep = 5, durationSteps = 1)
            )
        )
        val synthLead = Channel(
            name = "Sawtooth Arp",
            type = InstrumentType.SYNTH_LEAD,
            colorHex = 0xFF00E5FF,
            mixerTrackIndex = 4,
            osc1Type = OscType.SAWTOOTH,
            osc2Type = OscType.SAWTOOTH,
            detuneCents = 15f,
            cutoffHz = 5500f,
            resonanceQ = 3.0f,
            steps = BooleanArray(16) { true },
            notes = (0..15).map { step ->
                val scale = listOf(60, 63, 67, 70, 72, 70, 67, 63)
                NoteEvent(pitch = scale[step % scale.size], startStep = step, durationSteps = 1, velocity = 0.85f)
            }
        )

        val tracks = createStandardMixerTracks()
        val pat1 = Pattern(
            id = 1,
            name = "Electro Drive",
            colorHex = 0xFFFF3366,
            channelSteps = mapOf(
                kick.id to kick.steps.clone(),
                clap.id to clap.steps.clone(),
                hihat.id to hihat.steps.clone(),
                guitarFunk.id to guitarFunk.steps.clone(),
                synthLead.id to synthLead.steps.clone()
            ),
            channelNotes = mapOf(
                guitarFunk.id to guitarFunk.notes,
                synthLead.id to synthLead.notes
            )
        )

        val clips = listOf(
            PatternClip(patternIndex = 1, patternName = "Electro Beat", colorHex = 0xFFFF3366, trackIndex = 0, startBar = 0, lengthBars = 2),
            PatternClip(patternIndex = 1, patternName = "Electro Beat", colorHex = 0xFFFF3366, trackIndex = 0, startBar = 2, lengthBars = 2)
        )

        _uiState.update {
            it.copy(
                bpm = 128,
                swing = 0,
                channels = listOf(kick, clap, hihat, guitarFunk, synthLead),
                selectedChannelId = synthLead.id,
                patterns = listOf(pat1),
                selectedPatternId = 1,
                mixerTracks = tracks,
                playlistClips = clips,
                projectPresetName = "Cyber Electro"
            )
        }
        syncEngineState()
    }

    fun loadPresetLoFi() {
        val kick = Channel(
            name = "Lo-Fi Kick",
            type = InstrumentType.KICK,
            colorHex = 0xFFBCAAA4,
            mixerTrackIndex = 1,
            pitchSemi = -2,
            steps = BooleanArray(16) { it == 0 || it == 7 || it == 10 }
        )
        val snare = Channel(
            name = "Dusty Snare",
            type = InstrumentType.SNARE,
            colorHex = 0xFF8D6E63,
            mixerTrackIndex = 1,
            pitchSemi = -1,
            steps = BooleanArray(16) { it == 4 || it == 12 }
        )
        val hihat = Channel(
            name = "Warm Hi-Hat",
            type = InstrumentType.HIHAT_CLOSED,
            colorHex = 0xFFFFD54F,
            mixerTrackIndex = 1,
            steps = BooleanArray(16) { it % 2 == 0 }
        )
        val nylonGuitar = Channel(
            name = "Warm Nylon Guitar",
            type = InstrumentType.GUITAR_NYLON,
            colorHex = 0xFF10B981,
            mixerTrackIndex = 3,
            steps = BooleanArray(16) { it == 0 || it == 4 || it == 8 || it == 12 },
            notes = listOf(
                NoteEvent(pitch = 60, startStep = 0, durationSteps = 3),
                NoteEvent(pitch = 64, startStep = 4, durationSteps = 3),
                NoteEvent(pitch = 67, startStep = 8, durationSteps = 3),
                NoteEvent(pitch = 71, startStep = 12, durationSteps = 3)
            )
        )
        val rhodes = Channel(
            name = "Warm Pluck Keys",
            type = InstrumentType.PLUCK,
            colorHex = 0xFF9575CD,
            mixerTrackIndex = 4,
            cutoffHz = 2200f,
            decayMs = 280f,
            steps = BooleanArray(16) { it == 0 || it == 6 || it == 12 },
            notes = listOf(
                NoteEvent(pitch = 60, startStep = 0, durationSteps = 4),
                NoteEvent(pitch = 64, startStep = 0, durationSteps = 4),
                NoteEvent(pitch = 67, startStep = 0, durationSteps = 4),
                NoteEvent(pitch = 59, startStep = 6, durationSteps = 4),
                NoteEvent(pitch = 62, startStep = 6, durationSteps = 4),
                NoteEvent(pitch = 65, startStep = 6, durationSteps = 4)
            )
        )

        val tracks = createStandardMixerTracks()
        val pat1 = Pattern(
            id = 1,
            name = "Lo-Fi Tape Chill",
            colorHex = 0xFF8D6E63,
            channelSteps = mapOf(
                kick.id to kick.steps.clone(),
                snare.id to snare.steps.clone(),
                hihat.id to hihat.steps.clone(),
                nylonGuitar.id to nylonGuitar.steps.clone(),
                rhodes.id to rhodes.steps.clone()
            ),
            channelNotes = mapOf(
                nylonGuitar.id to nylonGuitar.notes,
                rhodes.id to rhodes.notes
            )
        )

        val clips = listOf(
            PatternClip(patternIndex = 1, patternName = "Chill Beat", colorHex = 0xFF8D6E63, trackIndex = 0, startBar = 0, lengthBars = 2),
            PatternClip(patternIndex = 1, patternName = "Chill Beat", colorHex = 0xFF8D6E63, trackIndex = 0, startBar = 2, lengthBars = 2)
        )

        _uiState.update {
            it.copy(
                bpm = 84,
                swing = 45,
                channels = listOf(kick, snare, hihat, nylonGuitar, rhodes),
                selectedChannelId = nylonGuitar.id,
                patterns = listOf(pat1),
                selectedPatternId = 1,
                mixerTracks = tracks,
                playlistClips = clips,
                projectPresetName = "Lo-Fi Chill"
            )
        }
        syncEngineState()
    }

    fun loadPresetAmapiano() {
        val logDrum = Channel(
            name = "Amapiano Log Drum",
            type = InstrumentType.AMAPIANO_LOG_DRUM,
            colorHex = 0xFFEAB308,
            mixerTrackIndex = 2,
            steps = BooleanArray(16) { it == 0 || it == 3 || it == 6 || it == 8 || it == 11 || it == 14 },
            notes = listOf(
                NoteEvent(pitch = 36, startStep = 0, durationSteps = 3),
                NoteEvent(pitch = 41, startStep = 3, durationSteps = 3),
                NoteEvent(pitch = 38, startStep = 6, durationSteps = 2),
                NoteEvent(pitch = 36, startStep = 8, durationSteps = 3),
                NoteEvent(pitch = 43, startStep = 11, durationSteps = 3),
                NoteEvent(pitch = 41, startStep = 14, durationSteps = 2)
            )
        )
        val shaker = Channel(
            name = "African Shaker Loop",
            type = InstrumentType.AMAPIANO_SHAKER,
            colorHex = 0xFF84CC16,
            mixerTrackIndex = 1,
            volume = 0.85f,
            steps = BooleanArray(16) { true }
        )
        val kick = Channel(
            name = "Deep Amapiano Kick",
            type = InstrumentType.AMAPIANO_KICK,
            colorHex = 0xFFFF7043,
            mixerTrackIndex = 1,
            steps = BooleanArray(16) { it % 4 == 0 }
        )
        val clapRoll = Channel(
            name = "Amapiano Claps",
            type = InstrumentType.AMAPIANO_CLAP_ROLL,
            colorHex = 0xFFFB923C,
            mixerTrackIndex = 1,
            steps = BooleanArray(16) { it == 4 || it == 12 || it == 14 || it == 15 }
        )
        val afroPerc = Channel(
            name = "Afro Bongo Perc",
            type = InstrumentType.AFRO_PERCUSSION,
            colorHex = 0xFFF97316,
            mixerTrackIndex = 1,
            steps = BooleanArray(16) { it == 2 || it == 7 || it == 10 || it == 13 }
        )
        val whistle = Channel(
            name = "Stadium Whistle",
            type = InstrumentType.AMAPIANO_WHISTLE,
            colorHex = 0xFF06B6D4,
            mixerTrackIndex = 1,
            steps = BooleanArray(16) { it == 14 }
        )
        val pianoKeys = Channel(
            name = "Soulful Rhodes Keys",
            type = InstrumentType.DRAKE_UNDERWATER_KEYS,
            colorHex = 0xFF818CF8,
            mixerTrackIndex = 4,
            volume = 0.9f,
            steps = BooleanArray(16) { it == 0 || it == 6 || it == 12 },
            notes = listOf(
                NoteEvent(pitch = 60, startStep = 0, durationSteps = 6),
                NoteEvent(pitch = 64, startStep = 0, durationSteps = 6),
                NoteEvent(pitch = 67, startStep = 0, durationSteps = 6),
                NoteEvent(pitch = 59, startStep = 6, durationSteps = 6),
                NoteEvent(pitch = 62, startStep = 6, durationSteps = 6),
                NoteEvent(pitch = 65, startStep = 6, durationSteps = 6),
                NoteEvent(pitch = 57, startStep = 12, durationSteps = 4),
                NoteEvent(pitch = 60, startStep = 12, durationSteps = 4),
                NoteEvent(pitch = 64, startStep = 12, durationSteps = 4)
            )
        )

        val tracks = createStandardMixerTracks()
        val pat1 = Pattern(
            id = 1,
            name = "Amapiano Groove & Log Drum",
            colorHex = 0xFFEAB308,
            channelSteps = mapOf(
                logDrum.id to logDrum.steps.clone(),
                shaker.id to shaker.steps.clone(),
                kick.id to kick.steps.clone(),
                clapRoll.id to clapRoll.steps.clone(),
                afroPerc.id to afroPerc.steps.clone(),
                whistle.id to whistle.steps.clone(),
                pianoKeys.id to pianoKeys.steps.clone()
            ),
            channelNotes = mapOf(
                logDrum.id to logDrum.notes,
                pianoKeys.id to pianoKeys.notes
            )
        )

        val clips = listOf(
            PatternClip(patternIndex = 1, patternName = "Amapiano Groove", colorHex = 0xFFEAB308, trackIndex = 0, startBar = 0, lengthBars = 4),
            PatternClip(patternIndex = 1, patternName = "Amapiano Groove", colorHex = 0xFFEAB308, trackIndex = 0, startBar = 4, lengthBars = 4)
        )

        _uiState.update {
            it.copy(
                bpm = 113,
                swing = 20,
                channels = listOf(logDrum, shaker, kick, clapRoll, afroPerc, whistle, pianoKeys),
                selectedChannelId = logDrum.id,
                patterns = listOf(pat1),
                selectedPatternId = 1,
                mixerTracks = tracks,
                playlistClips = clips,
                projectPresetName = "Amapiano Fever"
            )
        }
        syncEngineState()
    }

    fun loadPresetAlanWalkerEdm() {
        val pluckLead = Channel(
            name = "Walker Melodic Pluck",
            type = InstrumentType.EDM_PLUCK,
            colorHex = 0xFFA855F7,
            mixerTrackIndex = 4,
            steps = BooleanArray(16) { it == 0 || it == 2 || it == 4 || it == 6 || it == 8 || it == 10 || it == 12 || it == 14 },
            notes = listOf(
                NoteEvent(pitch = 72, startStep = 0, durationSteps = 2),
                NoteEvent(pitch = 76, startStep = 2, durationSteps = 2),
                NoteEvent(pitch = 79, startStep = 4, durationSteps = 2),
                NoteEvent(pitch = 76, startStep = 6, durationSteps = 2),
                NoteEvent(pitch = 81, startStep = 8, durationSteps = 2),
                NoteEvent(pitch = 79, startStep = 10, durationSteps = 2),
                NoteEvent(pitch = 76, startStep = 12, durationSteps = 2),
                NoteEvent(pitch = 74, startStep = 14, durationSteps = 2)
            )
        )
        val supersaw = Channel(
            name = "Anthem Drop Supersaw",
            type = InstrumentType.EDM_SUPERSAW,
            colorHex = 0xFF10B981,
            mixerTrackIndex = 4,
            steps = BooleanArray(16) { it == 0 || it == 6 || it == 12 },
            notes = listOf(
                NoteEvent(pitch = 60, startStep = 0, durationSteps = 6),
                NoteEvent(pitch = 64, startStep = 0, durationSteps = 6),
                NoteEvent(pitch = 67, startStep = 0, durationSteps = 6),
                NoteEvent(pitch = 57, startStep = 6, durationSteps = 6),
                NoteEvent(pitch = 60, startStep = 6, durationSteps = 6),
                NoteEvent(pitch = 64, startStep = 6, durationSteps = 6),
                NoteEvent(pitch = 53, startStep = 12, durationSteps = 4),
                NoteEvent(pitch = 57, startStep = 12, durationSteps = 4),
                NoteEvent(pitch = 60, startStep = 12, durationSteps = 4)
            )
        )
        val sidechainBass = Channel(
            name = "Pumping Sidechain Bass",
            type = InstrumentType.EDM_SIDECHAIN_BASS,
            colorHex = 0xFF3B82F6,
            mixerTrackIndex = 2,
            steps = BooleanArray(16) { it == 0 || it == 4 || it == 8 || it == 12 },
            notes = listOf(
                NoteEvent(pitch = 36, startStep = 0, durationSteps = 4),
                NoteEvent(pitch = 33, startStep = 4, durationSteps = 4),
                NoteEvent(pitch = 29, startStep = 8, durationSteps = 4),
                NoteEvent(pitch = 31, startStep = 12, durationSteps = 4)
            )
        )
        val dropKick = Channel(
            name = "Festival Drop Kick",
            type = InstrumentType.EDM_DROP_KICK,
            colorHex = 0xFFE11D48,
            mixerTrackIndex = 1,
            steps = BooleanArray(16) { it % 4 == 0 }
        )
        val stadiumClap = Channel(
            name = "Stadium Clap",
            type = InstrumentType.CLAP,
            colorHex = 0xFFFF9800,
            mixerTrackIndex = 1,
            steps = BooleanArray(16) { it == 4 || it == 12 }
        )
        val openHat = Channel(
            name = "Festival Open Hat",
            type = InstrumentType.HIHAT_OPEN,
            colorHex = 0xFFFFEB3B,
            mixerTrackIndex = 1,
            steps = BooleanArray(16) { it == 2 || it == 6 || it == 10 || it == 14 }
        )
        val riser = Channel(
            name = "White Noise Riser",
            type = InstrumentType.FX_RISER,
            colorHex = 0xFF14B8A6,
            mixerTrackIndex = 11,
            steps = BooleanArray(16) { it == 12 }
        )
        val crash = Channel(
            name = "Festival Crash",
            type = InstrumentType.FX_CRASH,
            colorHex = 0xFFE0E7FF,
            mixerTrackIndex = 11,
            steps = BooleanArray(16) { it == 0 }
        )

        val tracks = createStandardMixerTracks()
        val pat1 = Pattern(
            id = 1,
            name = "EDM Anthem Festival Drop",
            colorHex = 0xFFA855F7,
            channelSteps = mapOf(
                pluckLead.id to pluckLead.steps.clone(),
                supersaw.id to supersaw.steps.clone(),
                sidechainBass.id to sidechainBass.steps.clone(),
                dropKick.id to dropKick.steps.clone(),
                stadiumClap.id to stadiumClap.steps.clone(),
                openHat.id to openHat.steps.clone(),
                riser.id to riser.steps.clone(),
                crash.id to crash.steps.clone()
            ),
            channelNotes = mapOf(
                pluckLead.id to pluckLead.notes,
                supersaw.id to supersaw.notes,
                sidechainBass.id to sidechainBass.notes
            )
        )

        val clips = listOf(
            PatternClip(patternIndex = 1, patternName = "EDM Drop", colorHex = 0xFFA855F7, trackIndex = 0, startBar = 0, lengthBars = 4),
            PatternClip(patternIndex = 1, patternName = "EDM Drop", colorHex = 0xFFA855F7, trackIndex = 0, startBar = 4, lengthBars = 4)
        )

        _uiState.update {
            it.copy(
                bpm = 128,
                swing = 0,
                channels = listOf(pluckLead, supersaw, sidechainBass, dropKick, stadiumClap, openHat, riser, crash),
                selectedChannelId = pluckLead.id,
                patterns = listOf(pat1),
                selectedPatternId = 1,
                mixerTracks = tracks,
                playlistClips = clips,
                projectPresetName = "Alan Walker Festival EDM"
            )
        }
        syncEngineState()
    }

    fun loadPresetDrakeMoodyTrap() {
        val underwaterKeys = Channel(
            name = "Underwater Lo-Fi Keys",
            type = InstrumentType.DRAKE_UNDERWATER_KEYS,
            colorHex = 0xFF818CF8,
            mixerTrackIndex = 4,
            steps = BooleanArray(16) { it == 0 || it == 6 || it == 12 },
            notes = listOf(
                NoteEvent(pitch = 58, startStep = 0, durationSteps = 6),
                NoteEvent(pitch = 61, startStep = 0, durationSteps = 6),
                NoteEvent(pitch = 65, startStep = 0, durationSteps = 6),
                NoteEvent(pitch = 56, startStep = 6, durationSteps = 6),
                NoteEvent(pitch = 59, startStep = 6, durationSteps = 6),
                NoteEvent(pitch = 63, startStep = 6, durationSteps = 6),
                NoteEvent(pitch = 54, startStep = 12, durationSteps = 4),
                NoteEvent(pitch = 58, startStep = 12, durationSteps = 4),
                NoteEvent(pitch = 61, startStep = 12, durationSteps = 4)
            )
        )
        val distorted808 = Channel(
            name = "Toronto Distorted 808",
            type = InstrumentType.DRAKE_DISTORTED_808,
            colorHex = 0xFF8B5CF6,
            mixerTrackIndex = 2,
            steps = BooleanArray(16) { it == 0 || it == 6 || it == 10 || it == 14 },
            notes = listOf(
                NoteEvent(pitch = 34, startStep = 0, durationSteps = 6),
                NoteEvent(pitch = 34, startStep = 6, durationSteps = 4),
                NoteEvent(pitch = 32, startStep = 10, durationSteps = 4),
                NoteEvent(pitch = 39, startStep = 14, durationSteps = 2)
            )
        )
        val vocalPad = Channel(
            name = "Ambient OVO Vocal Pad",
            type = InstrumentType.DRAKE_VOCAL_PAD,
            colorHex = 0xFFC084FC,
            mixerTrackIndex = 7,
            steps = BooleanArray(16) { it == 0 || it == 8 },
            notes = listOf(
                NoteEvent(pitch = 70, startStep = 0, durationSteps = 8),
                NoteEvent(pitch = 68, startStep = 8, durationSteps = 8)
            )
        )
        val trapRim = Channel(
            name = "OVO Crisp Rim Snare",
            type = InstrumentType.TRAP_SNARE_RIM,
            colorHex = 0xFFD946EF,
            mixerTrackIndex = 1,
            steps = BooleanArray(16) { it == 4 || it == 12 }
        )
        val hihatRoll = Channel(
            name = "Rolling Trap Hi-Hats",
            type = InstrumentType.TRAP_HIHAT_ROLL,
            colorHex = 0xFFFACC15,
            mixerTrackIndex = 1,
            steps = BooleanArray(16) { it % 2 == 0 || it == 7 || it == 11 || it == 15 }
        )
        val kick = Channel(
            name = "Sub Heavy Kick",
            type = InstrumentType.KICK,
            colorHex = 0xFFFF5722,
            mixerTrackIndex = 1,
            steps = BooleanArray(16) { it == 0 || it == 6 || it == 10 }
        )

        val tracks = createStandardMixerTracks()
        val pat1 = Pattern(
            id = 1,
            name = "OVO Night Owl Trap",
            colorHex = 0xFF8B5CF6,
            channelSteps = mapOf(
                underwaterKeys.id to underwaterKeys.steps.clone(),
                distorted808.id to distorted808.steps.clone(),
                vocalPad.id to vocalPad.steps.clone(),
                trapRim.id to trapRim.steps.clone(),
                hihatRoll.id to hihatRoll.steps.clone(),
                kick.id to kick.steps.clone()
            ),
            channelNotes = mapOf(
                underwaterKeys.id to underwaterKeys.notes,
                distorted808.id to distorted808.notes,
                vocalPad.id to vocalPad.notes
            )
        )

        val clips = listOf(
            PatternClip(patternIndex = 1, patternName = "OVO Night Beat", colorHex = 0xFF8B5CF6, trackIndex = 0, startBar = 0, lengthBars = 4),
            PatternClip(patternIndex = 1, patternName = "OVO Night Beat", colorHex = 0xFF8B5CF6, trackIndex = 0, startBar = 4, lengthBars = 4)
        )

        _uiState.update {
            it.copy(
                bpm = 136,
                swing = 12,
                channels = listOf(underwaterKeys, distorted808, vocalPad, trapRim, hihatRoll, kick),
                selectedChannelId = underwaterKeys.id,
                patterns = listOf(pat1),
                selectedPatternId = 1,
                mixerTracks = tracks,
                playlistClips = clips,
                projectPresetName = "Drake OVO Moody Trap"
            )
        }
        syncEngineState()
    }

    fun loadPresetKatyPerryPop() {
        val brightPopSynth = Channel(
            name = "Bright Pop Anthem Brass",
            type = InstrumentType.POP_BRIGHT_SYNTH,
            colorHex = 0xFFF59E0B,
            mixerTrackIndex = 4,
            steps = BooleanArray(16) { it == 0 || it == 3 || it == 6 || it == 8 || it == 11 || it == 14 },
            notes = listOf(
                NoteEvent(pitch = 64, startStep = 0, durationSteps = 3),
                NoteEvent(pitch = 67, startStep = 3, durationSteps = 3),
                NoteEvent(pitch = 71, startStep = 6, durationSteps = 2),
                NoteEvent(pitch = 69, startStep = 8, durationSteps = 3),
                NoteEvent(pitch = 67, startStep = 11, durationSteps = 3),
                NoteEvent(pitch = 64, startStep = 14, durationSteps = 2)
            )
        )
        val funkGuitar = Channel(
            name = "Pop Funk Rhythm Guitar",
            type = InstrumentType.GUITAR_FUNK,
            colorHex = 0xFFA855F7,
            mixerTrackIndex = 3,
            steps = BooleanArray(16) { it % 2 == 1 }
        )
        val studioBass = Channel(
            name = "Punchy Studio Bass",
            type = InstrumentType.GUITAR_BASS,
            colorHex = 0xFF6366F1,
            mixerTrackIndex = 9,
            steps = BooleanArray(16) { it == 0 || it == 4 || it == 7 || it == 10 || it == 12 },
            notes = listOf(
                NoteEvent(pitch = 40, startStep = 0, durationSteps = 4),
                NoteEvent(pitch = 40, startStep = 4, durationSteps = 3),
                NoteEvent(pitch = 45, startStep = 7, durationSteps = 3),
                NoteEvent(pitch = 47, startStep = 10, durationSteps = 2),
                NoteEvent(pitch = 43, startStep = 12, durationSteps = 4)
            )
        )
        val popKick = Channel(
            name = "Chart Punch Kick",
            type = InstrumentType.POP_SUB_KICK,
            colorHex = 0xFFFF4081,
            mixerTrackIndex = 1,
            steps = BooleanArray(16) { it % 4 == 0 }
        )
        val popSnare = Channel(
            name = "Big Gated Pop Snare",
            type = InstrumentType.POP_ACOUSTIC_SNARE,
            colorHex = 0xFFF43F5E,
            mixerTrackIndex = 1,
            steps = BooleanArray(16) { it == 4 || it == 12 }
        )
        val popHat = Channel(
            name = "Bright Shimmer Hat",
            type = InstrumentType.HIHAT_CLOSED,
            colorHex = 0xFFFFEB3B,
            mixerTrackIndex = 1,
            steps = BooleanArray(16) { true }
        )
        val vocalLead = Channel(
            name = "California Vocal Lead",
            type = InstrumentType.POP_VOCAL_LEAD,
            colorHex = 0xFFEC4899,
            mixerTrackIndex = 7,
            steps = BooleanArray(16) { it == 0 || it == 4 || it == 8 || it == 12 },
            notes = listOf(
                NoteEvent(pitch = 72, startStep = 0, durationSteps = 4),
                NoteEvent(pitch = 74, startStep = 4, durationSteps = 4),
                NoteEvent(pitch = 76, startStep = 8, durationSteps = 4),
                NoteEvent(pitch = 72, startStep = 12, durationSteps = 4)
            )
        )

        val tracks = createStandardMixerTracks()
        val pat1 = Pattern(
            id = 1,
            name = "Stadium Pop Chorus",
            colorHex = 0xFFFF4081,
            channelSteps = mapOf(
                brightPopSynth.id to brightPopSynth.steps.clone(),
                funkGuitar.id to funkGuitar.steps.clone(),
                studioBass.id to studioBass.steps.clone(),
                popKick.id to popKick.steps.clone(),
                popSnare.id to popSnare.steps.clone(),
                popHat.id to popHat.steps.clone(),
                vocalLead.id to vocalLead.steps.clone()
            ),
            channelNotes = mapOf(
                brightPopSynth.id to brightPopSynth.notes,
                studioBass.id to studioBass.notes,
                vocalLead.id to vocalLead.notes
            )
        )

        val clips = listOf(
            PatternClip(patternIndex = 1, patternName = "Pop Chorus", colorHex = 0xFFFF4081, trackIndex = 0, startBar = 0, lengthBars = 4),
            PatternClip(patternIndex = 1, patternName = "Pop Chorus", colorHex = 0xFFFF4081, trackIndex = 0, startBar = 4, lengthBars = 4)
        )

        _uiState.update {
            it.copy(
                bpm = 122,
                swing = 8,
                channels = listOf(brightPopSynth, funkGuitar, studioBass, popKick, popSnare, popHat, vocalLead),
                selectedChannelId = brightPopSynth.id,
                patterns = listOf(pat1),
                selectedPatternId = 1,
                mixerTracks = tracks,
                playlistClips = clips,
                projectPresetName = "Katy Perry Pop Anthem"
            )
        }
        syncEngineState()
    }

    private fun syncEngineState() {
        val state = _uiState.value
        audioEngine.bpm = state.bpm.toFloat()
        audioEngine.swingPercent = state.swing.toFloat()
        audioEngine.playMode = state.playMode
        audioEngine.channels = state.channels
        audioEngine.mixerTracks = state.mixerTracks
        audioEngine.patternClips = state.playlistClips
        audioEngine.patterns = state.patterns
        audioEngine.currentPatternId = state.selectedPatternId
        audioEngine.totalBars = state.totalBars
    }
}
