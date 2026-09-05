package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SpectrumData
import com.example.model.WaterfallRow
import com.example.ui.theme.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@OptIn(ExperimentalTextApi::class)
@Composable
fun RFSpectrumChart(
  spectrumData: SpectrumData,
  alertThresholdDbm: Float,
  highThresholdDbm: Float,
  modifier: Modifier = Modifier,
  minDbm: Float = -110f,
  maxDbm: Float = -20f
) {
  val textMeasurer = rememberTextMeasurer()
  var touchedX by remember { mutableStateOf<Float?>(null) }

  val psd = spectrumData.psd
  val freqs = spectrumData.freqMhz

  Box(
    modifier = modifier
      .clip(RoundedCornerShape(8.dp))
      .background(CardSurface)
      .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
      .testTag("rf_spectrum_chart")
  ) {
    Canvas(
      modifier = Modifier
        .fillMaxSize()
        .pointerInput(Unit) {
          detectTapGestures(
            onPress = { offset ->
              touchedX = offset.x
              tryAwaitRelease()
              touchedX = null
            }
          )
        }
        .pointerInput(Unit) {
          detectDragGestures(
            onDragStart = { touchedX = it.x },
            onDrag = { change, _ ->
              touchedX = change.position.x
              change.consume()
            },
            onDragEnd = { touchedX = null },
            onDragCancel = { touchedX = null }
          )
        }
    ) {
      val w = size.width
      val h = size.height
      val padL = 40.dp.toPx()
      val padR = 12.dp.toPx()
      val padT = 16.dp.toPx()
      val padB = 24.dp.toPx()

      val graphW = w - padL - padR
      val graphH = h - padT - padB

      if (graphW <= 0 || graphH <= 0) return@Canvas

      // 1. Draw horizontal grid lines and dBm labels
      val dbmSteps = listOf(0f, -20f, -40f, -60f, -80f, -100f)
      dbmSteps.filter { it in minDbm..maxDbm }.forEach { dbm ->
        val normY = (maxDbm - dbm) / (maxDbm - minDbm)
        val y = padT + normY * graphH

        drawLine(
          color = GridLine,
          start = Offset(padL, y),
          end = Offset(w - padR, y),
          strokeWidth = 1.dp.toPx()
        )

        val textLayout = textMeasurer.measure(
          AnnotatedString("${dbm.toInt()}"),
          style = TextStyle(
            color = TextMuted,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
          )
        )
        drawText(
          textLayoutResult = textLayout,
          topLeft = Offset(padL - textLayout.size.width - 4.dp.toPx(), y - textLayout.size.height / 2f)
        )
      }

      // 2. Draw vertical grid lines (frequency markers)
      if (freqs.isNotEmpty()) {
        val minFreq = freqs.first()
        val maxFreq = freqs.last()
        val freqRange = max(1.0, maxFreq - minFreq)

        val stepCount = 4
        for (i in 0..stepCount) {
          val f = minFreq + i * (freqRange / stepCount)
          val normX = (f - minFreq) / freqRange
          val x = padL + (normX * graphW).toFloat()

          drawLine(
            color = GridLine,
            start = Offset(x, padT),
            end = Offset(x, h - padB),
            strokeWidth = 1.dp.toPx()
          )

          val labelStr = if (f >= 1000) String.format("%.1fG", f / 1000.0) else String.format("%.0fM", f)
          val textLayout = textMeasurer.measure(
            AnnotatedString(labelStr),
            style = TextStyle(
              color = TextMuted,
              fontSize = 9.sp,
              fontFamily = FontFamily.Monospace
            )
          )
          drawText(
            textLayoutResult = textLayout,
            topLeft = Offset(x - textLayout.size.width / 2f, h - padB + 4.dp.toPx())
          )
        }
      }

      // 3. Draw Alert & High Threshold dashed lines
      if (alertThresholdDbm in minDbm..maxDbm) {
        val normY = (maxDbm - alertThresholdDbm) / (maxDbm - minDbm)
        val y = padT + normY * graphH
        drawLine(
          color = AmberAlert,
          start = Offset(padL, y),
          end = Offset(w - padR, y),
          strokeWidth = 1.5.dp.toPx(),
          pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
        )
      }

      if (highThresholdDbm in minDbm..maxDbm) {
        val normY = (maxDbm - highThresholdDbm) / (maxDbm - minDbm)
        val y = padT + normY * graphH
        drawLine(
          color = LaserCrimson,
          start = Offset(padL, y),
          end = Offset(w - padR, y),
          strokeWidth = 1.5.dp.toPx(),
          pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 6f), 0f)
        )
      }

      // 4. Draw PSD Curve
      if (psd.size >= 2) {
        val strokePath = Path()
        val fillPath = Path()

        var peakX = 0f
        var peakY = h
        var peakDbm = -999.0
        var peakFreq = 0.0

        for (i in psd.indices) {
          val pDbm = psd[i].toFloat().coerceIn(minDbm, maxDbm)
          val normX = i.toFloat() / (psd.size - 1)
          val normY = (maxDbm - pDbm) / (maxDbm - minDbm)

          val x = padL + normX * graphW
          val y = padT + normY * graphH

          if (i == 0) {
            strokePath.moveTo(x, y)
            fillPath.moveTo(x, h - padB)
            fillPath.lineTo(x, y)
          } else {
            strokePath.lineTo(x, y)
            fillPath.lineTo(x, y)
          }

          if (psd[i] > peakDbm) {
            peakDbm = psd[i]
            peakX = x
            peakY = y
            if (i < freqs.size) peakFreq = freqs[i]
          }
        }

        fillPath.lineTo(padL + graphW, h - padB)
        fillPath.close()

        // Fill area under curve
        drawPath(
          path = fillPath,
          brush = Brush.verticalGradient(
            colors = listOf(
              NeonCyan.copy(alpha = 0.45f),
              NeonCyan.copy(alpha = 0.15f),
              Color.Transparent
            ),
            startY = padT,
            endY = h - padB
          )
        )

        // Draw glowing stroke line
        drawPath(
          path = strokePath,
          color = NeonCyan,
          style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Peak marker
        if (peakDbm > minDbm + 10) {
          drawCircle(
            color = if (peakDbm >= highThresholdDbm) LaserCrimson else if (peakDbm >= alertThresholdDbm) AmberAlert else NeonCyan,
            radius = 4.dp.toPx(),
            center = Offset(peakX, peakY)
          )

          val peakText = "${peakDbm.toInt()} dBm"
          val peakLayout = textMeasurer.measure(
            AnnotatedString(peakText),
            style = TextStyle(
              color = Color.White,
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace
            )
          )
          val bgW = peakLayout.size.width + 8.dp.toPx()
          val bgH = peakLayout.size.height + 4.dp.toPx()
          val tagX = (peakX - bgW / 2f).coerceIn(padL, w - padR - bgW)
          val tagY = (peakY - bgH - 6.dp.toPx()).coerceAtLeast(padT)

          drawRoundRect(
            color = CardSurfaceVariant,
            topLeft = Offset(tagX, tagY),
            size = Size(bgW, bgH),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
          )
          drawRoundRect(
            color = if (peakDbm >= highThresholdDbm) LaserCrimson else NeonCyan,
            topLeft = Offset(tagX, tagY),
            size = Size(bgW, bgH),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
            style = Stroke(1.dp.toPx())
          )
          drawText(
            textLayoutResult = peakLayout,
            topLeft = Offset(tagX + 4.dp.toPx(), tagY + 2.dp.toPx())
          )
        }

        // Touched crosshair readout
        touchedX?.let { tx ->
          if (tx in padL..(padL + graphW)) {
            val ratio = (tx - padL) / graphW
            val index = (ratio * (psd.size - 1)).roundToInt().coerceIn(0, psd.size - 1)
            val touchDbm = psd[index]
            val touchFreq = if (index < freqs.size) freqs[index] else 0.0
            val normY = ((maxDbm - touchDbm.toFloat().coerceIn(minDbm, maxDbm)) / (maxDbm - minDbm))
            val ty = padT + normY * graphH

            drawLine(
              color = Color.White.copy(alpha = 0.7f),
              start = Offset(tx, padT),
              end = Offset(tx, h - padB),
              strokeWidth = 1.dp.toPx()
            )
            drawCircle(
              color = NeonEmerald,
              radius = 5.dp.toPx(),
              center = Offset(tx, ty)
            )

            val infoText = "${String.format("%.1f", touchFreq)}MHz: ${touchDbm.toInt()}dBm"
            val infoLayout = textMeasurer.measure(
              AnnotatedString(infoText),
              style = TextStyle(
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
              )
            )
            val infoW = infoLayout.size.width + 10.dp.toPx()
            val infoH = infoLayout.size.height + 6.dp.toPx()
            val infoX = (tx - infoW / 2f).coerceIn(padL, w - padR - infoW)
            val infoY = (ty - infoH - 8.dp.toPx()).coerceAtLeast(padT)

            drawRoundRect(
              color = ObsidianBlack.copy(alpha = 0.9f),
              topLeft = Offset(infoX, infoY),
              size = Size(infoW, infoH),
              cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
            drawRoundRect(
              color = NeonEmerald,
              topLeft = Offset(infoX, infoY),
              size = Size(infoW, infoH),
              cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
              style = Stroke(1.dp.toPx())
            )
            drawText(
              textLayoutResult = infoLayout,
              topLeft = Offset(infoX + 5.dp.toPx(), infoY + 3.dp.toPx())
            )
          }
        }
      }
    }
  }
}

