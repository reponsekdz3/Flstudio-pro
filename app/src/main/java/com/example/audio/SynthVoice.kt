package com.example.audio

import com.example.model.Channel
import com.example.model.FilterType
import com.example.model.InstrumentType
import com.example.model.NoteEvent
import com.example.model.OscType
import kotlin.math.*
import kotlin.random.Random

/**
 * High-performance polyphonic sound synthesizer generating PCM float samples
 */
class SynthVoice(private val sampleRate: Float = 44100f) {

    private val biquad = BiquadFilter(sampleRate)

    companion object {
        fun midiToFreq(midiNote: Int): Float {
            return (440.0 * 2.0.pow((midiNote - 69) / 12.0)).toFloat()
        }
    }

    /**
     * Synthesizes audio samples for a triggered drum or melodic voice
     */
    fun renderVoice(
        channel: Channel,
        note: NoteEvent?,
        totalSamples: Int,
        outputL: FloatArray,
        outputR: FloatArray,
        velocity: Float = 1.0f
    ) {
        when (channel.type) {
            InstrumentType.KICK,
            InstrumentType.AMAPIANO_KICK,
            InstrumentType.POP_SUB_KICK,
            InstrumentType.EDM_DROP_KICK,
            InstrumentType.KICK_909_HEAVY,
            InstrumentType.KICK_ACOUSTIC_STUDIO,
            InstrumentType.KICK_TRAP_SHORT,
            InstrumentType.KICK_SUB_HEAVY,
            InstrumentType.KICK_AMAPIANO_DEEP,
            InstrumentType.KICK_EDM_PUNCH,
            InstrumentType.KICK_LOFI_WARM,
            InstrumentType.KICK_DRILL_KNOCK,
            InstrumentType.KICK_PUNCHY_POP,
            InstrumentType.KICK_GABBER_DISTORTED -> renderKick(channel, totalSamples, outputL, outputR, velocity)

            InstrumentType.SNARE,
            InstrumentType.POP_ACOUSTIC_SNARE,
            InstrumentType.TRAP_SNARE_RIM,
            InstrumentType.SNARE_909_PUNCH,
            InstrumentType.SNARE_ACOUSTIC_CRISP,
            InstrumentType.SNARE_TRAP_GHOST,
            InstrumentType.SNARE_DUBSTEP_HEAVY,
            InstrumentType.SNARE_LOFI_BRUSH -> renderSnare(channel, totalSamples, outputL, outputR, velocity)

            InstrumentType.CLAP,
            InstrumentType.AMAPIANO_CLAP_ROLL,
            InstrumentType.CLAP_LAYERED_STADIUM,
            InstrumentType.CLAP_RETRO_80S,
            InstrumentType.CLAP_SNAP_FINGER -> renderClap(channel, totalSamples, outputL, outputR, velocity)

            InstrumentType.HIHAT_CLOSED,
            InstrumentType.HIHAT_OPEN,
            InstrumentType.TRAP_HIHAT_ROLL,
            InstrumentType.HIHAT_TRAP_REVERSE,
            InstrumentType.RIDE_JAZZ_CYMBAL -> renderHiHat(channel.type == InstrumentType.HIHAT_OPEN || channel.type == InstrumentType.RIDE_JAZZ_CYMBAL, channel, totalSamples, outputL, outputR, velocity)

            InstrumentType.AMAPIANO_SHAKER -> renderAmapianoShaker(channel, totalSamples, outputL, outputR, velocity)
            InstrumentType.AMAPIANO_WHISTLE -> renderAmapianoWhistle(channel, note, totalSamples, outputL, outputR, velocity)
            InstrumentType.AFRO_PERCUSSION,
            InstrumentType.PERC_CONGA_HIGH,
            InstrumentType.PERC_BONGO_ROLL,
            InstrumentType.PERC_TAMBOURINE,
            InstrumentType.PERC_TIMBALE_LATIN,
            InstrumentType.TOM_FLOOR_BOOM,
            InstrumentType.TOM_RACK_DRUM -> renderAfroPercussion(channel, totalSamples, outputL, outputR, velocity)

            InstrumentType.PERC_COWBELL_808 -> renderSynth(channel, note ?: NoteEvent(pitch = 72), totalSamples, outputL, outputR, velocity)

            InstrumentType.BASS_808,
            InstrumentType.DRAKE_DISTORTED_808,
            InstrumentType.DRILL_808_SLIDE,
            InstrumentType.SYNTH_SUB_BASS -> render808(channel, note, totalSamples, outputL, outputR, velocity)

            InstrumentType.AMAPIANO_LOG_DRUM -> renderAmapianoLogDrum(channel, note, totalSamples, outputL, outputR, velocity)
            InstrumentType.EDM_SIDECHAIN_BASS -> renderEdmSidechainBass(channel, note, totalSamples, outputL, outputR, velocity)

            InstrumentType.SYNTH_LEAD,
            InstrumentType.PLUCK,
            InstrumentType.EDM_PLUCK,
            InstrumentType.BELL_CHIME -> renderSynth(channel, note, totalSamples, outputL, outputR, velocity)

            InstrumentType.EDM_SUPERSAW -> renderEdmSupersaw(channel, note, totalSamples, outputL, outputR, velocity)
            InstrumentType.POP_BRIGHT_SYNTH -> renderPopBrightSynth(channel, note, totalSamples, outputL, outputR, velocity)
            InstrumentType.DRAKE_UNDERWATER_KEYS -> renderDrakeUnderwaterKeys(channel, note, totalSamples, outputL, outputR, velocity)
            InstrumentType.SYNTH_PAD,
            InstrumentType.DRAKE_VOCAL_PAD -> renderSynthPad(channel, note, totalSamples, outputL, outputR, velocity)
            InstrumentType.BRASS_STAB -> renderBrassStab(channel, note, totalSamples, outputL, outputR, velocity)
            InstrumentType.POP_VOCAL_LEAD -> renderPopVocalLead(channel, note, totalSamples, outputL, outputR, velocity)

            InstrumentType.GUITAR_ELECTRIC_CLEAN,
            InstrumentType.GUITAR_DISTORTION,
            InstrumentType.GUITAR_ACOUSTIC,
            InstrumentType.GUITAR_NYLON,
            InstrumentType.GUITAR_FUNK,
            InstrumentType.GUITAR_BASS -> renderGuitar(channel, note, totalSamples, outputL, outputR, velocity)

            InstrumentType.MIC_SAMPLE -> renderSample(channel, totalSamples, outputL, outputR, velocity)
            InstrumentType.FX_RISER -> renderFxRiser(channel, totalSamples, outputL, outputR, velocity)
            InstrumentType.FX_CRASH -> renderFxCrash(channel, totalSamples, outputL, outputR, velocity)
        }
    }

