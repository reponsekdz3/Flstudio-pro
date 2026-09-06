package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import com.example.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.*

class AudioEngine(private val sampleRate: Int = 44100) {

    private var audioTrack: AudioTrack? = null
    private var playbackThread: Thread? = null
    @Volatile private var isRunning = false
    @Volatile private var isPlaying = false

    // State flows for UI sync
    private val _currentStep = MutableStateFlow(0)
    val currentStep = _currentStep.asStateFlow()

    private val _currentBar = MutableStateFlow(0)
    val currentBar = _currentBar.asStateFlow()

    private val _masterPeakL = MutableStateFlow(0f)
    val masterPeakL = _masterPeakL.asStateFlow()

    private val _masterPeakR = MutableStateFlow(0f)
    val masterPeakR = _masterPeakR.asStateFlow()

    // Real-time track peaks for all 65 tracks (Master + 14 inserts + 50 aux sends)
    private val _trackPeaks = MutableStateFlow(FloatArray(TOTAL_MIXER_TRACKS))
    val trackPeaks = _trackPeaks.asStateFlow()

    // Real-time project snapshot reference
    @Volatile var bpm: Float = 128f
    @Volatile var swingPercent: Float = 0f
    @Volatile var playMode: PlayMode = PlayMode.PATTERN
    @Volatile var channels: List<Channel> = emptyList()
    @Volatile var mixerTracks: List<MixerTrack> = emptyList()
    @Volatile var patternClips: List<PatternClip> = emptyList()
    @Volatile var patterns: List<Pattern> = emptyList()
    @Volatile var currentPatternId: Int = 1
    @Volatile var totalBars: Int = 16

    // Queue for instant preview notes (user taps piano roll or channel buttons)
    private data class LiveTrigger(val channel: Channel, val note: NoteEvent?, val velocity: Float)
    private val liveTriggers = ConcurrentLinkedQueue<LiveTrigger>()

    // DSP instances per mixer track (0 = Master, 1..14 = Inserts, 15..64 = 50 Aux Busses)
    private val eqList = List(TOTAL_MIXER_TRACKS) { ParametricEQ(sampleRate.toFloat()) }
    private val delayList = List(TOTAL_MIXER_TRACKS) { DelayFX(sampleRate) }
    private val reverbList = List(TOTAL_MIXER_TRACKS) { ReverbFX(sampleRate) }
    private val distList = List(TOTAL_MIXER_TRACKS) { DistortionFX() }
    private val chorusList = List(TOTAL_MIXER_TRACKS) { ChorusFX(sampleRate) }
    private val compList = List(TOTAL_MIXER_TRACKS) { CompressorFX(sampleRate) }

    companion object {
        const val TOTAL_MIXER_TRACKS = 129
        const val MASTER_TRACK_ID = 0
        const val CHANNEL_TRACK_START = 1
        const val CHANNEL_TRACK_END = 28
        const val AUX_TRACK_START = 29
        const val AUX_TRACK_END = 128
        const val TOTAL_AUX_BUSSES = 100
    }

    private val synthVoice = SynthVoice(sampleRate.toFloat())

    fun start() {
        if (isRunning) return
        isRunning = true

        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4096)

