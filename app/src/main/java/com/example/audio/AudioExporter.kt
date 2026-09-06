package com.example.audio

import android.content.Context
import android.os.Environment
import com.example.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*

enum class ExportAudioFormat(val displayName: String, val bitDepth: Int, val extension: String) {
    WAV_16BIT("16-Bit PCM WAV (CD Audio Master)", 16, ".wav"),
    WAV_24BIT("24-Bit Studio WAV (Hi-Res Pro Mix)", 24, ".wav"),
    WAV_32BIT_FLOAT("32-Bit Float WAV (DAW Interleaved)", 32, ".wav")
}

enum class ExportRange(val displayName: String) {
    FULL_SONG("Full Song (Playlist Arrangement)"),
    CURRENT_PATTERN("Current Pattern (Loop)"),
    ALL_STEMS_MULTI("Separate Track Stems (All Channels)")
}

data class AudioExportResult(
    val file: File,
    val format: ExportAudioFormat,
    val durationSeconds: Float,
    val sampleRate: Int,
    val fileSizeBytes: Long,
    val isStemFolder: Boolean = false,
    val stemFiles: List<File> = emptyList()
)

object AudioExporter {

    private const val DEFAULT_SAMPLE_RATE = 44100
    private const val BUFFER_SIZE = 512