    private fun renderKick(
        channel: Channel,
        totalSamples: Int,
        outputL: FloatArray,
        outputR: FloatArray,
        velocity: Float
    ) {
        val duration = min(totalSamples, (sampleRate * 0.35f).toInt())
        var phase = 0.0
        val baseFreq = 48.0 * 2.0.pow(channel.pitchSemi / 12.0)

        for (i in 0 until duration) {
            val t = i.toFloat() / duration
            // Pitch envelope drops quickly from 160Hz down to baseFreq
            val pitchEnv = exp(-t * 18.0)
            val currentFreq = baseFreq + (160.0 * pitchEnv)
            phase += 2.0 * Math.PI * currentFreq / sampleRate
            if (phase > 2.0 * Math.PI) phase -= 2.0 * Math.PI

            // Amplitude envelope
            val ampEnv = (1.0f - t).pow(2.2f)
            // Initial click transient
            val click = if (i < 80) (1.0f - i / 80f) * 0.4f * Random.nextFloat() else 0.0f

            var sample = (sin(phase).toFloat() * 0.9f + click) * ampEnv * velocity * channel.volume
            // Soft saturation / drive
            sample = tanh(sample * 1.4f)

            val panL = (1f - channel.pan).coerceIn(0f, 1f)
            val panR = (1f + channel.pan).coerceIn(0f, 1f)

            outputL[i] += sample * panL
            outputR[i] += sample * panR
        }
    }

    private fun renderSnare(
        channel: Channel,
        totalSamples: Int,
        outputL: FloatArray,
        outputR: FloatArray,
        velocity: Float
    ) {
        val duration = min(totalSamples, (sampleRate * 0.28f).toInt())
        var tonePhase = 0.0
        val toneFreq = 185.0 * 2.0.pow(channel.pitchSemi / 12.0)

        for (i in 0 until duration) {
            val t = i.toFloat() / duration
            // Tone body
            tonePhase += 2.0 * Math.PI * toneFreq / sampleRate
            val tone = sin(tonePhase).toFloat() * exp(-t * 22f) * 0.4f

            // Noise snare crack
            val noise = (Random.nextFloat() * 2f - 1f) * exp(-t * 12f) * 0.6f

            var sample = (tone + noise) * velocity * channel.volume
            sample = tanh(sample * 1.3f)

            val panL = (1f - channel.pan).coerceIn(0f, 1f)
            val panR = (1f + channel.pan).coerceIn(0f, 1f)

            outputL[i] += sample * panL
            outputR[i] += sample * panR
        }
    }

    private fun renderClap(
        channel: Channel,
        totalSamples: Int,
        outputL: FloatArray,
        outputR: FloatArray,
        velocity: Float
    ) {
        val duration = min(totalSamples, (sampleRate * 0.32f).toInt())
        // Staggered impulses at 0ms, 12ms, 25ms, then main decay
        val pulse1 = 0
        val pulse2 = (sampleRate * 0.012f).toInt()
        val pulse3 = (sampleRate * 0.024f).toInt()

        for (i in 0 until duration) {
            val t = i.toFloat() / duration
            val noise = Random.nextFloat() * 2f - 1f
            var env = 0f

            if (i >= pulse3) {
                val tMain = (i - pulse3).toFloat() / (duration - pulse3)
                env = exp(-tMain * 14f) * 0.8f
            } else if (i >= pulse2) {
                val t2 = (i - pulse2).toFloat() / (pulse3 - pulse2)
                env = exp(-t2 * 10f) * 0.5f
            } else if (i >= pulse1) {
                val t1 = i.toFloat() / pulse2
                env = exp(-t1 * 10f) * 0.4f
            }

            var sample = noise * env * velocity * channel.volume
            sample = tanh(sample * 1.2f)

            val panL = (1f - channel.pan).coerceIn(0f, 1f)
            val panR = (1f + channel.pan).coerceIn(0f, 1f)

            outputL[i] += sample * panL
            outputR[i] += sample * panR
        }
    }

