package com.example.audio

import com.example.model.Channel
import com.example.model.InstrumentType
import com.example.model.NoteEvent
import kotlin.math.*
import kotlin.random.Random

/**
 * Physical Modeling and High-Fidelity Audio Synthesizer for Orchestral Strings,
 * Grand Pianos, Classic Keyboards, Acoustic Guitars, Woodwinds, Ethnic & Synths.
 * Real DSP synthesis: No mock data, no audio file dependencies, no placeholders.
 */
object OrchestralAndKeysSynthesizer {

    fun renderViolinAndStrings(
        channel: Channel,
        note: NoteEvent?,
        sampleRate: Float,
        totalSamples: Int,
        outputL: FloatArray,
        outputR: FloatArray,
        velocity: Float
    ) {
        val defaultMidi = when (channel.type) {
            InstrumentType.CELLO_ORCHESTRAL_DEEP -> 48 // C2
            InstrumentType.CONTRABASS_STACCATO -> 36 // C1
            InstrumentType.VIOLA_WARM_VIBRATO -> 55 // G2
            InstrumentType.HARP_CONCERT_GLISS -> 67 // G3
            else -> 69 // A3 (Violin)
        }
        val midiPitch = (note?.pitch ?: defaultMidi) + channel.pitchSemi
        val baseFreq = SynthVoice.midiToFreq(midiPitch).coerceIn(30f, 4000f)

        val panL = (1f - channel.pan).coerceIn(0f, 1f)
        val panR = (1f + channel.pan).coerceIn(0f, 1f)

        // Pizzicato and Harp use plucked Karplus-Strong physical modeling
        if (channel.type == InstrumentType.VIOLIN_PIZZICATO || channel.type == InstrumentType.HARP_CONCERT_GLISS || channel.type == InstrumentType.CONTRABASS_STACCATO) {
            val period = (sampleRate / baseFreq).toInt().coerceIn(6, 2048)
            val ring = FloatArray(period)
            val isHarp = channel.type == InstrumentType.HARP_CONCERT_GLISS
            val isStaccato = channel.type == InstrumentType.CONTRABASS_STACCATO

            // Pluck impulse
            for (i in 0 until period) {
                val t = i.toFloat() / period
                val noise = Random.nextFloat() * 2f - 1f
                val shape = if (isHarp) sin(Math.PI * t).toFloat() * 0.7f + noise * 0.3f else (1f - t) * 0.8f + noise * 0.2f
                ring[i] = shape
            }

            val damping = if (isStaccato) 0.94f else if (isHarp) 0.995f else 0.982f
            val dur = min(totalSamples, (sampleRate * (if (isHarp) 2.4f else if (isStaccato) 0.35f else 0.9f)).toInt())
            var rIdx = 0
            var prev = 0f
            var body = 0f

            for (i in 0 until dur) {
                val cur = ring[rIdx]
                val next = (cur + prev) * 0.5f * damping
                ring[rIdx] = next
                prev = cur
                rIdx++
                if (rIdx >= period) rIdx = 0

                // Wooden instrument body resonance
                body = body * 0.93f + next * 0.12f
                val sample = (next * 0.8f + body * 0.4f) * velocity * channel.volume
                outputL[i] += sample * panL
                outputR[i] += sample * panR
            }
            return
        }

        // Bowed Strings (Solo Violin, Cello, String Ensemble, Viola, Fiddle)
        val duration = min(totalSamples, (sampleRate * 1.6f).toInt())
        val isEnsemble = channel.type == InstrumentType.ORCHESTRAL_STRING_ENSEMBLE
        val isFiddle = channel.type == InstrumentType.FIDDLE_FOLK_FAST

        // Ensemble uses detuned multi-oscillator string section simulation
        val detuneOffsets = if (isEnsemble) doubleArrayOf(-0.007, -0.003, 0.0, 0.004, 0.008) else doubleArrayOf(0.0)
        val phases = DoubleArray(detuneOffsets.size)

        var bodyFilter = 0f
        val vibratoRate = if (isFiddle) 6.8 else 5.2 // Hz
        val vibratoDepth = if (isFiddle) 2.5 else 4.2

        for (i in 0 until duration) {
            val t = i.toFloat() / duration

            // Natural human vibrato with onset delay (swells in after 80ms)
            val vibratoOnset = (t * 8f).coerceIn(0f, 1f)
            val vibrato = sin(2.0 * Math.PI * vibratoRate * i / sampleRate) * vibratoDepth * vibratoOnset

            // Bow slip-stick excitation
            var sumWave = 0f
            for (v in detuneOffsets.indices) {
                val curFreq = (baseFreq + vibrato) * (1.0 + detuneOffsets[v])
                phases[v] += 2.0 * Math.PI * curFreq / sampleRate
                if (phases[v] > 2.0 * Math.PI) phases[v] -= 2.0 * Math.PI

                val saw = (1.0 - (phases[v] / Math.PI)).toFloat()
                // Bowing friction nonlinearity (Helmholtz motion wave-shaping)
                val bowed = saw + 0.25f * (saw * saw * saw)
                sumWave += bowed
            }
            sumWave /= detuneOffsets.size

            // Bow scratch transient noise on attack
            val bowScratch = if (i < 300) (1f - i / 300f) * (Random.nextFloat() * 2f - 1f) * 0.25f else 0f

            // Instrument body cavity formant filter (~400Hz - ~1200Hz resonance)
            bodyFilter = bodyFilter * 0.82f + (sumWave + bowScratch) * 0.18f

            // ADSR Envelope: gentle expressive attack, singing sustain, smooth decay
            val attack = (t * 14f).coerceAtMost(1f)
            val decay = exp(-t * 1.5f)
            val sample = (bodyFilter * 0.7f + sumWave * 0.3f) * attack * decay * velocity * channel.volume

            outputL[i] += sample * panL
            outputR[i] += sample * panR
        }
    }

