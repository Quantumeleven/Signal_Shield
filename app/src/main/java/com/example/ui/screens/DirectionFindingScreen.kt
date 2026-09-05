package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.OpticalAoATarget
import com.example.model.SignalSource
import com.example.model.SpatialEmitter
import com.example.ui.components.QuadrantPhotodiodeTracker
import com.example.ui.components.TacticalCompassRadar
import com.example.ui.theme.*

@Composable
fun DirectionFindingScreen(
  emitters: List<SpatialEmitter>,
  opticalTarget: OpticalAoATarget,
  selectedEmitter: SpatialEmitter?,
  onSelectEmitter: (SpatialEmitter) -> Unit,
  modifier: Modifier = Modifier
) {
  var activeMode by remember { mutableStateOf("RF_AOA") } // "RF_AOA" or "FSO_QPD"

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(ObsidianBlack)
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
    contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp)
  ) {
    // 1. Mode Selector (RF MUSIC AoA vs FSO QPD Tracker)
    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(8.dp))
          .background(CardSurface)
          .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
          .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(6.dp))
            .background(if (activeMode == "RF_AOA") NeonCyan.copy(0.2f) else CardSurface)
            .border(1.dp, if (activeMode == "RF_AOA") NeonCyan else CardBorder, RoundedCornerShape(6.dp))
            .clickable { activeMode = "RF_AOA" }
            .padding(vertical = 8.dp)
            .testTag("aoa_mode_rf_tab"),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "RF AoA / MUSIC RADAR",
            style = MaterialTheme.typography.labelSmall.copy(
              color = if (activeMode == "RF_AOA") NeonCyan else TextSecondary,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
              fontSize = 11.sp
            )
          )
        }

        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(6.dp))
            .background(if (activeMode == "FSO_QPD") OpticPurple.copy(0.2f) else CardSurface)
            .border(1.dp, if (activeMode == "FSO_QPD") OpticPurple else CardBorder, RoundedCornerShape(6.dp))
            .clickable { activeMode = "FSO_QPD" }
            .padding(vertical = 8.dp)
            .testTag("aoa_mode_fso_tab"),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "FSO QPD OPTICAL RETICLE",
            style = MaterialTheme.typography.labelSmall.copy(
              color = if (activeMode == "FSO_QPD") OpticPurple else TextSecondary,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
              fontSize = 11.sp
            )
          )
        }
      }
    }

    // 2. Main Radar or QPD Reticle Canvas
    item {
      if (activeMode == "RF_AOA") {
        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "360° SPATIAL EMITTER LOCALIZATION",
              style = MaterialTheme.typography.labelMedium.copy(
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.5.sp
              )
            )
            Text(
              text = "KrakenSDR 5-Ch Array",
              style = MaterialTheme.typography.labelSmall.copy(
                color = NeonCyan,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
              )
            )
          }

          Spacer(modifier = Modifier.height(6.dp))

          TacticalCompassRadar(
            emitters = emitters,
            selectedEmitter = selectedEmitter,
            onSelectEmitter = onSelectEmitter,
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp)
          )
        }
      } else {
        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "QUADRANT PHOTODIODE (QPD) RETICLE",
              style = MaterialTheme.typography.labelMedium.copy(
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.5.sp
              )
            )
            Text(
              text = "arXiv:2303.07222",
              style = MaterialTheme.typography.labelSmall.copy(
                color = OpticPurple,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
              )
            )
          }

          Spacer(modifier = Modifier.height(6.dp))

          QuadrantPhotodiodeTracker(
            target = opticalTarget,
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp)
          )

          Spacer(modifier = Modifier.height(8.dp))

          Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Column {
                Text("INCIDENCE DELTA X", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace))
                Text(String.format("%.3f", opticalTarget.deltaX), style = MaterialTheme.typography.labelSmall.copy(color = NeonCyan, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp))
              }
              Column {
                Text("INCIDENCE DELTA Y", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace))
                Text(String.format("%.3f", opticalTarget.deltaY), style = MaterialTheme.typography.labelSmall.copy(color = NeonCyan, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp))
              }
              Column {
                Text("BEAM WAVELENGTH", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace))
                Text("${opticalTarget.wavelengthNm.toInt()} nm", style = MaterialTheme.typography.labelSmall.copy(color = OpticPurple, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp))
              }
              Column {
                Text("OPTICAL POWER", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace))
                Text(String.format("%.2f mW", opticalTarget.intensityWatts * 1000), style = MaterialTheme.typography.labelSmall.copy(color = LaserCrimson, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp))
              }
            }
          }
        }
      }
    }

    // 3. Emitter List Header
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "TRACKED SPATIAL EMITTERS (${emitters.size})",
          style = MaterialTheme.typography.labelMedium.copy(
            color = TextSecondary,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
          )
        )
      }
    }

    // 4. Emitter Cards
    items(emitters.size) { i ->
      val em = emitters[i]
      val isSel = selectedEmitter?.id == em.id
      val emColor = if (em.isHostile) LaserCrimson else if (em.source == SignalSource.FSO) OpticPurple else NeonCyan

      Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = CardDefaults.outlinedCardBorder().copy(
          brush = Brush.horizontalGradient(
            if (isSel) listOf(emColor, emColor.copy(0.4f))
            else listOf(CardBorder, CardBorder)
          ),
          width = if (isSel) 1.5.dp else 0.5.dp
        ),
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onSelectEmitter(em) }
          .testTag("emitter_item_${em.id}")
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = if (em.isHostile) Icons.Default.WarningAmber else Icons.Default.GpsFixed,
                contentDescription = null,
                tint = emColor,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = em.label,
                style = MaterialTheme.typography.titleMedium.copy(
                  color = TextPrimary,
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp
                )
              )
            }

            Surface(
              color = emColor.copy(alpha = 0.15f),
              shape = RoundedCornerShape(4.dp)
            ) {
              Text(
                text = if (em.isHostile) "THREAT" else "DETECTED",
                style = MaterialTheme.typography.labelSmall.copy(
                  color = emColor,
                  fontWeight = FontWeight.Bold,
                  fontSize = 8.5.sp,
                  fontFamily = FontFamily.Monospace
                ),
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(4.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = "AZIMUTH: ${em.azimuthDeg.toInt()}° ± ${em.uncertaintyDeg}°",
              style = MaterialTheme.typography.labelSmall.copy(
                color = emColor,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
              )
            )

            Text(
              text = "EST. DIST: ${em.distanceEstMeters.toInt()} m",
              style = MaterialTheme.typography.labelSmall.copy(
                color = TextSecondary,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
              )
            )
          }

          Text(
            text = "Freq/Band: ${em.frequencyOrWavelength} • Power: ${em.powerDbm.toInt()} dBm",
            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
          )

          Text(
            text = "Method: ${em.method}",
            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 9.5.sp, fontFamily = FontFamily.Monospace)
          )
        }
      }
    }
  }
}