    private fun renderHiHat(
        open: Boolean,
        channel: Channel,
        totalSamples: Int,
        outputL: FloatArray,
        outputR: FloatArray,
        velocity: Float
    ) {
        val decayRate = if (open) 6f else 38f
        val duration = min(totalSamples, (sampleRate * (if (open) 0.5f else 0.08f)).toInt())

        // Ring modulation of 6 square waves (classic 808/909 metallic synthesis)
        val freqs = doubleArrayOf(245.0, 306.0, 368.0, 415.0, 542.0, 843.0)
        val phases = DoubleArray(6)

        for (i in 0 until duration) {
            val t = i.toFloat() / duration
            var metallic = 0.0f
            for (f in freqs.indices) {
                phases[f] += 2.0 * Math.PI * freqs[f] / sampleRate
                metallic += if (sin(phases[f]) > 0) 0.16f else -0.16f
            }

            val noise = (Random.nextFloat() * 2f - 1f) * 0.5f
            val env = exp(-t * decayRate)

            var sample = (metallic * 0.5f + noise * 0.5f) * env * velocity * channel.volume
            sample = sample.coerceIn(-1.0f, 1.0f)

            val panL = (1f - channel.pan).coerceIn(0f, 1f)
            val panR = (1f + channel.pan).coerceIn(0f, 1f)

            outputL[i] += sample * panL
            outputR[i] += sample * panR
        }
    }

    private fun render808(
        channel: Channel,
        note: NoteEvent?,
        totalSamples: Int,
        outputL: FloatArray,
        outputR: FloatArray,
        velocity: Float
    ) {
        val midiPitch = (note?.pitch ?: 36) + channel.pitchSemi
        val baseFreq = midiToFreq(midiPitch)
        val duration = min(totalSamples, (sampleRate * 1.5f).toInt())

        var phase = 0.0
        for (i in 0 until duration) {
            val t = i.toFloat() / duration
            // Rapid pitch envelope at onset for that punchy 808 thump
            val pitchEnv = exp(-t * 40.0) * 45.0
            val curFreq = baseFreq + pitchEnv

            phase += 2.0 * Math.PI * curFreq / sampleRate
            if (phase > 2.0 * Math.PI) phase -= 2.0 * Math.PI

            // Amplitude envelope: strong sustain, gentle decay
            val ampEnv = exp(-t * 2.8f)

            // Sine with slight triangle harmonic
            val fundamental = sin(phase).toFloat()
            val harmonic = sin(phase * 2.0).toFloat() * 0.15f
            var sample = (fundamental + harmonic) * ampEnv * velocity * channel.volume

            // Warm saturation
            sample = tanh(sample * 1.8f)

            val panL = (1f - channel.pan).coerceIn(0f, 1f)
            val panR = (1f + channel.pan).coerceIn(0f, 1f)

            outputL[i] += sample * panL
            outputR[i] += sample * panR
        }
    }

    private fun renderSynth(
        channel: Channel,
        note: NoteEvent?,
        totalSamples: Int,
        outputL: FloatArray,
        outputR: FloatArray,
        velocity: Float
    ) {
        val midiPitch = (note?.pitch ?: 60) + channel.pitchSemi
        val baseFreq = midiToFreq(midiPitch)
        val detuneRatio = 2.0.pow(channel.detuneCents / 1200.0)
        val freq2 = baseFreq * detuneRatio

        val noteDurationSamples = if (note != null) {
            // duration in steps
            ((60f / 128f / 4f) * note.durationSteps * sampleRate).toInt().coerceAtMost(totalSamples)
        } else {
            (sampleRate * 0.8f).toInt().coerceAtMost(totalSamples)
        }

        biquad.reset()
        val filterType = when (channel.filterType) {
            FilterType.LOW_PASS -> BiquadFilter.Type.LOW_PASS
            FilterType.HIGH_PASS -> BiquadFilter.Type.HIGH_PASS
            FilterType.BAND_PASS -> BiquadFilter.Type.BAND_PASS
        }
        biquad.updateCoefficients(filterType, channel.cutoffHz, channel.resonanceQ)

        var phase1 = 0.0
        var phase2 = 0.0

        val attackSamples = (channel.attackMs / 1000f * sampleRate).toInt().coerceAtLeast(1)
        val decaySamples = (channel.decayMs / 1000f * sampleRate).toInt().coerceAtLeast(1)
        val releaseSamples = (channel.releaseMs / 1000f * sampleRate).toInt().coerceAtLeast(1)

        val totalDuration = min(totalSamples, noteDurationSamples + releaseSamples)

        for (i in 0 until totalDuration) {
            // ADSR Envelope
            val env = if (i < attackSamples) {
                i.toFloat() / attackSamples
            } else if (i < attackSamples + decaySamples) {
                val tDecay = (i - attackSamples).toFloat() / decaySamples
                1.0f - (1.0f - channel.sustainLevel) * tDecay
            } else if (i < noteDurationSamples) {
                channel.sustainLevel
            } else {
                val tRelease = (i - noteDurationSamples).toFloat() / releaseSamples
                (channel.sustainLevel * (1.0f - tRelease)).coerceAtLeast(0f)
            }

            phase1 += 2.0 * Math.PI * baseFreq / sampleRate
            if (phase1 > 2.0 * Math.PI) phase1 -= 2.0 * Math.PI

            phase2 += 2.0 * Math.PI * freq2 / sampleRate
            if (phase2 > 2.0 * Math.PI) phase2 -= 2.0 * Math.PI

            val s1 = generateOsc(channel.osc1Type, phase1)
            val s2 = generateOsc(channel.osc2Type, phase2)
            val mixed = s1 * (1f - channel.osc2Mix) + s2 * channel.osc2Mix

            var filtered = biquad.processLeft(mixed * env)
            filtered = tanh(filtered * 1.2f) * velocity * channel.volume

            val panL = (1f - channel.pan).coerceIn(0f, 1f)
            val panR = (1f + channel.pan).coerceIn(0f, 1f)

            outputL[i] += filtered * panL
            outputR[i] += filtered * panR
        }
    }