@Composable
fun WaterfallSpectrogram(
  history: List<WaterfallRow>,
  modifier: Modifier = Modifier,
  minDbm: Float = -110f,
  maxDbm: Float = -20f
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(8.dp))
      .background(ObsidianBlack)
      .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
      .testTag("waterfall_spectrogram")
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height

      if (history.isEmpty() || w <= 0 || h <= 0) return@Canvas

      val rowCount = history.size
      val rowHeight = h / rowCount.toFloat()

      history.forEachIndexed { rowIndex, row ->
        val powers = row.powers
        if (powers.isEmpty()) return@forEachIndexed
        val colWidth = w / powers.size.toFloat()
        val y = rowIndex * rowHeight

        for (colIndex in powers.indices) {
          val p = powers[colIndex].coerceIn(minDbm, maxDbm)
          val norm = (p - minDbm) / (maxDbm - minDbm)
          val color = getThermalColor(norm)

          val x = colIndex * colWidth
          drawRect(
            color = color,
            topLeft = Offset(x, y),
            size = Size(colWidth + 0.5f, rowHeight + 0.5f)
          )
        }
      }

      // Draw subtle scanline grid
      val lineCount = 4
      for (i in 1..lineCount) {
        val y = i * (h / lineCount)
        drawLine(
          color = Color(0x1A00F0FF),
          start = Offset(0f, y),
          end = Offset(w, y),
          strokeWidth = 1.dp.toPx()
        )
      }
    }

    // Legend on bottom right
    Row(
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(6.dp)
        .background(ObsidianBlack.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
        .padding(horizontal = 6.dp, vertical = 2.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "TIME \u2193 WATERFALL",
        style = MaterialTheme.typography.labelSmall.copy(
          fontSize = 9.sp,
          color = TextMuted,
          fontFamily = FontFamily.Monospace
        )
      )
    }
  }
}

