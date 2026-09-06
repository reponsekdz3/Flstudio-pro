package com.example.audio

import kotlin.math.*

/**
 * 2nd order Biquad Digital Filter for real-time DSP
 * Supports Low-Pass, High-Pass, Band-Pass, and Notch filtering.
 */
class BiquadFilter(
    var sampleRate: Float = 44100f
) {
    enum class Type {
        LOW_PASS, HIGH_PASS, BAND_PASS, NOTCH
    }

    private var b0 = 1.0f
    private var b1 = 0.0f
    private var b2 = 0.0f
    private var a1 = 0.0f
    private var a2 = 0.0f

    // Filter memory state (left & right channels)
    private var x1L = 0.0f
    private var x2L = 0.0f
    private var y1L = 0.0f
    private var y2L = 0.0f

    private var x1R = 0.0f
    private var x2R = 0.0f
    private var y1R = 0.0f
    private var y2R = 0.0f

    private var currentType: Type = Type.LOW_PASS
    private var currentCutoff: Float = 20000f
    private var currentQ: Float = 0.707f

    init {
        updateCoefficients(Type.LOW_PASS, 12000f, 0.707f)
    }

    fun updateCoefficients(type: Type, cutoffHz: Float, q: Float) {
        currentType = type
        currentCutoff = cutoffHz.coerceIn(20f, sampleRate * 0.48f)
        currentQ = q.coerceIn(0.1f, 15f)

        val omega = (2.0 * Math.PI * currentCutoff / sampleRate).toFloat()
        val sinOmega = sin(omega)
        val cosOmega = cos(omega)
        val alpha = sinOmega / (2.0f * currentQ)

        when (type) {
            Type.LOW_PASS -> {
                val a0 = 1.0f + alpha
                b0 = ((1.0f - cosOmega) / 2.0f) / a0
                b1 = (1.0f - cosOmega) / a0
                b2 = ((1.0f - cosOmega) / 2.0f) / a0
                a1 = (-2.0f * cosOmega) / a0
                a2 = (1.0f - alpha) / a0
            }
            Type.HIGH_PASS -> {
                val a0 = 1.0f + alpha
                b0 = ((1.0f + cosOmega) / 2.0f) / a0
                b1 = (-(1.0f + cosOmega)) / a0
                b2 = ((1.0f + cosOmega) / 2.0f) / a0
                a1 = (-2.0f * cosOmega) / a0
                a2 = (1.0f - alpha) / a0
            }
            Type.BAND_PASS -> {
                val a0 = 1.0f + alpha
                b0 = (alpha) / a0
                b1 = 0.0f
                b2 = (-alpha) / a0
                a1 = (-2.0f * cosOmega) / a0
                a2 = (1.0f - alpha) / a0
            }
            Type.NOTCH -> {
                val a0 = 1.0f + alpha
                b0 = 1.0f / a0
                b1 = (-2.0f * cosOmega) / a0
                b2 = 1.0f / a0
                a1 = (-2.0f * cosOmega) / a0
                a2 = (1.0f - alpha) / a0
            }
        }
    }

    fun processLeft(input: Float): Float {
        val out = b0 * input + b1 * x1L + b2 * x2L - a1 * y1L - a2 * y2L
        x2L = x1L
        x1L = input
        y2L = y1L
        y1L = out.coerceIn(-2.0f, 2.0f)
        return y1L
    }

    fun processRight(input: Float): Float {
        val out = b0 * input + b1 * x1R + b2 * x2R - a1 * y1R - a2 * y2R
        x2R = x1R
        x1R = input
        y2R = y1R
        y1R = out.coerceIn(-2.0f, 2.0f)
        return y1R
    }

    fun reset() {
        x1L = 0f; x2L = 0f; y1L = 0f; y2L = 0f
        x1R = 0f; x2R = 0f; y1R = 0f; y2R = 0f
    }
}

/**
 * Fruity Delay Effect: Stereo delay line with feedback, sync time, and wet/dry mix.
 */
class DelayFX(private val sampleRate: Int = 44100) {
    var enabled: Boolean = true
    var timeMs: Float = 250f // 250ms default
    var feedback: Float = 0.45f
    var wetMix: Float = 0.35f

    private val maxDelaySamples = sampleRate * 2 // up to 2 seconds
    private val bufferL = FloatArray(maxDelaySamples)
    private val bufferR = FloatArray(maxDelaySamples)
    private var writePos = 0