    private fun renderSample(
        channel: Channel,
        totalSamples: Int,
        outputL: FloatArray,
        outputR: FloatArray,
        velocity: Float
    ) {
        val buffer = channel.sampleBuffer ?: return
        val len = min(totalSamples, buffer.size)

        for (i in 0 until len) {
            val s = buffer[i] * velocity * channel.volume
            val panL = (1f - channel.pan).coerceIn(0f, 1f)
            val panR = (1f + channel.pan).coerceIn(0f, 1f)

            outputL[i] += s * panL
            outputR[i] += s * panR
        }
    }

    private fun renderGuitar(
        channel: Channel,
        note: NoteEvent?,
        totalSamples: Int,
        outputL: FloatArray,
        outputR: FloatArray,
        velocity: Float
    ) {
        val defaultMidi = when (channel.type) {
            InstrumentType.GUITAR_BASS -> 40 // E1
            InstrumentType.GUITAR_DISTORTION -> 52 // E2
            InstrumentType.GUITAR_ACOUSTIC -> 60 // C3
            InstrumentType.GUITAR_NYLON -> 64 // E3
            InstrumentType.GUITAR_FUNK -> 60 // C3
            else -> 64 // E3
        }
        val midiPitch = (note?.pitch ?: defaultMidi) + channel.pitchSemi
        val baseFreq = midiToFreq(midiPitch).coerceIn(40f, 4200f)

        // Karplus-Strong string period in samples
        val periodSamples = (sampleRate / baseFreq).toInt().coerceIn(6, 2048)
        val ringBuffer = FloatArray(periodSamples)

        // Excitation impulse depending on guitar type
        when (channel.type) {
            InstrumentType.GUITAR_ACOUSTIC -> {
                // Steel pick: bright high-energy noise burst + triangular attack
                for (i in 0 until periodSamples) {
                    val t = i.toFloat() / periodSamples
                    val noise = Random.nextFloat() * 2f - 1f
                    val triangle = if (t < 0.2f) t / 0.2f else (1f - t) / 0.8f
                    ringBuffer[i] = (triangle * 0.7f + noise * 0.3f)
                }
            }
            InstrumentType.GUITAR_DISTORTION -> {
                // High energy raw pick stroke with rich harmonics
                for (i in 0 until periodSamples) {
                    val t = i.toFloat() / periodSamples
                    val saw = (1f - 2f * t)
                    val noise = (Random.nextFloat() * 2f - 1f) * 0.4f
                    ringBuffer[i] = (saw + noise)
                }
            }
            InstrumentType.GUITAR_NYLON -> {
                // Soft rounded pluck
                for (i in 0 until periodSamples) {
                    val t = i.toFloat() / periodSamples
                    ringBuffer[i] = sin(Math.PI * t).toFloat() * (1f - 0.2f * Random.nextFloat())
                }
            }
            InstrumentType.GUITAR_FUNK -> {
                // Muted transient chank
                for (i in 0 until periodSamples) {
                    val noise = Random.nextFloat() * 2f - 1f
                    val env = exp(-i * 12.0 / periodSamples).toFloat()
                    ringBuffer[i] = noise * env
                }
            }
            InstrumentType.GUITAR_BASS -> {
                // Warm thumb pluck
                for (i in 0 until periodSamples) {
                    val t = i.toFloat() / periodSamples
                    ringBuffer[i] = sin(Math.PI * t).toFloat() + 0.3f * sin(2.0 * Math.PI * t).toFloat()
                }
            }
            else -> { // GUITAR_ELECTRIC_CLEAN
                for (i in 0 until periodSamples) {
                    val t = i.toFloat() / periodSamples
                    val noise = (Random.nextFloat() * 2f - 1f) * 0.25f
                    val pulse = if (t < 0.3f) 1.0f else -0.5f
                    ringBuffer[i] = (pulse * 0.7f + noise)
                }
            }
        }

        // Karplus-Strong parameters
        val dampingFactor = when (channel.type) {
            InstrumentType.GUITAR_FUNK -> 0.93f // Fast palm-muted decay
            InstrumentType.GUITAR_NYLON -> 0.985f
            InstrumentType.GUITAR_DISTORTION -> 0.996f // Extreme sustain
            InstrumentType.GUITAR_ACOUSTIC -> 0.993f
            InstrumentType.GUITAR_BASS -> 0.994f
            else -> 0.991f // Electric clean
        }

        val durationSamples = if (channel.type == InstrumentType.GUITAR_FUNK) {
            min(totalSamples, (sampleRate * 0.45f).toInt())
        } else {
            min(totalSamples, (sampleRate * 2.2f).toInt())
        }

        var readIndex = 0
        var prevSample = 0.0f
        var bodyResonance = 0.0f

        val panL = (1f - channel.pan).coerceIn(0f, 1f)
        val panR = (1f + channel.pan).coerceIn(0f, 1f)

        for (i in 0 until durationSamples) {
            val current = ringBuffer[readIndex]
            // Karplus-Strong lowpass averaging filter with damping
            val newSample = (current + prevSample) * 0.5f * dampingFactor
            ringBuffer[readIndex] = newSample
            prevSample = current

            readIndex++
            if (readIndex >= periodSamples) readIndex = 0

            var outputSample = newSample

            // Apply guitar-specific timbre/amp/body processing
            when (channel.type) {
                InstrumentType.GUITAR_DISTORTION -> {
                    // Marshall stack overdrive emulation: asymmetric soft clipping + mid boost
                    var driven = outputSample * 5.0f
                    driven = tanh(driven) + 0.15f * (driven * driven).coerceIn(-0.5f, 0.5f)
                    outputSample = driven * 0.75f
                }
                InstrumentType.GUITAR_ACOUSTIC -> {
                    // Body cavity resonance
                    bodyResonance = bodyResonance * 0.92f + outputSample * 0.12f
                    outputSample = outputSample * 0.85f + bodyResonance * 0.35f
                }
                InstrumentType.GUITAR_BASS -> {
                    // Warm low-end saturation
                    outputSample = tanh(outputSample * 1.6f) * 1.1f
                }
                InstrumentType.GUITAR_ELECTRIC_CLEAN -> {
                    // Strat pickup resonance & subtle warm drive
                    outputSample = tanh(outputSample * 1.4f)
                }
                else -> {}
            }

            val finalSample = outputSample * velocity * channel.volume
            outputL[i] += finalSample * panL
            outputR[i] += finalSample * panR
        }
    }

