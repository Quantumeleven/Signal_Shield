package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*

@Composable
fun TacticalTopBar(
  connectionStatus: ConnectionStatus,
  latencyMs: Long,
  isScanning: Boolean,
  threatCount: Int,
  onToggleScanning: () -> Unit,
  onOpenSettings: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    color = SurfaceDark,
    modifier = modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      // Left: App Logo & Status Indicator
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
              Brush.linearGradient(listOf(NeonCyan.copy(0.2f), OpticPurple.copy(0.2f)))
            )
            .border(1.dp, NeonCyan, RoundedCornerShape(8.dp)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = "Shield Icon",
            tint = if (threatCount > 0) LaserCrimson else NeonCyan,
            modifier = Modifier.size(20.dp)
          )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column {
          Text(
            text = "SIGNAL SHIELD",
            style = MaterialTheme.typography.titleMedium.copy(
              color = TextPrimary,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.sp
            )
          )
          Row(verticalAlignment = Alignment.CenterVertically) {
            val (statusText, statusColor) = when (connectionStatus) {
              ConnectionStatus.CONNECTED -> "BRIDGE ONLINE (${latencyMs}ms)" to NeonEmerald
              ConnectionStatus.CONNECTING -> "CONNECTING..." to AmberAlert
              ConnectionStatus.SIMULATION -> "SDR/FSO SIMULATOR" to NeonCyan
              ConnectionStatus.DISCONNECTED -> "STANDALONE" to TextMuted
            }

            Box(
              modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(statusColor)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
              text = statusText,
              style = MaterialTheme.typography.labelSmall.copy(
                color = statusColor,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
              )
            )
          }
        }
      }

      // Right: Actions (Scan toggle & Config)
      Row(verticalAlignment = Alignment.CenterVertically) {
        Button(
          onClick = onToggleScanning,
          colors = ButtonDefaults.buttonColors(
            containerColor = if (isScanning) NeonEmerald.copy(alpha = 0.15f) else AmberAlert.copy(alpha = 0.15f),
            contentColor = if (isScanning) NeonEmerald else AmberAlert
          ),
          border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = Brush.horizontalGradient(
              listOf(
                if (isScanning) NeonEmerald else AmberAlert,
                if (isScanning) NeonEmerald.copy(0.5f) else AmberAlert.copy(0.5f)
              )
            )
          ),
          shape = RoundedCornerShape(6.dp),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
          modifier = Modifier.testTag("toggle_scan_btn")
        ) {
          Icon(
            imageVector = if (isScanning) Icons.Default.Sensors else Icons.Default.Pause,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = if (isScanning) "SCANNING" else "PAUSED",
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 11.sp,
              fontFamily = FontFamily.Monospace
            )
          )
        }

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
          onClick = onOpenSettings,
          modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(CardSurface)
            .border(1.dp, CardBorder, RoundedCornerShape(6.dp))
            .testTag("bridge_settings_btn")
        ) {
          Icon(
            imageVector = Icons.Default.Tune,
            contentDescription = "Config & Settings",
            tint = TextSecondary,
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }
  }
}

@Composable
fun ThreatAlertBanner(
  event: DetectionEvent,
  onDismiss: () -> Unit,
  onViewDetails: () -> Unit,
  modifier: Modifier = Modifier
) {
  val infiniteTransition = rememberInfiniteTransition(label = "pulse")
  val pulseBorder by infiniteTransition.animateColor(
    initialValue = LaserCrimson,
    targetValue = LaserCrimson.copy(alpha = 0.3f),
    animationSpec = infiniteRepeatable(
      animation = tween(600, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "bannerBorder"
  )

  Surface(
    color = CardSurfaceVariant,
    shape = RoundedCornerShape(8.dp),
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 4.dp)
      .border(1.5.dp, pulseBorder, RoundedCornerShape(8.dp))
      .testTag("threat_alert_banner")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f)
      ) {
        Box(
          modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(LaserCrimson.copy(alpha = 0.2f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Threat Alert",
            tint = LaserCrimson,
            modifier = Modifier.size(18.dp)
          )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "INCURSION: ${event.threatType}",
              style = MaterialTheme.typography.labelSmall.copy(
                color = LaserCrimson,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
              )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "${event.powerDbm} dBm",
              style = MaterialTheme.typography.labelSmall.copy(
                color = AmberAlert,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
              )
            )
          }
          Text(
            text = "${event.source} • ${event.band} — ${event.actionTaken}",
            style = MaterialTheme.typography.bodyMedium.copy(
              color = TextSecondary,
              fontSize = 11.sp
            ),
            maxLines = 1
          )
        }
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(
          onClick = onViewDetails,
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Text(
            text = "LOG",
            style = MaterialTheme.typography.labelSmall.copy(
              color = NeonCyan,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace
            )
          )
        }

        IconButton(
          onClick = onDismiss,
          modifier = Modifier.size(28.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Dismiss",
            tint = TextMuted,
            modifier = Modifier.size(16.dp)
          )
        }
      }
    }
  }
}