    fun process(sampleL: Float, sampleR: Float): Pair<Float, Float> {
        if (!enabled || wetMix <= 0.001f) return Pair(sampleL, sampleR)

        val delaySamples = ((timeMs / 1000f) * sampleRate).toInt().coerceIn(1, maxDelaySamples - 1)
        var readPosL = writePos - delaySamples
        if (readPosL < 0) readPosL += maxDelaySamples

        var readPosR = writePos - (delaySamples * 1.1f).toInt().coerceIn(1, maxDelaySamples - 1)
        if (readPosR < 0) readPosR += maxDelaySamples

        val delayedL = bufferL[readPosL]
        val delayedR = bufferR[readPosR]

        bufferL[writePos] = (sampleL + delayedL * feedback).coerceIn(-1.5f, 1.5f)
        bufferR[writePos] = (sampleR + delayedR * feedback).coerceIn(-1.5f, 1.5f)

        writePos = (writePos + 1) % maxDelaySamples

        val outL = sampleL * (1f - wetMix * 0.5f) + delayedL * wetMix
        val outR = sampleR * (1f - wetMix * 0.5f) + delayedR * wetMix
        return Pair(outL, outR)
    }

    fun reset() {
        bufferL.fill(0f)
        bufferR.fill(0f)
        writePos = 0
    }
}

/**
 * Fruity Reverb Effect: Freeverb-style comb and allpass network for lush acoustic space.
 */
class ReverbFX(private val sampleRate: Int = 44100) {
    var enabled: Boolean = true
    var roomSize: Float = 0.6f
    var damping: Float = 0.3f
    var wetMix: Float = 0.30f

    // 4 comb filter delays (prime-ish lengths)
    private val combDelays = intArrayOf(1116, 1188, 1277, 1356)
    private val combBuffers = Array(4) { FloatArray(combDelays[it] + 10) }
    private val combIndices = IntArray(4)
    private val combFilters = FloatArray(4)

    // 2 allpass delays
    private val apDelays = intArrayOf(225, 341)
    private val apBuffers = Array(2) { FloatArray(apDelays[it] + 10) }
    private val apIndices = IntArray(2)

    fun process(sampleL: Float, sampleR: Float): Pair<Float, Float> {
        if (!enabled || wetMix <= 0.001f) return Pair(sampleL, sampleR)

        val monoInput = (sampleL + sampleR) * 0.5f
        var combSum = 0.0f

        val scaledRoom = roomSize.coerceIn(0.1f, 0.95f)
        val damp = damping.coerceIn(0.05f, 0.95f)

        for (i in 0 until 4) {
            val buf = combBuffers[i]
            val len = combDelays[i]
            val idx = combIndices[i]
            val out = buf[idx]

            combFilters[i] = out * (1.0f - damp) + combFilters[i] * damp
            buf[idx] = (monoInput + combFilters[i] * scaledRoom).coerceIn(-2.0f, 2.0f)
            combIndices[i] = (idx + 1) % len
            combSum += out
        }

        // Allpass diffusers
        var apInput = combSum * 0.25f
        for (i in 0 until 2) {
            val buf = apBuffers[i]
            val len = apDelays[i]
            val idx = apIndices[i]
            val delayed = buf[idx]
            val apOut = -apInput + delayed
            buf[idx] = (apInput + delayed * 0.5f).coerceIn(-2.0f, 2.0f)
            apIndices[i] = (idx + 1) % len
            apInput = apOut
        }

        val outL = sampleL * (1f - wetMix * 0.5f) + apInput * wetMix
        val outR = sampleR * (1f - wetMix * 0.5f) + apInput * wetMix * 0.95f
        return Pair(outL, outR)
    }

    fun reset() {
        for (buf in combBuffers) buf.fill(0f)
        for (buf in apBuffers) buf.fill(0f)
        combIndices.fill(0)
        apIndices.fill(0)
        combFilters.fill(0f)
    }
}

/**
 * Fruity Fast Dist: Tanh soft clipping saturation with drive control.
 */
class DistortionFX {
    var enabled: Boolean = true
    var drive: Float = 1.0f // 1.0 to 10.0
    var mix: Float = 0.0f // 0.0 (clean) to 1.0 (full distortion)

    fun process(sampleL: Float, sampleR: Float): Pair<Float, Float> {
        if (!enabled || mix <= 0.001f) return Pair(sampleL, sampleR)

        val boostedL = sampleL * drive
        val boostedR = sampleR * drive

        // Fast tanh approximation for warm saturation: x / (1 + |x|) or Math.tanh
        val saturatedL = tanh(boostedL)
        val saturatedR = tanh(boostedR)

        val outL = sampleL * (1f - mix) + saturatedL * mix
        val outR = sampleR * (1f - mix) + saturatedR * mix
        return Pair(outL, outR)
    }
}

/**
 * Fruity Parametric EQ: 3-band equalizer (Low Shelf, Mid Peak, High Shelf)
 */
class ParametricEQ(private val sampleRate: Float = 44100f) {
    var enabled: Boolean = true
    var lowGainDb: Float = 0.0f
    var midGainDb: Float = 0.0f
    var highGainDb: Float = 0.0f

    private val lowFilter = BiquadFilter(sampleRate)
    private val midFilter = BiquadFilter(sampleRate)
    private val highFilter = BiquadFilter(sampleRate)