@OptIn(ExperimentalTextApi::class)
@Composable
fun OpticalWavelengthSpectrum(
  psd: List<Double>,
  wavelengths: List<Double>,
  alertThresholdDbm: Float,
  blockThresholdDbm: Float,
  modifier: Modifier = Modifier,
  minDbm: Float = -80f,
  maxDbm: Float = 0f
) {
  val textMeasurer = rememberTextMeasurer()

  Box(
    modifier = modifier
      .clip(RoundedCornerShape(8.dp))
      .background(CardSurface)
      .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
      .testTag("optical_wavelength_spectrum")
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height
      val padL = 40.dp.toPx()
      val padR = 12.dp.toPx()
      val padT = 16.dp.toPx()
      val padB = 24.dp.toPx()

      val graphW = w - padL - padR
      val graphH = h - padT - padB

      if (graphW <= 0 || graphH <= 0) return@Canvas

      // Grid dBm lines
      val dbmSteps = listOf(0f, -20f, -40f, -60f, -80f)
      dbmSteps.forEach { dbm ->
        val normY = (maxDbm - dbm) / (maxDbm - minDbm)
        val y = padT + normY * graphH

        drawLine(
          color = GridLine,
          start = Offset(padL, y),
          end = Offset(w - padR, y),
          strokeWidth = 1.dp.toPx()
        )

        val textLayout = textMeasurer.measure(
          AnnotatedString("${dbm.toInt()}"),
          style = TextStyle(color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        )
        drawText(
          textLayoutResult = textLayout,
          topLeft = Offset(padL - textLayout.size.width - 4.dp.toPx(), y - textLayout.size.height / 2f)
        )
      }

      // Wavelength X Grid & labels
      if (wavelengths.isNotEmpty()) {
        val minWl = wavelengths.first()
        val maxWl = wavelengths.last()
        val wlRange = max(1.0, maxWl - minWl)

        val keyWls = listOf(850.0, 905.0, 1064.0, 1310.0, 1550.0)
        keyWls.forEach { wl ->
          if (wl in minWl..maxWl) {
            val normX = (wl - minWl) / wlRange
            val x = padL + (normX * graphW).toFloat()

            drawLine(
              color = Color(0x33BD00FF),
              start = Offset(x, padT),
              end = Offset(x, h - padB),
              strokeWidth = 1.dp.toPx()
            )

            val textLayout = textMeasurer.measure(
              AnnotatedString("${wl.toInt()}nm"),
              style = TextStyle(color = OpticPurple, fontSize = 8.5.sp, fontFamily = FontFamily.Monospace)
            )
            drawText(
              textLayoutResult = textLayout,
              topLeft = Offset(x - textLayout.size.width / 2f, h - padB + 4.dp.toPx())
            )
          }
        }
      }

      // Thresholds
      val alertY = padT + ((maxDbm - alertThresholdDbm) / (maxDbm - minDbm)) * graphH
      drawLine(
        color = AmberAlert,
        start = Offset(padL, alertY),
        end = Offset(w - padR, alertY),
        strokeWidth = 1.5.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
      )

      val blockY = padT + ((maxDbm - blockThresholdDbm) / (maxDbm - minDbm)) * graphH
      drawLine(
        color = LaserCrimson,
        start = Offset(padL, blockY),
        end = Offset(w - padR, blockY),
        strokeWidth = 1.5.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 6f), 0f)
      )

      // Plot FSO curve
      if (psd.size >= 2) {
        val strokePath = Path()
        val fillPath = Path()

        for (i in psd.indices) {
          val p = psd[i].toFloat().coerceIn(minDbm, maxDbm)
          val normX = i.toFloat() / (psd.size - 1)
          val normY = (maxDbm - p) / (maxDbm - minDbm)

          val x = padL + normX * graphW
          val y = padT + normY * graphH

          if (i == 0) {
            strokePath.moveTo(x, y)
            fillPath.moveTo(x, h - padB)
            fillPath.lineTo(x, y)
          } else {
            strokePath.lineTo(x, y)
            fillPath.lineTo(x, y)
          }
        }

        fillPath.lineTo(padL + graphW, h - padB)
        fillPath.close()

        drawPath(
          path = fillPath,
          brush = Brush.verticalGradient(
            colors = listOf(
              OpticPurple.copy(alpha = 0.5f),
              OpticPurple.copy(alpha = 0.15f),
              Color.Transparent
            ),
            startY = padT,
            endY = h - padB
          )
        )

        drawPath(
          path = strokePath,
          color = OpticPurple,
          style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
      }
    }
  }
}

