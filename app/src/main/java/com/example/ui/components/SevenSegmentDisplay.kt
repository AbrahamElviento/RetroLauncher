package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Calendar

/**
 * 7-Segment representation map for characters '0'..'9', ' ', '-', etc.
 * Segments: 0=A (Top), 1=B (Top-Right), 2=C (Bottom-Right), 3=D (Bottom), 4=E (Bottom-Left), 5=F (Top-Left), 6=G (Middle)
 */
object SevenSegmentPatterns {
    val DIGITS: Map<Char, BooleanArray> = mapOf(
        '0' to booleanArrayOf(true, true, true, true, true, true, false),
        '1' to booleanArrayOf(false, true, true, false, false, false, false),
        '2' to booleanArrayOf(true, true, false, true, true, false, true),
        '3' to booleanArrayOf(true, true, true, true, false, false, true),
        '4' to booleanArrayOf(false, true, true, false, false, true, true),
        '5' to booleanArrayOf(true, false, true, true, false, true, true),
        '6' to booleanArrayOf(true, false, true, true, true, true, true),
        '7' to booleanArrayOf(true, true, true, false, false, false, false),
        '8' to booleanArrayOf(true, true, true, true, true, true, true),
        '9' to booleanArrayOf(true, true, true, true, false, true, true),
        '-' to booleanArrayOf(false, false, false, false, false, false, true),
        ' ' to booleanArrayOf(false, false, false, false, false, false, false)
    )
}

@Composable
fun SevenSegmentDigit(
    char: Char,
    color: Color,
    modifier: Modifier = Modifier,
    inactiveAlpha: Float = 0.08f,
    slantDegrees: Float = 5f
) {
    val segments = SevenSegmentPatterns.DIGITS[char] ?: SevenSegmentPatterns.DIGITS[' ']!!

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val slantRad = Math.toRadians(slantDegrees.toDouble()).toFloat()
        val shearX = Math.tan(slantRad.toDouble()).toFloat()

        val t = (w * 0.16f).coerceIn(2f, 16f) // segment thickness
        val gap = t * 0.22f // gap between segments

        val midY = h / 2f

        // Helper function to draw a sheared polygon with slant
        fun drawSegment(points: List<Offset>, isActive: Boolean) {
            val drawColor = if (isActive) color else color.copy(alpha = inactiveAlpha)
            val path = Path().apply {
                val first = points.first()
                // Apply slant shear relative to bottom of digit
                val firstShearedX = first.x + (h - first.y) * shearX
                moveTo(firstShearedX, first.y)
                for (i in 1 until points.size) {
                    val pt = points[i]
                    val shearedX = pt.x + (h - pt.y) * shearX
                    lineTo(shearedX, pt.y)
                }
                close()
            }
            drawPath(path, drawColor, style = Fill)
        }

        // Segment A: Top Horizontal
        drawSegment(
            listOf(
                Offset(gap + t * 0.5f, 0f),
                Offset(w - gap - t * 0.5f, 0f),
                Offset(w - gap - t, t),
                Offset(gap + t, t)
            ),
            segments[0]
        )

        // Segment B: Top-Right Vertical
        drawSegment(
            listOf(
                Offset(w, gap + t * 0.5f),
                Offset(w, midY - gap * 0.5f),
                Offset(w - t, midY - gap * 0.5f - t * 0.5f),
                Offset(w - t, gap + t)
            ),
            segments[1]
        )

        // Segment C: Bottom-Right Vertical
        drawSegment(
            listOf(
                Offset(w, midY + gap * 0.5f),
                Offset(w, h - gap - t * 0.5f),
                Offset(w - t, h - gap - t),
                Offset(w - t, midY + gap * 0.5f + t * 0.5f)
            ),
            segments[2]
        )

        // Segment D: Bottom Horizontal
        drawSegment(
            listOf(
                Offset(gap + t, h - t),
                Offset(w - gap - t, h - t),
                Offset(w - gap - t * 0.5f, h),
                Offset(gap + t * 0.5f, h)
            ),
            segments[3]
        )

        // Segment E: Bottom-Left Vertical
        drawSegment(
            listOf(
                Offset(0f, midY + gap * 0.5f),
                Offset(t, midY + gap * 0.5f + t * 0.5f),
                Offset(t, h - gap - t),
                Offset(0f, h - gap - t * 0.5f)
            ),
            segments[4]
        )

        // Segment F: Top-Left Vertical
        drawSegment(
            listOf(
                Offset(0f, gap + t * 0.5f),
                Offset(t, gap + t),
                Offset(t, midY - gap * 0.5f - t * 0.5f),
                Offset(0f, midY - gap * 0.5f)
            ),
            segments[5]
        )

        // Segment G: Middle Horizontal
        drawSegment(
            listOf(
                Offset(gap + t * 0.6f, midY),
                Offset(gap + t, midY - t * 0.5f),
                Offset(w - gap - t, midY - t * 0.5f),
                Offset(w - gap - t * 0.6f, midY),
                Offset(w - gap - t, midY + t * 0.5f),
                Offset(gap + t, midY + t * 0.5f)
            ),
            segments[6]
        )
    }
}

