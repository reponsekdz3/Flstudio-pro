package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

/**
 * Realistic metal rack screw with crosshead slot for authentic studio rack look.
 */
@Composable
fun HardwareRackScrew(modifier: Modifier = Modifier.size(10.dp)) {
    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        // Outer beveled washer
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF6B7280), Color(0xFF1F2430), Color(0xFF11141A)),
                center = center - Offset(radius * 0.2f, radius * 0.2f),
                radius = radius
            ),
            radius = radius,
            center = center
        )

        // Rim highlight
        drawCircle(
            color = Color(0xFF9CA3AF).copy(alpha = 0.4f),
            radius = radius * 0.9f,
            center = center,
            style = Stroke(width = 0.8f)
        )

        // Inner screw head
        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF4B5563), Color(0xFF1F2937)),
                start = Offset(0f, 0f),
                end = Offset(size.width, size.height)
            ),
            radius = radius * 0.7f,
            center = center
        )

        // Crosshead slots
        val slotLen = radius * 0.45f
        val slotWidth = 1.4f
        val slotColor = Color(0xFF0F1115)
        // Horizontal slot
        drawLine(
            color = slotColor,
            start = center - Offset(slotLen, 0f),
            end = center + Offset(slotLen, 0f),
            strokeWidth = slotWidth,
            cap = StrokeCap.Round
        )
        // Vertical slot
        drawLine(
            color = slotColor,
            start = center - Offset(0f, slotLen),
            end = center + Offset(0f, slotLen),
            strokeWidth = slotWidth,
            cap = StrokeCap.Round
        )
    }
}

/**
 * 19-inch Studio Hardware Rack Unit Enclosure with real metal chassis,
 * rack ears, corner mounting screws, and illuminated title badge.
 */
@Composable
fun HardwareRackChassis(
    title: String,
    subtitle: String? = null,
    accentColor: Color = FruityOrange,
    modifier: Modifier = Modifier,
    headerTrailing: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF222631),
                        Color(0xFF181B22),
                        Color(0xFF121419)
                    )
                )
            )
            .border(
                width = 1.2.dp,
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF4A5265), Color(0xFF242A36), Color(0xFF111317))
                ),
                shape = RoundedCornerShape(6.dp)
            )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Hardware Faceplate Top Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF2C3240), Color(0xFF1D212A))
                        )
                    )
                    .border(
                        width = 0.8.dp,
                        color = Color(0xFF3B4354),
                        shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                    )
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HardwareRackScrew(modifier = Modifier.size(11.dp))
                    Spacer(modifier = Modifier.width(8.dp))

                    // Pilot LED
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                            .border(0.5.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))

                    // Rack Model Title
                    Text(
                        text = title.uppercase(),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.2.sp,
                        color = TextPrimary
                    )

                    if (subtitle != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = subtitle,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace,
                            color = accentColor.copy(alpha = 0.85f)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    headerTrailing?.invoke()
                    Spacer(modifier = Modifier.width(8.dp))
                    HardwareRackScrew(modifier = Modifier.size(11.dp))
                }
            }

            // Chassis Interior Bay
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                content()
            }
        }
    }
}

/**
 * Real Analog Rotary Potentiometer / Knob with tactile touch drag control,
 * radial pointer pip, arc track, and hardware silkscreen labeling.
 */