    private fun renderAmapianoLogDrum(
        channel: Channel,
        note: NoteEvent?,
        totalSamples: Int,
        outputL: FloatArray,
        outputR: FloatArray,
        velocity: Float
    ) {
        val midiPitch = (note?.pitch ?: 38) + channel.pitchSemi
        val baseFreq = midiToFreq(midiPitch)
        val duration = min(totalSamples, (sampleRate * 1.1f).toInt())
        var phase1 = 0.0
        var phase2 = 0.0
        val woodResonanceFreq = baseFreq * 2.85 // Hollow resonant wood harmonic

        val panL = (1f - channel.pan).coerceIn(0f, 1f)
        val panR = (1f + channel.pan).coerceIn(0f, 1f)

        for (i in 0 until duration) {
            val t = i.toFloat() / duration
            // Pitch slide: rapid downward transient envelope, then steady resonant body
            val pitchEnv = exp(-t * 30.0) * 80.0
            val curFreq = baseFreq + pitchEnv

            phase1 += 2.0 * Math.PI * curFreq / sampleRate
            phase2 += 2.0 * Math.PI * woodResonanceFreq / sampleRate
            if (phase1 > 2.0 * Math.PI) phase1 -= 2.0 * Math.PI
            if (phase2 > 2.0 * Math.PI) phase2 -= 2.0 * Math.PI

            // Wooden thud transient on attack
            val click = if (i < 120) (1.0f - i / 120f) * (Random.nextFloat() * 2f - 1f) * 0.45f else 0.0f
            val subFund = sin(phase1).toFloat()
            val woodHarmonic = sin(phase2).toFloat() * exp(-t * 8.0f) * 0.65f

            // Amapiano log drum saturation with soft clipping
            val raw = (subFund * 0.9f + woodHarmonic + click) * exp(-t * 3.2f)
            val driven = tanh(raw * 2.2f) * velocity * channel.volume

            outputL[i] += driven * panL
            outputR[i] += driven * panR
        }
    }