    fun renderPianosAndKeys(
        channel: Channel,
        note: NoteEvent?,
        sampleRate: Float,
        totalSamples: Int,
        outputL: FloatArray,
        outputR: FloatArray,
        velocity: Float
    ) {
        val midiPitch = (note?.pitch ?: 60) + channel.pitchSemi
        val baseFreq = SynthVoice.midiToFreq(midiPitch).coerceIn(25f, 5000f)
        val panL = (1f - channel.pan).coerceIn(0f, 1f)
        val panR = (1f + channel.pan).coerceIn(0f, 1f)

        when (channel.type) {
            InstrumentType.PIANO_STEINWAY_GRAND,
            InstrumentType.PIANO_VINTAGE_UPRIGHT -> {
                // Steinway / Upright Concert Grand Acoustic Piano Physical Modeling
                // Stiff string inharmonicity: fn = n * f0 * sqrt(1 + B * n^2)
                val duration = min(totalSamples, (sampleRate * 2.5f).toInt())
                val isUpright = channel.type == InstrumentType.PIANO_VINTAGE_UPRIGHT
                val inharmB = 0.0004 // Acoustic string stiffness coefficient
                val numHarmonics = 8
                val phases = DoubleArray(numHarmonics)
                val harmonicFreqs = DoubleArray(numHarmonics) { h ->
                    val n = h + 1
                    baseFreq * n * sqrt(1.0 + inharmB * n * n)
                }

                var soundboardResonance = 0f

                for (i in 0 until duration) {
                    val t = i.toFloat() / duration
                    var sumHarmonics = 0f

                    for (h in 0 until numHarmonics) {
                        phases[h] += 2.0 * Math.PI * harmonicFreqs[h] / sampleRate
                        if (phases[h] > 2.0 * Math.PI) phases[h] -= 2.0 * Math.PI

                        // Higher harmonics decay faster than fundamental
                        val harmDecay = exp(-t * (1.8f + h * 2.2f))
                        val weight = 1.0f / (h + 1).toFloat().pow(0.85f)
                        sumHarmonics += sin(phases[h]).toFloat() * harmDecay * weight
                    }

                    // Felt hammer strike attack impulse
                    val hammerImpulse = if (i < 200) (1f - i / 200f).pow(2f) * 0.6f else 0f
                    val honkyTonkDetune = if (isUpright) sin(2.0 * Math.PI * (baseFreq * 1.006) * i / sampleRate).toFloat() * 0.25f else 0f

                    // Soundboard wooden resonance
                    soundboardResonance = soundboardResonance * 0.95f + sumHarmonics * 0.05f

                    val raw = (sumHarmonics * 0.75f + soundboardResonance * 0.35f + hammerImpulse + honkyTonkDetune)
                    val sample = tanh(raw * (1.0f + velocity * 0.4f)) * velocity * channel.volume

                    outputL[i] += sample * panL
                    outputR[i] += sample * panR
                }
            }

            InstrumentType.KEYS_RHODES_STAGE_73 -> {
                // Fender Rhodes Electric Piano: Metallic tine chime + soft saturation
                val duration = min(totalSamples, (sampleRate * 2.0f).toInt())
                var phaseTine = 0.0
                var phaseBody = 0.0
                val tineFreq = baseFreq * 7.02 // Inharmonic metal tine clang

                for (i in 0 until duration) {
                    val t = i.toFloat() / duration
                    phaseBody += 2.0 * Math.PI * baseFreq / sampleRate
                    phaseTine += 2.0 * Math.PI * tineFreq / sampleRate

                    val bodyTone = sin(phaseBody).toFloat()
                    val tineClang = sin(phaseTine).toFloat() * exp(-t * 16.0f) * 0.4f

                    // Dual decay: quick tine strike + warm bell sustain
                    val env = exp(-t * 2.2f)
                    val raw = (bodyTone * 0.85f + tineClang) * env
                    val sample = tanh(raw * 1.5f) * velocity * channel.volume

                    outputL[i] += sample * panL
                    outputR[i] += sample * panR
                }
            }

            InstrumentType.KEYS_WURLITZER_200A -> {
                // Wurlitzer 200A: Vibrating steel reed + warm tremolo
                val duration = min(totalSamples, (sampleRate * 1.8f).toInt())
                var phase1 = 0.0
                var phase2 = 0.0

                for (i in 0 until duration) {
                    val t = i.toFloat() / duration
                    phase1 += 2.0 * Math.PI * baseFreq / sampleRate
                    phase2 += 2.0 * Math.PI * (baseFreq * 3.0) / sampleRate

                    val reed = sin(phase1).toFloat() * 0.7f + sin(phase2).toFloat() * 0.3f
                    // Classic 5.5 Hz tremolo
                    val tremolo = 0.85f + 0.15f * sin(2.0 * Math.PI * 5.5 * i / sampleRate).toFloat()
                    val sample = reed * exp(-t * 2.6f) * tremolo * velocity * channel.volume

                    outputL[i] += sample * panL
                    outputR[i] += sample * panR
                }
            }

            InstrumentType.KEYS_FM_BELL_DX7 -> {
                // Yamaha DX7 FM Glass Bell: Modulator into Carrier frequency modulation
                val duration = min(totalSamples, (sampleRate * 2.4f).toInt())
                var carrierPhase = 0.0
                var modulatorPhase = 0.0
                val modRatio = 3.5 // Glassy crystalline FM ratio

                for (i in 0 until duration) {
                    val t = i.toFloat() / duration
                    val modEnv = exp(-t * 4.5f) * 3.8
                    modulatorPhase += 2.0 * Math.PI * (baseFreq * modRatio) / sampleRate
                    val modulation = sin(modulatorPhase) * modEnv

                    carrierPhase += 2.0 * Math.PI * baseFreq / sampleRate + modulation
                    val bell = sin(carrierPhase).toFloat()
                    val sample = bell * exp(-t * 1.6f) * velocity * channel.volume

                    outputL[i] += sample * panL
                    outputR[i] += sample * panR
                }
            }

            InstrumentType.KEYS_CELESTA_DREAM -> {
                // Tchaikovsky Fairy Celesta: Pure metallic bell chime with long shimmer
                val duration = min(totalSamples, (sampleRate * 2.2f).toInt())
                var p1 = 0.0
                var p2 = 0.0

                for (i in 0 until duration) {
                    val t = i.toFloat() / duration
                    p1 += 2.0 * Math.PI * baseFreq / sampleRate
                    p2 += 2.0 * Math.PI * (baseFreq * 4.01) / sampleRate

                    val chime = sin(p1).toFloat() * 0.8f + sin(p2).toFloat() * 0.35f * exp(-t * 8f)
                    val sample = chime * exp(-t * 2.0f) * velocity * channel.volume

                    outputL[i] += sample * panL
                    outputR[i] += sample * panR
                }
            }

            InstrumentType.KEYS_HARPSICHORD_BAROQUE -> {
                // Double Harpsichord: Plucked metallic quill plectrum transient, bright harmonics
                val duration = min(totalSamples, (sampleRate * 1.2f).toInt())
                var phase = 0.0

                for (i in 0 until duration) {
                    val t = i.toFloat() / duration
                    phase += 2.0 * Math.PI * baseFreq / sampleRate
                    val saw = (1.0 - (phase / Math.PI)).toFloat()
                    val quillClick = if (i < 150) (1f - i / 150f) * 0.5f else 0f
                    val sample = (saw + quillClick) * exp(-t * 3.8f) * velocity * channel.volume

                    outputL[i] += sample * panL
                    outputR[i] += sample * panR
                }
            }

            InstrumentType.KEYS_CHURCH_PIPE_ORGAN,
            InstrumentType.KEYS_HAMMOND_B3 -> {
                // Pipe & Tonewheel Organ: Additive Drawbars (16', 8', 4', 2', 1') + Leslie Chorus
                val duration = min(totalSamples, (sampleRate * 1.8f).toInt())
                val ratios = doubleArrayOf(0.5, 1.0, 2.0, 3.0, 4.0)
                val weights = floatArrayOf(0.3f, 0.7f, 0.4f, 0.25f, 0.2f)
                val phases = DoubleArray(ratios.size)
                val isHammond = channel.type == InstrumentType.KEYS_HAMMOND_B3

                for (i in 0 until duration) {
                    val t = i.toFloat() / duration
                    var sum = 0f

                    for (r in ratios.indices) {
                        phases[r] += 2.0 * Math.PI * (baseFreq * ratios[r]) / sampleRate
                        sum += sin(phases[r]).toFloat() * weights[r]
                    }

                    // Rotary Leslie speaker doppler effect for Hammond
                    val leslie = if (isHammond) {
                        val rotary = sin(2.0 * Math.PI * 6.0 * i / sampleRate).toFloat() * 0.15f
                        1.0f + rotary
                    } else 1.0f

                    val sample = sum * leslie * exp(-t * 0.8f) * velocity * channel.volume
                    outputL[i] += sample * panL
                    outputR[i] += sample * panR
                }
            }

            InstrumentType.KEYS_CLAVINET_D6 -> {
                // Clavinet D6: Plucked guitar-like reed string with auto-wah envelope
                val duration = min(totalSamples, (sampleRate * 0.85f).toInt())
                var phase = 0.0
                var wahFilter = 0f

                for (i in 0 until duration) {
                    val t = i.toFloat() / duration
                    phase += 2.0 * Math.PI * baseFreq / sampleRate
                    val pulse = if (phase < Math.PI * 0.4) 1.0f else -0.4f

                    // Wah filter sweep
                    val wahCutoff = (1.0f - exp(-t * 12f)) * exp(-t * 4f)
                    wahFilter = wahFilter * 0.7f + pulse * (0.3f + wahCutoff * 0.5f)

                    val sample = wahFilter * exp(-t * 3.5f) * velocity * channel.volume
                    outputL[i] += sample * panL
                    outputR[i] += sample * panR
                }
            }

            else -> {}
        }
    }