@Composable
fun HardwareRotaryKnob(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    label: String,
    displayValue: String,
    color: Color = FruityOrange,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 48.dp
) {
    val rangeSpan = valueRange.endInclusive - valueRange.start
    val normalized = if (rangeSpan > 0f) {
        ((value - valueRange.start) / rangeSpan).coerceIn(0f, 1f)
    } else 0.5f

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Rotary Knob Wheel
        Box(
            modifier = Modifier
                .size(sizeDp)
                .pointerInput(Unit) {
                    detectDragGestures { _, dragAmount ->
                        val deltaNormalized = -dragAmount.y / 150f
                        val newNormalized = (normalized + deltaNormalized).coerceIn(0f, 1f)
                        val newValue = valueRange.start + newNormalized * rangeSpan
                        onValueChange(newValue)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val center = Offset(w / 2f, h / 2f)
                val outerRadius = (w.coerceAtMost(h) / 2f) - 2f
                val knobRadius = outerRadius * 0.72f

                // Angle range: from 135 deg to 405 deg (270 degrees sweep)
                val startAngle = 135f
                val totalSweep = 270f
                val currentAngle = startAngle + (normalized * totalSweep)

                // Background arc track
                drawArc(
                    color = Color(0xFF14171E),
                    startAngle = startAngle,
                    sweepAngle = totalSweep,
                    useCenter = false,
                    topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                    size = Size(outerRadius * 2, outerRadius * 2),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // Illuminated value arc
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(color.copy(alpha = 0.5f), color),
                        center = center
                    ),
                    startAngle = startAngle,
                    sweepAngle = normalized * totalSweep,
                    useCenter = false,
                    topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                    size = Size(outerRadius * 2, outerRadius * 2),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // Outer knurled metal knob body shadow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF323947), Color(0xFF181C23), Color(0xFF0C0E12)),
                        center = center - Offset(knobRadius * 0.2f, knobRadius * 0.2f),
                        radius = knobRadius
                    ),
                    radius = knobRadius,
                    center = center
                )

                // Bevel ring
                drawCircle(
                    color = Color(0xFF4B5565),
                    radius = knobRadius,
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )

                // Inner cap
                val innerCapRadius = knobRadius * 0.75f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF262C38), Color(0xFF13161C)),
                        center = center,
                        radius = innerCapRadius
                    ),
                    radius = innerCapRadius,
                    center = center
                )

                // Indicator line/pip
                val angleRad = Math.toRadians(currentAngle.toDouble())
                val pipStartRadius = innerCapRadius * 0.35f
                val pipEndRadius = knobRadius * 0.92f
                val p1 = Offset(
                    (center.x + pipStartRadius * cos(angleRad)).toFloat(),
                    (center.y + pipStartRadius * sin(angleRad)).toFloat()
                )
                val p2 = Offset(
                    (center.x + pipEndRadius * cos(angleRad)).toFloat(),
                    (center.y + pipEndRadius * sin(angleRad)).toFloat()
                )

                drawLine(
                    color = color,
                    start = p1,
                    end = p2,
                    strokeWidth = 2.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Silkscreen Label
        Text(
            text = label.uppercase(),
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            letterSpacing = 0.5.sp,
            maxLines = 1
        )

        // Digital Readout
        Text(
            text = displayValue,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            color = color,
            maxLines = 1
        )
    }
}

/**
 * Real Hardware Rocker / Pilot Toggle Switch with vintage metallic bezel and glowing LED.
 */
@Composable
fun HardwareToggleSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    ledColor: Color = FruityLime,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF14171F))
            .border(1.dp, Color(0xFF2B3242), RoundedCornerShape(4.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Pilot LED Bulb with glow
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(if (checked) ledColor else Color(0xFF2C3240))
                .border(
                    0.8.dp,
                    if (checked) Color.White.copy(alpha = 0.6f) else Color(0xFF1A1E27),
                    CircleShape
                )
        )

        Text(
            text = label.uppercase(),
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = if (checked) TextPrimary else TextMuted
        )

        // Miniature switch lever
        Box(
            modifier = Modifier
                .width(22.dp)
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (checked) Color(0xFF1E2837) else Color(0xFF0F1218))
                .border(0.5.dp, Color(0xFF333D50), RoundedCornerShape(6.dp)),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFFE2E8F0), Color(0xFF94A3B8))
                        )
                    )
            )
        }
    }
}

/**
 * Hardware Multi-Segment LED Ladder Peak Meter (-48dB to +6dB).
 */