    private fun renderAmapianoShaker(
        channel: Channel,
        totalSamples: Int,
        outputL: FloatArray,
        outputR: FloatArray,
        velocity: Float
    ) {
        val duration = min(totalSamples, (sampleRate * 0.22f).toInt())
        val panL = (1f - channel.pan).coerceIn(0f, 1f)
        val panR = (1f + channel.pan).coerceIn(0f, 1f)
        var lpFilter = 0.0f

        for (i in 0 until duration) {
            val t = i.toFloat() / duration
            val noise = Random.nextFloat() * 2f - 1f
            // Two shaker pulses per 16th hit (accent + shuffle)
            val env = (sin(t * Math.PI).toFloat().pow(1.5f)) * exp(-t * 5.0f)
            // Bandpass filter for authentic shaker bead sound
            lpFilter = lpFilter * 0.72f + noise * 0.28f
            val sample = (noise - lpFilter) * env * velocity * channel.volume * 0.8f

            outputL[i] += sample * panL
            outputR[i] += sample * panR
        }
    }

    private fun renderAmapianoWhistle(
        channel: Channel,
        note: NoteEvent?,
        totalSamples: Int,
        outputL: FloatArray,
        outputR: FloatArray,
        velocity: Float
    ) {
        val midiPitch = (note?.pitch ?: 72) + channel.pitchSemi
        val baseFreq = midiToFreq(midiPitch)
        val duration = min(totalSamples, (sampleRate * 0.4f).toInt())
        var phase1 = 0.0
        var phase2 = 0.0
        val panL = (1f - channel.pan).coerceIn(0f, 1f)
        val panR = (1f + channel.pan).coerceIn(0f, 1f)

        for (i in 0 until duration) {
            val t = i.toFloat() / duration
            val vibrato = sin(2.0 * Math.PI * 18.0 * i / sampleRate) * 20.0
            val curFreq = baseFreq + vibrato

            phase1 += 2.0 * Math.PI * curFreq / sampleRate
            phase2 += 2.0 * Math.PI * (curFreq * 1.05) / sampleRate
            val breathNoise = (Random.nextFloat() * 2f - 1f) * 0.12f

            val sound = (sin(phase1).toFloat() * 0.6f + sin(phase2).toFloat() * 0.4f + breathNoise)
            val env = sin(t * Math.PI.toFloat()).coerceAtLeast(0f) * exp(-t * 2.5f)
            val sample = sound * env * velocity * channel.volume

            outputL[i] += sample * panL
            outputR[i] += sample * panR
        }
    }

    private fun renderAfroPercussion(
        channel: Channel,
        totalSamples: Int,
        outputL: FloatArray,
        outputR: FloatArray,
        velocity: Float
    ) {
        val duration = min(totalSamples, (sampleRate * 0.28f).toInt())
        var phase = 0.0
        val baseFreq = 180.0 * 2.0.pow(channel.pitchSemi / 12.0)
        val panL = (1f - channel.pan).coerceIn(0f, 1f)
        val panR = (1f + channel.pan).coerceIn(0f, 1f)

        for (i in 0 until duration) {
            val t = i.toFloat() / duration
            val pitchEnv = exp(-t * 22.0) * 110.0
            phase += 2.0 * Math.PI * (baseFreq + pitchEnv) / sampleRate

            val slapClick = if (i < 90) (1f - i / 90f) * 0.4f * (Random.nextFloat() * 2f - 1f) else 0f
            val body = sin(phase).toFloat() * exp(-t * 9.0f)
            val sample = (body + slapClick) * velocity * channel.volume

            outputL[i] += sample * panL
            outputR[i] += sample * panR
        }
    }

    private fun renderEdmSidechainBass(
        channel: Channel,
        note: NoteEvent?,
        totalSamples: Int,
        outputL: FloatArray,
        outputR: FloatArray,
        velocity: Float
    ) {
        val midiPitch = (note?.pitch ?: 40) + channel.pitchSemi
        val baseFreq = midiToFreq(midiPitch)
        val duration = min(totalSamples, (sampleRate * 0.8f).toInt())
        var phase1 = 0.0
        var phase2 = 0.0
        val panL = (1f - channel.pan).coerceIn(0f, 1f)
        val panR = (1f + channel.pan).coerceIn(0f, 1f)

        for (i in 0 until duration) {
            val t = i.toFloat() / duration
            phase1 += 2.0 * Math.PI * baseFreq / sampleRate
            phase2 += 2.0 * Math.PI * (baseFreq * 1.008) / sampleRate // Detuned stereo width

            val saw1 = (1.0 - (phase1 / Math.PI)).toFloat()
            val saw2 = (1.0 - (phase2 / Math.PI)).toFloat()

            // Sidechain pumping curve: volume ducks during kick hit (t < 0.15) then sweeps up
            val sidechainPump = (1.0f - exp(-t * 18.0f)).coerceIn(0.1f, 1.0f)
            val ampEnv = exp(-t * 2.0f)

            val sample = (saw1 * 0.5f + saw2 * 0.5f) * sidechainPump * ampEnv * velocity * channel.volume
            val filtered = tanh(sample * 1.7f)

            outputL[i] += filtered * panL
            outputR[i] += filtered * panR
        }
    }