    fun getExportsDirectory(context: Context): File {
        val musicDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        val dir = if (musicDir != null) {
            File(musicDir, "FLStudioExports")
        } else {
            File(context.filesDir, "audio_exports")
        }
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Renders audio offline at maximum CPU speed, generating a bit-perfect WAV audio master
     */
    suspend fun renderAudioOffline(
        context: Context,
        projectName: String,
        bpm: Int,
        swing: Int,
        playMode: PlayMode,
        totalBars: Int,
        channels: List<Channel>,
        patterns: List<Pattern>,
        selectedPatternId: Int,
        playlistClips: List<PatternClip>,
        mixerTracks: List<MixerTrack>,
        format: ExportAudioFormat = ExportAudioFormat.WAV_16BIT,
        range: ExportRange = ExportRange.FULL_SONG,
        sampleRate: Int = DEFAULT_SAMPLE_RATE,
        onProgress: (Float) -> Unit
    ): AudioExportResult = withContext(Dispatchers.Default) {

        val exportsDir = getExportsDirectory(context)
        val cleanName = projectName.trim().replace(Regex("[^a-zA-Z0-9_-]"), "_")

        if (range == ExportRange.ALL_STEMS_MULTI) {
            return@withContext renderStemsOffline(
                context = context,
                exportsDir = exportsDir,
                cleanName = cleanName,
                bpm = bpm,
                swing = swing,
                totalBars = totalBars,
                channels = channels,
                patterns = patterns,
                selectedPatternId = selectedPatternId,
                playlistClips = playlistClips,
                mixerTracks = mixerTracks,
                format = format,
                sampleRate = sampleRate,
                onProgress = onProgress
            )
        }

        // Determine bars to render
        val barsToRender = when (range) {
            ExportRange.CURRENT_PATTERN -> 1
            ExportRange.FULL_SONG -> {
                val maxClipEnd = playlistClips.maxOfOrNull { it.startBar + it.lengthBars } ?: 4
                max(4, maxClipEnd)
            }
            else -> totalBars
        }

        val stepDurationSeconds = (60.0 / bpm) / 4.0 // 16th note
        val totalSteps = barsToRender * 16
        val totalSamples = (totalSteps * stepDurationSeconds * sampleRate).toInt()
        val totalDurationSeconds = totalSamples.toFloat() / sampleRate

        val suffix = when (format) {
            ExportAudioFormat.WAV_16BIT -> "_16bit"
            ExportAudioFormat.WAV_24BIT -> "_24bit"
            ExportAudioFormat.WAV_32BIT_FLOAT -> "_32float"
        }
        val outputFile = File(exportsDir, "${cleanName}${suffix}${format.extension}")

        // Initialize Synth & FX instances
        val synthVoice = SynthVoice(sampleRate.toFloat())
        val eqList = List(130) { ParametricEQ(sampleRate.toFloat()) }
        val delayList = List(130) { DelayFX(sampleRate) }
        val reverbList = List(130) { ReverbFX(sampleRate) }
        val distList = List(130) { DistortionFX() }
        val chorusList = List(130) { ChorusFX(sampleRate) }
        val compList = List(130) { CompressorFX(sampleRate) }

        // Configure FX from mixerTracks
        for (track in mixerTracks) {
            val idx = track.id.coerceIn(0, 129)
            eqList[idx].enabled = track.eqEnabled
            eqList[idx].lowGainDb = track.eqLowGain
            eqList[idx].midGainDb = track.eqMidGain
            eqList[idx].highGainDb = track.eqHighGain

            delayList[idx].enabled = track.delayEnabled
            delayList[idx].timeMs = track.delayTimeMs
            delayList[idx].feedback = track.delayFeedback
            delayList[idx].wetMix = track.delayWet

            reverbList[idx].enabled = track.reverbEnabled
            reverbList[idx].roomSize = track.reverbRoom
            reverbList[idx].wetMix = track.reverbWet

            distList[idx].enabled = track.distEnabled
            distList[idx].drive = track.distDrive
            distList[idx].mix = track.distMix

            chorusList[idx].enabled = track.chorusEnabled
            chorusList[idx].rateHz = track.chorusRate
            chorusList[idx].depth = track.chorusDepth
            chorusList[idx].mix = track.chorusMix

            compList[idx].enabled = track.compEnabled
            compList[idx].thresholdDb = track.compThresholdDb
            compList[idx].ratio = track.compRatio
        }

        val fos = FileOutputStream(outputFile)
        // Write placeholder WAV header (44 bytes)
        val dummyHeader = ByteArray(44)
        fos.write(dummyHeader)

        val samplesPerStep = (stepDurationSeconds * sampleRate).toInt().coerceAtLeast(1)
        val swingOffsetSamples = ((swing / 100f) * (samplesPerStep * 0.35f)).toInt()

        val masterL = FloatArray(BUFFER_SIZE)
        val masterR = FloatArray(BUFFER_SIZE)
        val voiceL = FloatArray(BUFFER_SIZE)
        val voiceR = FloatArray(BUFFER_SIZE)
        val insertL = FloatArray(BUFFER_SIZE)
        val insertR = FloatArray(BUFFER_SIZE)

        var renderedSamples = 0
        var lastStepIndex = -1
        var totalPayloadBytes = 0L

        // Active voices pool during render
        class RenderNoteVoice(
            val channel: Channel,
            val pitch: Int,
            val velocity: Float,
            var samplePosition: Int = 0,
            val durationSamples: Int
        )

        val activeVoices = mutableListOf<RenderNoteVoice>()

        while (renderedSamples < totalSamples) {
            val blockSize = min(BUFFER_SIZE, totalSamples - renderedSamples)

            java.util.Arrays.fill(masterL, 0, blockSize, 0f)
            java.util.Arrays.fill(masterR, 0, blockSize, 0f)

            // Calculate current step
            val currentGlobalStep = renderedSamples / samplesPerStep
            val currentBar = currentGlobalStep / 16
            val stepInBar = currentGlobalStep % 16

            if (currentGlobalStep != lastStepIndex && currentGlobalStep < totalSteps) {
                lastStepIndex = currentGlobalStep

                val isEvenStep = (stepInBar % 2) == 1
                val offset = if (isEvenStep) swingOffsetSamples else 0

                // Trigger channels active at this step
                if (range == ExportRange.CURRENT_PATTERN) {
                    val currentPat = patterns.find { it.id == selectedPatternId } ?: patterns.firstOrNull()
                    for (ch in channels) {
                        if (ch.isMuted) continue
                        val patSteps = currentPat?.channelSteps?.get(ch.id) ?: ch.steps
                        if (stepInBar < patSteps.size && patSteps[stepInBar]) {
                            val notes = currentPat?.channelNotes?.get(ch.id) ?: ch.notes
                            val note = notes.find { it.startStep == stepInBar }
                            val pitch = note?.pitch ?: (60 + ch.pitchSemi)
                            val vel = note?.velocity ?: 0.85f
                            val dur = if (ch.type.isMelodic) (note?.durationSteps ?: 2) * samplesPerStep else (sampleRate * 0.8f).toInt()
                            activeVoices.add(RenderNoteVoice(ch, pitch, vel, -offset, dur))
                        }
                    }
                } else {
                    // FULL SONG: check playlistClips
                    val activeClips = playlistClips.filter {
                        !it.isMuted && currentBar >= it.startBar && currentBar < (it.startBar + it.lengthBars)
                    }

                    for (clip in activeClips) {
                        val pat = patterns.find { it.id == clip.patternIndex } ?: continue
                        for (ch in channels) {
                            if (ch.isMuted) continue
                            val patSteps = pat.channelSteps[ch.id] ?: ch.steps
                            if (stepInBar < patSteps.size && patSteps[stepInBar]) {
                                val notes = pat.channelNotes[ch.id] ?: ch.notes
                                val note = notes.find { it.startStep == stepInBar }
                                val pitch = note?.pitch ?: (60 + ch.pitchSemi)
                                val vel = note?.velocity ?: 0.85f
                                val dur = if (ch.type.isMelodic) (note?.durationSteps ?: 2) * samplesPerStep else (sampleRate * 0.8f).toInt()
                                activeVoices.add(RenderNoteVoice(ch, pitch, vel, -offset, dur))
                            }
                        }
                    }
                }
            }

            // Render active voices into channels and route to inserts
            val channelBuffersL = mutableMapOf<Int, FloatArray>()
            val channelBuffersR = mutableMapOf<Int, FloatArray>()

            val iterator = activeVoices.iterator()
            while (iterator.hasNext()) {
                val voice = iterator.next()
                if (voice.samplePosition + blockSize < 0) {
                    voice.samplePosition += blockSize
                    continue
                }

                java.util.Arrays.fill(voiceL, 0, blockSize, 0f)
                java.util.Arrays.fill(voiceR, 0, blockSize, 0f)

                synthVoice.renderVoice(
                    channel = voice.channel,
                    note = NoteEvent(pitch = voice.pitch, velocity = voice.velocity),
                    totalSamples = blockSize,
                    outputL = voiceL,
                    outputR = voiceR,
                    velocity = voice.velocity
                )

                val trackIdx = voice.channel.mixerTrackIndex.coerceIn(0, 129)
                val bufL = channelBuffersL.getOrPut(trackIdx) { FloatArray(blockSize) }
                val bufR = channelBuffersR.getOrPut(trackIdx) { FloatArray(blockSize) }

                for (i in 0 until blockSize) {
                    bufL[i] += voiceL[i]
                    bufR[i] += voiceR[i]
                }

                voice.samplePosition += blockSize
                if (voice.samplePosition >= voice.durationSamples) {
                    iterator.remove()
                }
            }

            // Process Mixer FX per insert and sum to Master
            for ((trackIdx, bL) in channelBuffersL) {
                val bR = channelBuffersR[trackIdx] ?: continue
                val track = mixerTracks.find { it.id == trackIdx } ?: mixerTracks[0]
                if (track.isMuted) continue

                val eq = eqList[trackIdx]
                val dist = distList[trackIdx]
                val chorus = chorusList[trackIdx]
                val delay = delayList[trackIdx]
                val reverb = reverbList[trackIdx]
                val comp = compList[trackIdx]

                val vol = 10.0.pow(track.volumeDb / 20.0).toFloat()
                val panL = (1f - track.pan).coerceIn(0f, 1f)
                val panR = (1f + track.pan).coerceIn(0f, 1f)

                for (i in 0 until blockSize) {
                    val eqRes = eq.process(bL[i], bR[i])
                    val distRes = dist.process(eqRes.first, eqRes.second)
                    val chorusRes = chorus.process(distRes.first, distRes.second)
                    val delayRes = delay.process(chorusRes.first, chorusRes.second)
                    val revRes = reverb.process(delayRes.first, delayRes.second)
                    val compRes = comp.process(revRes.first, revRes.second)

                    val sL = compRes.first * vol
                    val sR = compRes.second * vol

                    masterL[i] += sL * panL
                    masterR[i] += sR * panR
                }
            }

            // Master channel insert FX
            val masterTrack = mixerTracks.firstOrNull { it.id == 0 } ?: mixerTracks[0]
            val mEq = eqList[0]
            val mRev = reverbList[0]
            val mComp = compList[0]

            for (i in 0 until blockSize) {
                val eqRes = mEq.process(masterL[i], masterR[i])
                val revRes = mRev.process(eqRes.first, eqRes.second)
                val compRes = mComp.process(revRes.first, revRes.second)
                masterL[i] = compRes.first
                masterR[i] = compRes.second
            }

            val mVol = 10.0.pow(masterTrack.volumeDb / 20.0).toFloat()

            // Write PCM to file based on chosen bit depth
            val byteBuffer = ByteBuffer.allocate(blockSize * 2 * (format.bitDepth / 8)).order(ByteOrder.LITTLE_ENDIAN)

            for (i in 0 until blockSize) {
                // Soft clipping limiter
                var sampleL = (masterL[i] * mVol).coerceIn(-1.0f, 1.0f)
                var sampleR = (masterR[i] * mVol).coerceIn(-1.0f, 1.0f)

                sampleL = tanh(sampleL)
                sampleR = tanh(sampleR)

                when (format) {
                    ExportAudioFormat.WAV_16BIT -> {
                        val sL = (sampleL * 32767f).roundToInt().coerceIn(-32768, 32767).toShort()
                        val sR = (sampleR * 32767f).roundToInt().coerceIn(-32768, 32767).toShort()
                        byteBuffer.putShort(sL)
                        byteBuffer.putShort(sR)
                    }
                    ExportAudioFormat.WAV_24BIT -> {
                        val iL = (sampleL * 8388607f).roundToInt().coerceIn(-8388608, 8388607)
                        val iR = (sampleR * 8388607f).roundToInt().coerceIn(-8388608, 8388607)
                        byteBuffer.put((iL and 0xFF).toByte())
                        byteBuffer.put(((iL shr 8) and 0xFF).toByte())
                        byteBuffer.put(((iL shr 16) and 0xFF).toByte())

                        byteBuffer.put((iR and 0xFF).toByte())
                        byteBuffer.put(((iR shr 8) and 0xFF).toByte())
                        byteBuffer.put(((iR shr 16) and 0xFF).toByte())
                    }
                    ExportAudioFormat.WAV_32BIT_FLOAT -> {
                        byteBuffer.putFloat(sampleL)
                        byteBuffer.putFloat(sampleR)
                    }
                }
            }

            fos.write(byteBuffer.array())
            totalPayloadBytes += byteBuffer.array().size
            renderedSamples += blockSize

            val progress = (renderedSamples.toFloat() / totalSamples).coerceIn(0f, 1f)
            onProgress(progress)
        }

        fos.flush()
        fos.close()

        // Patch WAV RIFF header in RandomAccessFile
        writeWavHeader(outputFile, totalPayloadBytes, sampleRate, format.bitDepth, 2)

        AudioExportResult(
            file = outputFile,
            format = format,
            durationSeconds = totalDurationSeconds,
            sampleRate = sampleRate,
            fileSizeBytes = outputFile.length(),
            isStemFolder = false
        )
    }

    private suspend fun renderStemsOffline(
        context: Context,
        exportsDir: File,
        cleanName: String,
        bpm: Int,
        swing: Int,
        totalBars: Int,
        channels: List<Channel>,
        patterns: List<Pattern>,
        selectedPatternId: Int,
        playlistClips: List<PatternClip>,
        mixerTracks: List<MixerTrack>,
        format: ExportAudioFormat,
        sampleRate: Int,
        onProgress: (Float) -> Unit
    ): AudioExportResult = withContext(Dispatchers.Default) {
        val stemFolder = File(exportsDir, "${cleanName}_Stems")
        if (!stemFolder.exists()) stemFolder.mkdirs()

        val stemFiles = mutableListOf<File>()
        val totalChannels = channels.size.coerceAtLeast(1)

        channels.forEachIndexed { index, channel ->
            val stemName = "${index + 1}_${channel.name.trim().replace(Regex("[^a-zA-Z0-9_-]"), "_")}"
            val singleChannelList = listOf(channel)

            // Render single channel
            val result = renderAudioOffline(
                context = context,
                projectName = stemName,
                bpm = bpm,
                swing = swing,
                playMode = PlayMode.SONG,
                totalBars = totalBars,
                channels = singleChannelList,
                patterns = patterns,
                selectedPatternId = selectedPatternId,
                playlistClips = playlistClips,
                mixerTracks = mixerTracks,
                format = format,
                range = ExportRange.FULL_SONG,
                sampleRate = sampleRate,
                onProgress = { p ->
                    val totalProgress = (index + p) / totalChannels
                    onProgress(totalProgress)
                }
            )

            val targetInFolder = File(stemFolder, "${stemName}${format.extension}")
            result.file.renameTo(targetInFolder)
            stemFiles.add(targetInFolder)
        }

        AudioExportResult(
            file = stemFolder,
            format = format,
            durationSeconds = 0f,
            sampleRate = sampleRate,
            fileSizeBytes = stemFiles.sumOf { it.length() },
            isStemFolder = true,
            stemFiles = stemFiles
        )
    }

    /**
     * Writes 44-byte standard RIFF WAVE header
     */
    private fun writeWavHeader(
        file: File,
        audioDataSize: Long,
        sampleRate: Int,
        bitDepth: Int,
        channels: Int
    ) {
        val totalDataLen = audioDataSize + 36
        val byteRate = sampleRate * channels * (bitDepth / 8)
        val blockAlign = channels * (bitDepth / 8)
        val audioFormat = if (bitDepth == 32) 3 else 1 // 1 = PCM, 3 = IEEE Float

        val header = ByteArray(44)
        val bb = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)

        bb.put('R'.code.toByte())
        bb.put('I'.code.toByte())
        bb.put('F'.code.toByte())
        bb.put('F'.code.toByte())
        bb.putInt((totalDataLen and 0xFFFFFFFFL).toInt())
        bb.put('W'.code.toByte())
        bb.put('A'.code.toByte())
        bb.put('V'.code.toByte())
        bb.put('E'.code.toByte())

        // "fmt " subchunk
        bb.put('f'.code.toByte())
        bb.put('m'.code.toByte())
        bb.put('t'.code.toByte())
        bb.put(' '.code.toByte())
        bb.putInt(16) // Subchunk1Size for PCM
        bb.putShort(audioFormat.toShort()) // AudioFormat
        bb.putShort(channels.toShort())
        bb.putInt(sampleRate)
        bb.putInt(byteRate)
        bb.putShort(blockAlign.toShort())
        bb.putShort(bitDepth.toShort())

        // "data" subchunk
        bb.put('d'.code.toByte())
        bb.put('a'.code.toByte())
        bb.put('t'.code.toByte())
        bb.put('a'.code.toByte())
        bb.putInt((audioDataSize and 0xFFFFFFFFL).toInt())

        val raf = RandomAccessFile(file, "rw")
        raf.seek(0)
        raf.write(header)
        raf.close()
    }
}
