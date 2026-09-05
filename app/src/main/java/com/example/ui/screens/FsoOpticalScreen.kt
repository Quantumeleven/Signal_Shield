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
import com.example.ui.theme.*

@Composable
fun FsoOpticalScreen(
  fsoFrame: FSOSpectrumFrame,
  shieldConfig: ShieldConfig,
  onToggleShutter: (String) -> Unit,
  onBlockAll: () -> Unit,
  onResetAll: () -> Unit,
  onToggleAutoShield: (Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  val blockedCount = fsoFrame.bands.count { it.blocked || it.shutterState == ShutterState.BLOCKED || it.shutterState == ShutterState.SHIELDED }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(ObsidianBlack)
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
    contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp)
  ) {
    // 1. Telemetry Overview Strip
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Attenuation
        Card(
          shape = RoundedCornerShape(10.dp),
          colors = CardDefaults.cardColors(containerColor = CardSurface),
          modifier = Modifier.weight(1f)
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text(
              text = "ATTENUATION",
              style = MaterialTheme.typography.labelSmall.copy(
                color = TextSecondary,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
              )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "${fsoFrame.overallAttenuationDb} dB",
              style = MaterialTheme.typography.titleMedium.copy(
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 15.sp
              )
            )
            Text(
              text = "Atmospheric Fog/Dust",
              style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 9.5.sp)
            )
          }
        }

        // Ambient Lux
        Card(
          shape = RoundedCornerShape(10.dp),
          colors = CardDefaults.cardColors(containerColor = CardSurface),
          modifier = Modifier.weight(1f)
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text(
              text = "AMBIENT LUX",
              style = MaterialTheme.typography.labelSmall.copy(
                color = TextSecondary,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
              )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "${fsoFrame.ambientLux.toInt()} lx",
              style = MaterialTheme.typography.titleMedium.copy(
                color = AmberAlert,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 15.sp
              )
            )
            Text(
              text = "Solar Noise Floor",
              style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 9.5.sp)
            )
          }
        }

        // Shutter State
        Card(
          shape = RoundedCornerShape(10.dp),
          colors = CardDefaults.cardColors(containerColor = CardSurface),
          modifier = Modifier.weight(1f)
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text(
              text = "SHUTTERS",
              style = MaterialTheme.typography.labelSmall.copy(
                color = TextSecondary,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
              )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "$blockedCount / ${fsoFrame.bands.size}",
              style = MaterialTheme.typography.titleMedium.copy(
                color = if (blockedCount > 0) LaserCrimson else NeonEmerald,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 15.sp
              )
            )
            Text(
              text = if (blockedCount > 0) "BEAMS BLOCKED" else "ALL CLEAR",
              style = MaterialTheme.typography.bodySmall.copy(
                color = if (blockedCount > 0) LaserCrimson else NeonEmerald,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
              )
            )
          }
        }
      }
    }

    // 2. Optical Wavelength Spectrum Canvas
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
              text = "FSO OPTICAL SPECTRUM PROFILE",
              style = MaterialTheme.typography.labelMedium.copy(
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
              )
            )
          }

          Text(
            text = "800 - 1650 nm",
            style = MaterialTheme.typography.labelSmall.copy(
              color = OpticPurple,
              fontSize = 10.sp,
              fontFamily = FontFamily.Monospace
            )
          )
        }

        Spacer(modifier = Modifier.height(6.dp))

        OpticalWavelengthSpectrum(
          psd = fsoFrame.psd,
          wavelengths = fsoFrame.wavelengthsNm,
          alertThresholdDbm = shieldConfig.fsoAlertDbm,
          blockThresholdDbm = shieldConfig.fsoBlockDbm,
          modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
        )
      }
    }

    // 3. Shutter Shielding Controls
    item {
      Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurfaceVariant),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "AUTO-SHIELD INTERCEPTOR",
              style = MaterialTheme.typography.labelSmall.copy(
                color = NeonCyan,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.5.sp
              )
            )

            Switch(
              checked = shieldConfig.autoBlockFso,
              onCheckedChange = onToggleAutoShield,
              colors = SwitchDefaults.colors(
                checkedThumbColor = NeonEmerald,
                checkedTrackColor = CardSurface,
                uncheckedTrackColor = CardSurface
              ),
              modifier = Modifier.testTag("auto_shield_switch")
            )
          }

          Text(
            text = "Automatically actuates optical physical shutters when beam power exceeds the high-energy threshold (${shieldConfig.fsoBlockDbm.toInt()} dBm).",
            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
          )

          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = onBlockAll,
              colors = ButtonDefaults.buttonColors(
                containerColor = LaserCrimson,
                contentColor = Color.White
              ),
              shape = RoundedCornerShape(6.dp),
              modifier = Modifier
                .weight(1f)
                .testTag("fso_block_all_btn"),
              contentPadding = PaddingValues(vertical = 8.dp)
            ) {
              Icon(imageVector = Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "BLOCK ALL BEAMS",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 10.5.sp,
                  fontFamily = FontFamily.Monospace
                )
              )
            }

            OutlinedButton(
              onClick = onResetAll,
              shape = RoundedCornerShape(6.dp),
              border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = Brush.horizontalGradient(listOf(NeonEmerald, NeonEmerald.copy(0.4f)))
              ),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonEmerald),
              modifier = Modifier
                .weight(1f)
                .testTag("fso_reset_all_btn"),
              contentPadding = PaddingValues(vertical = 8.dp)
            ) {
              Icon(imageVector = Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "RESET / UNBLOCK",
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

    // 4. Optical Laser Channels List
    item {
      Text(
        text = "OPTICAL BEAM CHANNELS (${fsoFrame.bands.size})",
        style = MaterialTheme.typography.labelMedium.copy(
          color = TextSecondary,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          fontSize = 12.sp
        )
      )
    }

    items(fsoFrame.bands.size) { index ->
      val band = fsoFrame.bands[index]
      FSOShutterCard(
        band = band,
        onToggleShutter = { onToggleShutter(band.name) }
      )
    }
  }
}