/**
 * Thermal heatmap color mapping for Waterfall
 */
private fun getThermalColor(value: Float): Color {
  val v = value.coerceIn(0f, 1f)
  return when {
    v < 0.2f -> {
      val t = v / 0.2f
      lerpColor(Color(0xFF030712), Color(0xFF0C2461), t)
    }
    v < 0.4f -> {
      val t = (v - 0.2f) / 0.2f
      lerpColor(Color(0xFF0C2461), Color(0xFF00A8FF), t)
    }
    v < 0.65f -> {
      val t = (v - 0.4f) / 0.25f
      lerpColor(Color(0xFF00A8FF), Color(0xFF00E676), t)
    }
    v < 0.85f -> {
      val t = (v - 0.65f) / 0.2f
      lerpColor(Color(0xFF00E676), Color(0xFFFFD600), t)
    }
    else -> {
      val t = (v - 0.85f) / 0.15f
      lerpColor(Color(0xFFFFD600), Color(0xFFFF1744), t)
    }
  }
}

private fun lerpColor(c1: Color, c2: Color, t: Float): Color {
  val r = c1.red + (c2.red - c1.red) * t
  val g = c1.green + (c2.green - c1.green) * t
  val b = c1.blue + (c2.blue - c1.blue) * t
  return Color(r, g, b, 1f)
}