    private fun renderEdmSupersaw(
        channel: Channel,
        note: NoteEvent?,
        totalSamples: Int,
        outputL: FloatArray,
        outputR: FloatArray,
        velocity: Float
    ) {
        val midiPitch = (note?.pitch ?: 64) + channel.pitchSemi
        val baseFreq = midiToFreq(midiPitch)
        val duration = min(totalSamples, (sampleRate * 1.0f).toInt())
        val detuneRatios = doubleArrayOf(0.985, 0.992, 0.997, 1.0, 1.003, 1.008, 1.015)
        val phases = DoubleArray(7)

        val panL = (1f - channel.pan).coerceIn(0f, 1f)
        val panR = (1f + channel.pan).coerceIn(0f, 1f)

        for (i in 0 until duration) {
            val t = i.toFloat() / duration
            var leftAccum = 0.0f
            var rightAccum = 0.0f

            for (idx in detuneRatios.indices) {
                phases[idx] += 2.0 * Math.PI * (baseFreq * detuneRatios[idx]) / sampleRate
                if (phases[idx] > 2.0 * Math.PI) phases[idx] -= 2.0 * Math.PI
                val saw = (1.0 - (phases[idx] / Math.PI)).toFloat()

                // Stereo spread across oscillators
                val p = idx.toFloat() / (detuneRatios.size - 1)
                leftAccum += saw * (1f - p)
                rightAccum += saw * p
            }

            val env = exp(-t * 2.2f)
            val sampleL = leftAccum * 0.35f * env * velocity * channel.volume
            val sampleR = rightAccum * 0.35f * env * velocity * channel.volume

            outputL[i] += sampleL * panL
            outputR[i] += sampleR * panR
        }
    }

    private fun renderPopBrightSynth(
        channel: Channel,
        note: NoteEvent?,
        totalSamples: Int,
        outputL: FloatArray,
        outputR: FloatArray,
        velocity: Float
    ) {
        val midiPitch = (note?.pitch ?: 60) + channel.pitchSemi
        val baseFreq = midiToFreq(midiPitch)
        val duration = min(totalSamples, (sampleRate * 0.9f).toInt())
        var phase1 = 0.0
        var phase2 = 0.0
        val panL = (1f - channel.pan).coerceIn(0f, 1f)
        val panR = (1f + channel.pan).coerceIn(0f, 1f)

        for (i in 0 until duration) {
            val t = i.toFloat() / duration
            phase1 += 2.0 * Math.PI * baseFreq / sampleRate
            phase2 += 2.0 * Math.PI * (baseFreq * 2.002) / sampleRate

            val saw = (1.0 - (phase1 / Math.PI)).toFloat()
            val pulse = if (phase2 < Math.PI) 0.8f else -0.8f

            val brassFilter = (1.0f + 2.0f * exp(-t * 12.0f)) // Bright initial brass attack
            val sample = (saw * 0.6f + pulse * 0.4f) * brassFilter * exp(-t * 2.4f) * velocity * channel.volume

            outputL[i] += sample * panL
            outputR[i] += sample * panR
        }
    }

    private fun renderDrakeUnderwaterKeys(
        channel: Channel,
        note: NoteEvent?,
        totalSamples: Int,
        outputL: FloatArray,
        outputR: FloatArray,
        velocity: Float
    ) {
        val midiPitch = (note?.pitch ?: 62) + channel.pitchSemi
        val baseFreq = midiToFreq(midiPitch)
        val duration = min(totalSamples, (sampleRate * 1.4f).toInt())
        var phase = 0.0
        var lpPrev = 0.0f
        val panL = (1f - channel.pan).coerceIn(0f, 1f)
        val panR = (1f + channel.pan).coerceIn(0f, 1f)

        for (i in 0 until duration) {
            val t = i.toFloat() / duration
            // Subtle lo-fi flutter
            val flutter = sin(2.0 * Math.PI * 4.5 * i / sampleRate) * 1.2
            phase += 2.0 * Math.PI * (baseFreq + flutter) / sampleRate

            // Electric piano tine + body
            val tine = sin(phase).toFloat() * 0.7f + sin(phase * 2.0).toFloat() * 0.25f + sin(phase * 3.0).toFloat() * 0.1f
            // Severe low-pass filtering at ~650Hz (signature underwater sound)
            lpPrev = lpPrev * 0.88f + tine * 0.12f

            val env = exp(-t * 1.8f)
            val sample = lpPrev * env * velocity * channel.volume * 1.4f

            outputL[i] += sample * panL
            outputR[i] += sample * panR
        }
    }

    private fun renderSynthPad(
        channel: Channel,
        note: NoteEvent?,
        totalSamples: Int,
        outputL: FloatArray,
        outputR: FloatArray,
        velocity: Float
    ) {
        val midiPitch = (note?.pitch ?: 57) + channel.pitchSemi
        val baseFreq = midiToFreq(midiPitch)
        val duration = min(totalSamples, (sampleRate * 1.6f).toInt())
        var phase1 = 0.0
        var phase2 = 0.0
        val panL = (1f - channel.pan).coerceIn(0f, 1f)
        val panR = (1f + channel.pan).coerceIn(0f, 1f)

        for (i in 0 until duration) {
            val t = i.toFloat() / duration
            phase1 += 2.0 * Math.PI * baseFreq / sampleRate
            phase2 += 2.0 * Math.PI * (baseFreq * 1.004) / sampleRate

            val s1 = sin(phase1).toFloat()
            val s2 = sin(phase2).toFloat()

            // Smooth slow attack and gentle release
            val attack = (t * 8f).coerceAtMost(1f)
            val env = attack * exp(-t * 1.2f)
            val sample = (s1 * 0.5f + s2 * 0.5f) * env * velocity * channel.volume

            outputL[i] += sample * panL
            outputR[i] += sample * panR
        }
    }