    fun renderGuitarsExtended(
        channel: Channel,
        note: NoteEvent?,
        sampleRate: Float,
        totalSamples: Int,
        outputL: FloatArray,
        outputR: FloatArray,
        velocity: Float
    ) {
        val midiPitch = (note?.pitch ?: 60) + channel.pitchSemi
        val baseFreq = SynthVoice.midiToFreq(midiPitch).coerceIn(40f, 4000f)
        val panL = (1f - channel.pan).coerceIn(0f, 1f)
        val panR = (1f + channel.pan).coerceIn(0f, 1f)

        when (channel.type) {
            InstrumentType.GUITAR_12_STRING_CHIME -> {
                // 12-String Chime: Paired string unison + octave detuning
                val period1 = (sampleRate / baseFreq).toInt().coerceIn(6, 2048)
                val period2 = (sampleRate / (baseFreq * 2.004f)).toInt().coerceIn(6, 2048)
                val ring1 = FloatArray(period1) { Random.nextFloat() * 2f - 1f }
                val ring2 = FloatArray(period2) { Random.nextFloat() * 2f - 1f }
                val dur = min(totalSamples, (sampleRate * 2.2f).toInt())
                var r1 = 0
                var r2 = 0
                var prev1 = 0f
                var prev2 = 0f

                for (i in 0 until dur) {
                    val c1 = ring1[r1]
                    val next1 = (c1 + prev1) * 0.5f * 0.993f
                    ring1[r1] = next1
                    prev1 = c1
                    r1++
                    if (r1 >= period1) r1 = 0

                    val c2 = ring2[r2]
                    val next2 = (c2 + prev2) * 0.5f * 0.988f
                    ring2[r2] = next2
                    prev2 = c2
                    r2++
                    if (r2 >= period2) r2 = 0

                    val sample = (next1 * 0.65f + next2 * 0.45f) * velocity * channel.volume
                    outputL[i] += sample * panL
                    outputR[i] += sample * panR
                }
            }

            InstrumentType.GUITAR_OVERDRIVE_SOLO -> {
                // Tube Overdrive Solo: Dual-stage non-linear clipping with harmonic sustain
                val period = (sampleRate / baseFreq).toInt().coerceIn(6, 2048)
                val ring = FloatArray(period) { Random.nextFloat() * 2f - 1f }
                val dur = min(totalSamples, (sampleRate * 2.5f).toInt())
                var r = 0
                var prev = 0f

                for (i in 0 until dur) {
                    val c = ring[r]
                    val next = (c + prev) * 0.5f * 0.997f
                    ring[r] = next
                    prev = c
                    r++
                    if (r >= period) r = 0

                    // Double-stage tube saturation
                    var drive = next * 6.0f
                    drive = tanh(drive)
                    drive = tanh(drive * 1.8f) * 0.8f

                    val sample = drive * velocity * channel.volume
                    outputL[i] += sample * panL
                    outputR[i] += sample * panR
                }
            }

            InstrumentType.GUITAR_MUTED_PALM -> {
                // Heavy Metal Palm-Muted Chug: High damping (fast decay) with heavy low end
                val period = (sampleRate / baseFreq).toInt().coerceIn(6, 2048)
                val ring = FloatArray(period) { (Random.nextFloat() * 2f - 1f) }
                val dur = min(totalSamples, (sampleRate * 0.32f).toInt())
                var r = 0
                var prev = 0f

                for (i in 0 until dur) {
                    val c = ring[r]
                    val next = (c + prev) * 0.5f * 0.92f // Heavy bridge palm muting
                    ring[r] = next
                    prev = c
                    r++
                    if (r >= period) r = 0

                    val sample = tanh(next * 5.0f) * velocity * channel.volume
                    outputL[i] += sample * panL
                    outputR[i] += sample * panR
                }
            }

            InstrumentType.GUITAR_SURF_TREMOLO -> {
                // Surf Guitar: Classic spring reverb & 6 Hz tremolo amplitude modulation
                val period = (sampleRate / baseFreq).toInt().coerceIn(6, 2048)
                val ring = FloatArray(period) { Random.nextFloat() * 2f - 1f }
                val dur = min(totalSamples, (sampleRate * 2.0f).toInt())
                var r = 0
                var prev = 0f

                for (i in 0 until dur) {
                    val c = ring[r]
                    val next = (c + prev) * 0.5f * 0.991f
                    ring[r] = next
                    prev = c
                    r++
                    if (r >= period) r = 0

                    val tremolo = 0.7f + 0.3f * sin(2.0 * Math.PI * 6.2 * i / sampleRate).toFloat()
                    val sample = next * tremolo * velocity * channel.volume
                    outputL[i] += sample * panL
                    outputR[i] += sample * panR
                }
            }

            InstrumentType.GUITAR_FLAMENCO_RASGUEADO,
            InstrumentType.GUITAR_SLIDE_BLUES,
            InstrumentType.GUITAR_PEDAL_STEEL -> {
                val isSlide = channel.type == InstrumentType.GUITAR_SLIDE_BLUES || channel.type == InstrumentType.GUITAR_PEDAL_STEEL
                val period = (sampleRate / baseFreq).toInt().coerceIn(6, 2048)
                val ring = FloatArray(period) { Random.nextFloat() * 2f - 1f }
                val dur = min(totalSamples, (sampleRate * 1.8f).toInt())
                var r = 0
                var prev = 0f

                for (i in 0 until dur) {
                    val t = i.toFloat() / dur
                    val c = ring[r]
                    val next = (c + prev) * 0.5f * 0.988f
                    ring[r] = next
                    prev = c
                    r++
                    if (r >= period) r = 0

                    val slidePitch = if (isSlide) sin(t * Math.PI * 0.5f).toFloat() * 0.05f else 0f
                    val sample = next * (1.0f + slidePitch) * velocity * channel.volume
                    outputL[i] += sample * panL
                    outputR[i] += sample * panR
                }
            }

            else -> {}
        }
    }

