package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.components.FSOShutterCard
import com.example.ui.components.OpticalWavelengthSpectrum
import com.example.ui.components.RFSpectrumChart
import com.example.ui.components.StreamAnomalyThreatCard
import com.example.ui.theme.*

@Composable
fun DefenseHudScreen(
  rfFrame: RFSpectrumFrame,
  fsoFrame: FSOSpectrumFrame,
  shieldConfig: ShieldConfig,
  isScanning: Boolean,
  events: List<DetectionEvent>,
  anomalyReport: StreamAnomalyReport,
  connectionStatus: ConnectionStatus,
  activeServerUrl: String,
  latencyMs: Long,
  autoScanEnabled: Boolean,
  onTriggerGeminiScan: () -> Unit,
  onToggleAutoScan: (Boolean) -> Unit,
  onMitigateThreat: () -> Unit,
  onToggleScanning: () -> Unit,
  onBlockAllFso: () -> Unit,
  onResetFso: () -> Unit,
  onToggleFsoShutter: (String) -> Unit,
  onTriggerTestLaser: () -> Unit,
  onNavigateToRf: () -> Unit,
  onNavigateToFso: () -> Unit,
  modifier: Modifier = Modifier
) {
  val blockedCount = fsoFrame.bands.count { it.blocked || it.shutterState == ShutterState.BLOCKED || it.shutterState == ShutterState.SHIELDED }
  val criticalCount = events.count { it.severity == EventSeverity.CRITICAL }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(ObsidianBlack)
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
    contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp)
  ) {
    // 0. Gemini WebSocket Stream Anomaly Threat Monitor Status Card
    item {
      StreamAnomalyThreatCard(
        report = anomalyReport,
        connectionStatus = connectionStatus,
        activeServerUrl = activeServerUrl,
        latencyMs = latencyMs,
        autoScanEnabled = autoScanEnabled,
        onTriggerGeminiScan = onTriggerGeminiScan,
        onToggleAutoScan = onToggleAutoScan,
        onMitigateThreat = onMitigateThreat
      )
    }

    // 1. Tactical Readiness Matrix Cards
    item {

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Shield Status
        Card(
          shape = RoundedCornerShape(10.dp),
          colors = CardDefaults.cardColors(containerColor = CardSurface),
          border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
              if (blockedCount > 0) listOf(LaserCrimson, LaserCrimsonSubdued)
              else listOf(NeonEmerald, EmeraldSubdued)
            ),
            width = 1.dp
          ),
          modifier = Modifier.weight(1f)
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = "OPTICAL SHIELD",
                style = MaterialTheme.typography.labelSmall.copy(
                  color = TextSecondary,
                  fontFamily = FontFamily.Monospace,
                  fontSize = 10.sp
                )
              )
              Icon(
                imageVector = if (blockedCount > 0) Icons.Default.Shield else Icons.Default.Security,
                contentDescription = null,
                tint = if (blockedCount > 0) LaserCrimson else NeonEmerald,
                modifier = Modifier.size(16.dp)
              )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = if (blockedCount > 0) "$blockedCount BLOCKED" else "DEFENSE ARMED",
              style = MaterialTheme.typography.titleMedium.copy(
                color = if (blockedCount > 0) LaserCrimson else NeonEmerald,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
              )
            )
            Text(
              text = "Auto-Shield: ${if (shieldConfig.autoBlockFso) "ENGAGED" else "OFF"}",
              style = MaterialTheme.typography.bodySmall.copy(
                color = TextMuted,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
              )
            )
          }
        }

        // RF Max Power
        Card(
          shape = RoundedCornerShape(10.dp),
          colors = CardDefaults.cardColors(containerColor = CardSurface),
          border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(listOf(CardBorder, CardBorder)),
            width = 1.dp
          ),
          modifier = Modifier.weight(1f)
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = "RF SPECTRUM PEAK",
                style = MaterialTheme.typography.labelSmall.copy(
                  color = TextSecondary,
                  fontFamily = FontFamily.Monospace,
                  fontSize = 10.sp
                )
              )
              Icon(
                imageVector = Icons.Default.Wifi,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(16.dp)
              )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "${rfFrame.peakPowerDbm} dBm",
              style = MaterialTheme.typography.titleMedium.copy(
                color = if (rfFrame.peakPowerDbm >= shieldConfig.rfHighDbm) LaserCrimson else if (rfFrame.peakPowerDbm >= shieldConfig.rfAlertDbm) AmberAlert else NeonCyan,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
              )
            )
            Text(
              text = rfFrame.peakBand,
              style = MaterialTheme.typography.bodySmall.copy(
                color = TextMuted,
                fontSize = 10.sp
              ),
              maxLines = 1
            )
          }
        }

        // Threat Anomaly Counter
        Card(
          shape = RoundedCornerShape(10.dp),
          colors = CardDefaults.cardColors(containerColor = CardSurface),
          border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
              if (criticalCount > 0) listOf(LaserCrimson, LaserCrimsonSubdued)
              else listOf(CardBorder, CardBorder)
            ),
            width = 1.dp
          ),
          modifier = Modifier.weight(1f)
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = "THREAT MATRIX",
                style = MaterialTheme.typography.labelSmall.copy(
                  color = TextSecondary,
                  fontFamily = FontFamily.Monospace,
                  fontSize = 10.sp
                )
              )
              Icon(
                imageVector = Icons.Default.WarningAmber,
                contentDescription = null,
                tint = if (criticalCount > 0) LaserCrimson else TextMuted,
                modifier = Modifier.size(16.dp)
              )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "$criticalCount CRITICAL",
              style = MaterialTheme.typography.titleMedium.copy(
                color = if (criticalCount > 0) LaserCrimson else TextPrimary,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
              )
            )
            Text(
              text = "${events.size} logged events",
              style = MaterialTheme.typography.bodySmall.copy(
                color = TextMuted,
                fontSize = 10.sp
              )
            )
          }
        }
      }
    }

    // 2. Emergency Quick Defense Action Bar
    item {
      Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurfaceVariant),
        border = CardDefaults.outlinedCardBorder().copy(
          brush = Brush.horizontalGradient(listOf(NeonCyan.copy(0.3f), OpticPurple.copy(0.3f))),
          width = 1.dp
        ),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Text(
            text = "TACTICAL DEFENSE COMMANDS",
            style = MaterialTheme.typography.labelSmall.copy(
              color = NeonCyan,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
              fontSize = 11.sp,
              letterSpacing = 0.5.sp
            )
          )
          Spacer(modifier = Modifier.height(8.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = onBlockAllFso,
              colors = ButtonDefaults.buttonColors(
                containerColor = LaserCrimson,
                contentColor = Color.White
              ),
              shape = RoundedCornerShape(6.dp),
              modifier = Modifier
                .weight(1f)
                .testTag("emergency_block_all_btn"),
              contentPadding = PaddingValues(vertical = 8.dp)
            ) {
              Icon(imageVector = Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "BLOCK ALL FSO",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 10.5.sp,
                  fontFamily = FontFamily.Monospace
                )
              )
            }

            OutlinedButton(
              onClick = onResetFso,
              shape = RoundedCornerShape(6.dp),
              border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = Brush.horizontalGradient(listOf(NeonEmerald, NeonEmerald.copy(0.4f)))
              ),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonEmerald),
              modifier = Modifier
                .weight(1f)
                .testTag("reset_shutters_btn"),
              contentPadding = PaddingValues(vertical = 8.dp)
            ) {
              Icon(imageVector = Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "RESET SHUTTERS",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 10.5.sp,
                  fontFamily = FontFamily.Monospace
                )
              )
            }

            OutlinedButton(
              onClick = onTriggerTestLaser,
              shape = RoundedCornerShape(6.dp),
              border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = Brush.horizontalGradient(listOf(AmberAlert, AmberAlert.copy(0.4f)))
              ),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberAlert),
              modifier = Modifier
                .weight(1f)
                .testTag("test_laser_attack_btn"),
              contentPadding = PaddingValues(vertical = 8.dp)
            ) {
              Icon(imageVector = Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "TEST SURGE",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 10.5.sp,
                  fontFamily = FontFamily.Monospace
                )
              )
            }
          }
        }
      }
    }

    // 3. Live RF Spectrum Preview
    item {
      val defaultSpec = rfFrame.spectra["2.4GHz"] ?: rfFrame.spectra.values.firstOrNull() ?: SpectrumData(emptyList(), emptyList())
      Column {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.GraphicEq, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "RF SPECTRUM SNAPSHOT (2.4 GHz)",
              style = MaterialTheme.typography.labelMedium.copy(
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
              )
            )
          }
          TextButton(
            onClick = onNavigateToRf,
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Text(
              text = "VIEW FULL ANALYZER \u2192",
              style = MaterialTheme.typography.labelSmall.copy(
                color = NeonCyan,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.5.sp
              )
            )
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        RFSpectrumChart(
          spectrumData = defaultSpec,
          alertThresholdDbm = shieldConfig.rfAlertDbm,
          highThresholdDbm = shieldConfig.rfHighDbm,
          modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
        )
      }
    }

    // 4. Optical Wavelength Spectrum Preview
    item {
      Column {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.LightMode, contentDescription = null, tint = OpticPurple, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "FSO OPTICAL SPECTRUM (800 - 1650 nm)",
              style = MaterialTheme.typography.labelMedium.copy(
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
              )
            )
          }
          TextButton(
            onClick = onNavigateToFso,
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Text(
              text = "VIEW SHUTTER MATRIX \u2192",
              style = MaterialTheme.typography.labelSmall.copy(
                color = OpticPurple,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.5.sp
              )
            )
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        OpticalWavelengthSpectrum(
          psd = fsoFrame.psd,
          wavelengths = fsoFrame.wavelengthsNm,
          alertThresholdDbm = shieldConfig.fsoAlertDbm,
          blockThresholdDbm = shieldConfig.fsoBlockDbm,
          modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
        )
      }
    }

    // 5. Active Optical Shutters Live Cards
    item {
      Text(
        text = "ACTIVE OPTICAL LASER CHANNELS",
        style = MaterialTheme.typography.labelMedium.copy(
          color = TextSecondary,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          fontSize = 12.sp
        )
      )
    }

    items(fsoFrame.bands.size) { i ->
      val band = fsoFrame.bands[i]
      FSOShutterCard(
        band = band,
        onToggleShutter = { onToggleFsoShutter(band.name) }
      )
    }
  }
}