    private fun renderBrassStab(
        channel: Channel,
        note: NoteEvent?,
        totalSamples: Int,
        outputL: FloatArray,
        outputR: FloatArray,
        velocity: Float
    ) {
        val midiPitch = (note?.pitch ?: 55) + channel.pitchSemi
        val baseFreq = midiToFreq(midiPitch)
        val duration = min(totalSamples, (sampleRate * 0.5f).toInt())
        var phase = 0.0
        val panL = (1f - channel.pan).coerceIn(0f, 1f)
        val panR = (1f + channel.pan).coerceIn(0f, 1f)

        for (i in 0 until duration) {
            val t = i.toFloat() / duration
            phase += 2.0 * Math.PI * baseFreq / sampleRate
            val saw = (1.0 - (phase / Math.PI)).toFloat()
            val env = exp(-t * 5.5f)
            val sample = tanh(saw * 1.8f) * env * velocity * channel.volume

            outputL[i] += sample * panL
            outputR[i] += sample * panR
        }
    }

    private fun renderPopVocalLead(
        channel: Channel,
        note: NoteEvent?,
        totalSamples: Int,
        outputL: FloatArray,
        outputR: FloatArray,
        velocity: Float
    ) {
        val midiPitch = (note?.pitch ?: 69) + channel.pitchSemi
        val baseFreq = midiToFreq(midiPitch)
        val duration = min(totalSamples, (sampleRate * 0.75f).toInt())
        var phase = 0.0
        val panL = (1f - channel.pan).coerceIn(0f, 1f)
        val panR = (1f + channel.pan).coerceIn(0f, 1f)

        // Formant frequencies for "Ah" / "Oh" pop vowel
        val f1 = 800.0
        val f2 = 1200.0

        for (i in 0 until duration) {
            val t = i.toFloat() / duration
            val vibrato = sin(2.0 * Math.PI * 5.8 * i / sampleRate) * 3.0
            phase += 2.0 * Math.PI * (baseFreq + vibrato) / sampleRate

            val pulse = if (phase < Math.PI) 1.0f else -1.0f
            val formant = sin(2.0 * Math.PI * f1 * i / sampleRate).toFloat() * 0.5f +
                    sin(2.0 * Math.PI * f2 * i / sampleRate).toFloat() * 0.35f

            val env = exp(-t * 2.8f)
            val sample = (pulse * 0.4f + formant * 0.6f) * env * velocity * channel.volume

            outputL[i] += sample * panL
            outputR[i] += sample * panR
        }
    }

    private fun renderFxRiser(
        channel: Channel,
        totalSamples: Int,
        outputL: FloatArray,
        outputR: FloatArray,
        velocity: Float
    ) {
        val duration = min(totalSamples, (sampleRate * 1.8f).toInt())
        var sinePhase = 0.0
        val panL = (1f - channel.pan).coerceIn(0f, 1f)
        val panR = (1f + channel.pan).coerceIn(0f, 1f)

        for (i in 0 until duration) {
            val t = i.toFloat() / duration
            // Exponential pitch rise from 150Hz to 4000Hz
            val curFreq = 150.0 * 2.0.pow(t * 4.8)
            sinePhase += 2.0 * Math.PI * curFreq / sampleRate

            val noise = Random.nextFloat() * 2f - 1f
            val sine = sin(sinePhase).toFloat()

            // Rising volume swell
            val swell = t.pow(1.8f)
            val sample = (noise * 0.6f + sine * 0.4f) * swell * velocity * channel.volume

            outputL[i] += sample * panL
            outputR[i] += sample * panR
        }
    }

    private fun renderFxCrash(
        channel: Channel,
        totalSamples: Int,
        outputL: FloatArray,
        outputR: FloatArray,
        velocity: Float
    ) {
        val duration = min(totalSamples, (sampleRate * 1.8f).toInt())
        val panL = (1f - channel.pan).coerceIn(0f, 1f)
        val panR = (1f + channel.pan).coerceIn(0f, 1f)

        for (i in 0 until duration) {
            val t = i.toFloat() / duration
            val noise = Random.nextFloat() * 2f - 1f
            val env = exp(-t * 2.0f)
            val sample = noise * env * velocity * channel.volume * 0.75f

            outputL[i] += sample * panL
            outputR[i] += sample * panR
        }
    }

    private fun generateOsc(type: OscType, phase: Double): Float {
        return when (type) {
            OscType.SINE -> sin(phase).toFloat()
            OscType.SAWTOOTH -> (1.0 - (phase / Math.PI)).toFloat()
            OscType.SQUARE -> if (phase < Math.PI) 1.0f else -1.0f
            OscType.TRIANGLE -> {
                val norm = phase / (2.0 * Math.PI)
                (abs(norm * 4.0 - 2.0) - 1.0).toFloat()
            }
            OscType.NOISE -> Random.nextFloat() * 2f - 1f
        }
    }
}