@Composable
fun SevenSegmentColon(
    color: Color,
    modifier: Modifier = Modifier,
    isActive: Boolean = true,
    inactiveAlpha: Float = 0.08f,
    slantDegrees: Float = 5f
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val slantRad = Math.toRadians(slantDegrees.toDouble()).toFloat()
        val shearX = Math.tan(slantRad.toDouble()).toFloat()

        val dotSize = (w * 0.7f).coerceAtLeast(3f)
        val dotRadius = dotSize / 2f
        val drawColor = if (isActive) color else color.copy(alpha = inactiveAlpha)

        val topDotCenterY = h * 0.33f
        val bottomDotCenterY = h * 0.67f

        val topShearedX = (w / 2f) + (h - topDotCenterY) * shearX
        val bottomShearedX = (w / 2f) + (h - bottomDotCenterY) * shearX

        drawCircle(
            color = drawColor,
            radius = dotRadius,
            center = Offset(topShearedX, topDotCenterY)
        )
        drawCircle(
            color = drawColor,
            radius = dotRadius,
            center = Offset(bottomShearedX, bottomDotCenterY)
        )
    }
}

@Composable
fun SevenSegmentClock(
    is24Hour: Boolean = false,
    color: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier,
    digitHeight: Dp = 32.dp,
    showSeconds: Boolean = true,
    showContainerBackground: Boolean = false
) {
    var hoursStr by remember { mutableStateOf("00") }
    var minutesStr by remember { mutableStateOf("00") }
    var secondsStr by remember { mutableStateOf("00") }
    var isAm by remember { mutableStateOf(true) }
    var isColonOn by remember { mutableStateOf(true) }

    LaunchedEffect(is24Hour) {
        while (true) {
            val cal = Calendar.getInstance()
            val rawHour = if (is24Hour) {
                cal.get(Calendar.HOUR_OF_DAY)
            } else {
                val h = cal.get(Calendar.HOUR)
                if (h == 0) 12 else h
            }
            val min = cal.get(Calendar.MINUTE)
            val sec = cal.get(Calendar.SECOND)
            val amPm = cal.get(Calendar.AM_PM)

            hoursStr = String.format("%02d", rawHour)
            minutesStr = String.format("%02d", min)
            secondsStr = String.format("%02d", sec)
            isAm = (amPm == Calendar.AM)
            isColonOn = true
            delay(500)
            isColonOn = false
            delay(500)
        }
    }

    val digitWidth = digitHeight * 0.52f
    val colonWidth = digitHeight * 0.22f
    val secDigitHeight = digitHeight * 0.72f
    val secDigitWidth = secDigitHeight * 0.52f
    val spacing = (digitHeight * 0.08f).coerceAtLeast(2.dp)

    val content = @Composable {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Hours
            SevenSegmentDigit(
                char = hoursStr.getOrNull(0) ?: '0',
                color = color,
                modifier = Modifier
                    .width(digitWidth)
                    .height(digitHeight)
            )
            Spacer(modifier = Modifier.width(spacing))
            SevenSegmentDigit(
                char = hoursStr.getOrNull(1) ?: '0',
                color = color,
                modifier = Modifier
                    .width(digitWidth)
                    .height(digitHeight)
            )

            // Colon
            Spacer(modifier = Modifier.width(spacing))
            SevenSegmentColon(
                color = color,
                isActive = isColonOn,
                modifier = Modifier
                    .width(colonWidth)
                    .height(digitHeight)
            )
            Spacer(modifier = Modifier.width(spacing))

            // Minutes
            SevenSegmentDigit(
                char = minutesStr.getOrNull(0) ?: '0',
                color = color,
                modifier = Modifier
                    .width(digitWidth)
                    .height(digitHeight)
            )
            Spacer(modifier = Modifier.width(spacing))
            SevenSegmentDigit(
                char = minutesStr.getOrNull(1) ?: '0',
                color = color,
                modifier = Modifier
                    .width(digitWidth)
                    .height(digitHeight)
            )

            // Optional Seconds & AM/PM
            if (showSeconds) {
                Spacer(modifier = Modifier.width(spacing))
                SevenSegmentColon(
                    color = color,
                    isActive = isColonOn,
                    modifier = Modifier
                        .width(colonWidth * 0.8f)
                        .height(secDigitHeight)
                )
                Spacer(modifier = Modifier.width(spacing))

                SevenSegmentDigit(
                    char = secondsStr.getOrNull(0) ?: '0',
                    color = color,
                    modifier = Modifier
                        .width(secDigitWidth)
                        .height(secDigitHeight)
                )
                Spacer(modifier = Modifier.width(spacing * 0.8f))
                SevenSegmentDigit(
                    char = secondsStr.getOrNull(1) ?: '0',
                    color = color,
                    modifier = Modifier
                        .width(secDigitWidth)
                        .height(secDigitHeight)
                )
            }

            // 12-hour AM / PM indicator
            if (!is24Hour) {
                Spacer(modifier = Modifier.width(spacing * 1.5f))
                Column(
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.height(digitHeight * 0.8f)
                ) {
                    Text(
                        text = "AM",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = (digitHeight.value * 0.28f).sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = if (isAm) color else color.copy(alpha = 0.12f)
                    )
                    Text(
                        text = "PM",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = (digitHeight.value * 0.28f).sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = if (!isAm) color else color.copy(alpha = 0.12f)
                    )
                }
            }
        }
    }

    if (showContainerBackground) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF0F1412).copy(alpha = 0.95f))
                .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    } else {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}
