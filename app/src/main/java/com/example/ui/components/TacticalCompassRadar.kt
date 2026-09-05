package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.OpticalAoATarget
import com.example.model.SignalSource
import com.example.model.SpatialEmitter
import com.example.ui.theme.*
import kotlin.math.*

@Composable
fun TacticalCompassRadar(
  emitters: List<SpatialEmitter>,
  selectedEmitter: SpatialEmitter?,
  onSelectEmitter: (SpatialEmitter) -> Unit,
  modifier: Modifier = Modifier
) {
  val infiniteTransition = rememberInfiniteTransition(label = "radar_sweep")
  val sweepAngle by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 3500, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "sweep_angle"
  )

  val textMeasurer = rememberTextMeasurer()

  Box(
    modifier = modifier
      .fillMaxWidth()
      .aspectRatio(1f)
      .clip(CircleShape)
      .background(CardSurface)
      .border(1.5.dp, NeonCyan.copy(alpha = 0.6f), CircleShape)
      .testTag("tactical_radar_canvas")
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val center = Offset(size.width / 2f, size.height / 2f)
      val radius = size.width / 2f * 0.92f

      // 1. Concentric Range Rings (5m, 10m, 15m, 20m)
      val rings = 4
      for (i in 1..rings) {
        val r = radius * (i.toFloat() / rings)
        drawCircle(
          color = GridLine.copy(alpha = 0.5f),
          radius = r,
          center = center,
          style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f))
        )
      }

      // 2. Cardinal Crosshairs
      drawLine(
        color = GridLine,
        start = Offset(center.x, center.y - radius),
        end = Offset(center.x, center.y + radius),
        strokeWidth = 1.dp.toPx()
      )
      drawLine(
        color = GridLine,
        start = Offset(center.x - radius, center.y),
        end = Offset(center.x + radius, center.y),
        strokeWidth = 1.dp.toPx()
      )

      // 3. Degree Tick Markers & Cardinal Text
      val cardinals = listOf("N" to 0f, "E" to 90f, "S" to 180f, "W" to 270f)
      for ((lbl, deg) in cardinals) {
        val rad = Math.toRadians((deg - 90).toDouble())
        val textPos = Offset(
          (center.x + (radius + 12f) * cos(rad)).toFloat(),
          (center.y + (radius + 12f) * sin(rad)).toFloat()
        )
        val textLayout = textMeasurer.measure(
          text = lbl,
          style = TextStyle(
            color = if (lbl == "N") LaserCrimson else NeonCyan,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
          )
        )
        drawText(
          textLayoutResult = textLayout,
          topLeft = Offset(textPos.x - textLayout.size.width / 2f, textPos.y - textLayout.size.height / 2f)
        )
      }

      // 4. Rotating Radar Beam Sweep with Gradient Arc
      val sweepRad = Math.toRadians((sweepAngle - 90).toDouble())
      val sweepEnd = Offset(
        (center.x + radius * cos(sweepRad)).toFloat(),
        (center.y + radius * sin(sweepRad)).toFloat()
      )

      val sweepBrush = Brush.sweepGradient(
        colors = listOf(
          NeonCyan.copy(alpha = 0.0f),
          NeonCyan.copy(alpha = 0.02f),
          NeonCyan.copy(alpha = 0.25f),
          NeonCyan.copy(alpha = 0.6f)
        ),
        center = center
      )

      drawArc(
        brush = sweepBrush,
        startAngle = sweepAngle - 60f,
        sweepAngle = 60f,
        useCenter = true,
        size = Size(radius * 2, radius * 2),
        topLeft = Offset(center.x - radius, center.y - radius)
      )

      drawLine(
        color = NeonCyan,
        start = center,
        end = sweepEnd,
        strokeWidth = 2.dp.toPx()
      )

      // 5. Render Detected Emitters & Uncertainty Cones (MUSIC AoA)
      emitters.forEach { em ->
        val rad = Math.toRadians((em.azimuthDeg - 90).toDouble())
        val maxDist = 25.0
        val distFraction = (em.distanceEstMeters / maxDist).coerceIn(0.15, 0.95).toFloat()
        val emRadius = radius * distFraction
        val emPos = Offset(
          (center.x + emRadius * cos(rad)).toFloat(),
          (center.y + emRadius * sin(rad)).toFloat()
        )

        val emColor = if (em.isHostile) LaserCrimson else if (em.source == SignalSource.FSO) OpticPurple else NeonCyan

        // Draw Line of Bearing (LoB)
        drawLine(
          color = emColor.copy(alpha = 0.4f),
          start = center,
          end = emPos,
          strokeWidth = 1.dp.toPx(),
          pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f)
        )

        // Draw AoA Uncertainty Arc
        drawArc(
          color = emColor.copy(alpha = 0.18f),
          startAngle = em.azimuthDeg - 90f - (em.uncertaintyDeg / 2f),
          sweepAngle = em.uncertaintyDeg,
          useCenter = true,
          size = Size(emRadius * 2, emRadius * 2),
          topLeft = Offset(center.x - emRadius, center.y - emRadius)
        )

        // Draw Emitter Dot / Target Icon
        val isSelected = selectedEmitter?.id == em.id
        if (isSelected) {
          drawCircle(
            color = emColor.copy(alpha = 0.35f),
            radius = 12.dp.toPx(),
            center = emPos
          )
        }

        drawCircle(
          color = emColor,
          radius = if (isSelected) 6.dp.toPx() else 4.5.dp.toPx(),
          center = emPos
        )

        // Draw Label Tag
        val tagLayout = textMeasurer.measure(
          text = "${em.label} (${em.azimuthDeg.toInt()}°)",
          style = TextStyle(
            color = emColor,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
          )
        )
        drawText(
          textLayoutResult = tagLayout,
          topLeft = Offset(emPos.x + 8f, emPos.y - tagLayout.size.height / 2f)
        )
      }
    }
  }
}