    init {
        lowFilter.updateCoefficients(BiquadFilter.Type.LOW_PASS, 250f, 0.707f)
        midFilter.updateCoefficients(BiquadFilter.Type.BAND_PASS, 1200f, 1.2f)
        highFilter.updateCoefficients(BiquadFilter.Type.HIGH_PASS, 4500f, 0.707f)
    }

    fun process(sampleL: Float, sampleR: Float): Pair<Float, Float> {
        if (!enabled) return Pair(sampleL, sampleR)

        val lowLinear = 10f.pow(lowGainDb / 20f)
        val midLinear = 10f.pow(midGainDb / 20f)
        val highLinear = 10f.pow(highGainDb / 20f)

        val lowL = lowFilter.processLeft(sampleL)
        val lowR = lowFilter.processRight(sampleR)

        val midL = midFilter.processLeft(sampleL)
        val midR = midFilter.processRight(sampleR)

        val highL = highFilter.processLeft(sampleL)
        val highR = highFilter.processRight(sampleR)

        val outL = sampleL + (lowL * (lowLinear - 1f)) + (midL * (midLinear - 1f)) + (highL * (highLinear - 1f))
        val outR = sampleR + (lowR * (lowLinear - 1f)) + (midR * (midLinear - 1f)) + (highR * (highLinear - 1f))

        return Pair(outL, outR)
    }

    fun reset() {
        lowFilter.reset()
        midFilter.reset()
        highFilter.reset()
    }
}

/**
 * Fruity Flanger / Chorus: Bucket-brigade analog delay modulation with quadrature LFO.
 */
class ChorusFX(private val sampleRate: Int = 44100) {
    var enabled: Boolean = false
    var rateHz: Float = 1.2f
    var depth: Float = 0.5f
    var mix: Float = 0.35f

    private val maxDelay = 2048
    private val bufferL = FloatArray(maxDelay)
    private val bufferR = FloatArray(maxDelay)
    private var writePos = 0
    private var lfoPhase = 0.0

    fun process(sampleL: Float, sampleR: Float): Pair<Float, Float> {
        if (!enabled || mix <= 0.001f) return Pair(sampleL, sampleR)

        bufferL[writePos] = sampleL
        bufferR[writePos] = sampleR

        lfoPhase += 2.0 * Math.PI * rateHz / sampleRate
        if (lfoPhase > 2.0 * Math.PI) lfoPhase -= 2.0 * Math.PI

        val baseDelaySamples = 400.0 // ~9ms base delay
        val modSamples = 250.0 * depth

        val modL = baseDelaySamples + modSamples * sin(lfoPhase)
        val modR = baseDelaySamples + modSamples * cos(lfoPhase) // 90 deg stereo spread

        var readL = (writePos - modL.toInt()) % maxDelay
        if (readL < 0) readL += maxDelay
        var readR = (writePos - modR.toInt()) % maxDelay
        if (readR < 0) readR += maxDelay

        val delayedL = bufferL[readL]
        val delayedR = bufferR[readR]

        writePos = (writePos + 1) % maxDelay

        val outL = sampleL * (1f - mix * 0.5f) + delayedL * mix
        val outR = sampleR * (1f - mix * 0.5f) + delayedR * mix
        return Pair(outL, outR)
    }

    fun reset() {
        bufferL.fill(0f)
        bufferR.fill(0f)
        writePos = 0
        lfoPhase = 0.0
    }
}

/**
 * Fruity Compressor / Limiter: Vintage studio optical/VCA compressor.
 */
class CompressorFX(private val sampleRate: Int = 44100) {
    var enabled: Boolean = false
    var thresholdDb: Float = -12f
    var ratio: Float = 4.0f
    var attackMs: Float = 15f
    var releaseMs: Float = 120f
    var gainReductionDb: Float = 0f

    private var envelope = 0.0f

    fun process(sampleL: Float, sampleR: Float): Pair<Float, Float> {
        if (!enabled) {
            gainReductionDb = 0f
            return Pair(sampleL, sampleR)
        }

        val peak = max(abs(sampleL), abs(sampleR))
        val attackCoeff = exp(-1.0f / (attackMs * 0.001f * sampleRate))
        val releaseCoeff = exp(-1.0f / (releaseMs * 0.001f * sampleRate))

        envelope = if (peak > envelope) {
            attackCoeff * envelope + (1.0f - attackCoeff) * peak
        } else {
            releaseCoeff * envelope + (1.0f - releaseCoeff) * peak
        }

        val envDb = if (envelope > 0.00001f) 20f * log10(envelope) else -100f
        val overDb = envDb - thresholdDb

        val gainDb = if (overDb > 0f) {
            -overDb * (1.0f - 1.0f / ratio.coerceAtLeast(1.0f))
        } else {
            0f
        }
        gainReductionDb = -gainDb

        val linearGain = 10f.pow(gainDb / 20f)
        return Pair(sampleL * linearGain, sampleR * linearGain)
    }

    fun reset() {
        envelope = 0f
        gainReductionDb = 0f
    }
}