@Composable
fun FSOShutterCard(
  band: FSOBandInfo,
  onToggleShutter: () -> Unit,
  modifier: Modifier = Modifier
) {
  val isBlocked = band.blocked || band.shutterState == ShutterState.BLOCKED || band.shutterState == ShutterState.SHIELDED

  Card(
    shape = RoundedCornerShape(10.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (isBlocked) CardSurfaceVariant else CardSurface
    ),
    border = CardDefaults.outlinedCardBorder().copy(
      brush = Brush.horizontalGradient(
        if (isBlocked) listOf(LaserCrimson, LaserCrimsonSubdued)
        else if (band.alert) listOf(AmberAlert, AmberSubdued)
        else listOf(CardBorder, CardBorder)
      ),
      width = if (isBlocked || band.alert) 1.5.dp else 1.dp
    ),
    modifier = modifier
      .fillMaxWidth()
      .testTag("fso_shutter_card_${band.name}")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      // Left: Laser info & wavelength badge
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Box(
          modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isBlocked) LaserCrimson.copy(0.15f) else OpticPurple.copy(0.15f))
            .border(
              1.dp,
              if (isBlocked) LaserCrimson else OpticPurple,
              RoundedCornerShape(8.dp)
            ),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
              imageVector = if (isBlocked) Icons.Default.Shield else Icons.Default.FlashOn,
              contentDescription = null,
              tint = if (isBlocked) LaserCrimson else OpticPurple,
              modifier = Modifier.size(16.dp)
            )
            Text(
              text = "${band.wavelengthNm.toInt()}nm",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.5.sp,
                color = if (isBlocked) LaserCrimson else OpticPurple,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
              )
            )
          }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = band.name,
              style = MaterialTheme.typography.titleMedium.copy(
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
              )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
              color = if (isBlocked) LaserCrimson.copy(0.2f) else EmeraldSubdued.copy(0.3f),
              shape = RoundedCornerShape(4.dp)
            ) {
              Text(
                text = if (isBlocked) "SHIELD ACTIVE" else "OPTICAL OPEN",
                style = MaterialTheme.typography.labelSmall.copy(
                  color = if (isBlocked) LaserCrimson else NeonEmerald,
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace
                ),
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(3.dp))

          Text(
            text = "${band.freqThz} THz • ${band.modulation}",
            style = MaterialTheme.typography.bodyMedium.copy(
              color = TextSecondary,
              fontSize = 11.sp,
              fontFamily = FontFamily.Monospace
            )
          )

          Spacer(modifier = Modifier.height(2.dp))

          // Power, SNR, Bitrate stats
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Text(
              text = "Pwr: ${band.powerDbm} dBm",
              style = MaterialTheme.typography.labelSmall.copy(
                color = if (band.powerDbm >= -20) LaserCrimson else if (band.powerDbm >= -40) AmberAlert else TextPrimary,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
            )
            Text(
              text = "SNR: ${band.snrDb} dB",
              style = MaterialTheme.typography.labelSmall.copy(
                color = TextMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
              )
            )
            Text(
              text = "Rate: ${band.bitrateGbps}G",
              style = MaterialTheme.typography.labelSmall.copy(
                color = TextMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
              )
            )
          }
        }
      }

      // Right: Shutter Actuate Button
      Button(
        onClick = onToggleShutter,
        colors = ButtonDefaults.buttonColors(
          containerColor = if (isBlocked) LaserCrimson else CardSurfaceVariant,
          contentColor = if (isBlocked) Color.White else NeonCyan
        ),
        shape = RoundedCornerShape(8.dp),
        border = ButtonDefaults.outlinedButtonBorder.copy(
          brush = Brush.horizontalGradient(
            if (isBlocked) listOf(LaserCrimson, LaserCrimson)
            else listOf(NeonCyan, NeonCyan.copy(0.4f))
          )
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        modifier = Modifier.testTag("toggle_shutter_btn_${band.name}")
      ) {
        Icon(
          imageVector = if (isBlocked) Icons.Default.Lock else Icons.Default.LockOpen,
          contentDescription = if (isBlocked) "Unblock" else "Block",
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = if (isBlocked) "BLOCKED" else "SHIELD",
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
          )
        )
      }
    }
  }
}