@Composable
fun QuadrantPhotodiodeTracker(
  target: OpticalAoATarget,
  modifier: Modifier = Modifier
) {
  val textMeasurer = rememberTextMeasurer()

  Box(
    modifier = modifier
      .fillMaxWidth()
      .aspectRatio(1f)
      .clip(RoundedCornerShape(10.dp))
      .background(CardSurface)
      .border(1.dp, OpticPurple.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
      .testTag("qpd_optical_tracker")
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val center = Offset(size.width / 2f, size.height / 2f)
      val qpdRadius = size.width / 2f * 0.85f

      // 4 Quadrants Partition (A, B, C, D)
      val gap = 4.dp.toPx()
      val qColor = CardSurfaceVariant

      // Draw Quadrant Segments
      drawArc(
        color = qColor,
        startAngle = 180f + 2f,
        sweepAngle = 90f - 4f,
        useCenter = true,
        size = Size(qpdRadius * 2, qpdRadius * 2),
        topLeft = Offset(center.x - qpdRadius, center.y - qpdRadius)
      ) // Top-Left: Quadrant A

      drawArc(
        color = qColor,
        startAngle = 270f + 2f,
        sweepAngle = 90f - 4f,
        useCenter = true,
        size = Size(qpdRadius * 2, qpdRadius * 2),
        topLeft = Offset(center.x - qpdRadius, center.y - qpdRadius)
      ) // Top-Right: Quadrant B

      drawArc(
        color = qColor,
        startAngle = 0f + 2f,
        sweepAngle = 90f - 4f,
        useCenter = true,
        size = Size(qpdRadius * 2, qpdRadius * 2),
        topLeft = Offset(center.x - qpdRadius, center.y - qpdRadius)
      ) // Bottom-Right: Quadrant C

      drawArc(
        color = qColor,
        startAngle = 90f + 2f,
        sweepAngle = 90f - 4f,
        useCenter = true,
        size = Size(qpdRadius * 2, qpdRadius * 2),
        topLeft = Offset(center.x - qpdRadius, center.y - qpdRadius)
      ) // Bottom-Left: Quadrant D

      // Outer Ring
      drawCircle(
        color = OpticPurple.copy(alpha = 0.4f),
        radius = qpdRadius,
        center = center,
        style = Stroke(1.5.dp.toPx())
      )

      // Center Crosshair
      drawLine(
        color = GridLine,
        start = Offset(center.x, center.y - qpdRadius),
        end = Offset(center.x, center.y + qpdRadius),
        strokeWidth = 1.dp.toPx()
      )
      drawLine(
        color = GridLine,
        start = Offset(center.x - qpdRadius, center.y),
        end = Offset(center.x + qpdRadius, center.y),
        strokeWidth = 1.dp.toPx()
      )

      // Quadrant Labels
      listOf("Q-A" to Offset(center.x - qpdRadius * 0.5f, center.y - qpdRadius * 0.5f),
        "Q-B" to Offset(center.x + qpdRadius * 0.5f, center.y - qpdRadius * 0.5f),
        "Q-C" to Offset(center.x + qpdRadius * 0.5f, center.y + qpdRadius * 0.5f),
        "Q-D" to Offset(center.x - qpdRadius * 0.5f, center.y + qpdRadius * 0.5f)
      ).forEach { (lbl, pos) ->
        val textLayout = textMeasurer.measure(
          text = lbl,
          style = TextStyle(color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        )
        drawText(textLayout, topLeft = Offset(pos.x - textLayout.size.width / 2f, pos.y - textLayout.size.height / 2f))
      }

      // Optical Laser Spot Position from normalized deltaX and deltaY
      val spotX = center.x + (target.deltaX * qpdRadius * 0.8f)
      val spotY = center.y + (target.deltaY * qpdRadius * 0.8f)
      val spotPos = Offset(spotX, spotY)

      val spotRadius = (target.spotSizeMm * 4f).coerceIn(8f, 28f)

      // Spot glow
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(
            LaserCrimson.copy(alpha = 0.9f),
            OpticPurple.copy(alpha = 0.5f),
            Color.Transparent
          ),
          center = spotPos,
          radius = spotRadius * 2
        ),
        radius = spotRadius * 2,
        center = spotPos
      )

      // Solid Spot Core
      drawCircle(
        color = LaserCrimson,
        radius = spotRadius,
        center = spotPos
      )

      // Crosshair on Spot
      drawLine(
        color = Color.White,
        start = Offset(spotPos.x - 12f, spotPos.y),
        end = Offset(spotPos.x + 12f, spotPos.y),
        strokeWidth = 1.5.dp.toPx()
      )
      drawLine(
        color = Color.White,
        start = Offset(spotPos.x, spotPos.y - 12f),
        end = Offset(spotPos.x, spotPos.y + 12f),
        strokeWidth = 1.5.dp.toPx()
      )
    }
  }
}