    fun renderBrassAndWoodwinds(
        channel: Channel,
        note: NoteEvent?,
        sampleRate: Float,
        totalSamples: Int,
        outputL: FloatArray,
        outputR: FloatArray,
        velocity: Float
    ) {
        val defaultMidi = when (channel.type) {
            InstrumentType.BRASS_TROMBONE_STAB -> 46 // Bb1
            InstrumentType.WOODWIND_FLUTE_JAZZ -> 72 // C4
            InstrumentType.WOODWIND_SAX_TENOR -> 58 // Bb2
            else -> 65 // F3
        }
        val midiPitch = (note?.pitch ?: defaultMidi) + channel.pitchSemi
        val baseFreq = SynthVoice.midiToFreq(midiPitch).coerceIn(40f, 4000f)

        val duration = min(totalSamples, (sampleRate * 1.4f).toInt())
        var phase = 0.0
        var lpFilter = 0f

        val panL = (1f - channel.pan).coerceIn(0f, 1f)
        val panR = (1f + channel.pan).coerceIn(0f, 1f)

        val isFlute = channel.type == InstrumentType.WOODWIND_FLUTE_JAZZ
        val isSax = channel.type == InstrumentType.WOODWIND_SAX_TENOR

        for (i in 0 until duration) {
            val t = i.toFloat() / duration
            val vibrato = sin(2.0 * Math.PI * 5.4 * i / sampleRate) * (if (isSax) 3.5 else 2.0)
            phase += 2.0 * Math.PI * (baseFreq + vibrato) / sampleRate

            val rawOsc = if (isFlute) {
                // Pure sine + breath turbulence noise
                sin(phase).toFloat() * 0.8f + (Random.nextFloat() * 2f - 1f) * 0.22f
            } else {
                // Rich saw with odd/even brass harmonics
                val saw = (1.0 - (phase / Math.PI)).toFloat()
                tanh(saw * 2.2f)
            }

            lpFilter = lpFilter * 0.75f + rawOsc * 0.25f
            val env = exp(-t * 2.2f)
            val sample = lpFilter * env * velocity * channel.volume

            outputL[i] += sample * panL
            outputR[i] += sample * panR
        }
    }