@Composable
fun HardwareVuMeter(
    peak: Float,
    modifier: Modifier = Modifier,
    height: Dp = 100.dp,
    width: Dp = 14.dp
) {
    val clampedPeak = peak.coerceIn(0f, 1.5f)

    Column(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(2.dp))
            .background(Color(0xFF090B0E))
            .border(0.8.dp, Color(0xFF1A1F29), RoundedCornerShape(2.dp))
            .padding(1.5.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        // 16 discrete LED ladder segments
        val segments = 16
        for (i in (segments - 1) downTo 0) {
            val threshold = (i.toFloat() / segments.toFloat())
            val isActive = clampedPeak > threshold

            val segmentColor = when {
                i >= 14 -> LedRed
                i >= 11 -> LedAmber
                else -> LedGreen
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(
                        if (isActive) segmentColor else segmentColor.copy(alpha = 0.15f)
                    )
            )
        }
    }
}

/**
 * High-End Brushed Aluminum Console Fader with etched dB markings.
 */
@Composable
fun HardwareConsoleFader(
    volumeDb: Float,
    onVolumeChange: (Float) -> Unit,
    peakLevel: Float,
    trackName: String,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    modifier: Modifier = Modifier
) {
    val normalizedVol = ((volumeDb + 48f) / 54f).coerceIn(0f, 1f)

    Column(
        modifier = modifier
            .width(64.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF161922))
            .border(1.dp, Color(0xFF262C3A), RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Track Label & Peak Readout
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = trackName.take(7).uppercase(),
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = TextPrimary,
                maxLines = 1
            )
            Text(
                text = if (volumeDb <= -47.9f) "-∞ dB" else String.format("%+.1f dB", volumeDb),
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                color = if (volumeDb > 0f) LedAmber else TextSecondary
            )
        }

        // Fader Slot + Fader Cap + VU Meter
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // dB Scale Legend
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                Text("+6", fontSize = 6.5.sp, color = TextMuted)
                Text("0", fontSize = 6.5.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                Text("-6", fontSize = 6.5.sp, color = TextMuted)
                Text("-18", fontSize = 6.5.sp, color = TextMuted)
                Text("-inf", fontSize = 6.5.sp, color = TextMuted)
            }

            Spacer(modifier = Modifier.width(3.dp))

            // Vertical Fader Track & Drag Cap
            Box(
                modifier = Modifier
                    .width(26.dp)
                    .fillMaxHeight()
                    .pointerInput(Unit) {
                        detectDragGestures { _, dragAmount ->
                            val deltaNorm = -dragAmount.y / 200f
                            val newNorm = (normalizedVol + deltaNorm).coerceIn(0f, 1f)
                            val newDb = -48f + (newNorm * 54f)
                            onVolumeChange(newDb)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Central Fader Slot Groove
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF090A0E))
                        .border(0.5.dp, Color(0xFF1E232E))
                )

                // Machined Brushed Aluminum Fader Cap
                Box(
                    modifier = Modifier
                        .fillMaxHeight(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight(normalizedVol)
                            .width(1.dp)
                    ) // Spacer to offset
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .height(28.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFFD1D5DB),
                                        Color(0xFF9CA3AF),
                                        Color(0xFF4B5563),
                                        Color(0xFF1F2937)
                                    )
                                )
                            )
                            .border(1.dp, Color(0xFFE5E7EB).copy(alpha = 0.7f), RoundedCornerShape(3.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        // White central indicator line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(2.dp)
                                .background(Color.White)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(3.dp))

            // Peak Meter
            HardwareVuMeter(peak = peakLevel, height = 120.dp, width = 9.dp)
        }

        // Mute / Solo Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(if (isMuted) Color(0xFF331616) else Color(0xFF1B3828))
                .border(
                    0.8.dp,
                    if (isMuted) LedRed else LedGreen,
                    RoundedCornerShape(3.dp)
                )
                .clickable { onToggleMute() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isMuted) "MUTED" else "ON",
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = if (isMuted) LedRed else LedGreen
            )
        }
    }
}