        audioTrack = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } else {
            @Suppress("DEPRECATION")
            AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
                AudioTrack.MODE_STREAM
            )
        }

        audioTrack?.play()

        playbackThread = Thread {
            audioLoop()
        }.apply {
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    fun stopEngine() {
        isRunning = false
        isPlaying = false
        try {
            playbackThread?.join(500)
        } catch (e: Exception) {
            // Ignore
        }
        playbackThread = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            // Ignore
        }
        audioTrack = null
    }

    fun play() {
        isPlaying = true
    }

    fun pause() {
        isPlaying = false
    }

    fun stop() {
        isPlaying = false
        _currentStep.value = 0
        _currentBar.value = 0
    }

    fun triggerNote(channel: Channel, note: NoteEvent?, velocity: Float = 1.0f) {
        liveTriggers.add(LiveTrigger(channel, note, velocity))
    }

    private fun audioLoop() {
        val frameChunk = 512
        val trackCount = TOTAL_MIXER_TRACKS
        // Mixer track accumulators (0 = Master, 1..14 = Inserts, 15..64 = 50 Aux Busses)
        val trackL = Array(trackCount) { FloatArray(frameChunk) }
        val trackR = Array(trackCount) { FloatArray(frameChunk) }
        val masterL = FloatArray(frameChunk)
        val masterR = FloatArray(frameChunk)
        val processedChannelL = FloatArray(frameChunk)
        val processedChannelR = FloatArray(frameChunk)
        val outShorts = ShortArray(frameChunk * 2)
        val currentTrackPeaks = FloatArray(trackCount)

        var sampleInStep = 0
        var internalStep = 0
        var internalBar = 0

        while (isRunning) {
            // Clear track buffers
            for (t in 0 until trackCount) {
                trackL[t].fill(0f)
                trackR[t].fill(0f)
            }
            masterL.fill(0f)
            masterR.fill(0f)

            // 1. Process Live interactive triggers (e.g. keyboard press, audition drum pad)
            while (!liveTriggers.isEmpty()) {
                val trig = liveTriggers.poll() ?: break
                val mIndex = trig.channel.mixerTrackIndex.coerceIn(0, CHANNEL_TRACK_END)
                synthVoice.renderVoice(
                    channel = trig.channel,
                    note = trig.note,
                    totalSamples = frameChunk,
                    outputL = trackL[mIndex],
                    outputR = trackR[mIndex],
                    velocity = trig.velocity
                )
            }

            // 2. Process Sequencer if playing
            if (isPlaying) {
                val baseSamplesPerStep = ((sampleRate * 60f) / bpm / 4f).toInt().coerceAtLeast(100)

                var framesToProcess = frameChunk
                var frameOffset = 0

                while (framesToProcess > 0) {
                    // Check swing offset for odd steps
                    val isOddStep = (internalStep % 2) != 0
                    val swingOffset = if (isOddStep) (baseSamplesPerStep * (swingPercent / 100f) * 0.4f).toInt() else 0
                    val currentStepTotalSamples = baseSamplesPerStep + swingOffset

                    val remainingInCurrentStep = currentStepTotalSamples - sampleInStep
                    val stepChunk = min(framesToProcess, remainingInCurrentStep)

                    // On the very first frame of a step, trigger events!
                    if (sampleInStep == 0) {
                        _currentStep.value = internalStep
                        _currentBar.value = internalBar

                        val currentSnapshot = channels
                        val mode = playMode

                        if (mode == PlayMode.PATTERN) {
                            // Fetch current pattern
                            val currentPattern = patterns.find { it.id == currentPatternId }
                            for (ch in currentSnapshot) {
                                if (ch.isMuted) continue
                                val mIndex = ch.mixerTrackIndex.coerceIn(0, CHANNEL_TRACK_END)
                                val steps = currentPattern?.channelSteps?.get(ch.id) ?: ch.steps
                                val notes = currentPattern?.channelNotes?.get(ch.id) ?: ch.notes

                                // Check step sequencer trigger
                                if (internalStep < steps.size && steps[internalStep]) {
                                    synthVoice.renderVoice(
                                        channel = ch,
                                        note = null,
                                        totalSamples = stepChunk,
                                        outputL = trackL[mIndex],
                                        outputR = trackR[mIndex],
                                        velocity = 0.9f
                                    )
                                }

                                // Check piano roll note triggers
                                for (n in notes) {
                                    if (n.startStep == internalStep) {
                                        synthVoice.renderVoice(
                                            channel = ch,
                                            note = n,
                                            totalSamples = stepChunk,
                                            outputL = trackL[mIndex],
                                            outputR = trackR[mIndex],
                                            velocity = n.velocity
                                        )
                                    }
                                }
                            }
                        } else {
                            // SONG MODE: arrangement clips
                            val clips = patternClips
                            val activeClips = clips.filter {
                                internalBar >= it.startBar && internalBar < (it.startBar + it.lengthBars)
                            }
                            for (clip in activeClips) {
                                val pattern = patterns.find { it.id == (clip.patternIndex + 1) || it.id == clip.patternIndex }
                                for (ch in currentSnapshot) {
                                    if (ch.isMuted) continue
                                    val mIndex = ch.mixerTrackIndex.coerceIn(0, CHANNEL_TRACK_END)
                                    val steps = pattern?.channelSteps?.get(ch.id) ?: ch.steps
                                    val notes = pattern?.channelNotes?.get(ch.id) ?: ch.notes

                                    if (internalStep < steps.size && steps[internalStep]) {
                                        synthVoice.renderVoice(
                                            channel = ch,
                                            note = null,
                                            totalSamples = stepChunk,
                                            outputL = trackL[mIndex],
                                            outputR = trackR[mIndex],
                                            velocity = 0.9f
                                        )
                                    }

                                    for (n in notes) {
                                        if (n.startStep == internalStep) {
                                            synthVoice.renderVoice(
                                                channel = ch,
                                                note = n,
                                                totalSamples = stepChunk,
                                                outputL = trackL[mIndex],
                                                outputR = trackR[mIndex],
                                                velocity = n.velocity
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    sampleInStep += stepChunk
                    frameOffset += stepChunk
                    framesToProcess -= stepChunk

                    if (sampleInStep >= currentStepTotalSamples) {
                        sampleInStep = 0
                        internalStep++
                        if (internalStep >= 16) {
                            internalStep = 0
                            internalBar++
                            if (internalBar >= totalBars) {
                                internalBar = 0
                            }
                        }
                    }
                }
            }

            // 3. Process Channel Inserts (Tracks 1..14) -> Local FX -> Route to 50 Aux Busses & Master
            val mTracks = mixerTracks
            for (t in CHANNEL_TRACK_START..CHANNEL_TRACK_END) {
                val mixerState = mTracks.find { it.id == t }
                val eq = eqList[t]
                val delay = delayList[t]
                val reverb = reverbList[t]
                val dist = distList[t]
                val chorus = chorusList[t]
                val comp = compList[t]

                if (mixerState != null) {
                    eq.enabled = mixerState.eqEnabled
                    eq.lowGainDb = mixerState.eqLowGain
                    eq.midGainDb = mixerState.eqMidGain
                    eq.highGainDb = mixerState.eqHighGain

                    delay.enabled = mixerState.delayEnabled
                    delay.timeMs = mixerState.delayTimeMs
                    delay.feedback = mixerState.delayFeedback
                    delay.wetMix = mixerState.delayWet

                    reverb.enabled = mixerState.reverbEnabled
                    reverb.roomSize = mixerState.reverbRoom
                    reverb.wetMix = mixerState.reverbWet

                    dist.enabled = mixerState.distEnabled
                    dist.drive = mixerState.distDrive
                    dist.mix = mixerState.distMix

                    chorus.enabled = mixerState.chorusEnabled
                    chorus.depth = mixerState.chorusDepth
                    chorus.rateHz = mixerState.chorusRate
                    chorus.mix = mixerState.chorusMix

                    comp.enabled = mixerState.compEnabled
                    comp.thresholdDb = mixerState.compThresholdDb
                    comp.ratio = mixerState.compRatio
                }

                val volLinear = if (mixerState != null) {
                    if (mixerState.isMuted) 0f else 10f.pow(mixerState.volumeDb / 20f)
                } else 1.0f

                val panVal = mixerState?.pan ?: 0f
                val panL = (1f - panVal).coerceIn(0f, 1f)
                val panR = (1f + panVal).coerceIn(0f, 1f)

                var maxTrackPeak = 0f

                for (i in 0 until frameChunk) {
                    var sL = trackL[t][i]
                    var sR = trackR[t][i]

                    // Apply hardware FX chain: EQ -> Dist -> Chorus -> Delay -> Reverb -> Comp
                    val eqRes = eq.process(sL, sR)
                    val distRes = dist.process(eqRes.first, eqRes.second)
                    val chorusRes = chorus.process(distRes.first, distRes.second)
                    val delayRes = delay.process(chorusRes.first, chorusRes.second)
                    val revRes = reverb.process(delayRes.first, delayRes.second)
                    val compRes = comp.process(revRes.first, revRes.second)

                    sL = compRes.first * volLinear
                    sR = compRes.second * volLinear

                    val p = max(abs(sL), abs(sR))
                    if (p > maxTrackPeak) maxTrackPeak = p

                    processedChannelL[i] = sL
                    processedChannelR[i] = sR

                    masterL[i] += sL * panL
                    masterR[i] += sR * panR
                }
                currentTrackPeaks[t] = maxTrackPeak

                // Route auxiliary sends to the 50 aux busses (15..64)
                mixerState?.auxSends?.forEach { (auxId, sendLevel) ->
                    if (auxId in AUX_TRACK_START..AUX_TRACK_END && sendLevel > 0.001f) {
                        val auxBufferL = trackL[auxId]
                        val auxBufferR = trackR[auxId]
                        for (i in 0 until frameChunk) {
                            auxBufferL[i] += processedChannelL[i] * sendLevel
                            auxBufferR[i] += processedChannelR[i] * sendLevel
                        }
                    }
                }
            }

            // 4. Process all 50 Auxiliary Send Busses (Tracks 15..64)
            for (t in AUX_TRACK_START..AUX_TRACK_END) {
                val mixerState = mTracks.find { it.id == t }
                val eq = eqList[t]
                val delay = delayList[t]
                val reverb = reverbList[t]
                val dist = distList[t]
                val chorus = chorusList[t]
                val comp = compList[t]

                if (mixerState != null) {
                    eq.enabled = mixerState.eqEnabled
                    eq.lowGainDb = mixerState.eqLowGain
                    eq.midGainDb = mixerState.eqMidGain
                    eq.highGainDb = mixerState.eqHighGain

                    delay.enabled = mixerState.delayEnabled
                    delay.timeMs = mixerState.delayTimeMs
                    delay.feedback = mixerState.delayFeedback
                    delay.wetMix = mixerState.delayWet

                    reverb.enabled = mixerState.reverbEnabled
                    reverb.roomSize = mixerState.reverbRoom
                    reverb.wetMix = mixerState.reverbWet

                    dist.enabled = mixerState.distEnabled
                    dist.drive = mixerState.distDrive
                    dist.mix = mixerState.distMix

                    chorus.enabled = mixerState.chorusEnabled
                    chorus.depth = mixerState.chorusDepth
                    chorus.rateHz = mixerState.chorusRate
                    chorus.mix = mixerState.chorusMix

                    comp.enabled = mixerState.compEnabled
                    comp.thresholdDb = mixerState.compThresholdDb
                    comp.ratio = mixerState.compRatio
                }

                val volLinear = if (mixerState != null) {
                    if (mixerState.isMuted) 0f else 10f.pow(mixerState.volumeDb / 20f)
                } else 1.0f

                val panVal = mixerState?.pan ?: 0f
                val panL = (1f - panVal).coerceIn(0f, 1f)
                val panR = (1f + panVal).coerceIn(0f, 1f)

                var maxAuxPeak = 0f

                for (i in 0 until frameChunk) {
                    var sL = trackL[t][i]
                    var sR = trackR[t][i]

                    val eqRes = eq.process(sL, sR)
                    val distRes = dist.process(eqRes.first, eqRes.second)
                    val chorusRes = chorus.process(distRes.first, distRes.second)
                    val delayRes = delay.process(chorusRes.first, chorusRes.second)
                    val revRes = reverb.process(delayRes.first, delayRes.second)
                    val compRes = comp.process(revRes.first, revRes.second)

                    sL = compRes.first * volLinear
                    sR = compRes.second * volLinear

                    val p = max(abs(sL), abs(sR))
                    if (p > maxAuxPeak) maxAuxPeak = p

                    masterL[i] += sL * panL
                    masterR[i] += sR * panR
                }
                currentTrackPeaks[t] = maxAuxPeak
            }

            // Track 0 (direct to master)
            for (i in 0 until frameChunk) {
                masterL[i] += trackL[0][i]
                masterR[i] += trackR[0][i]
            }

            // 5. Process Master Track (Track 0) DSP & Limiter
            val masterState = mTracks.find { it.id == MASTER_TRACK_ID } ?: mTracks.getOrNull(0)
            val masterVol = if (masterState != null) {
                if (masterState.isMuted) 0f else 10f.pow(masterState.volumeDb / 20f)
            } else 1.0f

            var peakMaxL = 0f
            var peakMaxR = 0f

            for (i in 0 until frameChunk) {
                var sL = masterL[i] * masterVol
                var sR = masterR[i] * masterVol

                // Fruity Soft Clipper / Master Limiter
                sL = tanh(sL).coerceIn(-1.0f, 1.0f)
                sR = tanh(sR).coerceIn(-1.0f, 1.0f)

                val absL = abs(sL)
                val absR = abs(sR)
                if (absL > peakMaxL) peakMaxL = absL
                if (absR > peakMaxR) peakMaxR = absR

                outShorts[i * 2] = (sL * 32767f).toInt().toShort()
                outShorts[i * 2 + 1] = (sR * 32767f).toInt().toShort()
            }

            currentTrackPeaks[0] = max(peakMaxL, peakMaxR)

            // Update live peak VU meters for UI
            _masterPeakL.value = _masterPeakL.value * 0.7f + peakMaxL * 0.3f
            _masterPeakR.value = _masterPeakR.value * 0.7f + peakMaxR * 0.3f
            _trackPeaks.value = currentTrackPeaks.clone()

            // 6. Stream PCM shorts into Android AudioTrack
            audioTrack?.write(outShorts, 0, outShorts.size)
        }
    }
}