@Composable
fun RFBandCard(
  band: RFBandInfo,
  alertThreshold: Float,
  highThreshold: Float,
  modifier: Modifier = Modifier
) {
  val isHigh = band.powerDbm >= highThreshold
  val isAlert = band.powerDbm >= alertThreshold

  Card(
    shape = RoundedCornerShape(10.dp),
    colors = CardDefaults.cardColors(containerColor = CardSurface),
    border = CardDefaults.outlinedCardBorder().copy(
      brush = Brush.horizontalGradient(
        if (isHigh) listOf(LaserCrimson, LaserCrimsonSubdued)
        else if (isAlert) listOf(AmberAlert, AmberSubdued)
        else listOf(CardBorder, CardBorder)
      ),
      width = if (isHigh || isAlert) 1.5.dp else 1.dp
    ),
    modifier = modifier
      .fillMaxWidth()
      .testTag("rf_band_card_${band.name}")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Box(
          modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
              if (isHigh) LaserCrimson.copy(0.15f)
              else if (isAlert) AmberAlert.copy(0.15f)
              else NeonCyan.copy(0.15f)
            )
            .border(
              1.dp,
              if (isHigh) LaserCrimson else if (isAlert) AmberAlert else NeonCyan,
              RoundedCornerShape(8.dp)
            ),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.WifiTethering,
            contentDescription = null,
            tint = if (isHigh) LaserCrimson else if (isAlert) AmberAlert else NeonCyan,
            modifier = Modifier.size(20.dp)
          )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = band.name,
              style = MaterialTheme.typography.titleMedium.copy(
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
              )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
              color = CardSurfaceVariant,
              shape = RoundedCornerShape(4.dp)
            ) {
              Text(
                text = "${band.centerMhz.toInt()} MHz",
                style = MaterialTheme.typography.labelSmall.copy(
                  color = NeonCyan,
                  fontSize = 9.sp,
                  fontFamily = FontFamily.Monospace
                ),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(2.dp))

          Text(
            text = "${band.protocol} • ${band.modulation}",
            style = MaterialTheme.typography.bodyMedium.copy(
              color = TextSecondary,
              fontSize = 11.sp,
              fontFamily = FontFamily.Monospace
            )
          )

          if (band.description.isNotEmpty()) {
            Text(
              text = band.description,
              style = MaterialTheme.typography.bodySmall.copy(
                color = TextMuted,
                fontSize = 10.sp
              ),
              maxLines = 1
            )
          }
        }
      }

      // Power Metric
      Column(horizontalAlignment = Alignment.End) {
        Text(
          text = "${band.powerDbm} dBm",
          style = MaterialTheme.typography.titleMedium.copy(
            color = if (isHigh) LaserCrimson else if (isAlert) AmberAlert else TextPrimary,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            fontSize = 15.sp
          )
        )
        Surface(
          color = if (isHigh) LaserCrimson.copy(0.2f) else if (isAlert) AmberAlert.copy(0.2f) else EmeraldSubdued.copy(0.2f),
          shape = RoundedCornerShape(3.dp)
        ) {
          Text(
            text = if (isHigh) "CRITICAL" else if (isAlert) "ALERT" else "NOMINAL",
            style = MaterialTheme.typography.labelSmall.copy(
              color = if (isHigh) LaserCrimson else if (isAlert) AmberAlert else NeonEmerald,
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace
            ),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
          )
        }
      }
    }
  }
}