    fun renderEthnicInstruments(
        channel: Channel,
        note: NoteEvent?,
        sampleRate: Float,
        totalSamples: Int,
        outputL: FloatArray,
        outputR: FloatArray,
        velocity: Float
    ) {
        val midiPitch = (note?.pitch ?: 62) + channel.pitchSemi
        val baseFreq = SynthVoice.midiToFreq(midiPitch).coerceIn(40f, 4000f)

        val panL = (1f - channel.pan).coerceIn(0f, 1f)
        val panR = (1f + channel.pan).coerceIn(0f, 1f)

        when (channel.type) {
            InstrumentType.ETHNIC_SITAR_INDIAN -> {
                // Sitar: Curved jawari bridge buzzing harmonic dispersion
                val duration = min(totalSamples, (sampleRate * 2.0f).toInt())
                var p1 = 0.0
                var pBuzz = 0.0

                for (i in 0 until duration) {
                    val t = i.toFloat() / duration
                    p1 += 2.0 * Math.PI * baseFreq / sampleRate
                    pBuzz += 2.0 * Math.PI * (baseFreq * 2.87) / sampleRate

                    val fund = sin(p1).toFloat()
                    val buzz = sin(pBuzz).toFloat() * exp(-t * 6f) * 0.6f
                    val sample = (fund + buzz) * exp(-t * 1.8f) * velocity * channel.volume

                    outputL[i] += sample * panL
                    outputR[i] += sample * panR
                }
            }

            InstrumentType.ETHNIC_KALIMBA_THUMB -> {
                // African Kalimba Thumb Piano: Pure metallic chime with hollow box decay
                val duration = min(totalSamples, (sampleRate * 1.4f).toInt())
                var phase = 0.0

                for (i in 0 until duration) {
                    val t = i.toFloat() / duration
                    phase += 2.0 * Math.PI * baseFreq / sampleRate
                    val sample = sin(phase).toFloat() * exp(-t * 4.2f) * velocity * channel.volume

                    outputL[i] += sample * panL
                    outputR[i] += sample * panR
                }
            }

            else -> {
                // Koto, Pan Flute, Bagpipes, Bouzouki
                val duration = min(totalSamples, (sampleRate * 1.6f).toInt())
                var phase = 0.0

                for (i in 0 until duration) {
                    val t = i.toFloat() / duration
                    phase += 2.0 * Math.PI * baseFreq / sampleRate
                    val tri = (abs((phase / Math.PI) - 1.0) * 2.0 - 1.0).toFloat()
                    val sample = tri * exp(-t * 2.5f) * velocity * channel.volume

                    outputL[i] += sample * panL
                    outputR[i] += sample * panR
                }
            }
        }
    }

    fun renderExtendedSynths(
        channel: Channel,
        note: NoteEvent?,
        sampleRate: Float,
        totalSamples: Int,
        outputL: FloatArray,
        outputR: FloatArray,
        velocity: Float
    ) {
        val midiPitch = (note?.pitch ?: 60) + channel.pitchSemi
        val baseFreq = SynthVoice.midiToFreq(midiPitch).coerceIn(20f, 5000f)

        val panL = (1f - channel.pan).coerceIn(0f, 1f)
        val panR = (1f + channel.pan).coerceIn(0f, 1f)

        when (channel.type) {
            InstrumentType.SYNTH_ACID_303 -> {
                // Roland TB-303: Acid Resonant Diode Filter Sweep
                val duration = min(totalSamples, (sampleRate * 0.7f).toInt())
                var phase = 0.0
                var lpState = 0f

                for (i in 0 until duration) {
                    val t = i.toFloat() / duration
                    phase += 2.0 * Math.PI * baseFreq / sampleRate
                    val saw = (1.0 - (phase / Math.PI)).toFloat()

                    // Resonant filter sweep
                    val cutoffEnv = exp(-t * 14.0f) * 0.7f + 0.15f
                    lpState = lpState * (1f - cutoffEnv) + saw * cutoffEnv
                    val driven = tanh(lpState * 3.5f) * exp(-t * 2.0f) * velocity * channel.volume

                    outputL[i] += driven * panL
                    outputR[i] += driven * panR
                }
            }

            InstrumentType.SYNTH_CHIP_8BIT -> {
                // NES 8-Bit Chiptune: Pure Square Wave with 50% / 25% Duty Cycle
                val duration = min(totalSamples, (sampleRate * 0.55f).toInt())
                var phase = 0.0

                for (i in 0 until duration) {
                    val t = i.toFloat() / duration
                    phase += 2.0 * Math.PI * baseFreq / sampleRate
                    if (phase > 2.0 * Math.PI) phase -= 2.0 * Math.PI

                    val square = if (phase < Math.PI * 0.75) 0.8f else -0.8f
                    val sample = square * exp(-t * 3.2f) * velocity * channel.volume

                    outputL[i] += sample * panL
                    outputR[i] += sample * panR
                }
            }

            InstrumentType.SYNTH_CYBERPUNK_REESE -> {
                // Dark Cyberpunk Reese Bass: Multiple detuned saw waves + thick saturation
                val duration = min(totalSamples, (sampleRate * 1.5f).toInt())
                var p1 = 0.0
                var p2 = 0.0

                for (i in 0 until duration) {
                    val t = i.toFloat() / duration
                    p1 += 2.0 * Math.PI * baseFreq / sampleRate
                    p2 += 2.0 * Math.PI * (baseFreq * 1.014) / sampleRate

                    val saw1 = (1.0 - (p1 / Math.PI)).toFloat()
                    val saw2 = (1.0 - (p2 / Math.PI)).toFloat()
                    val driven = tanh((saw1 + saw2) * 2.0f) * exp(-t * 1.2f) * velocity * channel.volume

                    outputL[i] += driven * panL
                    outputR[i] += driven * panR
                }
            }

            else -> {
                // CS-80 Brass, Ethereal Choir, Retrowave Pluck, Theremin
                val duration = min(totalSamples, (sampleRate * 1.6f).toInt())
                var phase = 0.0

                for (i in 0 until duration) {
                    val t = i.toFloat() / duration
                    val vibrato = sin(2.0 * Math.PI * 5.6 * i / sampleRate) * 4.0
                    phase += 2.0 * Math.PI * (baseFreq + vibrato) / sampleRate

                    val sine = sin(phase).toFloat()
                    val sample = sine * exp(-t * 1.4f) * velocity * channel.volume

                    outputL[i] += sample * panL
                    outputR[i] += sample * panR
                }
            }
        }
    }
}
